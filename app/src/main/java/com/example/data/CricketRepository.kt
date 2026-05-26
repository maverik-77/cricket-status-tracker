package com.example.data

import kotlinx.coroutines.flow.Flow

class CricketRepository(private val cricketDao: CricketDao) {
    fun getDao(): CricketDao = cricketDao

    val allPlayers: Flow<List<Player>> = cricketDao.getAllPlayers()
    val allPerformances: Flow<List<MatchPerformance>> = cricketDao.getAllPerformances()

    fun getPlayerById(playerId: Int): Flow<Player?> {
        return cricketDao.getPlayerById(playerId)
    }

    fun getPerformancesForPlayer(playerId: Int): Flow<List<MatchPerformance>> {
        return cricketDao.getPerformancesForPlayer(playerId)
    }

    suspend fun insertPlayer(player: Player): Long {
        return cricketDao.insertPlayer(player)
    }

    suspend fun deletePlayer(player: Player) {
        cricketDao.deletePlayer(player)
    }

    suspend fun insertPerformance(performance: MatchPerformance): Long {
        return cricketDao.insertPerformance(performance)
    }

    suspend fun deletePerformance(performance: MatchPerformance) {
        cricketDao.deletePerformance(performance)
    }
}
