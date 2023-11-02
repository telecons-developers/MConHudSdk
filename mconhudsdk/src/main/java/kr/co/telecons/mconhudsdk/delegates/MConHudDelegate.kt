package kr.co.telecons.mconhudsdk.delegates

import kr.co.telecons.mconhudsdk.models.BrightnessLevel
import kr.co.telecons.mconhudsdk.models.BuzzerStatus

interface MConHudDelegate {
    fun receiveHudBrightnessLevel(brightnessLevel: BrightnessLevel)
    fun receiveHudBuzzerStatus(buzzerStatus: BuzzerStatus)
    fun receiveTimeUpdate()
}