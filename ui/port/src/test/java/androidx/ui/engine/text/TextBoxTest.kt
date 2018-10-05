package androidx.ui.engine.text

import androidx.ui.engine.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextBoxTest {

    @Test
    fun `toRect`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertEquals(Rect.fromLTRB(1.0, 2.0, 3.0, 4.0), textBox.toRect())
    }

    @Test
    fun `start for LTR`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertEquals(1.0, textBox.start(), 0.0)
    }

    @Test
    fun `start for RTL`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.RTL)
        assertEquals(3.0, textBox.start(), 0.0)
    }

    @Test
    fun `end for LTR`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertEquals(3.0, textBox.end(), 0.0)
    }

    @Test
    fun `end for RTL`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.RTL)
        assertEquals(1.0, textBox.end(), 0.0)
    }

    @Test
    fun `fromLTRBD`() {
        val textBox = TextBox.fromLTRBD(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertEquals(Rect.fromLTRB(1.0, 2.0, 3.0, 4.0), textBox.toRect())
        assertEquals(TextDirection.LTR, textBox.direction)
    }

    @Test
    fun `toString `() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertEquals(
            "TextBox.fromLTRBD(1.0, 2.0, 3.0, 4.0, ${TextDirection.LTR})",
            textBox.toString()
        )
    }
}