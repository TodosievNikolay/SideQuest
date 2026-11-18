# SideQuest

SideQuest is a native Android game that turns everyday urban exploration into three-chapter adventures. Find a city, select the matching country and region, choose the time, mood, companions, budget, and interests, then generate an adventure with Firebase AI Logic. The preview shows the goal, mapped locations, estimated time and cost, and each chapter's brief story context, action, and completion criteria.

Tap **Start quest** to save it to Room and open its first chapter. **Complete mission** advances one chapter at a time. After restarting the app, **Continue adventure** resumes the saved chapter, including offline. SideQuest keeps one active quest at a time and retains completed quests in the database.

Each completed chapter earns 50 XP. The player starts at level 1 and gains a level every 100 XP. The home screen shows progress based on all completed chapters saved in Room.

Location lookup and nearby parks and squares use Photon and OpenStreetMap. Search covers mapped cities, towns, villages, hamlets, and municipalities, including one-character names. City and smaller-settlement searches both run, and the choices retain country and region so you can select the intended location. The app searches when you press **Find city**, caches results, and limits requests to the public service.

New quests use mapped parks and squares within 0.5–2 km of the selected settlement reference point, depending on the chosen time. When none are mapped nearby, the quest uses the actual settlement reference point, labelled **Area near [name]**. The player chooses one accessible public spot nearby for three different tasks. Each chapter provides an **Open map** link. Names, coordinates, and links are supplied by the app and persisted with the quest. Coverage depends on the map index; unlisted settlements need another spelling or a country qualifier. Network and map-service failures remain visible. Check current access and the walking route before setting off. The public Photon endpoint permits moderate project use and has no availability guarantee. [Photon service and API](https://github.com/komoot/photon), [OpenStreetMap attribution](https://www.openstreetmap.org/copyright)

Generation uses Gemini 3.5 Flash-Lite, with one attempt on Gemini 3.1 Flash-Lite after a timeout, overloaded service, or invalid story. Each attempt has a fixed deadline. Both requests use Firebase AI Logic and the same validated city context.

## Run on Windows

1. Open this folder in Android Studio and sync Gradle. Use the bundled JDK 25 for Gradle. Install Android SDK Platform 37 in **Tools > SDK Manager** if it is missing.
2. Open **Tools > Device Manager** and start a virtual device. The app requires Android 8.0 (API 26) or higher.
3. Select the `app` configuration and the emulator, then click **Run**. Open **Start new adventure**, choose your preferences, and tap **Generate adventure**. Use **Start quest** to begin playing.

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
5. Run the cloud smoke test from PowerShell. It generates an adventure through the app's Firebase AI Logic integration and checks the chapter count, time, and cost. Installing with `-r` and running the test through ADB preserves the emulator's registered debug token across test runs.

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

Debug builds use the App Check debug provider. Release builds install the Play Integrity provider before Firebase AI is used. A debug APK's registered emulator token does not authorize another phone.

## Share a signed test APK

Release signing reads `SIDEQUEST_SIGNING_STORE_FILE`, `SIDEQUEST_SIGNING_STORE_PASSWORD`, `SIDEQUEST_SIGNING_KEY_ALIAS`, and `SIDEQUEST_SIGNING_KEY_PASSWORD` from the environment. Supply all four values for a private PKCS12 keystore, then run `:app:assembleRelease --no-configuration-cache --no-daemon`. Keep the key and passwords outside the repository and APK, and reuse the same key for updates. A checkout without these values can still build debug APKs and an unsigned release.

For an APK distributed exclusively outside Google Play, enable the **Play Integrity API** in the same Cloud project, register the APK signing certificate's SHA-256 with the Firebase Android app, and configure **App Check > Apps > Play Integrity > Advanced settings** as follows.

| Setting | Value for this sideloaded APK |
| --- | --- |
| Require `PLAY_RECOGNIZED` | Off |
| Require `LICENSED` | Off |
| Minimum device integrity | Device integrity (`MEETS_DEVICE_INTEGRITY`) |

Keep Firebase AI Logic enforcement enabled. Do not embed a shared debug token or disable App Check to distribute the app. These settings follow the [Firebase Play Integrity guide](https://firebase.google.com/docs/app-check/android/play-integrity-provider). The API can be enabled directly in Cloud Console as described in the [Android setup guide](https://developer.android.com/google/play/integrity/setup).

Test the signed APK on a genuine Android 8+ phone with Google Play services, a certified system, and internet access. Rooted devices, custom systems, and emulators may fail device integrity. Successful debug inference on the emulator does not verify the release attestation path. Test generation on a physical phone after applying the cloud settings. Friends share the project's existing Gemini quota and region restrictions.

For the hybrid milestone, add the on-device SDK and verify Gemini Nano inference on a compatible device. Record the source of the model response. [Firebase Android hybrid guide](https://firebase.google.com/docs/ai-logic/hybrid/android/get-started)

## License

[Apache License 2.0](LICENSE)
