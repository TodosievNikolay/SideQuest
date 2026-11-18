package com.example.sidequest.data.quest

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val migrationHelper = MigrationTestHelper(instrumentation, QuestDatabase::class.java)

    @Test
    fun migratesStartedQuestWithItsChaptersAndXp() = runBlocking {
        val databaseName = "sidequest-migration-${UUID.randomUUID()}.db"
        try {
            migrationHelper.createDatabase(databaseName, 1).use { database ->
                database.execSQL(
                    """
                    INSERT INTO quests
                        (id, title, introduction, estimatedDurationMinutes, estimatedCostPln, completedChapters)
                    VALUES ('saved', 'Evening Keys', 'Follow the clues.', 30, 0, 1)
                    """.trimIndent(),
                )
                for (position in 0 until 3) {
                    database.execSQL(
                        """
                        INSERT INTO quest_chapters (questId, position, title, mission, completionCriteria)
                        VALUES ('saved', ?, ?, 'Find a public landmark.', 'Write down one detail.')
                        """.trimIndent(),
                        arrayOf<Any?>(position, "Chapter ${position + 1}"),
                    )
                }
            }

            val database = Room.databaseBuilder(context, QuestDatabase::class.java, databaseName)
                .addMigrations(QuestDatabase.MIGRATION_1_2, QuestDatabase.MIGRATION_2_3)
                .build()
            try {
                val repository = QuestRepository(database)
                val restored = requireNotNull(repository.latestQuest.first())

                assertEquals("saved", restored.adventure.id)
                assertEquals("Evening Keys", restored.adventure.title)
                assertEquals(3, restored.adventure.chapters.size)
                assertEquals(1, restored.completedChapters)
                assertEquals("Chapter 2", restored.currentChapter?.title)
                assertNull(restored.currentChapter?.storyContext)
                assertNull(restored.currentChapter?.place)
                assertNull(restored.adventure.cityName)
                assertEquals(50L, repository.playerProgress.first().totalXp)

                assertTrue(repository.completeChapter(restored.adventure.id, 1))
                assertEquals(100L, repository.playerProgress.first().totalXp)
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migratesContextualQuestWithoutInventingAPlace() = runBlocking {
        val databaseName = "sidequest-city-migration-${UUID.randomUUID()}.db"
        try {
            migrationHelper.createDatabase(databaseName, 2).use { database ->
                database.execSQL(
                    """
                    INSERT INTO quests
                        (id, title, introduction, estimatedDurationMinutes, estimatedCostPln, completedChapters)
                    VALUES ('saved-city', 'Evening Keys', 'Follow the clues.', 30, 0, 2)
                    """.trimIndent(),
                )
                for (position in 0 until 3) {
                    database.execSQL(
                        """
                        INSERT INTO quest_chapters
                            (questId, position, title, mission, completionCriteria, storyContext)
                        VALUES ('saved-city', ?, ?, 'Find a public landmark.', 'Write down one detail.', 'Follow the signal.')
                        """.trimIndent(),
                        arrayOf<Any?>(position, "Chapter ${position + 1}"),
                    )
                }
            }

            val database = Room.databaseBuilder(context, QuestDatabase::class.java, databaseName)
                .addMigrations(QuestDatabase.MIGRATION_2_3)
                .build()
            try {
                val repository = QuestRepository(database)
                val restored = requireNotNull(repository.latestQuest.first())
                assertEquals("saved-city", restored.adventure.id)
                assertEquals(2, restored.completedChapters)
                assertEquals("Follow the signal.", restored.currentChapter?.storyContext)
                assertNull(restored.adventure.cityName)
                assertTrue(restored.adventure.chapters.all { it.place == null })
                assertEquals(100L, repository.playerProgress.first().totalXp)
                assertTrue(repository.completeChapter(restored.adventure.id, 2))
                assertEquals(150L, repository.playerProgress.first().totalXp)
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
