package com.vickers.digitdice

import androidx.compose.ui.geometry.Offset
import kotlin.random.Random

enum class Phase { Idle, Counting, Chosen }

class Finger(
    val id: Long,
    var pos: Offset,
    val colorIndex: Int
)

/**
 * The whole rule set, with no Compose and no Android in it.
 *
 * Every time a finger lands or lifts the countdown restarts, so nobody can
 * sneak on at the last second. Once a pick is made it stays on screen until
 * every finger has lifted, which stops the "my finger slipped" rematch.
 * The picked finger can also linger for a while after it lifts, so the
 * winner gets a proper look. A fresh touch during the linger starts over.
 *
 * All times come in from the caller and must share one clock.
 */
class Game(private val paletteSize: Int) {

    private val current = LinkedHashMap<Long, Finger>()
    private val usedColors = HashSet<Int>()

    val fingers: Collection<Finger> get() = current.values
    val count: Int get() = current.size

    var phase: Phase = Phase.Idle
        private set

    var pickedId: Long? = null
        private set

    /** The picked finger, kept even after it lifts so it can linger on screen. */
    var picked: Finger? = null
        private set

    /** True while the picked finger has lifted but the reveal is still showing. */
    val pickedLifted: Boolean
        get() = phase == Phase.Chosen && pickedId?.let { current.containsKey(it) } == false

    private var countStart = 0L
    private var pickedAt = 0L
    private var lingerUntil = 0L

    fun has(id: Long): Boolean = current.containsKey(id)

    /** @return true if this touch became a new finger. */
    fun down(id: Long, pos: Offset, now: Long): Boolean {
        if (phase == Phase.Chosen) {
            if (current.isNotEmpty()) return false
            reset() // everyone is up and the winner is only lingering
        }
        if (current.containsKey(id)) return false

        val colorIndex = (0 until paletteSize).firstOrNull { it !in usedColors } ?: 0
        usedColors.add(colorIndex)
        current[id] = Finger(id, pos, colorIndex)
        restart(now)
        return true
    }

    fun move(id: Long, pos: Offset) {
        current[id]?.pos = pos
    }

    /** @param lingerMillis how long the picked finger stays on screen after it lifts. */
    fun up(id: Long, now: Long, lingerMillis: Int = 0) {
        val finger = current.remove(id) ?: return
        usedColors.remove(finger.colorIndex)
        if (phase == Phase.Chosen) {
            if (id == pickedId) lingerUntil = now + lingerMillis
            if (current.isEmpty() && now >= lingerUntil) reset()
        } else {
            restart(now)
        }
    }

    /** @return true if the pick happened on this tick. */
    fun tick(now: Long, holdMillis: Int): Boolean {
        if (phase == Phase.Chosen && current.isEmpty() && now >= lingerUntil) {
            reset()
            return false
        }
        if (phase != Phase.Counting) return false
        if (now - countStart < holdMillis) return false

        val ids = current.keys.toList()
        if (ids.isEmpty()) return false

        pickedId = ids[Random.nextInt(ids.size)]
        picked = current[pickedId]
        pickedAt = now
        phase = Phase.Chosen
        return true
    }

    /** How far through the countdown, 0f to 1f. Zero when not counting. */
    fun progress(now: Long, holdMillis: Int): Float =
        if (phase != Phase.Counting) 0f
        else ((now - countStart).toFloat() / holdMillis).coerceIn(0f, 1f)

    /** Seconds since the pick was revealed. */
    fun secondsSincePick(now: Long): Float = (now - pickedAt) / 1000f

    /** Milliseconds left before a lifted pick disappears. */
    fun lingerLeftMillis(now: Long): Long = lingerUntil - now

    fun reset() {
        current.clear()
        usedColors.clear()
        phase = Phase.Idle
        pickedId = null
        picked = null
    }

    private fun restart(now: Long) {
        phase = if (current.size >= 2) Phase.Counting else Phase.Idle
        countStart = now
    }
}
