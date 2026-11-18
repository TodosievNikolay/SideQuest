package com.example.sidequest.data.quest

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sidequest.domain.adventure.QuestPlace
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "quests")
internal data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val introduction: String,
    val estimatedDurationMinutes: Int,
    val estimatedCostPln: Int,
    val completedChapters: Int = 0,
    val cityName: String? = null,
)

@Entity(
    tableName = "quest_chapters",
    primaryKeys = ["questId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class QuestChapterEntity(
    val questId: String,
    val position: Int,
    val title: String,
    val mission: String,
    val completionCriteria: String,
    val storyContext: String?,
    @Embedded(prefix = "place_")
    val place: QuestPlace? = null,
)

internal data class QuestWithChapters(
    @Embedded val quest: QuestEntity,
    @Relation(parentColumn = "id", entityColumn = "questId")
    val chapters: List<QuestChapterEntity>,
)

@Dao
internal abstract class QuestDao {
    @Transaction
    @Query("SELECT * FROM quests ORDER BY rowid DESC LIMIT 1")
    abstract fun observeLatestQuest(): Flow<QuestWithChapters?>

    @Query("SELECT COALESCE(SUM(completedChapters), 0) FROM quests")
    abstract fun observeCompletedChapterCount(): Flow<Long>

    @Transaction
    open suspend fun start(
        quest: QuestEntity,
        chapters: List<QuestChapterEntity>,
        chapterCount: Int,
    ): Boolean {
        if (hasActiveQuest(chapterCount) || insertQuest(quest) == -1L) return false

        insertChapters(chapters)
        return true
    }

    @Query("SELECT EXISTS(SELECT 1 FROM quests WHERE completedChapters < :chapterCount)")
    protected abstract suspend fun hasActiveQuest(chapterCount: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertQuest(quest: QuestEntity): Long

    @Insert
    protected abstract suspend fun insertChapters(chapters: List<QuestChapterEntity>)

    @Query(
        """
        UPDATE quests
        SET completedChapters = completedChapters + 1
        WHERE id = :questId
          AND completedChapters = :expectedCompletedChapters
          AND completedChapters < :chapterCount
        """,
    )
    abstract suspend fun completeChapter(
        questId: String,
        expectedCompletedChapters: Int,
        chapterCount: Int,
    ): Int
}

@Database(
    entities = [QuestEntity::class, QuestChapterEntity::class],
    version = 3,
    exportSchema = true,
)
internal abstract class QuestDatabase : RoomDatabase() {
    abstract fun quests(): QuestDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN storyContext TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quests ADD COLUMN cityName TEXT")
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN place_id TEXT")
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN place_name TEXT")
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN place_category TEXT")
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN place_latitude REAL")
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN place_longitude REAL")
                db.execSQL("ALTER TABLE quest_chapters ADD COLUMN place_sourceUrl TEXT")
            }
        }

        @Volatile
        private var instance: QuestDatabase? = null

        fun getInstance(context: Context): QuestDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                QuestDatabase::class.java,
                "sidequest.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
