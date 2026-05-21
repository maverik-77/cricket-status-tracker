package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CricketDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    fun getPlayerById(playerId: Int): Flow<Player?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Delete
    suspend fun deletePlayer(player: Player)

    @Query("SELECT * FROM match_performances WHERE playerId = :playerId ORDER BY date DESC")
    fun getPerformancesForPlayer(playerId: Int): Flow<List<MatchPerformance>>

    @Query("SELECT * FROM match_performances ORDER BY date DESC")
    fun getAllPerformances(): Flow<List<MatchPerformance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: MatchPerformance): Long

    @Delete
    suspend fun deletePerformance(performance: MatchPerformance)
}
