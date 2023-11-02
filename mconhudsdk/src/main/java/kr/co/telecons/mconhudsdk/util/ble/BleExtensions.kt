package kr.co.telecons.mconhudsdk.util.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import java.util.*
import kotlin.experimental.and

internal object UUIDS {
    object MCON {
        val UUID_SERVICE = UUID.fromString("0000ffea-0000-1000-8000-00805f9b34fb")
        val UUID_DATA_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        val UUID_DATA_READ = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    }

    object THUD {
        val UUID_SERVICE = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb")
        val UUID_DATA_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        val UUID_DATA_READ = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    }

    object PONTUS {
        val UUID_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-f7d72c3c02a3")
        val UUID_DATA_WRITE = UUID.fromString("0000fff2-0000-1000-8000-f7d72c3c02a3")
        val UUID_DATA_READ = UUID.fromString("0000fff1-0000-1000-8000-f7d72c3c02a3")
    }
}
/** UUID of the Client Characteristic Configuration Descriptor (0x2902). */
internal var UUID_DATA_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
internal var UUID_DATA_READ = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
internal val UUID_DATA_CCC = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
internal val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"

internal var BLE_MESSAGE_QUEUE = LinkedList<String>()

// BluetoothGatt

fun BluetoothGatt.printGattTable() {
    if (services.isEmpty()) {
        Log.i("","No service and characteristic available, call discoverServices() first?")
        return
    }
    services.forEach { service ->
        val characteristicsTable = service.characteristics.joinToString(
            separator = "\n|--",
            prefix = "|--"
        ) { char ->
            var description = "${char.uuid}: ${char.printProperties()}"
            if (char.descriptors.isNotEmpty()) {
                description += "\n" + char.descriptors.joinToString(
                    separator = "\n|------",
                    prefix = "|------"
                ) { descriptor ->
                    "${descriptor.uuid}: ${descriptor.printProperties()}"
                }
            }
            description
        }
        Log.i("","Service ${service.uuid}\nCharacteristics:\n$characteristicsTable")
    }
}

fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
    services?.forEach { service ->
        service.characteristics?.firstOrNull { characteristic ->
            characteristic.uuid == uuid
        }?.let { matchingCharacteristic ->
            return matchingCharacteristic
        }
    }
    return null
}

fun BluetoothGatt.findDescriptor(uuid: UUID): BluetoothGattDescriptor? {
    services?.forEach { service ->
        service.characteristics.forEach { characteristic ->
            characteristic.descriptors?.firstOrNull { descriptor ->
                descriptor.uuid == uuid
            }?.let { matchingDescriptor ->
                return matchingDescriptor
            }
        }
    }
    return null
}

// BluetoothGattCharacteristic

fun BluetoothGattCharacteristic.printProperties(): String = mutableListOf<String>().apply {
    if (isReadable()) add("READABLE")
    if (isWritable()) add("WRITABLE")
    if (isWritableWithoutResponse()) add("WRITABLE WITHOUT RESPONSE")
    if (isIndicatable()) add("INDICATABLE")
    if (isNotifiable()) add("NOTIFIABLE")
    if (isEmpty()) add("EMPTY")
}.joinToString()

fun BluetoothGattCharacteristic.isReadable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    properties and property != 0

// BluetoothGattDescriptor

fun BluetoothGattDescriptor.printProperties(): String = mutableListOf<String>().apply {
    if (isReadable()) add("READABLE")
    if (isWritable()) add("WRITABLE")
    if (isEmpty()) add("EMPTY")
}.joinToString()

fun BluetoothGattDescriptor.isReadable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_READ)

fun BluetoothGattDescriptor.isWritable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE)

fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
    permissions and permission != 0

/**
 * Convenience extension function that returns true if this [BluetoothGattDescriptor]
 * is a Client Characteristic Configuration Descriptor.
 */
fun BluetoothGattDescriptor.isCccd() =
    uuid.toString().uppercase(Locale.US) == CCC_DESCRIPTOR_UUID.uppercase(Locale.US)

// ByteArray

internal fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

internal fun String.hexStringToByteArray() = ByteArray(this.length / 2) {
    this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
}

internal fun binary2Hex(string: String): String {
    val buffer: ByteArray = string.toByteArray(Charsets.UTF_8)
    var res = ""
    var token = ""
    for (i in buffer.indices) {
        token = Integer.toHexString(buffer[i].toInt())
        if (token.length > 2) {
            token = token.substring(token.length - 2)
        } else {
            for (j in 0 until 2 - token.length) {
                token = "0$token"
            }
        }
        res += " $token"
    }
    return res.replace(" ","")
}

internal fun Byte.toHexString() : String {
    val builder = StringBuilder()
    builder.append(String.format("%02x", this and (0xFF).toByte()))
    return builder.toString()
}

internal fun ByteArray.toFirmwareHexString(): String = joinToString(separator = "", prefix = "") { String.format("%02X", it) }