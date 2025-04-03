package com.userspost.jsonplaceholder

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class TimeProvider {
    companion object {
        private val clock: Clock = Clock.systemUTC()
    }

    fun now() = Instant.now(clock)
}