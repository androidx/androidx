package androidx.compose.foundation.text.modifiers

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextAnnotatedStringNodeTest {
    @get:Rule
    val rule = createComposeRule()
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun draw_whenNotAttached_doesNotCrash() {
        val subject = TextAnnotatedStringNode(
            AnnotatedString("text"), TextStyle.Default, createFontFamilyResolver(context)
        )
        rule.setContent {
            Canvas(Modifier.fillMaxSize()) {
                val contentDrawScope = object : ContentDrawScope, DrawScope by this {
                    override fun drawContent() {
                        fail("Not used")
                    }
                } as ContentDrawScope
                with(subject) {
                    contentDrawScope.draw()
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun exceedsMaxConstraintSize_doesNotCrash() {
        rule.setContent {
            val state = rememberScrollState()
            Column(Modifier.verticalScroll(state)) {
                BasicText(
                    text = AnnotatedString("text\n".repeat(10_000)),
                    style = TextStyle(fontSize = 50.sp),
                )
            }
        }
    }

    @Test
    fun movableContent_updateOnDetach_textIsUpdated() {
        var flag by mutableStateOf(false)

        rule.setContent {
            val content =
                remember {
                    movableContentOf {
                        BoxWithConstraints {
                            BasicText(
                                text = AnnotatedString(if (!flag) "" else "LOADED"),
                                modifier = Modifier.testTag("target")
                            )
                        }
                    }
                }

            key(flag) {
                content()
            }
        }

        val textLayout1 = rule.onNodeWithTag("target").fetchTextLayoutResult()
        assertEquals(0, textLayout1.size.width)

        flag = true

        val textLayout2 = rule.onNodeWithTag("target").fetchTextLayoutResult()
        assertNotEquals(0, textLayout2.size.width)
    }
}
