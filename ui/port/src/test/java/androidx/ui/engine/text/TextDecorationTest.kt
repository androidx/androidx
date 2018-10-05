package androidx.ui.engine.text

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextDecorationTest {

    @Test
    fun `contains with single decoration`() {
        val textDecoration = TextDecoration.none
        assertTrue(textDecoration.contains(TextDecoration.none))
        assertFalse(textDecoration.contains(TextDecoration.lineThrough))
    }

    @Test
    fun `contains,combines with multiple decorations`() {
        val textDecoration =
            TextDecoration.combine(listOf(TextDecoration.underline, TextDecoration.lineThrough))
        assertTrue(textDecoration.contains(TextDecoration.underline))
        assertTrue(textDecoration.contains(TextDecoration.lineThrough))
        // since 0 is always included
        assertTrue(textDecoration.contains(TextDecoration.none))

        assertFalse(textDecoration.contains(TextDecoration.overline))
    }

    @Test
    fun `combine with empty list returns none`() {
        Assert.assertEquals(TextDecoration.none, TextDecoration.combine(listOf()))
    }

    @Test
    fun `combine with single element`() {
        Assert.assertEquals(
            TextDecoration.underline,
            TextDecoration.combine(listOf(TextDecoration.underline))
        )
    }

    @Test
    fun `toString with single decoration`() {
        assertEquals("TextDecoration.none", TextDecoration.none.toString())
        assertEquals("TextDecoration.underline", TextDecoration.underline.toString())
        assertEquals("TextDecoration.lineThrough", TextDecoration.lineThrough.toString())
        assertEquals("TextDecoration.overline", TextDecoration.overline.toString())
    }

    @Test
    fun `toString with empty combined`() {
        assertEquals("TextDecoration.none", TextDecoration.combine(listOf()).toString())
    }

    @Test
    fun `toString with single combined`() {
        assertEquals(
            "TextDecoration.lineThrough",
            TextDecoration.combine(listOf(TextDecoration.lineThrough)).toString()
        )
    }

    @Test
    fun `toString with multiple decorations`() {
        assertEquals(
            "TextDecoration.combine([underline, lineThrough])",
            TextDecoration.combine(
                listOf(
                    TextDecoration.underline,
                    TextDecoration.lineThrough
                )
            ).toString()
        )
    }
}