package kr.co.telecons.mconhudsdk.delegates

import kr.co.telecons.mconhudsdk.errors.MConHudSdkError
import kr.co.telecons.mconhudsdk.models.MConHudPeripheral
import java.util.ArrayList

interface MConHudScanDelegate {
    fun scanPeripheral(peripherals: MutableList<MConHudPeripheral>)
    fun scanTimeOut()
    fun connectPeripheralResult(peripheral: MConHudPeripheral)
    fun disconnectedPeripheral()
    fun error(error: MConHudSdkError)
}