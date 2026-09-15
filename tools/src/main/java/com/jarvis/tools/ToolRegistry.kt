package com.jarvis.tools

import com.jarvis.core.result.Result
import com.jarvis.security.RiskTier
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal interface for all executable tools in the JARVIS ecosystem.
 */
interface JarvisTool {
    val id: String
    val name: String
    val description: String
    val riskTier: RiskTier

    suspend fun execute(params: Map<String, Any?> = emptyMap()): Result<Any?>
}

/**
 * Thread-safe registry maintaining available system, assistant, and integration tools.
 */
interface ToolRegistry {
    fun register(tool: JarvisTool)
    fun unregister(id: String)
    fun get(id: String): JarvisTool?
    fun getAll(): List<JarvisTool>
}

/**
 * Default in-memory implementation of [ToolRegistry].
 */
class InMemoryToolRegistry : ToolRegistry {
    private val tools = ConcurrentHashMap<String, JarvisTool>()

    override fun register(tool: JarvisTool) {
        tools[tool.id] = tool
    }

    override fun unregister(id: String) {
        tools.remove(id)
    }

    override fun get(id: String): JarvisTool? = tools[id]

    override fun getAll(): List<JarvisTool> = tools.values.toList()
}
