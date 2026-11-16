package com.example.sidequest.domain.adventure

data class AdventureRequest(
    val durationMinutes: Int,
    val mood: AdventureMood,
    val party: AdventureParty,
    val maxBudgetPln: Int,
    val interests: String,
) {
    init {
        require(durationMinutes in DURATIONS) { "Unsupported adventure duration." }
        require(maxBudgetPln >= 0) { "Budget must be nonnegative." }
        require(interests.length <= MAX_INTERESTS_LENGTH) { "Interests are too long." }
    }

    companion object {
        val DURATIONS = listOf(30, 60, 90, 180)
        const val MAX_INTERESTS_LENGTH = 300
    }
}

enum class AdventureMood { ADVENTURE, CHILL, DISCOVERY, SOCIAL }

enum class AdventureParty { SOLO, GROUP }
