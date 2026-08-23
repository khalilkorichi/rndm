package com.rndm.app.core.util

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.random.asKotlinRandom

interface RandomProvider {
    fun nextInt(from: Int, until: Int): Int
    fun nextInt(until: Int): Int
    fun <T> shuffle(list: List<T>): List<T>
    fun <T> pickRandom(list: List<T>): T?
}

@Singleton
class DefaultRandomProvider @Inject constructor() : RandomProvider {
    private val random: Random = SecureRandom().asKotlinRandom()

    override fun nextInt(from: Int, until: Int): Int {
        require(until > from) { "until must be greater than from" }
        return random.nextInt(from, until)
    }

    override fun nextInt(until: Int): Int {
        require(until > 0) { "until must be positive" }
        return random.nextInt(until)
    }

    override fun <T> shuffle(list: List<T>): List<T> {
        return list.shuffled(random)
    }

    override fun <T> pickRandom(list: List<T>): T? {
        if (list.isEmpty()) return null
        return list[random.nextInt(list.size)]
    }
}
