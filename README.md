# MConHudSdk
## Installation
MConHudSdk is available through <https://jitpack.io/>. To install
it, simply add the follow these steps:

Step 1. Add the JitPack repository to your build file.
Add it in your root build.gradle at the end of repositories:

```
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

Step 2. Add the dependency(app:build.gradle)

```
implementation "com.github.telecons-developers:MConHudSdk:$latestVersion"
```

## Auth
You can use the SDK after initializing it with the following code.
```kotlin
// notification is for executing [MConHudCore]. If null is passed, the default Notification will be posted.
MConHudSdk.shared().initialize(application= application, appkey= "appkey", notification= notification) { error ->
    if(error == null) {
       // authorization success
    } else {
       // authorization fail
    }
}
```

## Scan Device
The following code allows you to scan your HUD device via Bluetooth.
```kotlin
class MainActivity : AppCompatActivity(), MConHudScanDelegate {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ...
        // Set Scan delegate
        MConHudSdk.shared().hudScanDelegate = this

        // The scanner will automatically stops according to the timeout set by the system or when trying to connect HUD device
        MConHudSdk.shared().startScanPeripheral()
        ...
    }
}
```
You will receive a list of scanned HUD devices, scan completion, and scan errors through [MConHudScanDelegate]
```kotlin
//
// MConHudScanDelegate
//
override fun scanPeripheral(peripherals: MutableList<MConHudPeripheral>) {
    // List sorted by rssi sensitivity of scanned HUD devices
}
override fun scanTimeOut() {
    // When scan finished
}
override fun error(error: MConHudSdkError) {
    // An error occurred
}
```

## Bluetooth Connect Device
You can try connecting the HUD with the following code.
```kotlin
// peripheral is MConHudPeripheral
MConHudSdk.shared().connectPeripheral(peripheral= MConHudPeripheral)
```
You will receive a signal via [MConHudScanDelegate] when a connection is established or a connection error occurs.
```kotlin
//
// MConHudScanDelegate
//
override fun connectPeripheralResult(peripheral: peripheral) {
    // Connect success
}
override fun error(error: MConHudSdkError) {
    // Connect failed or an error occurred
}
```

## Bluetooth Connect Device
Bluetooth connection will be disconnected once turn off HUD device's power or call the following code.
```kotlin
// peripheral is MConHudPeripheral
MConHudSdk.shared().disconnectPeripheral(peripheral= peripheral)
```
You are to receive a disconnection signal via [MConHudScanDelegate] when the connection is terminated.
```kotlin
//
// MConHudScanDelegate
//
override fun disconnectedPeripheral() {
    // lost connection with HUD device
}
```

## Turn by turn Message
```kotlin
val turnByTurnCode: TurnByTurnCode = TurnByTurnCode.STRAIGHT
// distance is in meters
val distance = 200
MConHudSdk.shared().sendTurnByTurnInfo(tbtCode= turnByTurnCode, distance= distance)
```

## Safety Message
```kotlin
val safetyCode: ArrayList<SafetyCode> = arrayListOf(SafetyCode.CAMERA)
val limitSpeed = 30,
val remainDistance = 150,
val isOverSpeed = false

MConHudSdk.shared().sendSafetyInfo(
  safetyCodes= safetyCode,           // Provide the camera types which is to be flashed in the form of an array.  
  limitSpeed= limitSpeed,            // If thee is a speed limit in the specific section, provide the speed limit as an integer,Int. if it is nil then speed restriction will be turned off..
  remainDistance= remainDistance,    // Remaining distance (m)
  isOverSpeed= isOverSpeed           // If the current vehicle is speeding, pass 'true'. When HUD get 'true', it activates the speeding alert buzzer.
)
```
If the multiple safety indicators are to be flashed at the same time, provide the value for safetyCode in the form of an arrray.
```kotlin
val safetyCodes: ArrayList<SafetyCode> = arrayListOf(SafetyCode.CAMERA, SafetyCode.PROTECT_CHILDREN)
val limitSpeed = 30,
val remainDistance = 150,
val isOverSpeed = false

MConHudSdk.shared().sendSafetyInfo(
  safetyCodes= safetyCodes,          // Provide the camera types which is to be flashed in the form of an array.  
  limitSpeed= limitSpeed,            // If thee is a speed limit in the specific section, provide the speed limit as an integer,Int. if it is nil then speed restriction will be turned off..
  remainDistance= remainDistance,    // Remaining distance (m)
  isOverSpeed= isOverSpeed           // If the current vehicle is speeding, pass 'true'. When HUD get 'true', it activates the speeding alert buzzer.
)
```

## Car Speed Message
```kotlin
val carSpeedCode: CarSpeedCode = CarSpeedCode.GPS_SPEED // GPS_SPEED: driving speed, SECTION_AVERAGE_SPEED: average speed in the section enforcement area.
val speed = 100
MConHudSdk.shared().sendCarSpeed(carSpeedCode= carSpeedCode, speed= speed)
```

## HUD Brightness
Change the brightness of HUD.
```kotlin
// LOW, MEDIUM, HIGH
val brightnessLevel: BrightnessLevel = BrightnessLevel.LOW
MConHudSdk.shared().sendHudBrightnessLevel(brightnessLevel= brightnessLevel)
```
You can receive the current brightness settings of the HUD through [MConHudDelegate].
```kotlin
class MainActivity : AppCompatActivity(), MConHudDelegate {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ...
        // Set delegate
        MConHudSdk.shared().hudDelegate = this
        MConHudSdk.shared().getHudBrightnessLevel()
        ...
    }
}

//
// MConHudDelegate
//
override fun receiveHudBrightnessLevel(brightnessLevel: BrightnessLevel) {
    // LOW, MEDIUM, HIGH
}
```

## HUD Buzzer
Change the beep sound volume of HUD.
```kotlin
// MUTE, LOW, MEDIUM, HIGH
val buzzerLevel: BuzzerLevel = BuzzerLevel.LOW
MConHudSdk.shared().sendHudBuzzerLevel(buzzerLevel= buzzerLevel)
```
You can receive the current buzzer status of the HUD through [MConHudDelegate].
Do NOT retrive the buzzer information in low, medium, or high but only obtain on/off status.
```kotlin
class MainActivity : AppCompatActivity(), MConHudDelegate {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ...
        // Set delegate
        MConHudSdk.shared().hudDelegate = this
        MConHudSdk.shared().getHudBuzzerLevel()
        ...
    }
}

//
// MConHudDelegate
//
override fun receiveHudBuzzerStatus(buzzerStatus: BuzzerStatus) {
    // ON, OFF
}
```

## Firmware Update
To be Updated.

## License
N/A. Update if needed.

