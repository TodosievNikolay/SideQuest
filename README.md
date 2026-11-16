# SideQuest

SideQuest is a native Android game that turns everyday urban exploration into three-chapter adventures. Choose the time, mood, companions, budget, and interests, then generate an adventure with Firebase AI Logic. The preview shows the estimated time and cost, the mission in each chapter, and its completion criteria. Playing and saving adventures are planned for the next milestone.

## Run on Windows

1. Open this folder in Android Studio and sync Gradle. Use the bundled JDK 25 for Gradle. Install Android SDK Platform 37 in **Tools > SDK Manager** if it is missing.
2. Open **Tools > Device Manager** and start a virtual device. The app requires Android 8.0 (API 26) or higher.
3. Select the `app` configuration and the emulator, then click **Run**. Open **Start new adventure**, choose your preferences, and tap **Generate adventure**.

Build the debug APK and run the local unit tests in PowerShell.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

## Connect Firebase and verify cloud inference

The UI builds before Firebase registration. The Google Services plugin is applied when `app/google-services.json` is present.

1. Create or open the **SideQuest** Firebase project. In **Project settings > General > Your apps**, register the Android application ID `com.example.sidequest`. Download `google-services.json` into the local `app/` folder. [Firebase Console](https://console.firebase.google.com/)
2. In **AI Services > AI Logic**, click **Get started**. Select **Gemini Developer API** and complete the setup. See the [Firebase AI Logic setup guide](https://firebase.google.com/docs/ai-logic/get-started).
3. Build and install both debug APKs on the emulator with the PowerShell commands below, then open SideQuest. In Android Studio **Logcat**, filter by `tag~:.*DebugAppCheckProvider`. If the provider has yet to print a token, run the smoke test command once to trigger it.
4. In Firebase Console, open **Security > App Check > Apps**. Open the Android app's overflow menu, choose **Manage debug tokens**, and register the token from Logcat. In **App Check > APIs**, verify that Firebase AI Logic is enforced. Keep the debug token private. See the [Android App Check debug guide](https://firebase.google.com/docs/app-check/android/debug-provider).
5. Run the cloud smoke test from PowerShell. It sends a short prompt via Firebase AI Logic and checks for a text response. Installing with `-r` and running the test through ADB preserves the emulator's registered debug token across test runs.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$sideQuestAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
& $sideQuestAdb install -r app/build/outputs/apk/debug/app-debug.apk
& $sideQuestAdb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $sideQuestAdb shell am start -W -n com.example.sidequest/.MainActivity
```

```powershell
& $sideQuestAdb shell am instrument -w `
  -e class com.example.sidequest.FirebaseCloudSmokeTest `
  -e firebaseSmokeTest true `
  com.example.sidequest.test/androidx.test.runner.AndroidJUnitRunner
```

Debug builds use the App Check debug provider. Before distributing a release that calls Firebase, configure the production App Check provider.

For the hybrid milestone, add the on-device SDK and verify Gemini Nano inference on a compatible device. Record the source of the model response. [Firebase Android hybrid guide](https://firebase.google.com/docs/ai-logic/hybrid/android/get-started)

## License

[Apache License 2.0](LICENSE)
