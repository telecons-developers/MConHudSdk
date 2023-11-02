package kr.co.telecons.mconhudsdk.delegates

import kr.co.telecons.mconhudsdk.errors.MConHudSdkError
import kr.co.telecons.mconhudsdk.models.FirmwareInfo

interface MConHudFirmwareDelegate {
    fun receiveFirmwareInfo(firmwareInfo: FirmwareInfo)
    fun firmwareUpdate(progress: Int)
    fun firmwareUpdateComplete(firmwareInfo: FirmwareInfo)
    fun firmwareUpdateError(firmwareUpdateError: MConHudSdkError)
}