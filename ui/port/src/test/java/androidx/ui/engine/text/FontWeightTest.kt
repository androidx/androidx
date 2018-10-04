package androidx.ui.engine.text

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontWeightTest {

    @Test
    fun `lerp with null parameters`() {
        Assert.assertEquals(FontWeight.normal, FontWeight.lerp(null, null, 0.0))
    }

    @Test
    fun `lerp with one null parameter`() {
        Assert.assertEquals(FontWeight.w300, FontWeight.lerp(FontWeight.w200, null, 0.5))
        Assert.assertEquals(FontWeight.w300, FontWeight.lerp(null, FontWeight.w200, 0.5))
    }

    @Test
    fun `lerp at start`() {
        Assert.assertEquals(
            FontWeight.w200,
            FontWeight.lerp(FontWeight.w200, FontWeight.normal, 0.0)
        )
    }

    @Test
    fun `lerp at end`() {
        Assert.assertEquals(
            FontWeight.w400,
            FontWeight.lerp(FontWeight.w200, FontWeight.normal, 1.0)
        )
    }

    @Test
    fun `lerp in the mid-time`() {
        Assert.assertEquals(FontWeight.w500, FontWeight.lerp(FontWeight.w200, FontWeight.w800, 0.5))
    }

    @Test
    fun `lerp in the mid-time with odd distance should be rounded to up`() {
        Assert.assertEquals(FontWeight.w600, FontWeight.lerp(FontWeight.w200, FontWeight.w900, 0.5))
    }

    @Test
    fun `toString return FontsWeight`() {
        Assert.assertEquals("FontWeight.w100", FontWeight.w100.toString())
        Assert.assertEquals("FontWeight.w900", FontWeight.w900.toString())
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
        Assert.assertEquals(expectedValues, FontWeight.values)
    }
}