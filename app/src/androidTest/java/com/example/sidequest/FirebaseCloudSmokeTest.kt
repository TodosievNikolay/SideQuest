package com.example.sidequest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidequest.data.ai.FirebaseAdventureGenerator
import com.example.sidequest.data.places.PhotonPlacesService
import com.example.sidequest.domain.adventure.AdventureMood
import com.example.sidequest.domain.adventure.AdventureParty
import com.example.sidequest.domain.adventure.AdventureRequest
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseCloudSmokeTest {
    @Test
    fun geminiGeneratesAnAdventureWithinPlayerLimits() = runBlocking {
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

        val places = PhotonPlacesService()
        val city = places.searchCities("Odesa, Ukraine").first { it.country == "Ukraine" }
        val request = AdventureRequest(
            durationMinutes = 30,
            mood = AdventureMood.ADVENTURE,
            party = AdventureParty.GROUP,
            maxBudgetPln = 30,
            interests = "street art",
            city = city,
        )
        val adventure = FirebaseAdventureGenerator(places).generate(request)

        assertEquals(3, adventure.chapters.size)
        assertTrue("Expected story context for all three chapters.", adventure.chapters.all { !it.storyContext.isNullOrBlank() })
        assertTrue(adventure.estimatedDurationMinutes in 1..request.durationMinutes)
        assertTrue(adventure.estimatedCostPln in 0..request.maxBudgetPln)
        assertEquals(city.displayName, adventure.cityName)
        assertTrue("Expected mapped locations for all three chapters.", adventure.chapters.all { it.place != null })
    }
}
