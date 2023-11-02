package kr.co.telecons.mconhudsdk.models

data class MConHudPeripheral (
    var name: String,
    var uuid: String,
    var paired: Boolean,
    var rssi : Float = 0f
)