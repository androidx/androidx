/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.appwidget

import android.graphics.Typeface
import android.os.Build
import android.text.SpannedString
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.Modifier
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.unit.Dp
import androidx.glance.unit.DpSize
import androidx.glance.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetReceiverTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @Before
    fun setUp() {
        // Reset the size mode to the default
        TestGlanceAppWidget.sizeMode = SizeMode.Single
    }

    @Test
    fun createSimpleAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            val density = LocalContext.current.resources.displayMetrics.density
            val size = LocalSize.current
            assertThat(size.width.value).isWithin(1 / density).of(40f)
            assertThat(size.height.value).isWithin(1 / density).of(40f)
            Text(
                "text content",
                style = TextStyle(
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                )
            )
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("text content")
            val content = child.text as SpannedString
            content.checkHasSingleTypedSpan<UnderlineSpan> { }
            content.checkHasSingleTypedSpan<StyleSpan> {
                assertThat(it.style).isEqualTo(Typeface.ITALIC)
            }
            content.checkHasSingleTypedSpan<TextAppearanceSpan> {
                assertThat(it.textFontWeight).isEqualTo(500)
            }
        }
    }

    @Test
    fun createExactAppWidget() {
        TestGlanceAppWidget.sizeMode = SizeMode.Exact
        TestGlanceAppWidget.uiDefinition = {
            val size = LocalSize.current
            Text("size = ${size.width} x ${size.height}")
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("size = 200.0.dp x 300.0.dp")
        }

        mHostRule.setLandscapeOrientation()
        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("size = 300.0.dp x 200.0.dp")
        }
    }

    @Test
    fun createResponsiveAppWidget() {
        TestGlanceAppWidget.sizeMode =
            SizeMode.Responsive(DpSize(100.dp, 150.dp), DpSize(250.dp, 150.dp))

        TestGlanceAppWidget.uiDefinition = {
            val size = LocalSize.current
            Text("size = ${size.width} x ${size.height}")
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("size = 100.0.dp x 150.0.dp")
        }

        mHostRule.setLandscapeOrientation()
        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("size = 250.0.dp x 150.0.dp")
        }

        mHostRule.setSizes(
            DpSize(50.dp, 100.dp), DpSize(100.dp, 50.dp),
            updateRemoteViews = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
        )

        mHostRule.setPortraitOrientation()
        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("size = 100.0.dp x 150.0.dp")
        }

        mHostRule.setLandscapeOrientation()
        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertThat(child.text.toString()).isEqualTo("size = 100.0.dp x 150.0.dp")
        }
    }

    @Test
    fun createTextWithFillMaxDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Text("expanded text", modifier = Modifier.fillMaxWidth().fillMaxHeight())
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0))
            assertViewSize(child, mHostRule.portraitSize)
        }
    }

    @Test
    fun createTextViewWithExactDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Text("expanded text", modifier = Modifier.width(150.dp).height(100.dp))
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0).getTargetView())
            assertViewSize(child, DpSize(150.dp, 100.dp))
        }
    }

    @Test
    fun createTextViewWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Text("expanded text", modifier = Modifier.fillMaxWidth().height(110.dp))
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<TextView>(hostView.getChildAt(0).getTargetView())
            assertViewSize(child, DpSize(mHostRule.portraitSize.width, 110.dp))
        }
    }

    @Test
    fun createBoxWithExactDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Box(modifier = Modifier.width(150.dp).height(180.dp)) {
                Text("Inside")
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertNotNull(
                hostView.findChild<RelativeLayout> {
                    it.id == R.id.glanceView
                }
            )
            assertViewSize(child, DpSize(150.dp, 180.dp))
        }
    }

    @Test
    fun createBoxWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Box(modifier = Modifier.width(150.dp).wrapContentHeight()) {
                Text("Inside")
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertNotNull(hostView.findChildByType<RelativeLayout>())
            val text = assertNotNull(child.findChildByType<TextView>())
            assertThat(child.height).isEqualTo(text.height)
            assertViewDimension(child, child.width, 150.dp)
        }
    }

    @Test
    fun createColumnWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = Modifier.width(150.dp).fillMaxHeight()) {
                Text("Inside 1")
                Text("Inside 2")
                Text("Inside 3")
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertNotNull(
                hostView.findChild<LinearLayout> {
                    it.orientation == LinearLayout.VERTICAL
                }
            )
            assertViewSize(child, DpSize(150.dp, mHostRule.portraitSize.height))
        }
    }

    @Test
    fun createRowWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Text("Inside 1")
                Text("Inside 2")
                Text("Inside 3")
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertNotNull(
                hostView.findChild<LinearLayout> {
                    it.orientation == LinearLayout.HORIZONTAL
                }
            )
            assertViewSize(child, DpSize(mHostRule.portraitSize.width, 200.dp))
        }
    }

    @Test
    fun createRowWithTwoTexts() {
        TestGlanceAppWidget.uiDefinition = {
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Text("Inside 1", modifier = Modifier.defaultWeight().height(100.dp))
                Text("Inside 2", modifier = Modifier.defaultWeight().fillMaxHeight())
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val row = assertIs<LinearLayout>(hostView.getChildAt(0))
            assertThat(row.orientation).isEqualTo(LinearLayout.HORIZONTAL)
            assertThat(row.childCount).isEqualTo(2)
            val child1 = assertIs<TextView>(row.getChildAt(0).getTargetView())
            val child2 = assertIs<TextView>(row.getChildAt(1))
            assertViewSize(child1, DpSize(mHostRule.portraitSize.width / 2, 100.dp))
            assertViewSize(
                child2,
                DpSize(mHostRule.portraitSize.width / 2, mHostRule.portraitSize.height),
            )
        }
    }

    @Test
    fun createColumnWithTwoTexts() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Text("Inside 1", modifier = Modifier.fillMaxWidth().defaultWeight())
                Text("Inside 2", modifier = Modifier.width(100.dp).defaultWeight())
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val row = assertIs<LinearLayout>(hostView.getChildAt(0))
            assertThat(row.orientation).isEqualTo(LinearLayout.VERTICAL)
            assertThat(row.childCount).isEqualTo(2)
            val child1 = assertIs<TextView>(row.getChildAt(0))
            val child2 = assertIs<TextView>(row.getChildAt(1).getTargetView())
            assertViewSize(
                child1,
                DpSize(mHostRule.portraitSize.width, mHostRule.portraitSize.height / 2),
            )
            assertViewSize(child2, DpSize(100.dp, mHostRule.portraitSize.height / 2))
        }
    }

    @Test
    fun createColumnWithTwoTexts2() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Text("Inside 1", modifier = Modifier.fillMaxWidth().defaultWeight())
                Text("Inside 2", modifier = Modifier.width(100.dp).fillMaxHeight())
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val row = assertIs<LinearLayout>(hostView.getChildAt(0))
            assertThat(row.orientation).isEqualTo(LinearLayout.VERTICAL)
            assertThat(row.childCount).isEqualTo(2)
            val child1 = assertIs<TextView>(row.getChildAt(0))
            val child2 = assertIs<TextView>(row.getChildAt(1).getTargetView())
            assertViewSize(
                child1,
                DpSize(mHostRule.portraitSize.width, 0.dp),
            )
            assertViewSize(child2, DpSize(100.dp, mHostRule.portraitSize.height))
        }
    }

    // Check there is a single span of the given type and that it passes the [check].
    private inline fun <reified T> SpannedString.checkHasSingleTypedSpan(check: (T) -> Unit) {
        val spans = getSpans(0, length, T::class.java)
        assertThat(spans).hasLength(1)
        check(spans[0])
    }

    private fun assertViewSize(view: View, expectedSize: DpSize) {
        val density = view.context.resources.displayMetrics.density
        assertThat(view.width / density).isWithin(1.1f / density).of(expectedSize.width.value)
        assertThat(view.height / density).isWithin(1.1f / density).of(expectedSize.height.value)
    }

    private fun assertViewDimension(view: View, sizePx: Int, expectedSize: Dp) {
        val density = view.context.resources.displayMetrics.density
        assertThat(sizePx / density).isWithin(1.1f / density).of(expectedSize.value)
    }
}

// Extract the target view if it is a complex view in Android R-.
private fun View.getTargetView(): View {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return this
    }
    val layout = assertIs<RelativeLayout>(this)
    assertThat(layout.childCount).isEqualTo(2)
    return layout.getChildAt(1)
}
