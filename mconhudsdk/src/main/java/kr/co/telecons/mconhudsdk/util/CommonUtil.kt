package kr.co.telecons.mconhudsdk.util

import android.Manifest
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kr.co.telecons.mconhudsdk.HudModel
import java.lang.reflect.Method

object CommonUtil {
    internal fun targetDevices() = arrayOf(HudModel.MANDO_HUD_T, HudModel.MANDO_T_HUD, HudModel.GENIUS_HUD_Q, HudModel.TPLAY_HUD_Q, HudModel.KERNECT_APP, HudModel.TMAP_API_HUD, HudModel.TMAP_API_HUD2, HudModel.TMAP_API_DUAL_HUD)

    internal fun Context.isTargetDevice(device: BluetoothDevice?) : Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED
            ){
                return false
            }
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
            ){
                return false
            }
        }

        device?.let {
            if(it.name != null) {
                val deviceName = it.name
                targetDevices().forEach { prefix ->
                    if (deviceName.startsWith(prefix)) {
                        Logger.i("[Device Name : ${deviceName}]")
                        return true
                    }
                }
            }
            return false
        } ?: run {
            return false
        }
    }

    internal fun isRunningService(context: Context, serviceName: String): Boolean{
        try{
            val activityManager: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for(runningServiceInfo: ActivityManager.RunningServiceInfo in activityManager.getRunningServices(
                Integer.MAX_VALUE
            )){
                if(serviceName == runningServiceInfo.service.className){
                    return true
                }
            }
        }catch (e: java.lang.Exception){
            e.printStackTrace()
            return false
        }
        return false
    }

    internal fun getBondedDevices(context: Context): ArrayList<BluetoothDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }
        }
        val bluetoothManager = context.getSystemService(Service.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val deviceList = ArrayList<BluetoothDevice>()
        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                try {
                    if (context.isTargetDevice(device)) {
                        if(isProfileConnected(device)) {
                            deviceList.add(device)
                        }
                        Logger.d("[address= ${device.address}, isProfileConnected= ${isProfileConnected(device)}]")
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return deviceList
    }

    /**
     * Returns whether the device is in the "Profile Connected" state among the devices bonded in the Bluetooth settings.
     */
    internal fun isProfileConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    internal fun getActivity(context: Context?, id: Int, intent: Intent?, flag: Int): PendingIntent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_MUTABLE or flag)
        } else {
            PendingIntent.getActivity(context, id, intent, flag)
        }
    }
}