package kr.co.telecons.mconhudsdk

object HudModel {
    const val KERNECT_APP       = "Kernect_App"
    const val MANDO_T_HUD       = "Mando T-HUD"
    const val MANDO_HUD_T       = "Mando HUD T"
    const val TPLAY_HUD_Q       = "TPLAY HUD Q"
    const val GENIUS_HUD_Q      = "Genius HUD Q"
    const val VOICE_HUD         = "VOICE_HUD"
    const val HUD_MAX_Q         = "HUD_MAX_Q"
    const val TMAP_API_HUD      = "Tmap api HUD"
    const val TMAP_API_HUD2     = "Tmap api HUD2"
    const val TMAP_API_DUAL_HUD = "Tmap API DUAL-HUD"
}

object Actions {
    const val AUTHORIZATION          = "AUTHORIZATION"
    const val CHECK_PERMISSION       = "CHECK_PERMISSION"
    const val REGISTER_GATT_LISTENER = "REGISTER_GATT_LISTENER"
    const val STOP_FOREGROUND        = "STOP_FOREGROUND"
    const val START_DISCOVERY        = "START_DISCOVERY"
    const val STOP_DISCOVERY         = "STOP_DISCOVERY"
    const val CONNECT_DEVICE         = "CONNECT_DEVICE"
    const val DISCONNECT_DEVICE      = "DISCONNECT_DEVICE"
    const val START_NOTIFICATION     = "START_NOTIFICATION"
    const val STOP_NOTIFICATION      = "STOP_NOTIFICATION"
}

object ExtraKeys {
    const val APP_KEY               = "APP_KEY"
    const val FOUNDED_DEVICE        = "FOUNDED_DEVICE"
}

const val PERMISSION_REQUEST_CODE       = 1000
const val PERMISSION_GPS_REQUEST_CODE   = 1001
const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

internal const val BASE_URL = "https://asd-test001.azurewebsites.net/"