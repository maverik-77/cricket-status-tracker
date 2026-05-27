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
    val avatarColorIndex: Int = 0, // 0 to 5 for profile thumbnail backgrounds
    val profileImageUri: String? = null,
    val assignedAgeGroups: String = "U15,U17,Adult"
) {
    fun getAssignedAgeGroupsList(): List<String> {
        if (assignedAgeGroups.isBlank()) return emptyList()
        return assignedAgeGroups.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { 
                if (it.contains(":")) {
                    it.substringAfter(":") 
                } else {
                    it
                }
            }
            .distinct()
    }

    fun getYearlyAgeGroupsMap(): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        if (assignedAgeGroups.isBlank()) return map
        assignedAgeGroups.split(",").map { it.trim() }.forEach { part ->
            if (part.contains(":")) {
                val year = part.substringBefore(":")
                val group = part.substringAfter(":")
                if (year.isNotEmpty() && group.isNotEmpty()) {
                    map.getOrPut(year) { mutableListOf() }.add(group)
                }
            }
        }
        return map
    }

    fun getGeneralAgeGroups(): List<String> {
        if (assignedAgeGroups.isBlank()) return emptyList()
        return assignedAgeGroups.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.contains(":") }
    }

    fun getAgeGroupsForYear(year: String): List<String> {
        val yearlyMap = getYearlyAgeGroupsMap()
        val yearlyList = yearlyMap[year]
        if (yearlyList != null && yearlyList.isNotEmpty()) {
            return yearlyList
        }
        // Fallback to general/default groups
        val general = getGeneralAgeGroups()
        if (general.isNotEmpty()) {
            return general
        }
        // Fallback to all age groups of the player
        return getAssignedAgeGroupsList()
    }

    fun getFirstAgeGroupForYear(year: String): String? {
        return getAgeGroupsForYear(year).firstOrNull()
    }
}
