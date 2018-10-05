package androidx.ui.engine.text

import androidx.ui.engine.geometry.Rect
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextBoxTest {

    @Test
    fun `toRect`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertThat(textBox.toRect(), `is`(equalTo(Rect.fromLTRB(1.0, 2.0, 3.0, 4.0))))
    }

    @Test
    fun `start for LTR`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertThat(textBox.start(), `is`(equalTo(1.0)))
    }

    @Test
    fun `start for RTL`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.RTL)
        assertThat(textBox.start(), `is`(equalTo(3.0)))
    }

    @Test
    fun `end for LTR`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertThat(textBox.end(), `is`(equalTo(3.0)))
    }

    @Test
    fun `end for RTL`() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.RTL)
        assertThat(textBox.end(), `is`(equalTo(1.0)))
    }

    @Test
    fun `fromLTRBD`() {
        val textBox = TextBox.fromLTRBD(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertThat(textBox.toRect(), `is`(equalTo(Rect.fromLTRB(1.0, 2.0, 3.0, 4.0))))
        assertThat(textBox.direction, `is`(equalTo(TextDirection.LTR)))
    }

    @Test
    fun `toString `() {
        val textBox = TextBox(1.0, 2.0, 3.0, 4.0, TextDirection.LTR)
        assertThat(
            textBox.toString(),
            `is`(equalTo("TextBox.fromLTRBD(1.0, 2.0, 3.0, 4.0, ${TextDirection.LTR})"))
        )
    }
}