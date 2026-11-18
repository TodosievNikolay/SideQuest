package com.example.sidequest.domain.adventure

data class Adventure(
    val id: String,
    val title: String,
    val introduction: String,
    val estimatedDurationMinutes: Int,
    val estimatedCostPln: Int,
    val chapters: List<AdventureChapter>,
    val cityName: String? = null,
)

data class AdventureChapter(
    val title: String,
    val mission: String,
    val completionCriteria: String,
    val storyContext: String? = null,
    val place: QuestPlace? = null,
)
