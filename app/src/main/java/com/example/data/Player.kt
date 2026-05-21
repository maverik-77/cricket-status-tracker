package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // e.g. "Batter", "Bowler", "All-Rounder", "Wicketkeeper"
    val team: String,
    val jerseyNumber: String = "",
    val avatarColorIndex: Int = 0 // 0 to 5 for profile thumbnail backgrounds
)
