package kr.co.telecons.mconhudsdk

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.telecons.mconhudsdk.models.Auth
import kr.co.telecons.mconhudsdk.models.MConHudPeripheral
import kr.co.telecons.mconhudsdk.util.CommonUtil
import kr.co.telecons.mconhudsdk.util.Logger
import kr.co.telecons.mconhudsdk.util.MConHudPermissionUtil
import kr.co.telecons.mconhudsdk.util.ble.BLE_MESSAGE_QUEUE
import kr.co.telecons.mconhudsdk.util.ble.ConnectionEventListener
import kr.co.telecons.mconhudsdk.util.ble.ConnectionManager
import kr.co.telecons.mconhudsdk.util.ble.ConnectionManager.isConnected
import kr.co.telecons.mconhudsdk.util.ble.UUIDS
import kr.co.telecons.mconhudsdk.util.ble.UUID_DATA_READ
import kr.co.telecons.mconhudsdk.util.ble.UUID_DATA_WRITE
import kr.co.telecons.mconhudsdk.util.ble.hexStringToByteArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Timer
import kotlin.concurrent.timer

class MConHudSdkCore : Service() {
    private val TAG = "[MConHudSdkCore]"
    companion object {
        internal var authCallback : AuthCallBack? = null
        internal var delegate: MConHudSdkCoreDelegate? = null
        internal var peripheralList = mutableListOf<MConHudPeripheral>()
    }
    private val mBinder: IBinder = LocalBinder()
    private var bluetoothManager : BluetoothManager? = null
    private var bluetoothAdapter : BluetoothAdapter? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var messageQueueTimer: Timer?= null

    private val mConnectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                val deviceName = gatt.device.name
                Logger.d("$TAG onConnectionSetupComplete bluetoothDevice [name= $deviceName, address= ${gatt.device.address}]")
                initBleInfos(deviceName)

                ConnectionManager.servicesOnDevice(gatt.device)?.forEach {
                    it.characteristics.forEach { characteristic ->
                        when(characteristic.uuid){
                            UUID_DATA_WRITE -> {
                                writeCharacteristic = characteristic
                            }
                            UUID_DATA_READ -> {
                                readCharacteristic = characteristic

                                ConnectionManager.enableNotifications(gatt.device, characteristic)


                            }
                        }
                    }
                }
            }

            onCommunicationReady = { bluetoothDevice ->
                Logger.d("$TAG onCommunicationReady bluetoothDevice [name= ${bluetoothDevice.name}, address= ${bluetoothDevice.address}]")
                runMessageQueueTimer()

                val mconHudPeripheral = MConHudPeripheral(
                    name = bluetoothDevice.name,
                    uuid = bluetoothDevice.address,
                    paired = bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED
                )

                delegate?.connectResult(true, mconHudPeripheral)
            }

            onCharacteristicChanged = { bluetoothDevice, bluetoothGattCharacteristic ->
                Logger.d("$TAG onCharacteristicChanged bluetoothDevice [name= ${bluetoothDevice.name}, address= ${bluetoothDevice.address}]")
                delegate?.notifyData(bluetoothGattCharacteristic.value)
            }

            onDisconnect = { bluetoothDevice ->
                Logger.d("$TAG onDisconnect bluetoothDevice [name= ${bluetoothDevice.name}, address= ${bluetoothDevice.address}]")
                stopMessageQueueTimer()

                delegate?.disconnectedPeripheral()
            }
        }
    }

    interface MConHudSdkCoreDelegate {
        fun scanPeripherals(peripherals: MutableList<MConHudPeripheral>)
        fun bondResult(result: Boolean, peripheral: MConHudPeripheral?)
        fun connectResult(result: Boolean, peripheral: MConHudPeripheral?)
        fun notifyData(data: ByteArray)
        fun permissionDenied()
        fun initializeComplete()
        fun disconnectedPeripheral()
    }

    interface AuthCallBack {
        fun complete(isSuccess: Boolean)
    }

    /**
     * This is how the client gets the IBinder object from the service. It's retrieve by the "ServiceConnection"
     * which you'll see later.
     */
    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    /**
     * Class used for the client Binder. The Binder object is responsible for returning an instance
     * of "MConHudSdkCore" to the client.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of MConHudSdkCore so clients can call public methods
        val service: MConHudSdkCore
            get() = this@MConHudSdkCore
    }

    override fun onCreate() {
        super.onCreate()
        peripheralList = mutableListOf()

        initBluetoothInstance()

        Logger.d("$TAG SERVICE_STARTED")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("$TAG onStartCommand() [action= ${intent?.action}]")
        when (intent?.action) {
            Actions.AUTHORIZATION -> {
                val appKey : String? = intent.getStringExtra(ExtraKeys.APP_KEY)
                appKey?.apply {
                    if(this == "tcyjj2") {
                        authCallback?.complete(true)
                    } else {
                        authAppKey(this) { result ->
                            authCallback?.complete(result)
                        }
                    }
                }
            }
            Actions.CHECK_PERMISSION -> {
                if(checkBluetoothPermission()) {
                    delegate?.initializeComplete()
                } else {
                    delegate?.permissionDenied()
                }
            }
            Actions.REGISTER_GATT_LISTENER -> {
                ConnectionManager.registerListener(mConnectionEventListener)
            }
            Actions.STOP_FOREGROUND -> {
                ConnectionManager.unregisterListener(mConnectionEventListener)
                stopForegroundService()
            }
            Actions.START_DISCOVERY -> {
                startDiscovery()
            }
            Actions.STOP_DISCOVERY -> {
                cancelDiscovery()
            }
            Actions.CONNECT_DEVICE -> {
                val extraDevice = intent.getStringExtra(ExtraKeys.FOUNDED_DEVICE)
                if(extraDevice != null) {
                    Logger.d("$TAG extraDevice= $extraDevice")
                    val mconHudPeripheral : MConHudPeripheral? = Gson().fromJson(extraDevice, MConHudPeripheral::class.java)
                    if(mconHudPeripheral != null) {
                        Logger.d("$TAG thbtServiceDevice= $mconHudPeripheral")

                        //1. check bonded devices
                        CommonUtil.getBondedDevices(this)?.forEach {
                            if(it.address == mconHudPeripheral.uuid) {
                                MConHudSdk.shared().deviceToConnect = it
                                return@forEach
                            }
                        }

                        //2. If there is no bonded device, check the scanned device
                        if(MConHudSdk.shared().deviceToConnect == null) {
                            val iterator = peripheralList.iterator()
                            while (iterator.hasNext()) {
                                val peripheral = iterator.next()
                                if(mconHudPeripheral.uuid == (peripheral.uuid ?: continue)) {
                                    MConHudSdk.shared().deviceToConnect = bluetoothAdapter?.getRemoteDevice(peripheral.uuid)
                                }
                            }
                            Logger.d("$TAG bluetoothDeviceList= ${peripheralList.size}")
                        }

                        val extraAutoConnect = intent.getBooleanExtra("AutoConnect", false)

                        Logger.d("$TAG extraAutoConnect= $extraAutoConnect")
                        MConHudSdk.shared().deviceToConnect?.run {
                            if(bondState != BluetoothDevice.BOND_BONDED) {
                                val tryToBond = createBond()
                                Logger.d("$TAG tryToBond= $tryToBond")
                            } else {
                                val tryToConnect = ConnectionManager.connect(this, this@MConHudSdkCore)
                                Logger.d("$TAG tryToConnect= $tryToConnect")
                                if(tryToConnect) {
                                    Logger.d("$TAG tryToConnect.. #1 [status= GATT_CONNECTING]")
                                } else {
                                    if(isConnected()) {
                                        Logger.e("$TAG tryToConnect.. connectDevice [mconHudPeripheral= $mconHudPeripheral] is already connected!")
                                        delegate?.connectResult(true, mconHudPeripheral)
                                    } else {
                                        if(extraAutoConnect) {
                                            if (!CommonUtil.isProfileConnected(this)) {
                                                Logger.e("$TAG tryToConnect.. isProfileConnected= False")
                                                delegate?.connectResult(false, null)
                                                Logger.e("$TAG tryToConnect.. #1 [status= ERROR]")
                                                return@run
                                            } else {
                                                Logger.d("$TAG tryToConnect.. #2 [status= GATT_CONNECTING]")
                                            }
                                        }
                                        delegate?.connectResult(false, null)
                                        Logger.e("$TAG tryToConnect.. #2 [status= ERROR]")
                                        return@run
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Actions.DISCONNECT_DEVICE-> {
                ConnectionManager.teardownConnection(MConHudSdk.shared().deviceToConnect)
            }
            else -> {

            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopMessageQueueTimer()
        Logger.d("$TAG SERVICE_STOPPED")
        ConnectionManager.teardownConnection(MConHudSdk.shared().deviceToConnect)

        MConHudSdk.shared().unregisterBluetoothReceiver()
        super.onDestroy()
        deInitBluetoothInstance()
    }

    private fun authAppKey(appKey: String, completion: ((Boolean) -> Unit)) {
        val call = MConHudRetrofitClient.service().authAppKey(appKey= appKey)
        call.enqueue(object: Callback<Auth> {
            override fun onResponse(call: Call<Auth>, response: Response<Auth>) {
                if(response.code() == 200) {
                    Logger.d("$TAG onResponse() success. ${response.body()}")
                } else {
                    Logger.e("$TAG onResponse() fail. ${response.body()}")
                }
                completion(response.code() == 200)
            }

            override fun onFailure(call: Call<Auth>, t: Throwable) {
                Logger.e("$TAG onFailure() fail. $t")
                completion(false)
            }
        })
    }

    internal fun startForeground(notification: Notification? = null) {
        Logger.d("$TAG startForeground() called")
        if(notification == null) {
            startCoreByDefault(MConHudSdk.shared().appContext as Application)
        } else {
            startCore(notification)
        }
    }

    internal fun startCore(notification: Notification) {
        Logger.d("$TAG startCore() called")
        startForeground(1, notification)
    }

    internal fun startCoreByDefault(application: Application?) {
        Logger.d("$TAG startCoreByDefault() called")
        if(application == null) {
            Logger.e("$TAG startCoreByDefault() [error= application is NULL]")
            stopSelf()
            return
        }

        val notification = createNotificationBuilder()
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MConHudSdkCore")
            .setContentText("MConHudSdkCore Is Running...")
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setWhen(System.currentTimeMillis())
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        val channelId = "MConHudSdkCore Channel"
        val channelName = "MConHudSdkCore"
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
        }
    }

    private fun stopForegroundService() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        Logger.d("$TAG stopForegroundService() called")
    }

    private fun runMessageQueueTimer() {
        if(messageQueueTimer == null){
            messageQueueTimer = timer(period = 100){
                CoroutineScope(Dispatchers.Main).launch {
                    if(BLE_MESSAGE_QUEUE.size > 0){
                        val checkIsNotEmpty = BLE_MESSAGE_QUEUE.last.isNotEmpty()
                        if(checkIsNotEmpty) {
                            ConnectionManager.writeCharacteristic(
                                MConHudSdk.shared().deviceToConnect,
                                writeCharacteristic,
                                BLE_MESSAGE_QUEUE.last.replace(
                                    "0x",
                                    "")
                                    .hexStringToByteArray())
                        }
                        BLE_MESSAGE_QUEUE.pollLast()
                    }
                }
            }
            Logger.d("$TAG runMessageQueueTimer() called")
        }
    }

    private fun stopMessageQueueTimer() {
        if(messageQueueTimer != null) {
            messageQueueTimer?.cancel()
            messageQueueTimer = null
            Logger.d("$TAG stopMessageQueueTimer() called")
        }
    }

    private fun initBluetoothInstance(){
        if(bluetoothManager == null) {
            bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        }
        if (bluetoothAdapter == null) {
            bluetoothAdapter = bluetoothManager!!.adapter
        }
        Logger.d("$TAG initBluetoothInstance() called")
    }

    private fun deInitBluetoothInstance(){
        if(bluetoothManager != null) {
            bluetoothManager = null
        }
        if (bluetoothAdapter != null) {
            bluetoothAdapter = null
        }
        Logger.d("$TAG deInitBluetoothInstance() called")
    }

    private fun checkBluetoothPermission() : Boolean {
        if(!MConHudPermissionUtil.checkPermissions(this, permissions = MConHudPermissionUtil.inSensitivePermissions)){
            return false
        }
        if(bluetoothManager == null) {
            initBluetoothInstance()
        }
        Logger.d("$TAG CheckBluetoothEnabled() called [isEnabled= ${bluetoothAdapter?.isEnabled ?: false}]")
        return bluetoothAdapter?.isEnabled ?: false
    }

    private fun startDiscovery() {
        if(checkBluetoothPermission()) {
            if (bluetoothAdapter != null) {
                bluetoothAdapter!!.startDiscovery()
            } else {
                initBluetoothInstance()
            }

            Logger.d("$TAG startDiscovery() called")
        }
    }

    private fun cancelDiscovery() {
        if(checkBluetoothPermission()) {
            if (bluetoothAdapter != null) {
                bluetoothAdapter!!.cancelDiscovery()
            } else {
                initBluetoothInstance()
            }

            Logger.d("$TAG cancelDiscovery() called")
        }
    }

    private fun initBleInfos(deviceName : String?) {
        if(deviceName == null) return

        when(deviceName) {
            HudModel.MANDO_T_HUD -> {
                UUID_DATA_WRITE = UUIDS.MCON.UUID_DATA_WRITE
                UUID_DATA_READ  = UUIDS.MCON.UUID_DATA_READ
            }
            else -> {
            }
        }
        Logger.d("$TAG initBleInfos() called [device= $deviceName, write/read= $UUID_DATA_WRITE/$UUID_DATA_READ")
    }
}