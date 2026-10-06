package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FighterDao
import com.example.data.local.FighterEntity
import com.example.data.local.FighterRepository
import com.example.game.Faction
import com.example.game.Fighter
import com.example.game.FighterClass
import com.example.game.toEntity
import com.example.game.toFighter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FighterRoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var fighterDao: FighterDao
    private lateinit var repository: FighterRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fighterDao = db.fighterDao()
        repository = FighterRepository(fighterDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testFighterDataClassAttributes() {
        val fighter = Fighter(
            name = "Ironclad Warrior",
            health = 150,
            attackPower = 28,
            defense = 14,
            faction = Faction.ORC,
            fighterClass = FighterClass.TANK
        )

        assertEquals("Ironclad Warrior", fighter.name)
        assertEquals(150, fighter.health)
        assertEquals(150, fighter.hp)
        assertEquals(28, fighter.attackPower)
        assertEquals(28, fighter.physAttack)
        assertEquals(14, fighter.defense)
        assertEquals(14, fighter.physDefense)
        assertTrue(fighter.isAlive)
    }

    @Test
    fun testRoomFighterEntityAttributes() {
        val entity = FighterEntity(
            id = "fighter-1",
            name = "Berserker",
            health = 180,
            maxHealth = 180,
            attackPower = 35,
            defense = 12,
            faction = "ORC",
            fighterClass = "ASSASSIN"
        )

        assertEquals("fighter-1", entity.id)
        assertEquals("Berserker", entity.name)
        assertEquals(180, entity.health)
        assertEquals(180, entity.maxHealth)
        assertEquals(35, entity.attackPower)
        assertEquals(12, entity.defense)
        assertEquals("ORC", entity.faction)
        assertEquals("ASSASSIN", entity.fighterClass)
    }

    @Test
    fun testFighterToEntityAndBackMapping() {
        val domainFighter = Fighter(
            name = "Shadow Assassin",
            health = 110,
            attackPower = 38,
            defense = 8,
            faction = Faction.DARK,
            fighterClass = FighterClass.ASSASSIN
        )

        val entity = domainFighter.toEntity()
        assertEquals(domainFighter.id, entity.id)
        assertEquals(domainFighter.name, entity.name)
        assertEquals(domainFighter.health, entity.health)
        assertEquals(domainFighter.attackPower, entity.attackPower)
        assertEquals(domainFighter.defense, entity.defense)
        assertEquals("DARK", entity.faction)
        assertEquals("ASSASSIN", entity.fighterClass)

        val mappedBack = entity.toFighter()
        assertEquals(domainFighter.id, mappedBack.id)
        assertEquals(domainFighter.name, mappedBack.name)
        assertEquals(domainFighter.health, mappedBack.health)
        assertEquals(domainFighter.attackPower, mappedBack.attackPower)
        assertEquals(domainFighter.defense, mappedBack.defense)
        assertEquals(Faction.DARK, mappedBack.faction)
        assertEquals(FighterClass.ASSASSIN, mappedBack.fighterClass)
    }

    @Test
    fun testRoomDaoInsertAndQueryFlow() = runBlocking {
        val fighter = FighterEntity(
            id = "f-101",
            name = "Paladin",
            health = 200,
            attackPower = 22,
            defense = 20,
            isPlayer = true
        )

        repository.insert(fighter)

        val fetched = repository.getFighterById("f-101")
        assertNotNull(fetched)
        assertEquals("Paladin", fetched?.name)
        assertEquals(200, fetched?.health)
        assertEquals(22, fetched?.attackPower)
        assertEquals(20, fetched?.defense)

        val all = repository.allFighters.first()
        assertEquals(1, all.size)
        assertEquals("Paladin", all[0].name)
    }

    @Test
    fun testRoomDaoUpdateAndDelete() = runBlocking {
        val fighter = FighterEntity(
            id = "f-202",
            name = "Pyromancer",
            health = 90,
            attackPower = 40,
            defense = 6
        )
        repository.insert(fighter)

        // Update health and attackPower
        val updated = fighter.copy(health = 75, attackPower = 45)
        repository.update(updated)

        val fetched = repository.getFighterById("f-202")
        assertEquals(75, fetched?.health)
        assertEquals(45, fetched?.attackPower)

        // Delete by ID
        repository.deleteById("f-202")
        val deleted = repository.getFighterById("f-202")
        assertNull(deleted)
    }

    @Test
    fun testAppDatabaseInitializationAndProvidesDao() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        assertNotNull("AppDatabase instance should not be null", database)

        val dao = database.fighterDao()
        assertNotNull("FighterDao instance provided by AppDatabase should not be null", dao)

        val directDao = AppDatabase.getFighterDao(context)
        assertNotNull("Direct FighterDao getter should not be null", directDao)
    }
}
