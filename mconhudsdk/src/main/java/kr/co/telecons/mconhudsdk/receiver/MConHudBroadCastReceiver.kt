package kr.co.telecons.mconhudsdk.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.co.telecons.mconhudsdk.MConHudSdk
import kr.co.telecons.mconhudsdk.MConHudSdkCore
import kr.co.telecons.mconhudsdk.models.MConHudPeripheral
import kr.co.telecons.mconhudsdk.util.CommonUtil
import kr.co.telecons.mconhudsdk.util.CommonUtil.isTargetDevice
import kr.co.telecons.mconhudsdk.util.Logger

class MConHudBroadCastReceiver : BroadcastReceiver() {
    private val TAG = "[MConHudBroadCastReceiver]"
    private val devices: MutableList<MConHudPeripheral> = mutableListOf()
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        when(intent.action){
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Logger.d("$TAG ACTION_STATE_CHANGED [state= STATE_OFF]")
                        devices.clear()

                        MConHudSdkCore.delegate?.permissionDenied()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Logger.d("$TAG ACTION_STATE_CHANGED [state= STATE_TURNING_OFF]")
                        devices.clear()
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        Logger.d("$TAG ACTION_STATE_CHANGED [state= STATE_TURNING_ON]")
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Logger.d("$TAG ACTION_STATE_CHANGED [state= STATE_ON]")
//                        MConHudSdkCore.delegate?.initializeComplete()
                    }
                    else -> {
                        Logger.e("$TAG ACTION_STATE_CHANGED [state= ERROR]")
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if(device != null) {
                    if(context.isTargetDevice(device)) {
                        Logger.d("$TAG ACTION_ACL_CONNECTED")
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if(device != null) {
                    if(context.isTargetDevice(device)) {
                        Logger.d("$TAG ACTION_ACL_DISCONNECTED")
                        with(device) {
                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                                .toFloat()
                            val thbtServiceDevice = MConHudPeripheral(
                                name = name,
                                uuid = address,
                                paired = bondState == BluetoothDevice.BOND_BONDED,
                                rssi = rssi
                            )
                            MConHudSdkCore.delegate?.connectResult(false, thbtServiceDevice)
                        }
                    }
                }
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                with(intent){
                    val previousBondState = getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    val bondState = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    if(device != null) {
                        if(context.isTargetDevice(device)) {
                            when(bondState) {
                                BluetoothDevice.BOND_BONDED -> {
                                    if(previousBondState == BluetoothDevice.BOND_BONDING) {
                                        with(device) {
                                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                                                .toFloat()
                                            val thbtServiceDevice = MConHudPeripheral(
                                                name = name,
                                                uuid = address,
                                                paired = true,
                                                rssi = rssi
                                            )
                                            MConHudSdkCore.delegate?.bondResult(true, thbtServiceDevice)
                                        }
                                    }
                                }
                                BluetoothDevice.BOND_BONDING -> {
                                }
                                BluetoothDevice.BOND_NONE -> {
                                    if(previousBondState == BluetoothDevice.BOND_BONDED || previousBondState == BluetoothDevice.BOND_BONDING) {
                                        with(device) {
                                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                                                .toFloat()
                                            val thbtServiceDevice = MConHudPeripheral(
                                                name = name,
                                                uuid = address,
                                                paired = false,
                                                rssi = rssi
                                            )
                                            MConHudSdkCore.delegate?.bondResult(false, thbtServiceDevice)
                                        }
                                    }
                                }
                                else -> {
                                    if(previousBondState == BluetoothDevice.BOND_BONDED || previousBondState == BluetoothDevice.BOND_BONDING) {
                                        with(device) {
                                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                                                .toFloat()
                                            val thbtServiceDevice = MConHudPeripheral(
                                                name = name,
                                                uuid = address,
                                                paired = false,
                                                rssi = rssi
                                            )
                                            MConHudSdkCore.delegate?.bondResult(false, thbtServiceDevice)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val bondTransition = "${previousBondState.toBondStateDescription()} to " +
                            bondState.toBondStateDescription()
                    Logger.d("$TAG ACTION_BOND_STATE_CHANGED [$bondTransition]")
                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                if(devices.isNotEmpty()) {
                    devices.clear()
                }
                if(MConHudSdkCore.peripheralList.isNotEmpty()) {
                    MConHudSdkCore.peripheralList.clear()
                }
                Logger.d("$TAG ACTION_DISCOVERY_STARTED")
                val bondedDevices = CommonUtil.getBondedDevices(context)
                if(!bondedDevices.isNullOrEmpty()) {
                    bondedDevices.forEach { bluetoothDevice ->
                        with(bluetoothDevice) {
                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toFloat()
                            if(MConHudSdkCore.peripheralList.isNotEmpty()) {
                                val iterator = MConHudSdkCore.peripheralList.iterator()
                                while (iterator.hasNext()) {
                                    if(address == (iterator.next().uuid ?: continue)) {
                                        iterator.remove()
                                    }
                                }
                            }

                            MConHudSdkCore.peripheralList.add(MConHudPeripheral(
                                name= name,
                                uuid= address,
                                paired = bondState == BluetoothDevice.BOND_BONDED,
                                rssi = rssi
                            ))

                            val result = MConHudSdkCore.peripheralList.sortedBy { rssi }

                            MConHudSdkCore.delegate?.scanPeripherals(result.toMutableList())
                        }
                    }
                }
            }

            BluetoothDevice.ACTION_FOUND -> {
                if(device != null && device.name != null) {
                    Logger.d("$TAG ACTION_FOUND [name= ${device.name}]")
                    if(context.isTargetDevice(device)) {
                        with(device) {
                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toFloat()
                            val mconHudPeripheral = MConHudPeripheral(
                                name = name,
                                uuid = address,
                                paired = bondState == BluetoothDevice.BOND_BONDED,
                                rssi = rssi
                            )
                            Logger.d("$TAG ACTION_FOUND [address= ${mconHudPeripheral.uuid}, rssi= $rssi]")

                            if(devices.isNotEmpty()) {
                                val iterator = devices.iterator()
                                while (iterator.hasNext()) {
                                    if(device.address == (iterator.next().uuid ?: continue)) {
                                        iterator.remove()
                                    }
                                }
                            }

                            devices.add(mconHudPeripheral)

                            if(MConHudSdkCore.peripheralList.isNotEmpty()) {
                                val iterator = MConHudSdkCore.peripheralList.iterator()
                                while (iterator.hasNext()) {
                                    if(device.address == (iterator.next().uuid ?: continue)) {
                                        iterator.remove()
                                    }
                                }
                            }

                            MConHudSdkCore.peripheralList.add(MConHudPeripheral(
                                name= device.name,
                                uuid= device.address,
                                paired = bondState == BluetoothDevice.BOND_BONDED,
                                rssi = rssi
                            ))

                            val result = MConHudSdkCore.peripheralList.sortedBy { rssi }

                            MConHudSdkCore.delegate?.scanPeripherals(result.toMutableList())
                        }
                    }
                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Logger.d("$TAG ACTION_DISCOVERY_FINISHED [resultSize= ${MConHudSdkCore.peripheralList.size}]")
                MConHudSdk.shared().hudScanDelegate?.scanTimeOut()
            }
        }

    }

    private fun Int.toBondStateDescription() = when (this) {
        BluetoothDevice.BOND_BONDED -> "BONDED"
        BluetoothDevice.BOND_BONDING -> "BONDING"
        BluetoothDevice.BOND_NONE -> "NOT BONDED"
        else -> "ERROR: $this"
    }
}