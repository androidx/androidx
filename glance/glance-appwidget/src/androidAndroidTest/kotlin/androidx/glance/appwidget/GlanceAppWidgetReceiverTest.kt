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

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.SpannedString
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.action.toParametersKey
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.test.R
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetReceiverTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    val context = InstrumentationRegistry.getInstrumentation().targetContext!!

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

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("text content")
            val content = textView.text as SpannedString
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

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("size = 200.0.dp x 300.0.dp")
        }

        mHostRule.setLandscapeOrientation()
        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("size = 300.0.dp x 200.0.dp")
        }
    }

    @Test
    fun createResponsiveAppWidget() {
        TestGlanceAppWidget.sizeMode =
            SizeMode.Responsive(setOf(DpSize(100.dp, 150.dp), DpSize(250.dp, 150.dp)))

        TestGlanceAppWidget.uiDefinition = {
            val size = LocalSize.current
            Text("size = ${size.width} x ${size.height}")
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("size = 100.0.dp x 150.0.dp")
        }

        mHostRule.setLandscapeOrientation()
        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("size = 250.0.dp x 150.0.dp")
        }

        mHostRule.setSizes(
            DpSize(50.dp, 100.dp), DpSize(100.dp, 50.dp),
            updateRemoteViews = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
        )

        mHostRule.setPortraitOrientation()
        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("size = 100.0.dp x 150.0.dp")
        }

        mHostRule.setLandscapeOrientation()
        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.text.toString()).isEqualTo("size = 100.0.dp x 150.0.dp")
        }
    }

    @Test
    fun createTextWithFillMaxDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Text("expanded text", modifier = GlanceModifier.fillMaxWidth().fillMaxHeight())
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertViewSize(textView, mHostRule.portraitSize)
        }
    }

    @Test
    fun createTextViewWithExactDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Text("expanded text", modifier = GlanceModifier.width(150.dp).height(100.dp))
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertViewSize(textView, DpSize(150.dp, 100.dp))
        }
    }

    @Test
    fun createTextViewWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Text("expanded text", modifier = GlanceModifier.fillMaxWidth().height(110.dp))
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertViewSize(textView, DpSize(mHostRule.portraitSize.width, 110.dp))
        }
    }

    @Test
    fun createBoxWithExactDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Box(modifier = GlanceModifier.width(150.dp).height(180.dp)) {
                Text("Inside")
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            assertThat(box.notGoneChildCount).isEqualTo(1)
            assertViewSize(box, DpSize(150.dp, 180.dp))
        }
    }

    @Test
    fun createBoxWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Box(modifier = GlanceModifier.width(150.dp).wrapContentHeight()) {
                Text("Inside")
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            val text = assertNotNull(box.findChild<TextView> { it.text.toString() == "Inside" })
            assertThat(box.height).isEqualTo(text.height)
            assertViewDimension(box, box.width, 150.dp)
        }
    }

    @Test
    fun createColumnWithMixedDimensions() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = GlanceModifier.width(150.dp).fillMaxHeight()) {
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
            Row(modifier = GlanceModifier.fillMaxWidth().height(200.dp)) {
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
            Row(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                Text("Inside 1", modifier = GlanceModifier.defaultWeight().height(100.dp))
                Text("Inside 2", modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<LinearLayout> { row ->
            assertThat(row.orientation).isEqualTo(LinearLayout.HORIZONTAL)
            assertThat(row.notGoneChildCount).isEqualTo(2)
            val children = row.notGoneChildren.toList()
            val child1 = children[0].getTargetView<TextView>()
            val child2 = assertIs<TextView>(children[1])
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
            Column(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                Text("Inside 1", modifier = GlanceModifier.fillMaxWidth().defaultWeight())
                Text("Inside 2", modifier = GlanceModifier.width(100.dp).defaultWeight())
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<LinearLayout> { column ->
            assertThat(column.orientation).isEqualTo(LinearLayout.VERTICAL)
            assertThat(column.notGoneChildCount).isEqualTo(2)
            val children = column.notGoneChildren.toList()
            val child1 = assertIs<TextView>(children[0])
            val child2 = children[1].getTargetView<TextView>()
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
            Column(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                Text("Inside 1", modifier = GlanceModifier.fillMaxWidth().defaultWeight())
                Text("Inside 2", modifier = GlanceModifier.width(100.dp).fillMaxHeight())
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<LinearLayout> { column ->
            assertThat(column.orientation).isEqualTo(LinearLayout.VERTICAL)
            assertThat(column.notGoneChildCount).isEqualTo(2)
            val children = column.notGoneChildren.toList()
            val child1 = assertIs<TextView>(children[0])
            val child2 = children[1].getTargetView<TextView>()
            assertViewSize(
                child1,
                DpSize(mHostRule.portraitSize.width, 0.dp),
            )
            assertViewSize(child2, DpSize(100.dp, mHostRule.portraitSize.height))
        }
    }

    @Test
    fun createButton() {
        TestGlanceAppWidget.uiDefinition = {
            Button("Button", onClick = actionStartActivity<Activity>(), enabled = false)
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<Button> { button ->
            assertThat(button.text).isEqualTo("Button")
            assertThat(button.isEnabled).isFalse()
            assertThat(button.hasOnClickListeners()).isFalse()
        }
    }

    @Test
    fun createImage() {
        TestGlanceAppWidget.uiDefinition = {
            Image(provider = ImageProvider(R.drawable.oval), contentDescription = "oval")
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<ImageView> { image ->
            assertThat(image.contentDescription).isEqualTo("oval")
            val gradientDrawable = assertIs<GradientDrawable>(image.drawable)
            assertThat(gradientDrawable.shape).isEqualTo(GradientDrawable.OVAL)
        }
    }

    @Test
    fun drawableBackground() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "Some useful text",
                modifier = GlanceModifier.fillMaxWidth().height(220.dp)
                    .background(ImageProvider(R.drawable.oval))
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<TextView> { textView ->
            assertThat(textView.background).isNotNull()
        }
    }

    @Test
    fun drawableFitBackground() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "Some useful text",
                modifier = GlanceModifier.fillMaxWidth().height(220.dp)
                    .background(ImageProvider(R.drawable.oval), contentScale = ContentScale.Fit)
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            assertThat(box.notGoneChildCount).isEqualTo(2)
            val (boxedImage, boxedText) = box.notGoneChildren.toList()
            val image = boxedImage.getTargetView<ImageView>()
            val text = boxedText.getTargetView<TextView>()
            assertThat(image.drawable).isNotNull()
            assertThat(text.background).isNull()
        }
    }

    @Test
    fun bitmapBackground() {
        TestGlanceAppWidget.uiDefinition = {
            val context = LocalContext.current
            val bitmap =
                (context.resources.getDrawable(R.drawable.compose, null) as BitmapDrawable).bitmap
            Text(
                "Some useful text",
                modifier = GlanceModifier.fillMaxSize()
                    .background(ImageProvider(bitmap))
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            assertThat(box.notGoneChildCount).isEqualTo(2)
            val (boxedImage, boxedText) = box.notGoneChildren.toList()
            val image = boxedImage.getTargetView<ImageView>()
            val text = boxedText.getTargetView<TextView>()
            assertIs<BitmapDrawable>(image.drawable)
            assertThat(text.background).isNull()
        }
    }

    @Test
    fun removeAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            Text("something")
        }

        mHostRule.startHost()

        val appWidgetManager = GlanceAppWidgetManager(context)
        val glanceId = runBlocking {
            appWidgetManager.getGlanceIds(TestGlanceAppWidget::class.java).single()
        }

        runBlocking {
            updateAppWidgetState(context, glanceId) {
                it[testKey] = 3
            }
        }

        val fileKey = createUniqueRemoteUiName((glanceId as AppWidgetId).appWidgetId)
        val preferencesFile = PreferencesGlanceStateDefinition.getLocation(context, fileKey)

        assertThat(preferencesFile.exists())

        val deleteLatch = CountDownLatch(1)
        TestGlanceAppWidget.setOnDeleteBlock {
            deleteLatch.countDown()
        }

        mHostRule.removeAppWidget()

        deleteLatch.await(5, TimeUnit.SECONDS)
        val interval = 200L
        for (timeout in 0..2000L step interval) {
            if (!preferencesFile.exists()) return
            Thread.sleep(interval)
        }
        assertWithMessage("View state file exists").that(preferencesFile.exists())
            .isFalse()
    }

    @Test
    fun updateAll() {
        TestGlanceAppWidget.uiDefinition = {
            Text("before")
        }

        mHostRule.startHost()

        val didRun = AtomicBoolean(false)
        TestGlanceAppWidget.uiDefinition = {
            didRun.set(true)
            Text("after")
        }

        runBlocking {
            TestGlanceAppWidget.updateAll(context)
        }
        assertThat(didRun.get()).isTrue()
    }

    @Test
    fun updateIf() {
        TestGlanceAppWidget.uiDefinition = {
            Text("before")
        }

        mHostRule.startHost()

        val appWidgetManager = GlanceAppWidgetManager(context)
        runBlocking {
            appWidgetManager.getGlanceIds(TestGlanceAppWidget::class.java)
                .forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) {
                        it[testKey] = 2
                    }
                }
        }

        // Make sure the app widget is updated if the test is true
        val didRun = AtomicBoolean(false)
        TestGlanceAppWidget.uiDefinition = {
            didRun.set(true)
            Text("after")
        }
        runBlocking {
            TestGlanceAppWidget.updateIf<Preferences>(context) { prefs ->
                prefs[testKey] == 2
            }
        }

        assertThat(didRun.get()).isTrue()

        // Make sure it is not if the test is false
        didRun.set(false)
        runBlocking {
            TestGlanceAppWidget.updateIf<Preferences>(context) { prefs ->
                prefs[testKey] == 3
            }
        }

        assertThat(didRun.get()).isFalse()
    }

    @Test
    fun viewState() {
        TestGlanceAppWidget.uiDefinition = {
            val value = currentState<Preferences>()[testKey] ?: -1
            Text("Value = $value")
        }

        mHostRule.startHost()

        val appWidgetId = AtomicReference<GlanceId>()
        mHostRule.onHostView { view ->
            appWidgetId.set(AppWidgetId(view.appWidgetId))
        }

        runBlocking {
            updateAppWidgetState(context, appWidgetId.get()) {
                it[testKey] = 2
            }

            val prefs =
                TestGlanceAppWidget.getAppWidgetState<Preferences>(context, appWidgetId.get())
            assertThat(prefs[testKey]).isEqualTo(2)
        }
    }

    @Test
    fun actionCallback() {
        TestGlanceAppWidget.uiDefinition = {
            Column {
                Text(
                    "text1",
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<CallbackTest>(actionParametersOf(CallbackTest.key to 1))
                    )
                )
                Text(
                    "text2",
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<CallbackTest>(actionParametersOf(CallbackTest.key to 2))
                    )
                )
            }
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(2)
        mHostRule.onHostView { root ->
            checkNotNull(root.findChild<TextView> { it.text == "text1" }).performClick()
            checkNotNull(root.findChild<TextView> { it.text == "text2" }).performClick()
        }
        assertThat(CallbackTest.latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(CallbackTest.received.get()).containsExactly(1, 2)
    }

    @Test
    fun multipleActionCallback() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "text1",
                modifier = GlanceModifier.clickable(
                    actionRunCallback<CallbackTest>(actionParametersOf(CallbackTest.key to 1))
                ).clickable(
                    actionRunCallback<CallbackTest>(actionParametersOf(CallbackTest.key to 2))
                )
            )
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(1)
        mHostRule.onHostView { root ->
            checkNotNull(root.findChild<TextView> { it.text == "text1" }).performClick()
        }
        assertThat(CallbackTest.latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(CallbackTest.received.get()).containsExactly(2)
    }

    @Test
    fun wrapAroundFillMaxSize() {
        TestGlanceAppWidget.uiDefinition = {
            val wrapperModifier = GlanceModifier
                .background(ColorProvider(Color.LightGray))
                .fillMaxSize()
                .padding(8.dp)
            Column(modifier = wrapperModifier) {
                val boxModifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
                BoxRowBox(modifier = boxModifier, text = "Text 1")
                BoxRowBox(modifier = boxModifier, text = "Text 2")
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<LinearLayout> { column ->
            val displayMetrics = column.context.resources.displayMetrics
            val targetHeight = (column.height.pixelsToDp(displayMetrics) - 16.dp) / 2
            val targetWidth = column.width.pixelsToDp(displayMetrics) - 16.dp

            val text1 = checkNotNull(column.findChild<TextView> { it.text == "Text 1" })
            val row1 = text1.getParentView<FrameLayout>().getParentView<LinearLayout>()
            assertThat(row1.orientation).isEqualTo(LinearLayout.HORIZONTAL)
            assertViewSize(row1, DpSize(targetWidth, targetHeight))

            val text2 = checkNotNull(column.findChild<TextView> { it.text == "Text 2" })
            val row2 = text2.getParentView<FrameLayout>().getParentView<LinearLayout>()
            assertThat(row2.orientation).isEqualTo(LinearLayout.HORIZONTAL)
            assertThat(row2.height).isGreaterThan(20.dp.toPixels(context))
            assertViewSize(row2, DpSize(targetWidth, targetHeight))
        }
    }

    @Test
    fun compoundButtonAction() {
        val checkbox = "checkbox"
        val switch = "switch"

        TestGlanceAppWidget.uiDefinition = {
            Column {
                CheckBox(
                    checked = false,
                    onCheckedChange = actionRunCallback<CompoundButtonActionTest>(
                        actionParametersOf(CompoundButtonActionTest.key to checkbox)
                    ),
                    text = checkbox
                )
                Switch(
                    checked = true,
                    onCheckedChange = actionRunCallback<CompoundButtonActionTest>(
                        actionParametersOf(CompoundButtonActionTest.key to switch)
                    ),
                    text = switch
                )
            }
        }

        mHostRule.startHost()

        CompoundButtonActionTest.received.set(emptyList())
        CompoundButtonActionTest.latch = CountDownLatch(2)
        mHostRule.onHostView { root ->
            checkNotNull(root.findChild<TextView> { it.text == checkbox })
                .performCompoundButtonClick()
            checkNotNull(root.findChild<TextView> { it.text == switch })
                .performCompoundButtonClick()
        }
        CompoundButtonActionTest.latch.await(5, TimeUnit.SECONDS)
        assertThat(CompoundButtonActionTest.received.get()).containsExactly(
            checkbox to true, switch to false
        )
    }

    @Test
    fun radioActionCallback() {
        TestGlanceAppWidget.uiDefinition = {
            RadioButton(
                checked = true,
                onClick = actionRunCallback<CallbackTest>(
                    actionParametersOf(CallbackTest.key to 2)
                ),
                text = "text1"
            )
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(1)
        mHostRule.onHostView { root ->
            checkNotNull(root.findChild<TextView> { it.text == "text1" })
                .performCompoundButtonClick()
        }
        assertThat(CallbackTest.latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(CallbackTest.received.get()).containsExactly(2)
    }

    // Check there is a single span of the given type and that it passes the [check].
    private inline
    fun <reified T> SpannedString.checkHasSingleTypedSpan(check: (T) -> Unit) {
        val spans = getSpans(0, length, T::class.java)
        assertThat(spans).hasLength(1)
        check(spans[0])
    }

    private fun assertViewSize(view: View, expectedSize: DpSize) {
        val density = view.context.resources.displayMetrics.density
        assertThat(view.width / density).isWithin(1.1f / density)
            .of(expectedSize.width.value)
        assertThat(view.height / density).isWithin(1.1f / density)
            .of(expectedSize.height.value)
    }

    private fun assertViewDimension(view: View, sizePx: Int, expectedSize: Dp) {
        val density = view.context.resources.displayMetrics.density
        assertThat(sizePx / density).isWithin(1.1f / density).of(expectedSize.value)
    }
}

private val testKey = intPreferencesKey("testKey")

internal class CallbackTest : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val value = checkNotNull(parameters[key])
        received.update { it + value }
        latch.countDown()
    }

    companion object {
        lateinit var latch: CountDownLatch
        val received = AtomicReference<List<Int>>(emptyList())
        val key = testKey.toParametersKey()
    }
}

@Composable
private fun BoxRowBox(modifier: GlanceModifier, text: String) {
    Box(modifier) {
        val rowModifier = GlanceModifier
            .background(ColorProvider(Color.Gray))
            .fillMaxWidth()
            .padding(8.dp)
        Row(modifier = rowModifier) {
            val boxModifier = GlanceModifier
                .background(ColorProvider(Color.DarkGray))
                .width(64.dp)
                .fillMaxHeight()
            Box(
                modifier = boxModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(text)
            }
        }
    }
}

internal class CompoundButtonActionTest : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val target = checkNotNull(parameters[key])
        val value = checkNotNull(parameters[ToggleableStateKey])
        received.update { it + (target to value) }
        latch.countDown()
    }

    companion object {
        lateinit var latch: CountDownLatch
        val received = AtomicReference<List<Pair<String, Boolean>>>(emptyList())
        val key = ActionParameters.Key<String>("eventTarget")
    }
}
