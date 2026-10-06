package com.example.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Repository to abstract data access from DAO for Fighters.
 */
class FighterRepository(private val fighterDao: FighterDao) {
    val allFighters: Flow<List<FighterEntity>> = fighterDao.getAllFighters()

    fun getFightersByOwnership(isPlayer: Boolean): Flow<List<FighterEntity>> {
        return fighterDao.getFightersByOwnership(isPlayer)
    }

    suspend fun getFighterById(id: String): FighterEntity? {
        return fighterDao.getFighterById(id)
    }

    suspend fun insert(fighter: FighterEntity) {
        fighterDao.insertFighter(fighter)
    }

    suspend fun insertAll(fighters: List<FighterEntity>) {
        fighterDao.insertFighters(fighters)
    }

    suspend fun update(fighter: FighterEntity) {
        fighterDao.updateFighter(fighter)
    }

    suspend fun delete(fighter: FighterEntity) {
        fighterDao.deleteFighter(fighter)
    }

    suspend fun deleteById(id: String) {
        fighterDao.deleteFighterById(id)
    }

    suspend fun deleteAll() {
        fighterDao.deleteAllFighters()
    }
}
