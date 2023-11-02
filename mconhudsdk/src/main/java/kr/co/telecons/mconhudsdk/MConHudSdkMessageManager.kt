package kr.co.telecons.mconhudsdk

import android.util.Log
import kr.co.telecons.mconhudsdk.enums.MConHudNotifyType
import kr.co.telecons.mconhudsdk.models.BrightnessLevel
import kr.co.telecons.mconhudsdk.models.BuzzerLevel
import kr.co.telecons.mconhudsdk.models.CarSpeedCode
import kr.co.telecons.mconhudsdk.models.ClearCode
import kr.co.telecons.mconhudsdk.models.DeviceInfoType
import kr.co.telecons.mconhudsdk.models.SafetyCode
import kr.co.telecons.mconhudsdk.models.TurnByTurnCode
import kr.co.telecons.mconhudsdk.util.ble.binary2Hex
import kr.co.telecons.mconhudsdk.util.ble.hexStringToByteArray
import kr.co.telecons.mconhudsdk.util.ble.toFirmwareHexString
import kr.co.telecons.mconhudsdk.util.ble.toHexString
import java.util.Date
import java.util.Locale
import kotlin.experimental.and

object MConHudSdkMessageManager {
    private val header = "19"
    private val writeId = "4D"
    private val fwId = "4F"
    private val tail = "2F"

    fun tbtMessage(turnByTurnCode: TurnByTurnCode, distance: Int) : String {
        val tbt ="$header$writeId"+"0105"+"${toTbtData(turnByTurnCode= turnByTurnCode)}${distance.formatDataLength(6)}$tail"
        return tbt
    }
    fun safetyMessage(safetyCodes: ArrayList<SafetyCode>, limitSpeed: Int?, remainDistance: Int, isOverSpeed: Boolean) : String {
        var cameraType = 0
        safetyCodes.forEach { item ->
            cameraType += item.value
        }
        var onViolation = 0
        if(isOverSpeed) {
            onViolation = 1
        }
        var speed = 0

        limitSpeed?.let {
            speed = it
        }

        return "$header$writeId"+"0206"+"${cameraType.formatDataLength(2)}${speed.formatDataLength(2)}${onViolation.formatDataLength(2)}${remainDistance.formatDataLength(6)}$tail"
    }
    fun carSpeedMessage(carSpeedCode: CarSpeedCode, speed: Int) : String {
        var type = 0
        if(carSpeedCode == CarSpeedCode.SECTION_AVERAGE_SPEED) {
            type = 1
        }

        val speedType = type.formatDataLength(2)
        val carSpeed = speed.formatDataLength(2)
        return "$header$writeId"+"0302"+"$speedType$carSpeed$tail"
    }
    fun clearMessage(clearCodes: ArrayList<ClearCode>) : String {
        var clearType = 0
        clearCodes.forEach { item ->
            clearType += item.value
        }
        val turnOffCode = clearType.formatDataLength(2)
        return "$header$writeId"+"0501"+"$turnOffCode$tail"
    }

    fun timeMessage(yyyyMMddHHmmss: String) : String {
        val date = binary2Hex(yyyyMMddHHmmss)
        return "$header$writeId"+"090E"+"$date$tail"
    }

    fun deviceInfoMessage(deviceInfoType: DeviceInfoType) : String {
        val settingType = deviceInfoType.value.formatDataLength(2)
        return "$header$writeId"+"0601"+"$settingType$tail"
    }

    fun brightnessMessage(brightnessLevel: BrightnessLevel) : String {
        val dayMessage = brightnessLevel.value.formatDataLength(2)
        return "$header$writeId"+"0702"+"$dayMessage$dayMessage$tail"
    }

    fun buzzerLevelMessage(buzzerLevel: BuzzerLevel) : String {
        var soundPower = 0

        if(buzzerLevel != BuzzerLevel.MUTE) {
            soundPower = 1
        }
        val soundOnOffMessage = soundPower.formatDataLength(2)
        val soundLevelMessage = buzzerLevel.value.formatDataLength(2)

        return "$header$writeId"+"0802"+"$soundOnOffMessage$soundLevelMessage$tail"
    }

    private fun readIlluminanceData(inputData: ByteArray): Array<String> {
        val output = arrayOf("","")
        val day = (inputData[4] and (0xFF).toByte()).toString()
        val night = (inputData[5] and (0xFF).toByte()).toString()

        output[0] = day
        output[1] = night

        return output
    }

    private fun readFirmWareData(inputData: ByteArray): Array<String> {
        val output = arrayOf("","")
        val hexStringData = inputData.toFirmwareHexString()
        val padding = (4 * 2)
        val model = String(hexStringData.substring(8, 9 + padding).hexStringToByteArray(),Charsets.UTF_8)
        val version = String(hexStringData.substring(8 + padding, (hexStringData.length - 2)).hexStringToByteArray(),Charsets.UTF_8)

        output[0] = model
        output[1] = version

        return output
    }

    private fun readHudSoundData(inputData: ByteArray): Array<String> {
        val output = arrayOf("","")
        val soundEnable = (inputData[4] and (0xFF).toByte()).toString()
        //val soundLevel = (inputData[5] and (0xFF).toByte()).toString()

        output[0] = soundEnable
        output[1] = ""

        return output
    }

    private fun readFirmwareUpdateInfoData(inputData: ByteArray) : Array<String> {
        val output = arrayOf("","")
        val updateEnable = (inputData[4] and (0xFF).toByte()).toString()
        var lowBlockSize = ""

        for(i in 5..6) {
            lowBlockSize += String.format("%02X", inputData[i])
        }

        lowBlockSize = Integer.parseInt(lowBlockSize,16).toString()

        output[0] = updateEnable
        output[1] = lowBlockSize

        return output
    }

    private fun readUpdateFirmwareBlockFileData(inputData: ByteArray): Array<String> {
        val output    = arrayOf("","")
        var seqNumber = ""
        var errCode   = ""

        for(i in 4..5) {
            seqNumber += String.format("%02X", inputData[i])
        }

        seqNumber = Integer.parseInt(seqNumber,16).toString()
        errCode   = (inputData[6] and (0xFF).toByte()).toString() // 0: No Error , 1: Retry , 2: Cancel

        output[0] = seqNumber
        output[1] = errCode

        return output
    }

    private fun readFirmwareUpdateComplete(inputData: ByteArray): Array<String> {
        val output    = arrayOf("")
        val errCode = (inputData[4] and (0xFF).toByte()).toString()

        output[0] = errCode // 0 : No Error(Complete), 1 : Error(Fail)

        return output
    }

    private fun toTbtData(turnByTurnCode: TurnByTurnCode) : String {
        return when(turnByTurnCode) {
            TurnByTurnCode.STRAIGHT             -> "0000"
            TurnByTurnCode.LEFT_TURN            -> "0002"
            TurnByTurnCode.RIGHT_TURN           -> "0005"
            TurnByTurnCode.U_TURN               -> "0007"
            TurnByTurnCode.SHARP_LEFT_TURN      -> "0003"
            TurnByTurnCode.SHARP_RIGHT_TURN     -> "0004"
            TurnByTurnCode.CURVE_LEFT_TURN      -> "0001"
            TurnByTurnCode.CURVE_RIGHT_TURN     -> "0006"
            TurnByTurnCode.LEFT_OUT_HIGHWAY     -> "000A"
            TurnByTurnCode.RIGHT_OUT_HIGHWAY    -> "000B"
            TurnByTurnCode.LEFT_IN_HIGHWAY      -> "0008"
            TurnByTurnCode.RIGHT_IN_HIGHWAY     -> "0009"
            TurnByTurnCode.TUNNEL               -> "000E"
            TurnByTurnCode.OVERPATH             -> "000F"
            TurnByTurnCode.UNDERPATH            -> "0011"
            TurnByTurnCode.OVERPATH_SIDE        -> "0010"
            TurnByTurnCode.UNDERPATH_SIDE       -> "0012"
            TurnByTurnCode.TOLLGATE             -> "000C"
            TurnByTurnCode.REST_AREA            -> "000D"
            TurnByTurnCode.ROTARY_DIRECTION_1   -> "0104"
            TurnByTurnCode.ROTARY_DIRECTION_2   -> "0104"
            TurnByTurnCode.ROTARY_DIRECTION_3   -> "0105"
            TurnByTurnCode.ROTARY_DIRECTION_4   -> "0106"
            TurnByTurnCode.ROTARY_DIRECTION_5   -> "0106"
            TurnByTurnCode.ROTARY_DIRECTION_6   -> "0107"
            TurnByTurnCode.ROTARY_DIRECTION_7   -> "0101"
            TurnByTurnCode.ROTARY_DIRECTION_8   -> "0101"
            TurnByTurnCode.ROTARY_DIRECTION_9   -> "0102"
            TurnByTurnCode.ROTARY_DIRECTION_10  -> "0103"
            TurnByTurnCode.ROTARY_DIRECTION_11  -> "0103"
            TurnByTurnCode.ROTARY_DIRECTION_12  -> "0100"
        }
    }

    private fun Int.formatDataLength(dataLen: Int): String {
        val distanceValue: Int = kotlin.math.floor(this.toDouble()).toInt()
        val length = dataLen - Integer.toHexString(distanceValue).length
        var hexValue = Integer.toHexString(distanceValue)

        for (i in 1..length) {
            hexValue = "0$hexValue"
        }

        return hexValue
    }
    
    internal fun parseNotifyValue(value : ByteArray, completion: (MConHudNotifyType?, Array<String>?) -> Unit) {
        try {
            var type: MConHudNotifyType? = null
            var data: Array<String>? = null

            // Find By Command
            when (value[2].toHexString()) {
                READ.ILLUMINANCE -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_ILLUMINANCE
                    data = readIlluminanceData(value)
                }
                READ.FIRMWARE -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_INFO
                    data = readFirmWareData(value)
                }
                READ.TIMEUPDATE -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_TIME_UPDATE
                }
                READ.SOUND -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_BUZZER_SOUND
                    data = readHudSoundData(value)
                }
                FIRMWARE_UPDATE.READ.REQ_COMMAND -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_UPDATE
                    data = readFirmwareUpdateInfoData(value)
                }
                FIRMWARE_UPDATE.READ.RECV_COMMAN -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_UPDATE_BLOCK
                    data = readUpdateFirmwareBlockFileData(value)
                }
                FIRMWARE_UPDATE.READ.COMPLETE_COMMAN -> {
                    type = MConHudNotifyType.NOTIFY_TYPE_FIRMWARE_UPDATE_COMPLETE
                    data = readFirmwareUpdateComplete(value)
                }
            }
            completion(type, data)
        } catch (e : Exception) {
            Log.d("parseNotifyValue", "Read Error \n ${e.printStackTrace()}")
        }
    }

    private object READ {
        const val ILLUMINANCE = "0b"
        const val FIRMWARE    = "0c"
        const val TIMEUPDATE  = "0d"
        const val SOUND       = "0a"
    }

    private object FIRMWARE_UPDATE {
        const val INFO_COMMAND = "01"
        const val INFO_DATALEN = "000D"
        const val BLOCK_COMMAN = "03"
        val BLOCK_DATALEN = fun(len : Int) : String {
            val totalLen = 2 + (len)
            val length   = 4 - Integer.toHexString(totalLen).length
            var hexValue = Integer.toHexString(totalLen)

            for (i in 1..length) {
                hexValue = "0$hexValue"
            }

            return hexValue
        }

        object READ {
            const val REQ_COMMAND  = "02"
            const val RECV_COMMAN  = "04"
            const val COMPLETE_COMMAN = "05"
        }
    }
}