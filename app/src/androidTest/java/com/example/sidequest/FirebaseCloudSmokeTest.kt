package com.example.sidequest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseCloudSmokeTest {
    @Test
    fun geminiReturnsText() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val enabled = InstrumentationRegistry.getArguments().getString("firebaseSmokeTest")
        assumeTrue("Enable explicitly to make a cloud request.", enabled == "true")

        assertTrue(
            "Add the Firebase configuration to app/google-services.json and rebuild.",
            FirebaseApp.getApps(instrumentation.targetContext).isNotEmpty(),
        )

        val appCheckToken = Tasks.await(
            FirebaseAppCheck.getInstance().getAppCheckToken(false),
            20L,
            TimeUnit.SECONDS,
        )
        assertFalse("Expected an App Check token.", appCheckToken.token.isBlank())

        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-3.8-flash",
                requestOptions = RequestOptions(timeoutInMillis = 90_000L),
            )
        val response = model.generateContent("Respond with one short greeting in English.")
        assertFalse("Expected a text response from Firebase AI Logic.", response.text.isNullOrBlank())
    }
}
