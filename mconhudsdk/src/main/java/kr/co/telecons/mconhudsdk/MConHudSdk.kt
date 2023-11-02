package kr.co.telecons.mconhudsdk

import android.app.Application
import android.app.Notification
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kr.co.telecons.mconhudsdk.delegates.MConHudDelegate
import kr.co.telecons.mconhudsdk.delegates.MConHudScanDelegate
import kr.co.telecons.mconhudsdk.enums.MConHudNotifyType
import kr.co.telecons.mconhudsdk.errors.MConHudSdkError
import kr.co.telecons.mconhudsdk.models.BrightnessLevel
import kr.co.telecons.mconhudsdk.models.BuzzerLevel
import kr.co.telecons.mconhudsdk.models.BuzzerStatus
import kr.co.telecons.mconhudsdk.models.CarSpeedCode
import kr.co.telecons.mconhudsdk.models.ClearCode
import kr.co.telecons.mconhudsdk.models.DeviceInfoType
import kr.co.telecons.mconhudsdk.models.MConHudPeripheral
import kr.co.telecons.mconhudsdk.models.SafetyCode
import kr.co.telecons.mconhudsdk.models.TurnByTurnCode
import kr.co.telecons.mconhudsdk.receiver.MConHudBroadCastReceiver
import kr.co.telecons.mconhudsdk.util.CommonUtil
import kr.co.telecons.mconhudsdk.util.Logger
import kr.co.telecons.mconhudsdk.util.ble.BLE_MESSAGE_QUEUE
import java.util.stream.Collectors

class MConHudSdk : MConHudSdkCore.MConHudSdkCoreDelegate {
    companion object {
        @Volatile
        private var instance : MConHudSdk? = null

        @JvmStatic
        fun shared() : MConHudSdk =
            instance ?: synchronized(this){
                instance ?: MConHudSdk().also {
                    instance = it
                }
            }
        
        private const val TAG = "[MConHudSdk]"
    }
    
    private var authStatus: Boolean = false

    var hudScanDelegate: MConHudScanDelegate? = null
    var hudDelegate: MConHudDelegate? = null

    private var initializeCompletion: ((MConHudSdkError?)-> Unit)? = null
    internal var appContext: Context? = null

    internal var deviceToConnect: BluetoothDevice? = null
    private var isReceiverRegistered: Boolean = false
    private var serviceConnection : ServiceConnection?= null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var core : MConHudSdkCore?= null
    private var isServiceBound : Boolean = false

    fun checkBluetoothPermission(completion: ((MConHudSdkError?)-> Unit)) {
        if(!authStatus) {
            completion(MConHudSdkError.INVALID_AUTHORIZATION)
            return
        }

        appContext?.apply {
            initializeCompletion = completion
            MConHudSdkCore.delegate = shared()
            Intent(this, MConHudSdkCore::class.java).also { intent ->
                intent.action = Actions.CHECK_PERMISSION
                if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    bindService(this, intent, null)
                }
                Logger.d("$TAG checkBluetoothPermission() called")
            }
        }
    }

    fun initialize(application: Application, appKey: String, notification: Notification?, completion: ((MConHudSdkError?)-> Unit)) {
        Logger.d("$TAG initialize() called")
        appContext = application
        MConHudSdkCore.authCallback = object : MConHudSdkCore.AuthCallBack {
            override fun complete(isSuccess: Boolean) {
                Logger.d("$TAG initialize() complete [isSuccess= $isSuccess]")
                if(isSuccess) {
                    authStatus = true
                    initializeCompletion = completion
                    MConHudSdkCore.delegate = shared()
                    appContext?.apply {
                        Intent(this, MConHudSdkCore::class.java).also { intent ->
                            intent.action = Actions.CHECK_PERMISSION
                            if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                                ContextCompat.startForegroundService(this, intent)
                            } else {
                                bindService(this, intent, notification)
                            }
                            Logger.d("$TAG checkBluetoothPermission() called")
                        }
                    }
                } else {
                    authStatus = false
                    completion(MConHudSdkError.INVALID_AUTHORIZATION)
                }
            }
        }

        appContext?.apply {
            Intent(this, MConHudSdkCore::class.java).also { intent ->
                intent.putExtra(ExtraKeys.APP_KEY, appKey)
                intent.action = Actions.AUTHORIZATION
                if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    bindService(this, intent, null)
                }
            }
        }
    }

    fun startScanPeripheral() {
        Logger.d("$TAG startScanPeripheral() called")
        if(!authStatus) {
            Logger.e("$TAG startScanPeripheral() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        if(isBluetoothAdapterEnabled()) {
            appContext?.apply {
                Intent(this, MConHudSdkCore::class.java).also { intent ->
                    intent.action = Actions.START_DISCOVERY
                    startService(intent)
                }
            }
        } else {
            Logger.e("$TAG startScanPeripheral() failed! BluetoothAdapter is disabled")
        }
    }

    fun stopScanPeripheral() {
        Logger.d("$TAG stopScanPeripheral() called")
        if(!authStatus) {
            Logger.e("$TAG stopScanPeripheral() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        appContext?.apply {
            Intent(appContext, MConHudSdkCore::class.java).also { intent ->
                intent.action = Actions.STOP_DISCOVERY
                startService(intent)
            }
        }
    }

    fun connectPeripheral(peripheral: MConHudPeripheral?) {
        Logger.d("$TAG connectPeripheral() called [peripheral= $peripheral]")
        if(!authStatus) {
            Logger.e("$TAG connectPeripheral() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        if(peripheral == null) {
            Logger.e("$TAG connectPeripheral() failed! bluetoothDevice is null")
            return
        }
        appContext?.apply {
            Intent(this, MConHudSdkCore::class.java).also { intent ->
                intent.action = Actions.CONNECT_DEVICE
                intent.putExtra(ExtraKeys.FOUNDED_DEVICE, Gson().toJson(peripheral))
                intent.putExtra("AutoConnect", true)
                if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    bindService(this, intent, null)
                }
            }
        }
    }

    fun disconnectPeripheral(peripheral: MConHudPeripheral) {
        Logger.d("$TAG disconnectPeripheral() called [peripheral= $peripheral]")
        if(!authStatus) {
            Logger.e("$TAG connectPeripheral() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        appContext?.apply {
            Intent(this, MConHudSdkCore::class.java).also { intent ->
                intent.action = Actions.DISCONNECT_DEVICE
                if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    bindService(this, intent, null)
                }
            }
        }
    }

    fun sendTurnByTurnInfo(tbtCode: TurnByTurnCode, distance: Int) {
        Logger.d("$TAG sendTurnByTurnInfo() [tbtCode= $tbtCode, distance= $distance]")
        if(!authStatus) {
            Logger.e("$TAG sendTurnByTurnInfo() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.tbtMessage(turnByTurnCode= tbtCode, distance= distance))
    }

    fun sendSafetyInfo(safetyCodes: ArrayList<SafetyCode>, limitSpeed: Int?, remainDistance: Int, isOverSpeed: Boolean) {
        val codes = safetyCodes.stream().map(SafetyCode::toString).collect(Collectors.joining(", "))
        Logger.d("$TAG sendSafetyInfo() [safetyCodes= $codes, limitSpeed= $limitSpeed, distance= $remainDistance, isOverSpeed= $isOverSpeed]")
        if(!authStatus) {
            Logger.e("$TAG sendSafetyInfo() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.safetyMessage(safetyCodes= safetyCodes, limitSpeed= limitSpeed, remainDistance= remainDistance, isOverSpeed= isOverSpeed))
    }

    fun sendCarSpeed(carSpeedCode: CarSpeedCode, speed: Int) {
        Logger.d("$TAG sendCarSpeed() [carSpeedCode= $carSpeedCode, speed= $speed]")
        if(!authStatus) {
            Logger.e("$TAG sendSafetyInfo() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        if(speed > -1) {
            BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.carSpeedMessage(carSpeedCode= carSpeedCode, speed= speed))
        }
    }

    fun sendClear(clearCodes: ArrayList<ClearCode>) {
        val codes = clearCodes.stream().map(ClearCode::toString).collect(Collectors.joining(", "))
        Logger.d("$TAG sendClear() [clearCodes= $codes]")
        if(!authStatus) {
            Logger.e("$TAG sendClear() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.clearMessage(clearCodes= clearCodes))
    }

    fun sendTime(yyyyMMddHHmmss: String) {
        Logger.d("$TAG sendTime() [yyyyMMddHHmmss= $yyyyMMddHHmmss]")
        if(!authStatus) {
            Logger.e("$TAG sendTime() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.timeMessage(yyyyMMddHHmmss= yyyyMMddHHmmss))
    }

    fun sendHudBrightnessLevel(brightnessLevel: BrightnessLevel) {
        Logger.d("$TAG sendHudBrightnessLevel() [brightnessLevel= $brightnessLevel]")
        if(!authStatus) {
            Logger.e("$TAG sendHudBrightnessLevel() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.brightnessMessage(brightnessLevel= brightnessLevel))
    }

    fun getHudBrightnessLevel() {
        Logger.d("$TAG getHudBrightnessLevel()")
        if(!authStatus) {
            Logger.e("$TAG sendHudBrightnessLevel() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.deviceInfoMessage(deviceInfoType= DeviceInfoType.BRIGHT))
    }

    fun sendHudBuzzerLevel(buzzerLevel: BuzzerLevel) {
        Logger.d("$TAG sendHudBuzzerLevel() [buzzerLevel= $buzzerLevel]")
        if(!authStatus) {
            Logger.e("$TAG sendHudBuzzerLevel() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.buzzerLevelMessage(buzzerLevel= buzzerLevel))
    }

    fun getHudBuzzerLevel() {
        Logger.d("$TAG getHudBuzzerLevel()")
        if(!authStatus) {
            Logger.e("$TAG getHudBuzzerLevel() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        BLE_MESSAGE_QUEUE.addFirst(MConHudSdkMessageManager.deviceInfoMessage(deviceInfoType= DeviceInfoType.BUZZER))
    }

    private fun startForegroundService() {
        Logger.d("$TAG startForegroundService() called")
        if(!authStatus) {
            Logger.e("$TAG startForegroundService() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        appContext?.apply {
            Intent(this, MConHudSdkCore::class.java).also { intent ->
                intent.action = Actions.REGISTER_GATT_LISTENER
                if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    bindService(this, intent, null)
                }
            }
        }
    }

    fun stopForegroundService() {
        Logger.d("$TAG stopForegroundService() called")
        if(!authStatus) {
            Logger.e("$TAG stopForegroundService() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        appContext?.apply {
            Intent(this, MConHudSdkCore::class.java).also { intent ->
                intent.action = Actions.STOP_FOREGROUND
                unregisterBluetoothReceiver()
                if (CommonUtil.isRunningService(this, MConHudSdkCore::class.java.name)) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    unbindService()
                }
                Logger.d("$TAG stopForegroundService() called")
            }
        }
    }

    private fun registerBluetoothReceiver() {
        Logger.d("$TAG registerBluetoothReceiver() called")
        if(!authStatus) {
            Logger.e("$TAG registerBluetoothReceiver() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        appContext?.apply {
            if(!isReceiverRegistered) {
                isReceiverRegistered = true
                bluetoothReceiver = MConHudBroadCastReceiver()
                this.registerReceiver(
                    bluetoothReceiver,
                    setIntentFilter()
                )
            }
        }
    }

    internal fun unregisterBluetoothReceiver() {
        Logger.d("$TAG unregisterBluetoothReceiver() called")
        if(!authStatus) {
            Logger.e("$TAG unregisterBluetoothReceiver() return [cause= INVALID_AUTHORIZATION]")
            return
        }
        appContext?.apply {
            if (isReceiverRegistered) {
                isReceiverRegistered = false
                this.unregisterReceiver(bluetoothReceiver)
            }
        }
    }

    private fun setIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED) //연결 확인
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) //연결 끊김 확인

        return intentFilter
    }

    private fun getBluetoothAdapter() : BluetoothAdapter? {
        val bluetoothManager = appContext?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        return bluetoothManager?.adapter
    }

    private fun isBluetoothAdapterEnabled() : Boolean {
        appContext?.let {
            val isEnabled = getBluetoothAdapter()?.isEnabled
            Logger.d("$TAG isBluetoothAdapterEnabled() [isEnabled= $isEnabled]")
            return isEnabled ?: false
        }
        return false
    }

    private fun bindService(context: Context, intent: Intent, notification: Notification?) {
        Logger.d("$TAG bindService() called")
        if(serviceConnection != null) {
            if(!isServiceBound) {
                context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
            }
        } else {
            serviceConnection = (object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                    Logger.d("$TAG bindService() onServiceDisConnected [service= MConHudSdkCore]")
                }

                override fun onServiceConnected(
                    name: ComponentName?,
                    service: IBinder?
                ) {
                    Logger.d("$TAG bindService() onServiceConnected [service= MConHudSdkCore]")
                    val binder = service as MConHudSdkCore.LocalBinder
                    core = binder.service

                    if(intent.action == Actions.AUTHORIZATION) {
                        ContextCompat.startForegroundService(context, intent)
                        core?.apply {
                            startForeground(notification)
                        }
                    } else {
                        context.startService(intent)
                    }

                    if(isServiceBound) {
                        context.unbindService(this)
                        isServiceBound = false
                    }
                }
            })

            isServiceBound = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        if(serviceConnection != null && core != null && isServiceBound) {
            core?.unbindService(serviceConnection!!)

            serviceConnection = null
            core = null
            Logger.d("$TAG unbindService() called")
        }
    }

    override fun scanPeripherals(peripherals: MutableList<MConHudPeripheral>) {
        Logger.d("$TAG scanPeripherals() called")
        hudScanDelegate?.scanPeripheral(peripherals= peripherals)
    }

    override fun bondResult(result: Boolean, peripheral: MConHudPeripheral?) {
        Logger.d("$TAG bondResult() called [result= $result, peripheral= $peripheral]")
        if(result) {
            connectPeripheral(peripheral)
        } else {
            hudScanDelegate?.error(error = MConHudSdkError.PERIPHERAL_CONNECT_FAIL)
        }
    }

    override fun connectResult(result: Boolean, peripheral: MConHudPeripheral?) {
        Logger.d("$TAG connectResult() called [result= $result]")
        if(result) {
            if(peripheral != null) {
                hudScanDelegate?.connectPeripheralResult(peripheral= peripheral)
            } else {
                Logger.e("$TAG connectResult() fail #1")
                hudScanDelegate?.error(error = MConHudSdkError.PERIPHERAL_CONNECT_FAIL)
            }
        } else {
            Logger.e("$TAG connectResult() fail #2")
            hudScanDelegate?.error(error = MConHudSdkError.PERIPHERAL_CONNECT_FAIL)
        }
    }

    override fun notifyData(data: ByteArray) {
        Logger.d("$TAG notifyData() called")
        MConHudSdkMessageManager.parseNotifyValue(data) { notifyType, dataArray ->
            Logger.d("$TAG notifyData() [notifyType= $notifyType]")
            if(notifyType != null) {
                when (notifyType) {
                    MConHudNotifyType.NOTIFY_TYPE_ILLUMINANCE -> {
                        if(dataArray != null && dataArray.size == 2) {
                            val dayLevel = dataArray[0].toInt()
                            val nightLevel = dataArray[1].toInt() // Not Used

                            when(dayLevel) {
                                0 -> {
                                    hudDelegate?.receiveHudBrightnessLevel(brightnessLevel = BrightnessLevel.LOW)
                                }
                                1 -> {
                                    hudDelegate?.receiveHudBrightnessLevel(brightnessLevel = BrightnessLevel.MEDIUM)
                                }
                                2 -> {
                                    hudDelegate?.receiveHudBrightnessLevel(brightnessLevel = BrightnessLevel.HIGH)
                                }
                                else -> { Logger.e("$TAG notifyData() ILLUMINANCE is not defined") }
                            }
                        }
                    }

                    MConHudNotifyType.NOTIFY_TYPE_BUZZER_SOUND -> {
                        if(dataArray != null && dataArray.size == 2) {
                            val soundEnable = dataArray[0].toInt()
                            val soundLevel = dataArray[1].toInt()

                            when(soundEnable) {
                                0 -> {
                                    hudDelegate?.receiveHudBuzzerStatus(buzzerStatus= BuzzerStatus.OFF)
                                }
                                1 -> {
                                    hudDelegate?.receiveHudBuzzerStatus(buzzerStatus= BuzzerStatus.ON)
                                }
                                else -> { Logger.e("$TAG notifyData() BUZZER_SOUND is not defined") }
                            }
                        }
                    }
                    MConHudNotifyType.NOTIFY_TYPE_TIME_UPDATE -> {
                        hudDelegate?.receiveTimeUpdate()
                    }
                    else -> {

                    }
                    /*MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_INFO -> {
                        if(dataArray != null && dataArray.size == 2) {
                            val modelName = dataArray[0]
                            val firmwareVersion = dataArray[1]
                            hudDelegate?.receiveFirmwareInfo(FirmwareInfo(modelName, firmwareVersion, ""))
                        }
                    }
                    MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_UPDATE -> {
                        if(dataArray != null && dataArray.size == 2) {
                            val updateEnable = dataArray[0] == "1"
                            val blockSize = dataArray[1].toInt()

                        }
                    }
                    MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_UPDATE_BLOCK -> {
                        if (dataArray != null && dataArray.size == 2) {
                            val seqNumber = dataArray[0].toInt()
                            val errorCode = dataArray[1]
                        }
                    }
                    MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_UPDATE_COMPLETE -> {
                        if (dataArray != null && dataArray.size == 1) {
                            val errorCode = dataArray[0]
                        }
                    }*/
                }
            }
        }
    }

    override fun permissionDenied() {
        Logger.d("$TAG permissionDenied() called")
        if(initializeCompletion != null) {
            initializeCompletion?.invoke(MConHudSdkError.BLUETOOTH_PERMISSION_DENIED)
            initializeCompletion = null
        }
    }

    override fun initializeComplete() {
        Logger.d("$TAG initializeComplete() called")
        if(initializeCompletion != null) {
            initializeCompletion?.invoke(null)
            initializeCompletion = null
        }
        startForegroundService()
        registerBluetoothReceiver()
    }

    override fun disconnectedPeripheral() {
        Logger.d("$TAG disconnectedPeripheral() called")
        hudScanDelegate?.disconnectedPeripheral()
    }
}