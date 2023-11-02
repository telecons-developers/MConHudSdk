# MConHudSdk
## Installation
MConHudSdk is available through <https://jitpack.io/>. To install
it, simply add the follow these steps:

Step 1. Add the JitPack repository to your build file
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

```
MConHudSdk.shared().initialize(application, "appkey", notification) { error ->
            if(error == null) {
                //authorization success
            } else {
                //authorization fail
            }
        }
```
