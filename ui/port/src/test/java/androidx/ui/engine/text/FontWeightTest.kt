package androidx.ui.engine.text

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontWeightTest {

    @Test
    fun `lerp with null parameters`() {
        assertThat(FontWeight.lerp(null, null, 0.0), `is`(equalTo(FontWeight.normal)))
    }

    @Test
    fun `lerp with one null parameter should use normal for null value`() {
        assertThat(FontWeight.lerp(FontWeight.w200, null, 0.5), `is`(equalTo(FontWeight.w300)))
        assertThat(FontWeight.lerp(null, FontWeight.w200, 0.5), `is`(equalTo(FontWeight.w300)))
    }

    @Test
    fun `lerp at start returns start value`() {
        assertThat(
            FontWeight.lerp(FontWeight.w200, FontWeight.w400, 0.0),
            `is`(equalTo(FontWeight.w200))
        )
    }

    @Test
    fun `lerp at end returns end value`() {
        assertThat(
            FontWeight.lerp(FontWeight.w200, FontWeight.w400, 1.0),
            `is`(equalTo(FontWeight.w400))
        )
    }

    @Test
    fun `lerp in the mid-time`() {
        assertThat(
            FontWeight.lerp(FontWeight.w200, FontWeight.w800, 0.5),
            `is`(equalTo(FontWeight.w500))
        )
    }

    @Test
    fun `lerp in the mid-time with odd distance should be rounded to up`() {
        assertThat(
            FontWeight.lerp(FontWeight.w200, FontWeight.w900, 0.5),
            `is`(equalTo(FontWeight.w600))
        )
    }

    @Test
    fun `toString return FontsWeight`() {
        assertThat(FontWeight.w100.toString(), `is`(equalTo("FontWeight.w100")))
        assertThat(FontWeight.w900.toString(), `is`(equalTo("FontWeight.w900")))
    }

    @Test
    fun `values return all weights`() {
        val expectedValues = listOf(
            FontWeight.w100,
            FontWeight.w200,
            FontWeight.w300,
            FontWeight.w400,
            FontWeight.w500,
            FontWeight.w600,
            FontWeight.w700,
            FontWeight.w800,
            FontWeight.w900
        )
        assertThat(FontWeight.values, `is`(equalTo(expectedValues)))
    }
}