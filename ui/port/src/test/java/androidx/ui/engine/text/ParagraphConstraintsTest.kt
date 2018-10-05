package androidx.ui.engine.text

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphConstraintsTest {

    @Test
    fun `toString with values`() {
        val paragraphConstraints = ParagraphConstraints(width = 101.0)
        assertThat(
            paragraphConstraints.toString(),
            `is`(equalTo("ParagraphConstraints(width: 101.0)"))
        )
    }
}