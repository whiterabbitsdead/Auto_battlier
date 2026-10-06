package com.example.game

import java.util.UUID

/**
 * CombatLog captures combat events as strings during combat simulations
 * and provides methods to retrieve recent history and manage fight lifecycles.
 */
class CombatLog(
    val maxCapacity: Int = 100,
    val defaultRecentLimit: Int = 10
) {
    private val _currentFightEvents = mutableListOf<String>()
    private val _allEvents = mutableListOf<String>()
    private val _archivedFights = mutableMapOf<String, List<String>>()

    var currentFightId: String = UUID.randomUUID().toString()
        private set

    var isFightActive: Boolean = true
        private set

    /**
     * Appends a combat event to the current fight log and overall history.
     */
    fun appendEvent(event: String) {
        if (!isFightActive) {
            startFight()
        }
        _currentFightEvents.add(event)
        _allEvents.add(event)
        if (_currentFightEvents.size > maxCapacity) {
            _currentFightEvents.removeAt(0)
        }
        if (_allEvents.size > maxCapacity * 5) {
            _allEvents.removeAt(0)
        }
    }

    /**
     * Alias methods for appending events.
     */
    fun log(event: String) = appendEvent(event)
    fun addEvent(event: String) = appendEvent(event)
    fun record(event: String) = appendEvent(event)
    operator fun plusAssign(event: String) = appendEvent(event)

    /**
     * Retrieves the recent history of events for the current fight.
     */
    fun getRecentHistory(count: Int = defaultRecentLimit): List<String> {
        val n = count.coerceAtLeast(0)
        return if (_currentFightEvents.size <= n) {
            _currentFightEvents.toList()
        } else {
            _currentFightEvents.takeLast(n)
        }
    }

    /**
     * Retrieves all events recorded during the current fight.
     */
    fun getCurrentFightHistory(): List<String> = _currentFightEvents.toList()

    /**
     * Retrieves all events recorded across all fights in this session.
     */
    fun getAllHistory(): List<String> = _allEvents.toList()

    /**
     * Starts a new fight session, optionally archiving the current fight.
     */
    fun startFight(fightId: String = UUID.randomUUID().toString()) {
        if (_currentFightEvents.isNotEmpty()) {
            _archivedFights[currentFightId] = _currentFightEvents.toList()
        }
        currentFightId = fightId
        isFightActive = true
        _currentFightEvents.clear()
    }

    /**
     * Ends the current fight session and archives its events.
     */
    fun endFight() {
        if (isFightActive) {
            _archivedFights[currentFightId] = _currentFightEvents.toList()
            isFightActive = false
        }
    }

    /**
     * Clears all events for the current fight.
     */
    fun clearCurrentFight() {
        _currentFightEvents.clear()
    }

    /**
     * Clears all log history.
     */
    fun clear() {
        _currentFightEvents.clear()
        _allEvents.clear()
        _archivedFights.clear()
        isFightActive = true
    }

    val currentFightEvents: List<String>
        get() = _currentFightEvents.toList()

    val allEvents: List<String>
        get() = _allEvents.toList()

    val size: Int get() = _currentFightEvents.size
    val count: Int get() = _currentFightEvents.size
    fun isEmpty(): Boolean = _currentFightEvents.isEmpty()
    fun isNotEmpty(): Boolean = _currentFightEvents.isNotEmpty()
}
