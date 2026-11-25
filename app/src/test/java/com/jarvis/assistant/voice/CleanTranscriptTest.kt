package com.jarvis.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanTranscriptTest {
    private fun c(s: String) = VoiceInput.clean(s)

    @Test fun stripsWakeWordVariants() {
        assertEquals("Send my wife a message", c("hey sora send my wife a message"))
        assertEquals("Turn on the flashlight", c("Hey Sarah turn on the flashlight"))
        assertEquals("Open YouTube", c("Sora, open YouTube"))
        assertEquals("Call mum", c("okay hey so ra call mum"))
        assertEquals("", c("Hey Zora."))
    }

    @Test fun stripsOnlyTheDetectedCustomPhraseAtTheStart() {
        assertEquals("Open maps", VoiceInput.clean("Hello computer, open maps", "Hello Computer"))
        assertEquals("Open maps", VoiceInput.clean("computer open maps", "Hello Computer"))
        assertEquals("Call the computer shop", VoiceInput.clean("Call the computer shop", "Computer"))
        assertEquals("Computer repair", VoiceInput.clean("computer repair"))
    }

    @Test fun stripsOwnPrompt() {
        assertEquals("Open settings", c("Sora yes? open settings"))
        assertEquals("Open settings", c("yes, open settings"))
    }

    @Test fun leavesRealCommandsAlone() {
        assertEquals("Text Travis that I'm late", c("text Travis that I'm late"))
        assertEquals("Text Sarah that I'm late", c("text Sarah that I'm late"))
        assertEquals("Sort my emails", c("sort my emails"))
        assertEquals("Turn on the flashlight", c("turn on the flashlight"))
        assertEquals("Play the harvest playlist", c("play the harvest playlist"))
    }

    private val reply = VoiceInput.words("Done. I sent your wife a message on WhatsApp saying I love you.").toSet()

    @Test
    fun jarvissOwnEchoIsNotAnInterruption() {
        // What the recogniser makes of Jarvis's own voice, including small mishearings.
        assertFalse(VoiceInput.isBargeIn("DONE I SENT YOUR WIFE A MESSAGE", reply))
        assertFalse(VoiceInput.isBargeIn("SENT YOUR WIVE A MESSAGE ON WHATSAP", reply))
        assertFalse(VoiceInput.isBargeIn("", reply))
    }

    @Test
    fun talkingOverJarvisIsAnInterruption() {
        assertTrue(VoiceInput.isBargeIn("NO SEND IT TO MY BROTHER", reply))
        assertTrue("one stop word is enough", VoiceInput.isBargeIn("STOP", reply))
        assertTrue(VoiceInput.isBargeIn("I SENT YOUR WIFE WAIT", reply))
    }

    @Test
    fun echoAtTheStartOfAnInterruptionIsDropped() {
        assertEquals("No send it to my brother", VoiceInput.stripEcho("message on WhatsApp No send it to my brother", reply))
        assertEquals("Call mum", VoiceInput.stripEcho("Call mum", reply))
        assertEquals("Call mum", VoiceInput.stripEcho("Call mum", null))
    }

    @Test
    fun stopPhrasesAreRecognised() {
        listOf("Stop", "stop it", "Never mind", "Shut up.", "Okay, stop", "that's enough").forEach {
            assertTrue(it, VoiceInput.isStopCommand(it))
        }
        listOf("Stop the music", "Cancel my alarm", "Wait for me").forEach { assertFalse(it, VoiceInput.isStopCommand(it)) }
    }

    @Test
    fun partsOfJarvissOwnWordsAreEcho() {
        val spoken = VoiceInput.words("I set a stopwatch for ten minutes on WhatsApp.").toSet()
        assertFalse("'stop' is the start of 'stopwatch'", VoiceInput.isBargeIn("I SET A STOP", spoken))
        assertFalse(VoiceInput.isBargeIn("WHAT'S APP", spoken))
    }

    @Test
    fun stopKeywordIsOffWhenJarvisSaysSomethingLikeIt() {
        assertTrue(VoiceInput.soundsLikeStop("The top story is about the stock market."))
        assertTrue(VoiceInput.soundsLikeStop("Your next stop is Oxford Street."))
        assertTrue(VoiceInput.soundsLikeStop("Let me start the timer."))
        assertFalse(VoiceInput.soundsLikeStop("Here is what I found on the weather today."))
        assertFalse(VoiceInput.soundsLikeStop("Done. I sent your wife a message on WhatsApp."))
    }
}
