package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_performances",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playerId"])]
)
data class MatchPerformance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerId: Int,
    val opponent: String,
    val date: Long = System.currentTimeMillis(),
    val matchFormat: String, // "T20", "ODI", "Test"
    val ageGroup: String = "Adult", // "U15", "U17", "Adult"
    
    // Batting stats
    val didBat: Boolean = false,
    val runsScored: Int = 0,
    val ballsFaced: Int = 0,
    val isNotOut: Boolean = false,
    val fours: Int = 0,
    val sixes: Int = 0,

    // Bowling stats
    val didBowl: Boolean = false,
    val ballsBowled: Int = 0, // easier to compute fractional overs (e.g. 19 balls = 3.1 overs)
    val runsConceded: Int = 0,
    val wicketsTaken: Int = 0,
    val maidensBowled: Int = 0
) {
    // Utility functions to format overs or calculate stats helper
    fun getOversFormatted(): String {
        val completeOvers = ballsBowled / 6
        val remainingBalls = ballsBowled % 6
        return if (remainingBalls == 0) "$completeOvers" else "$completeOvers.$remainingBalls"
    }
}
