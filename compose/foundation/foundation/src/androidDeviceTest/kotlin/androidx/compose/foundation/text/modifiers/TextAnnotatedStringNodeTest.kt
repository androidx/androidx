package androidx.compose.foundation.text.modifiers

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TEST_LOCALE_LIST
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.DrawAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextAnnotatedStringNodeTest {
    @get:Rule val rule = createComposeRule()
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    // TextAnnotatedStringNode.updateText ignores DrawAnnotation-only changes so animating or
    // toggling draw annotations only invalidates draw without re-measuring text. Verify that
    // filtering out DrawAnnotations when comparing annotations does not mask changes to other
    // layout-affecting annotations (such as Bullet) present on the same AnnotatedString.
    @Test
    fun updateText_whenBulletAnnotationChangesWithDrawAnnotationPresent_invalidatesLayout() {
        val noopDraw =
            object : DrawAnnotation() {
                override fun equals(other: Any?): Boolean = this === other

                override fun hashCode(): Int = 0
            }
        val first = buildAnnotatedString {
            append("Item")
            addBullet(Bullet.Default.copy(width = 4.sp), 0, length)
            addDrawAnnotation(noopDraw, 0, length)
        }
        val second = buildAnnotatedString {
            append("Item")
            addBullet(Bullet.Default.copy(width = 20.sp), 0, length)
            addDrawAnnotation(noopDraw, 0, length)
        }

        val subject =
            TextAnnotatedStringNode(
                first,
                TextStyle.Default,
                createFontFamilyResolver(context),
                TEST_LOCALE_LIST,
            )

        assertTrue(
            subject.updateText(second),
            "Filtering out DrawAnnotations in updateText must not ignore changes to layout-affecting Bullet annotations on the same AnnotatedString",
        )
    }

    @Test
    fun updateText_whenOnlyDrawAnnotationChanges_invalidatesDrawWithoutRemeasuring() {
        val drawACount = AtomicInteger(0)
        val drawBCount = AtomicInteger(0)
        val outerDrawCount = AtomicInteger(0)
        val textLayoutCount = AtomicInteger(0)
        val layoutPassCount = AtomicInteger(0)

        val drawA = DrawAnnotation(drawBehind = { drawACount.incrementAndGet() })
        val drawB = DrawAnnotation(drawBehind = { drawBCount.incrementAndGet() })

        val plain = AnnotatedString("Hello world")
        val withDrawA = buildAnnotatedString {
            append("Hello ")
            addDrawAnnotation(drawA, 6, 11)
            append("world")
        }
        val withDrawB = buildAnnotatedString {
            append("Hello ")
            addDrawAnnotation(drawB, 6, 11)
            append("world")
        }

        val node =
            TextAnnotatedStringNode(
                plain,
                TextStyle.Default,
                createFontFamilyResolver(context),
                TEST_LOCALE_LIST,
            )
        assertTrue(
            node.updateDraw(null, TextStyle.Default, withDrawA),
            "Adding a DrawAnnotation must report a draw-affecting change",
        )
        assertFalse(
            node.updateText(withDrawA),
            "Adding a DrawAnnotation as the sole annotation must not report a layout-affecting text change",
        )
        assertTrue(
            node.updateDraw(null, TextStyle.Default, withDrawB),
            "Replacing a DrawAnnotation must report a draw-affecting change",
        )
        assertFalse(
            node.updateText(withDrawB),
            "Replacing a DrawAnnotation must not report a layout-affecting text change",
        )
        assertFalse(
            node.updateDraw(null, TextStyle.Default, withDrawB),
            "Repeating the same DrawAnnotation must not report a draw-affecting change",
        )
        assertTrue(
            node.updateDraw(null, TextStyle.Default, plain),
            "Removing a DrawAnnotation must report a draw-affecting change",
        )
        assertFalse(
            node.updateText(plain),
            "Removing a DrawAnnotation must not report a layout-affecting text change",
        )

        var currentText by mutableStateOf(plain)
        rule.setContent {
            BasicText(
                text = currentText,
                onTextLayout = { textLayoutCount.incrementAndGet() },
                modifier =
                    Modifier.layout { measurable, constraints ->
                            layoutPassCount.incrementAndGet()
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .drawBehind { outerDrawCount.incrementAndGet() },
            )
        }

        rule.runOnIdle {
            assertEquals(1, textLayoutCount.get())
            assertEquals(1, layoutPassCount.get())
            assertEquals(0, drawACount.get())
            assertEquals(0, drawBCount.get())
            currentText = withDrawA
        }

        var drawAAfterFirst = 0
        rule.runOnIdle {
            assertEquals(1, textLayoutCount.get())
            assertEquals(1, layoutPassCount.get())
            drawAAfterFirst = drawACount.get()
            Truth.assertThat(drawAAfterFirst).isGreaterThan(0)
            assertEquals(0, drawBCount.get())
            currentText = withDrawB
        }

        var drawBAfterSecond = 0
        var outerDrawBeforeRemoval = 0
        rule.runOnIdle {
            assertEquals(1, textLayoutCount.get())
            assertEquals(1, layoutPassCount.get())
            assertEquals(drawAAfterFirst, drawACount.get())
            drawBAfterSecond = drawBCount.get()
            Truth.assertThat(drawBAfterSecond).isGreaterThan(0)
            outerDrawBeforeRemoval = outerDrawCount.get()
            currentText = plain
        }

        rule.runOnIdle {
            assertEquals(1, textLayoutCount.get())
            assertEquals(1, layoutPassCount.get())
            assertEquals(drawAAfterFirst, drawACount.get())
            assertEquals(drawBAfterSecond, drawBCount.get())
            Truth.assertThat(outerDrawCount.get()).isGreaterThan(outerDrawBeforeRemoval)
        }
    }

    @Test
    fun draw_whenNotAttached_doesNotCrash() {
        val subject =
            TextAnnotatedStringNode(
                AnnotatedString("text"),
                TextStyle.Default,
                createFontFamilyResolver(context),
                TEST_LOCALE_LIST,
            )
        rule.setContent {
            Canvas(Modifier.fillMaxSize()) {
                val contentDrawScope =
                    object : ContentDrawScope, DrawScope by this {
                        override fun drawContent() {
                            fail("Not used")
                        }
                    }
                        as ContentDrawScope
                with(subject) { contentDrawScope.draw() }
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
            val content = remember {
                movableContentOf {
                    BoxWithConstraints {
                        BasicText(
                            text = AnnotatedString(if (!flag) "" else "LOADED"),
                            modifier = Modifier.testTag("target"),
                        )
                    }
                }
            }

            key(flag) { content() }
        }

        val textLayout1 = rule.onNodeWithTag("target").fetchTextLayoutResult()
        assertEquals(0, textLayout1.size.width)

        flag = true

        val textLayout2 = rule.onNodeWithTag("target").fetchTextLayoutResult()
        assertNotEquals(0, textLayout2.size.width)
    }

    @Test
    fun setTextSubstitution_invalidatesDraw() {
        val drawCount = AtomicInteger(0)

        val subject =
            TextAnnotatedStringElement(
                AnnotatedString("til"),
                TextStyle.Default,
                createFontFamilyResolver(context),
                TEST_LOCALE_LIST,
            )

        val modifier =
            Modifier.fillMaxSize().drawBehind {
                drawRect(Color.Magenta, size = Size(100f, 100f))
                drawCount.incrementAndGet()
            } then subject

        rule.setContent { Box(modifier) }
        val initialCount = drawCount.get()
        rule.runOnIdle { Truth.assertThat(initialCount).isGreaterThan(0) }

        val node = rule.onNodeWithText("til").fetchSemanticsNode()
        rule.runOnIdle {
            node.config[SemanticsActions.SetTextSubstitution].action?.invoke(AnnotatedString("T"))
            node.config[SemanticsActions.ShowTextSubstitution].action?.invoke(true)
        }
        rule.runOnIdle { Truth.assertThat(drawCount.get()).isGreaterThan(initialCount) }
    }

    @Test
    fun setTextSubstitution_setsSemantics() {

        val subject =
            TextAnnotatedStringElement(
                AnnotatedString("til"),
                TextStyle.Default,
                createFontFamilyResolver(context),
                TEST_LOCALE_LIST,
            )

        rule.setContent { Box(Modifier.fillMaxSize() then subject) }

        val node = rule.onNodeWithText("til").fetchSemanticsNode()

        rule.runOnIdle {
            node.config[SemanticsActions.SetTextSubstitution].action?.invoke(AnnotatedString("T"))
            node.config[SemanticsActions.ShowTextSubstitution].action?.invoke(true)
        }
        val replacedNode = rule.onNodeWithText("til").fetchSemanticsNode()
        rule.runOnIdle {
            Truth.assertThat(replacedNode.config[SemanticsProperties.TextSubstitution].text)
                .isEqualTo("T")
        }
    }
}
