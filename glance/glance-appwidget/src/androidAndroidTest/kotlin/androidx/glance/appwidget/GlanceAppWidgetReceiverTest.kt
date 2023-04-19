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
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
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
import androidx.glance.color.ColorProvider
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
import androidx.test.filters.FlakyTest
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Enable verbose logging for test failure investigation. Enable for b/267494219 */
const val VERBOSE_LOG = true

const val RECEIVER_TEST_TAG = "GAWRT" // shorten to avoid long tag lint

@OptIn(ExperimentalCoroutinesApi::class)
@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetReceiverTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @get:Rule
    val mViewDumpRule = ViewHierarchyFailureWatcher()

    val context = InstrumentationRegistry.getInstrumentation().targetContext!!

    @Before
    fun setUp() {
        // Reset the size mode to the default
        TestGlanceAppWidget.sizeMode = SizeMode.Single
    }

    @After
    fun cleanUp() {
        TestGlanceAppWidget.resetOnDeleteBlock()
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

    @FlakyTest(bugId = 249803914)
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
    fun columnWithWeightedItemRespectsSizeOfOtherItem() {
        // When one item has a default weight, it should scale to respect the size of the other item
        // in this case the other item fills the column, so the weighted item should take up no
        // space, that is, has height 0.
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
    @SdkSuppress(minSdkVersion = 31)
    fun createButton() {
        TestGlanceAppWidget.uiDefinition = {
            Button(
                text = "Button",
                onClick = actionStartActivity<Activity>(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(Color.Transparent),
                    contentColor = ColorProvider(Color.DarkGray)
                ),
                enabled = false
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<Button> { button ->
            checkNotNull(button.text.toString() == "Button") {
                "Couldn't find 'Button'"
            }

            assertThat(button.isEnabled).isFalse()
            assertThat(button.hasOnClickListeners()).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    fun createButtonBackport() {
        TestGlanceAppWidget.uiDefinition = {
            Button(
                text = "Button",
                onClick = actionStartActivity<Activity>(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(Color.Transparent),
                    contentColor = ColorProvider(Color.DarkGray)
                ),
                enabled = false
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { button ->
            checkNotNull(button.findChild<TextView> { it.text.toString() == "Button" }) {
                "Couldn't find TextView 'Button'"
            }

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

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            assertThat(box.notGoneChildCount).isEqualTo(2)
            val (boxedImage, boxedText) = box.notGoneChildren.toList()
            val image = boxedImage.getTargetView<ImageView>()
            val text = boxedText.getTargetView<TextView>()
            assertThat(image.drawable).isNotNull()
            assertThat(image.scaleType).isEqualTo(ScaleType.FIT_XY)
            assertThat(text.background).isNull()
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
            assertThat(image.scaleType).isEqualTo(ScaleType.FIT_CENTER)
            assertThat(text.background).isNull()
        }
    }

    @Test
    fun drawableCropBackground() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "Some useful text",
                modifier = GlanceModifier.fillMaxWidth().height(220.dp)
                    .background(ImageProvider(R.drawable.oval), contentScale = ContentScale.Crop)
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            assertThat(box.notGoneChildCount).isEqualTo(2)
            val (boxedImage, boxedText) = box.notGoneChildren.toList()
            val image = boxedImage.getTargetView<ImageView>()
            val text = boxedText.getTargetView<TextView>()
            assertThat(image.drawable).isNotNull()
            assertThat(image.scaleType).isEqualTo(ScaleType.CENTER_CROP)
            assertThat(text.background).isNull()
        }
    }

    @Test
    fun bitmapBackground() {
        TestGlanceAppWidget.uiDefinition = {
            val context = LocalContext.current
            val bitmap =
                (context.resources.getDrawable(R.drawable.compose, null) as BitmapDrawable)
                    .bitmap
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
    fun updateAll() = runTest {
        TestGlanceAppWidget.uiDefinition = {
            Text("text")
        }

        mHostRule.startHost()

        mHostRule.runAndWaitForUpdate {
            TestGlanceAppWidget.updateAll(context)
        }
    }

    @Test
    fun updateIf() = runTest {
        val didRun = AtomicBoolean(false)
        TestGlanceAppWidget.uiDefinition = {
            currentState<Preferences>()
            didRun.set(true)
            Text("text")
        }

        mHostRule.startHost()
        assertThat(didRun.get()).isTrue()

        GlanceAppWidgetManager(context)
            .getGlanceIds(TestGlanceAppWidget::class.java)
            .forEach { glanceId ->
                updateAppWidgetState(context, glanceId) {
                    it[testKey] = 2
                }
            }

        // Make sure the app widget is updated if the test is true
        didRun.set(false)
        mHostRule.runAndWaitForUpdate {
            TestGlanceAppWidget.updateIf<Preferences>(context) { prefs ->
                prefs[testKey] == 2
            }
        }
        assertThat(didRun.get()).isTrue()

        // Make sure it is not if the test is false
        didRun.set(false)

        // Waiting for the update should timeout since it is never triggered.
        val exception = assertThrows(IllegalArgumentException::class.java) {
            // AppWidgetService may send an APPWIDGET_UPDATE broadcast, which is not relevant to
            // this and should be ignored.
            mHostRule.ignoreBroadcasts {
                runBlocking {
                    mHostRule.runAndWaitForUpdate {
                        TestGlanceAppWidget.updateIf<Preferences>(context) { prefs ->
                            prefs[testKey] == 3
                        }
                    }
                }
            }
        }
        assertThat(exception).hasMessageThat().contains("Timeout before getting RemoteViews")

        assertThat(didRun.get()).isFalse()
    }

    @Test
    fun viewState() {
        TestGlanceAppWidget.uiDefinition = {
            val value = currentState(testKey) ?: -1
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
                        actionRunCallback<CallbackTest>(
                            actionParametersOf(CallbackTest.key to 1)
                        )
                    )
                )
                Text(
                    "text2",
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<CallbackTest>(
                            actionParametersOf(CallbackTest.key to 2)
                        )
                    )
                )
            }
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(2)
        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            checkNotNull(
                root.findChild<TextView> { it.text.toString() == "text1" }?.parent as? View
            )
                .performClick()
            checkNotNull(
                root.findChild<TextView> { it.text.toString() == "text2" }?.parent as? View
            )
                .performClick()
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
                    actionRunCallback<CallbackTest>(
                        actionParametersOf(CallbackTest.key to 1)
                    )
                ).clickable(
                    actionRunCallback<CallbackTest>(
                        actionParametersOf(CallbackTest.key to 2)
                    )
                )
            )
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(1)
        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            checkNotNull(
                root.findChild<TextView> { it.text.toString() == "text1" }?.parent as? View
            )
                .performClick()
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

            val text1 = checkNotNull(column.findChild<TextView> { it.text.toString() == "Text 1" })
            val row1 = text1.getParentView<FrameLayout>().getParentView<LinearLayout>()
            assertThat(row1.orientation).isEqualTo(LinearLayout.HORIZONTAL)
            assertViewSize(row1, DpSize(targetWidth, targetHeight))

            val text2 = checkNotNull(column.findChild<TextView> { it.text.toString() == "Text 2" })
            val row2 = text2.getParentView<FrameLayout>().getParentView<LinearLayout>()
            assertThat(row2.orientation).isEqualTo(LinearLayout.HORIZONTAL)
            assertThat(row2.height).isGreaterThan(20.dp.toPixels(context))
            assertViewSize(row2, DpSize(targetWidth, targetHeight))
        }
    }

    @Test
    fun compoundButtonAction() = runTest {
        val checkbox = "checkbox"
        val switch = "switch"
        val checkBoxClicked = MutableStateFlow(false)
        val switchClicked = MutableStateFlow(false)

        TestGlanceAppWidget.uiDefinition = {
            Column {
                CheckBox(
                    checked = false,
                    onCheckedChange = { assert(checkBoxClicked.tryEmit(true)) },
                    text = checkbox
                )
                Switch(
                    checked = true,
                    onCheckedChange = { assert(switchClicked.tryEmit(true)) },
                    text = switch
                )
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            checkNotNull(root.findChild<TextView> { it.text.toString() == checkbox })
                .performCompoundButtonClick()
            checkNotNull(root.findChild<TextView> { it.text.toString() == switch })
                .performCompoundButtonClick()
        }
        checkBoxClicked.first { it }
        switchClicked.first { it }
    }

    @Test
    fun canCreateCheckableColorProvider() {
        TestGlanceAppWidget.uiDefinition = {
            Switch(
                checked = true,
                onCheckedChange = null,
                text = "Hello Checked Switch (day: Blue/Green, night: Red/Yellow)",
                style = TextStyle(
                    color = ColorProvider(
                        day = Color.Black,
                        night = Color.White
                    ),
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal,
                ),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(
                        day = Color.Blue,
                        night = Color.Red
                    ),
                    checkedTrackColor = ColorProvider(
                        day = Color.Green,
                        night = Color.Yellow
                    ),
                    uncheckedThumbColor = ColorProvider(Color.Magenta),
                    uncheckedTrackColor = ColorProvider(Color.Magenta),
                )
            )
        }

        mHostRule.startHost()
        runBlocking {
            mHostRule.runAndWaitForUpdate {
                TestGlanceAppWidget.update(context, AppWidgetId(mHostRule.appWidgetId))
            }
        }

        // if no crash, we're good
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
        mHostRule.onUnboxedHostView<View> { root ->
            checkNotNull(root.findChild<TextView> { it.text.toString() == "text1" })
                .performCompoundButtonClick()
        }
        assertThat(CallbackTest.latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(CallbackTest.received.get()).containsExactly(2)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun lambdaActionCallback() = runTest {
        TestGlanceAppWidget.uiDefinition = {
            val text = remember { mutableStateOf("initial") }
            Button(
                text = text.value,
                onClick = {
                    text.value = "clicked"
                }
            )
        }

        mHostRule.startHost()
        var button: View? = null
        mHostRule.onUnboxedHostView<Button> { buttonView ->
            assertThat(buttonView.text.toString()).isEqualTo("initial")
            button = buttonView
        }
        mHostRule.runAndWaitForUpdate {
            button!!.performClick()
        }

        mHostRule.onUnboxedHostView<Button> { buttonView ->
            assertThat(buttonView.text.toString()).isEqualTo("clicked")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    fun lambdaActionCallback_backportButton() = runTest {
        TestGlanceAppWidget.uiDefinition = {
            val text = remember { mutableStateOf("initial") }
            Button(
                text = text.value,
                onClick = {
                    text.value = "clicked"
                }
            )
        }

        mHostRule.startHost()
        var button: View? = null
        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            val text = checkNotNull(root.findChild<TextView> { it.text.toString() == "initial" })
            button = text.parent as View
        }
        mHostRule.runAndWaitForUpdate {
            button!!.performClick()
        }

        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            checkNotNull(root.findChild<TextView> { it.text.toString() == "clicked" })
        }
    }

    @Test
    fun unsetActionCallback() = runTest {
        var enabled by mutableStateOf(true)
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "text1",
                modifier = if (enabled) GlanceModifier.clickable {} else GlanceModifier
            )
        }

        mHostRule.startHost()
        mHostRule.onUnboxedHostView<View> { root ->
            val view =
                checkNotNull(
                    root.findChild<TextView> { it.text.toString() == "text1" }?.parent as? View
                )
            assertThat(view.hasOnClickListeners()).isTrue()
        }

        mHostRule.runAndWaitForUpdate {
            enabled = false
        }

        mHostRule.onUnboxedHostView<TextView> { root ->
            val view =
                checkNotNull(
                    root.findChild<TextView> { it.text.toString() == "text1" }?.parent as? View
                )
            assertThat(view.hasOnClickListeners()).isFalse()
        }
    }

    @Test
    fun unsetCompoundButtonActionCallback() = runTest {
        TestGlanceAppWidget.uiDefinition = {
            val enabled = currentState<Preferences>()[testBoolKey] ?: true
            CheckBox(
                checked = false,
                onCheckedChange = if (enabled) {
                    actionRunCallback<CompoundButtonActionTest>(
                        actionParametersOf(CompoundButtonActionTest.key to "checkbox")
                    )
                } else null,
                text = "checkbox"
            )
        }

        mHostRule.startHost()

        CompoundButtonActionTest.received.set(emptyList())
        CompoundButtonActionTest.latch = CountDownLatch(1)
        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            checkNotNull(root.findChild<TextView> { it.text.toString() == "checkbox" })
                .performCompoundButtonClick()
        }
        CompoundButtonActionTest.latch.await(5, TimeUnit.SECONDS)
        assertThat(CompoundButtonActionTest.received.get()).containsExactly(
            "checkbox" to true
        )

        updateAppWidgetState(context, AppWidgetId(mHostRule.appWidgetId)) {
            it[testBoolKey] = false
        }
        mHostRule.runAndWaitForUpdate {
            TestGlanceAppWidget.update(context, AppWidgetId(mHostRule.appWidgetId))
        }

        CompoundButtonActionTest.received.set(emptyList())
        CompoundButtonActionTest.latch = CountDownLatch(1)
        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            checkNotNull(root.findChild<TextView> { it.text.toString() == "checkbox" })
                .performCompoundButtonClick()
        }
        assertThat(CompoundButtonActionTest.latch.await(5, TimeUnit.SECONDS)).isFalse()
        assertThat(CompoundButtonActionTest.received.get()).isEmpty()
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun compoundButtonsOnlyHaveOneAction() {
        TestGlanceAppWidget.uiDefinition = {
            Column {
                CheckBox(
                    checked = false,
                    onCheckedChange = actionRunCallback<CompoundButtonActionTest>(
                        actionParametersOf()
                    ),
                    text = "checkbox",
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<CompoundButtonActionTest>(actionParametersOf())
                    ),
                )
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<ViewGroup> { root ->
            val checkbox =
                checkNotNull(root.findChild<CompoundButton> { it.text.toString() == "checkbox" })
            assertThat(checkbox.hasOnClickListeners()).isFalse()
        }
    }

    @Test
    fun elementsWithActionsHaveRipples() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                text = "text1",
                modifier = GlanceModifier.clickable(actionRunCallback<CallbackTest>()),
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            assertThat(box.notGoneChildCount).isEqualTo(2)
            val (boxedText, boxedImage) = box.notGoneChildren.toList()
            val text = boxedText.getTargetView<TextView>()
            val image = boxedImage.getTargetView<ImageView>()
            assertThat(text.background).isNull()
            assertThat(image.drawable).isNotNull()
            assertThat(image.isClickable()).isFalse()
        }
    }

    @Test
    fun elementsWithNoActionsDontHaveRipples() {
        TestGlanceAppWidget.uiDefinition = {
            Text("text1")
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<View> { view ->
            assertIs<TextView>(view)
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun compoundButtonsDoNotHaveRipples() {
        TestGlanceAppWidget.uiDefinition = {
            RadioButton(
                checked = true,
                onClick = actionRunCallback<CallbackTest>(),
                text = "text1",
            )
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<View> { view ->
            assertIs<RadioButton>(view)
        }
    }

    @Test
    fun cancellingContentCoroutineCausesContentToLeaveComposition() = runBlocking {
        val currentEffectState = MutableStateFlow(EffectState.Initial)
        var contentJob: Job? = null
        TestGlanceAppWidget.onProvideGlance = {
            coroutineScope {
                contentJob = launch {
                    provideContent {
                        DisposableEffect(true) {
                            currentEffectState.tryEmit(EffectState.Started)
                            onDispose { currentEffectState.tryEmit(EffectState.Disposed) }
                        }
                    }
                }
            }
        }
        launch { mHostRule.startHost() }
        currentEffectState.take(3).collectIndexed { index, state ->
            when (index) {
                0 -> assertThat(state).isEqualTo(EffectState.Initial)
                1 -> {
                    assertThat(state).isEqualTo(EffectState.Started)
                    assertNotNull(contentJob).cancel()
                }
                2 -> assertThat(state).isEqualTo(EffectState.Disposed)
            }
        }
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
        assertWithMessage("${view.accessibilityClassName} width").that(view.width / density)
            .isWithin(1.1f / density)
            .of(expectedSize.width.value)
        assertWithMessage("${view.accessibilityClassName} height").that(view.height / density)
            .isWithin(1.1f / density)
            .of(expectedSize.height.value)
    }

    private fun assertViewDimension(view: View, sizePx: Int, expectedSize: Dp) {
        val density = view.context.resources.displayMetrics.density
        assertThat(sizePx / density).isWithin(1.1f / density).of(expectedSize.value)
    }

    enum class EffectState { Initial, Started, Disposed }

    inner class ViewHierarchyFailureWatcher : TestWatcher() {
        override fun starting(description: Description) {
            super.starting(description)
            if (VERBOSE_LOG) {
                Log.d(RECEIVER_TEST_TAG, "")
                Log.d(RECEIVER_TEST_TAG, "")
                Log.d(RECEIVER_TEST_TAG, "Starting: ${description.methodName}")
                Log.d(RECEIVER_TEST_TAG, "---------------------")
            }
        }

        override fun failed(e: Throwable?, description: Description?) {
            Log.e(RECEIVER_TEST_TAG, "$description failed")
            Log.e(RECEIVER_TEST_TAG, "Host view hierarchy at failure:")
            logViewHierarchy(RECEIVER_TEST_TAG, mHostRule.mHostView, "")
        }
    }
}

private val testKey = intPreferencesKey("testKey")
private val testBoolKey = booleanPreferencesKey("testKey")

internal class CallbackTest : ActionCallback {
    override suspend fun onAction(
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
    override suspend fun onAction(
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
