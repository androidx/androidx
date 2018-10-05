package androidx.ui.engine.text

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
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
        assertThat(TextDecoration.combine(listOf()), `is`(equalTo(TextDecoration.none)))
    }

    @Test
    fun `combine with single element`() {
        assertThat(
            TextDecoration.combine(listOf(TextDecoration.underline)),
            `is`(equalTo(TextDecoration.underline))
        )
    }

    @Test
    fun `toString with single decoration`() {
        assertThat(TextDecoration.none.toString(), `is`(equalTo("TextDecoration.none")))
        assertThat(TextDecoration.underline.toString(), `is`(equalTo("TextDecoration.underline")))
        assertThat(
            TextDecoration.lineThrough.toString(),
            `is`(equalTo("TextDecoration.lineThrough"))
        )
        assertThat(TextDecoration.overline.toString(), `is`(equalTo("TextDecoration.overline")))
    }

    @Test
    fun `toString with empty combined`() {
        assertThat(
            TextDecoration.combine(listOf()).toString(),
            `is`(equalTo("TextDecoration.none"))
        )
    }

    @Test
    fun `toString with single combined`() {
        assertThat(
            TextDecoration.combine(listOf(TextDecoration.lineThrough)).toString(),
            `is`(equalTo("TextDecoration.lineThrough"))
        )
    }

    @Test
    fun `toString with multiple decorations`() {
        assertThat(
            TextDecoration.combine(
                listOf(
                    TextDecoration.underline,
                    TextDecoration.lineThrough
                )
            ).toString(),
            `is`(equalTo("TextDecoration.combine([underline, lineThrough])"))
        )
    }
}