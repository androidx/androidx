package androidx.ui.engine.text

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextPositionTest {

    @Test
    fun `toString with empty combined`() {
        val textPosition = TextPosition(1, TextAffinity.downstream)
        assertEquals(
            "TextPosition(offset: 1, affinity: ${TextAffinity.downstream})",
            textPosition.toString()
        )
    }
}