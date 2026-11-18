package com.example.sidequest.domain.adventure

data class QuestProgress(
    val adventure: Adventure,
    val completedChapters: Int,
) {
    val currentChapter: AdventureChapter?
        get() = adventure.chapters.getOrNull(completedChapters)

    val isComplete: Boolean
        get() = completedChapters == adventure.chapters.size
}
