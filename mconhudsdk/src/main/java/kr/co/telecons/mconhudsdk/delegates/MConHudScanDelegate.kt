package kr.co.telecons.mconhudsdk.delegates

import kr.co.telecons.mconhudsdk.models.MConHudPeripheral
import java.util.ArrayList

interface MConHudScanDelegate {
    fun scanPeripheral(peripherals: ArrayList<MConHudPeripheral>)
    fun scanTimeOut()
    fun connectPeripheralResult(peripheral: MConHudPeripheral)
}