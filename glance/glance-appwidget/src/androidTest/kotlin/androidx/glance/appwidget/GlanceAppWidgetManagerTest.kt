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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.collection.intSetOf
import androidx.glance.text.Text
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth.assertThat
import java.io.FileInputStream
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetManagerTest {
    @get:Rule val mHostRule = AppWidgetHostRule()

    @Before
    fun setUp() {
        // Reset the size mode to the default
        TestGlanceAppWidget.sizeMode = SizeMode.Single
    }

    @After
    fun tearDown() {
        context.startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    @Test
    fun noAppWidget() = runBlocking {
        val manager = GlanceAppWidgetManager(context)

        assertThat(manager.getGlanceIds(TestGlanceAppWidget::class.java)).isEmpty()
    }

    @Test
    fun withAppWidget() = runBlocking {
        TestGlanceAppWidget.uiDefinition = { Text("Something") }
        val manager = GlanceAppWidgetManager(context)

        suspend fun verifyGlanceIdsAndSizes() {
            val glanceIds = manager.getGlanceIds(TestGlanceAppWidget::class.java)
            assertThat(glanceIds).hasSize(1)
            assertThat(manager.listKnownReceivers())
                .containsExactly(TestGlanceAppWidgetReceiver::class.java.canonicalName)

            val glanceId = manager.getGlanceIdBy((glanceIds[0] as AppWidgetId).appWidgetId)
            assertThat(glanceId).isEqualTo(glanceIds[0])

            val sizes = manager.getAppWidgetSizes(glanceIds[0])
            assertThat(sizes).containsExactly(mHostRule.portraitSize, mHostRule.landscapeSize)
        }

        mHostRule.startHost()

        verifyGlanceIdsAndSizes()

        // using "pm clear <package>" is not suitable here - it will crash process.
        // See https://github.com/android/testing-samples/issues/98
        // So, we clear datastore to mimic clearing appData.
        manager.clearDataStore()

        verifyGlanceIdsAndSizes()
    }

    @Test
    fun pinAppWidget() = runTest {
        val text = "Something"
        TestGlanceAppWidget.uiDefinition = { Text(text) }

        val result =
            GlanceAppWidgetManager(context)
                .requestPinGlanceAppWidget(
                    TestGlanceAppWidgetReceiver::class.java,
                    preview = TestGlanceAppWidget
                )

        assertThat(result).isTrue()
        mHostRule.onHostActivity {
            assertThat(mHostRule.device.findObject(UiSelector().text(text)).exists())
        }
    }

    @Test
    fun pinInvalidAppWidget() = runTest {
        val result =
            GlanceAppWidgetManager(context)
                .requestPinGlanceAppWidget(
                    DummyGlanceAppWidgetReceiver::class.java,
                )

        assertThat(result).isFalse()
    }

    @Ignore("b/285198114")
    @Test
    fun cleanReceivers() =
        runBlocking<Unit> {
            val manager = GlanceAppWidgetManager(context)

            manager.updateReceiver(DummyGlanceAppWidgetReceiver(), TestGlanceAppWidget)

            assertThat(manager.listKnownReceivers())
                .containsExactly(
                    DummyGlanceAppWidgetReceiver::class.java.canonicalName,
                    TestGlanceAppWidgetReceiver::class.java.canonicalName
                )

            manager.cleanReceivers()

            assertThat(manager.listKnownReceivers())
                .containsExactly(TestGlanceAppWidgetReceiver::class.java.canonicalName)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun setWidgetPreview() = runTest {
        disableGeneratedPreviewsRateLimit {
            TestGlanceAppWidget.withProvidePreview(
                previewBlock = { widgetCategory -> Text("$widgetCategory preview") }
            ) {
                val categories =
                    intSetOf(
                        WIDGET_CATEGORY_HOME_SCREEN,
                        WIDGET_CATEGORY_KEYGUARD,
                        WIDGET_CATEGORY_SEARCHBOX,
                    )
                val result =
                    GlanceAppWidgetManager(context)
                        .setWidgetPreviews<TestGlanceAppWidgetReceiver>(categories)
                assertThat(result).isTrue()

                categories.forEach { category ->
                    val preview =
                        AppWidgetManager.getInstance(context)
                            .getWidgetPreview(
                                ComponentName(context, TestGlanceAppWidgetReceiver::class.java),
                                /* profile= */ null,
                                category
                            )
                    assertNotNull(preview)

                    val view = preview.apply(context, FrameLayout(context))
                    val textView = assertNotNull(view.findChildByType<TextView>())
                    assertThat(textView.text.toString()).isEqualTo("$category preview")
                }
            }
        }
    }
}

private class DummyGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TestGlanceAppWidget
}

private const val GENERATED_PREVIEW_API_MAX_CALLS_PER_INTERVAL =
    "generated_preview_api_max_calls_per_interval"

@RequiresApi(Build.VERSION_CODES.Q)
private inline fun disableGeneratedPreviewsRateLimit(block: () -> Unit) {
    val automator = InstrumentationRegistry.getInstrumentation().uiAutomation
    automator.adoptShellPermissionIdentity()
    val initialMaxCalls =
        automator
            .executeShellCommand(
                "device_config get systemui $GENERATED_PREVIEW_API_MAX_CALLS_PER_INTERVAL"
            )
            .use { FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim() }
    try {
        val newValue = Int.MAX_VALUE
        automator.executeShellCommand(
            "device_config put systemui $GENERATED_PREVIEW_API_MAX_CALLS_PER_INTERVAL " +
                "$newValue"
        )
        block()
    } finally {
        if (initialMaxCalls == "null") {
            automator.executeShellCommand(
                "device_config delete systemui $GENERATED_PREVIEW_API_MAX_CALLS_PER_INTERVAL"
            )
        } else {
            automator.executeShellCommand(
                "device_config put systemui $GENERATED_PREVIEW_API_MAX_CALLS_PER_INTERVAL " +
                    initialMaxCalls
            )
        }
        automator.dropShellPermissionIdentity()
    }
}
