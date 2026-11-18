package com.example.sidequest.data.quest

import android.content.Context
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.AdventureChapter
import com.example.sidequest.domain.adventure.AdventureResponseParser
import com.example.sidequest.domain.adventure.PlayerProgress
import com.example.sidequest.domain.adventure.QuestProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuestRepository internal constructor(database: QuestDatabase) {
    constructor(context: Context) : this(QuestDatabase.getInstance(context))

    private val quests = database.quests()

    val playerProgress: Flow<PlayerProgress> = quests.observeCompletedChapterCount().map { completedChapters ->
        PlayerProgress.fromCompletedChapters(completedChapters)
    }

    val latestQuest: Flow<QuestProgress?> = quests.observeLatestQuest().map { stored ->
        stored?.let {
            QuestProgress(
                adventure = Adventure(
                    id = it.quest.id,
                    title = it.quest.title,
                    introduction = it.quest.introduction,
                    estimatedDurationMinutes = it.quest.estimatedDurationMinutes,
                    estimatedCostPln = it.quest.estimatedCostPln,
                    chapters = it.chapters.sortedBy(QuestChapterEntity::position).map { chapter ->
                        AdventureChapter(
                            title = chapter.title,
                            mission = chapter.mission,
                            completionCriteria = chapter.completionCriteria,
                            storyContext = chapter.storyContext,
                            place = chapter.place,
                        )
                    },
                    cityName = it.quest.cityName,
                ),
                completedChapters = it.quest.completedChapters,
            )
        }
    }

    suspend fun start(adventure: Adventure): Boolean {
        val chapterCount = AdventureResponseParser.CHAPTER_COUNT
        if (adventure.id.isBlank() || adventure.chapters.size != chapterCount) return false

        val quest = QuestEntity(
            id = adventure.id,
            title = adventure.title,
            introduction = adventure.introduction,
            estimatedDurationMinutes = adventure.estimatedDurationMinutes,
            estimatedCostPln = adventure.estimatedCostPln,
            cityName = adventure.cityName,
        )
        val chapters = adventure.chapters.mapIndexed { position, chapter ->
            QuestChapterEntity(
                questId = adventure.id,
                position = position,
                title = chapter.title,
                mission = chapter.mission,
                completionCriteria = chapter.completionCriteria,
                storyContext = chapter.storyContext,
                place = chapter.place,
            )
        }
        return quests.start(quest, chapters, chapterCount)
    }

    suspend fun completeChapter(questId: String, expectedCompletedChapters: Int): Boolean {
        val chapterCount = AdventureResponseParser.CHAPTER_COUNT
        if (expectedCompletedChapters !in 0 until chapterCount) return false

        return quests.completeChapter(questId, expectedCompletedChapters, chapterCount) == 1
    }
}
