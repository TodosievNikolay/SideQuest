package com.example.sidequest.data.quest

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.AdventureChapter
import com.example.sidequest.domain.adventure.QuestPlace
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun validatesChapterCountAndStartsOnlyOneConcurrentQuest() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, QuestDatabase::class.java).build()
        try {
            val repository = QuestRepository(database)
            val firstQuest = adventure("first")
            val secondQuest = adventure("second")

            assertFalse(repository.start(firstQuest.copy(chapters = firstQuest.chapters.dropLast(1))))
            assertNull(repository.latestQuest.first())
            assertPlayerProgress(repository, xp = 0, level = 1, xpToNextLevel = 100)

            val firstStart = async { repository.start(firstQuest) }
            val secondStart = async { repository.start(secondQuest) }
            val startedFirst = firstStart.await()
            val startedSecond = secondStart.await()
            assertTrue(startedFirst xor startedSecond)

            val saved = requireNotNull(repository.latestQuest.first())
            assertEquals(if (startedFirst) firstQuest else secondQuest, saved.adventure)
            assertEquals(0, saved.completedChapters)
            assertPlayerProgress(repository, xp = 0, level = 1, xpToNextLevel = 100)
        } finally {
            database.close()
        }
    }

    @Test
    fun resumesProgressAfterReopeningAndRejectsDuplicateCompletion() = runBlocking {
        val databaseName = "sidequest-test-${UUID.randomUUID()}.db"
        val quest = adventure("first")
        try {
            val initialDatabase = Room.databaseBuilder(context, QuestDatabase::class.java, databaseName).build()
            try {
                val repository = QuestRepository(initialDatabase)
                assertTrue(repository.start(quest))

                val firstCompletion = async { repository.completeChapter(quest.id, 0) }
                val duplicateCompletion = async { repository.completeChapter(quest.id, 0) }
                assertTrue(firstCompletion.await() xor duplicateCompletion.await())
            } finally {
                initialDatabase.close()
            }

            val reopenedDatabase = Room.databaseBuilder(context, QuestDatabase::class.java, databaseName).build()
            try {
                val repository = QuestRepository(reopenedDatabase)
                val resumed = requireNotNull(repository.latestQuest.first())
                assertEquals(quest, resumed.adventure)
                assertEquals(1, resumed.completedChapters)
                assertEquals(quest.chapters[1], resumed.currentChapter)
                assertPlayerProgress(repository, xp = 50, level = 1, xpToNextLevel = 50)

                assertFalse(repository.completeChapter(quest.id, 2))
                assertPlayerProgress(repository, xp = 50, level = 1, xpToNextLevel = 50)
                assertTrue(repository.completeChapter(quest.id, 1))
                assertPlayerProgress(repository, xp = 100, level = 2, xpToNextLevel = 100)
                assertTrue(repository.completeChapter(quest.id, 2))
                assertFalse(repository.completeChapter(quest.id, 2))
                assertFalse(repository.completeChapter(quest.id, 3))
                assertPlayerProgress(repository, xp = 150, level = 2, xpToNextLevel = 50)

                val completed = requireNotNull(repository.latestQuest.first())
                assertTrue(completed.isComplete)
                assertNull(completed.currentChapter)
                assertFalse(repository.start(quest))

                val nextQuest = adventure("second")
                assertTrue(repository.start(nextQuest))
                assertEquals(nextQuest, repository.latestQuest.first()?.adventure)
                assertPlayerProgress(repository, xp = 150, level = 2, xpToNextLevel = 50)
                assertTrue(repository.completeChapter(nextQuest.id, 0))
                assertPlayerProgress(repository, xp = 200, level = 3, xpToNextLevel = 100)
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private suspend fun assertPlayerProgress(
        repository: QuestRepository,
        xp: Long,
        level: Long,
        xpToNextLevel: Int,
    ) {
        val progress = repository.playerProgress.first()
        assertEquals(xp, progress.totalXp)
        assertEquals(level, progress.level)
        assertEquals(xpToNextLevel, progress.xpToNextLevel)
    }

    private fun adventure(id: String) = Adventure(
        id = id,
        title = "The Evening Keys",
        introduction = "Look for patterns along your usual route.",
        estimatedDurationMinutes = 30,
        estimatedCostPln = 0,
        cityName = "Warsaw, Mazowieckie, Poland",
        chapters = (1..3).map { number ->
            AdventureChapter(
                title = "Chapter $number",
                mission = "Find a public landmark.",
                completionCriteria = "Write down one detail.",
                storyContext = "The landmark holds clue $number.",
                place = QuestPlace(
                    id = "W$number",
                    name = "Public square $number",
                    category = "square",
                    latitude = 52.2297 + number * 0.0001,
                    longitude = 21.0122 + number * 0.0001,
                    sourceUrl = "https://www.openstreetmap.org/way/$number",
                ),
            )
        },
    )
}
