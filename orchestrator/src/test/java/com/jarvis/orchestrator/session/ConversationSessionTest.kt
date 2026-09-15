package com.jarvis.orchestrator.session

import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.TurnRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSessionTest {

    @Test
    fun `turns within capacity are preserved in chronological order`() {
        val session = DefaultConversationSession(maxTurns = 5)

        val turn1 = ConversationTurn(role = TurnRole.USER, text = "Hello")
        val turn2 = ConversationTurn(role = TurnRole.ASSISTANT, text = "Hello Sir.")
        val turn3 = ConversationTurn(role = TurnRole.USER, text = "How are you?")

        session.addTurn(turn1)
        session.addTurn(turn2)
        session.addTurn(turn3)

        val recent = session.getRecentTurns()
        assertEquals(3, recent.size)
        assertEquals(turn1, recent[0])
        assertEquals(turn2, recent[1])
        assertEquals(turn3, recent[2])
    }

    @Test
    fun `exceeding capacity drops oldest turns maintaining bounded window`() {
        val session = DefaultConversationSession(maxTurns = 3)

        val turns = (1..5).map { i ->
            ConversationTurn(
                role = if (i % 2 == 1) TurnRole.USER else TurnRole.ASSISTANT,
                text = "Turn $i"
            )
        }

        turns.forEach { session.addTurn(it) }

        val recent = session.getRecentTurns()
        assertEquals(3, recent.size)
        assertEquals("Turn 3", recent[0].text)
        assertEquals("Turn 4", recent[1].text)
        assertEquals("Turn 5", recent[2].text)
    }

    @Test
    fun `clear empties all turns from session`() {
        val session = DefaultConversationSession(maxTurns = 5)
        session.addTurn(ConversationTurn(role = TurnRole.USER, text = "Test"))
        assertEquals(1, session.getRecentTurns().size)

        session.clear()

        assertTrue(session.getRecentTurns().isEmpty())
        assertTrue(session.turns.value.isEmpty())
    }
}
