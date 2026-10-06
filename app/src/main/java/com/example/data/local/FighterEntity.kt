package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room database entity representing a Fighter in the AutoBattle engine.
 *
 * Contains core combat attributes including health, attack power, defense,
 * along with class/faction progression, cost, star level, and battle grid positions.
 */
@Entity(tableName = "fighters")
data class FighterEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val health: Int,
    val attackPower: Int,
    val defense: Int,
    val maxHealth: Int = health,
    val magicAttack: Int = 0,
    val magicDefense: Int = 0,
    val attackSpeed: Int = 10,
    val attackRange: Int = 1,
    val cost: Int = 1,
    val starLevel: Int = 1,
    val faction: String = "ORC",
    val fighterClass: String = "TANK",
    val isPlayer: Boolean = true,
    val boardX: Int = 0,
    val boardY: Int = 0
)
