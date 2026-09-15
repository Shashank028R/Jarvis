package com.jarvis.memory

import com.jarvis.core.result.Result
import java.util.concurrent.ConcurrentHashMap

data class MemoryEntry(
    val id: String,
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Storage contract for user memory and preference persistence.
 */
interface MemoryStore {
    suspend fun put(key: String, value: String): Result<Unit>
    suspend fun get(key: String): Result<String?>
}

/**
 * In-memory fallback implementation for V1 foundation (encrypted SQLite added in V5).
 */
class InMemoryMemoryStore : MemoryStore {
    private val store = ConcurrentHashMap<String, String>()

    override suspend fun put(key: String, value: String): Result<Unit> {
        store[key] = value
        return Result.success(Unit)
    }

    override suspend fun get(key: String): Result<String?> {
        return Result.success(store[key])
    }
}
