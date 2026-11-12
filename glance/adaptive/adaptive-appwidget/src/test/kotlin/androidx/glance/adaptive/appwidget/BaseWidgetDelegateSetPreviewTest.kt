/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Build
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestTemplate
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests [BaseWidgetDelegate.setPreview]. */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateSetPreviewTest {

    @get:Rule val rule = BaseWidgetDelegateTestRule()

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_sdk35_withMatchingReceiver_rendersAndCallsSetWidgetPreview() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val testData = TestTemplate()

        rule.delegate().setPreview(widgetName = "test_widget", previewData = testData)

        // A home screen provider also gets a keyguard picker entry by default.
        verify(rule.mockRenderer, times(2)).invoke(eq(testData), any())
        verify(rule.appWidgetManager)
            .setWidgetPreview(
                eq(rule.componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
                any(),
            )
        verify(rule.appWidgetManager)
            .setWidgetPreview(
                eq(rule.componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD),
                any(),
            )
    }

    @Test
    @Config(sdk = [34])
    fun setPreview_preSdk35_gracefullyNoOps() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule.delegate().setPreview(widgetName = "test_widget", previewData = TestTemplate())

        verify(rule.mockRenderer, never()).invoke(any(), any())
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_withoutMatchingReceivers_throwsAndDoesNotComposeOrSetPreview() = runTest {
        val error = runCatching {
            rule
                .delegate()
                .setPreview(widgetName = "unregistered_widget", previewData = TestTemplate())
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.mockRenderer, never()).invoke(any(), any())
        verify(rule.appWidgetManager, never())
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenSetWidgetPreviewFailsForOneEntry_setsRemainingPreviewsThenThrows() =
        runTest {
            rule.registerReceiverInManifest(TestReceiver::class.java.name)
            // Fail whichever picker entry is delivered first, then behave normally.
            doThrow(IllegalStateException("Simulated host failure"))
                .doCallRealMethod()
                .whenever(rule.appWidgetManager)
                .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())

            val error = setPreviewFailure()

            verify(rule.appWidgetManager)
                .setWidgetPreview(
                    eq(rule.componentOf(TestReceiver::class.java.name)),
                    eq(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
                    any(),
                )
            verify(rule.appWidgetManager)
                .setWidgetPreview(
                    eq(rule.componentOf(TestReceiver::class.java.name)),
                    eq(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD),
                    any(),
                )
            // The original failure is thrown, with a summary naming where it happened.
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Simulated host failure")
            assertThat(error.suppressed.map { it.message }.single()).contains("test_widget")
        }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenSeveralLayoutsFail_listsEachFailureOnItsOwnLine() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doAnswer<Boolean> { throw IllegalStateException("Simulated host failure") }
            .whenever(rule.appWidgetManager)
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())

        val error = setPreviewFailure()

        // A header line, then one line per failed layout.
        val summaryLines = error.suppressed.first().message!!.lines()
        assertThat(summaryLines).hasSize(3)
        assertThat(summaryLines[0]).isEqualTo("setPreview failed for widgetName test_widget:")
        assertThat(summaryLines[1])
            .startsWith(
                "category ${AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN} on surface " +
                    "${AppWidgetSurface.MOBILE_HOME_SCREEN} at "
            )
        assertThat(summaryLines[2])
            .startsWith(
                "category ${AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD} on surface " +
                    "${AppWidgetSurface.MOBILE_LOCK_SCREEN} at "
            )
        assertThat(summaryLines[1]).endsWith("(IllegalStateException)")
        assertThat(summaryLines[2]).endsWith("(IllegalStateException)")
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenPlatformRejectsAPreview_throwsIllegalStateAndSkipsRemainingCategories() =
        runTest {
            rule.registerReceiverInManifest(TestReceiver::class.java.name)
            doReturn(false)
                .whenever(rule.appWidgetManager)
                .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())

            val error = setPreviewFailure()

            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().contains("rate limit")
            assertThat(error).hasMessageThat().contains("Do not retry immediately")
            assertThat(error).hasMessageThat().contains("static preview")
            // The rate limit applies to the whole provider, so keyguard is not attempted.
            verify(rule.appWidgetManager, times(1))
                .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
            verify(rule.mockRenderer, never())
                .invoke(any(), eq(AppWidgetSurface.MOBILE_LOCK_SCREEN))
            // Only the summary is attached.
            assertThat(error.suppressed.map { it.message }.single()).contains("test_widget")
        }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_withCategoriesSharingALayout_setsThemInOneCall() = runTest {
        rule.registerReceiverInManifest(
            TestReceiver::class.java.name,
            widgetCategory =
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX,
        )

        rule.delegate().setPreview(widgetName = "test_widget", previewData = TestTemplate())

        // Home screen and search box share a layout, while keyguard has its own.
        verify(rule.appWidgetManager)
            .setWidgetPreview(
                eq(rule.componentOf(TestReceiver::class.java.name)),
                eq(
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                        AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
                ),
                any(),
            )
        verify(rule.appWidgetManager)
            .setWidgetPreview(
                eq(rule.componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD),
                any(),
            )
        verify(rule.appWidgetManager, times(2))
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenComposeFailsForOneCategory_setsTheOtherThenThrows() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        whenever(rule.mockRenderer.invoke(any(), eq(AppWidgetSurface.MOBILE_LOCK_SCREEN)))
            .thenThrow(IllegalStateException("Simulated renderer failure"))

        setPreviewFailure()

        verify(rule.appWidgetManager)
            .setWidgetPreview(
                eq(rule.componentOf(TestReceiver::class.java.name)),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
                any(),
            )
        verify(rule.appWidgetManager, never())
            .setWidgetPreview(
                any<ComponentName>(),
                eq(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD),
                any<RemoteViews>(),
            )
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenPlatformThrowsCancellation_rethrowsIt() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doThrow(CancellationException("Simulated cancellation"))
            .whenever(rule.appWidgetManager)
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())

        val error = setPreviewFailure()

        assertThat(error).isInstanceOf(CancellationException::class.java)
        verify(rule.appWidgetManager, times(1))
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
    }

    @Test
    @Config(sdk = [35])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setPreview_whenCancelledAfterAFailure_throwsCancellationAndSkipsRemainingCategories() =
        runTest {
            rule.registerReceiverInManifest(TestReceiver::class.java.name)
            lateinit var call: Deferred<Unit>
            // The first picker entry fails, and the call is cancelled while it is handled.
            doAnswer<Boolean> {
                    call.cancel()
                    throw IllegalStateException("Simulated host failure")
                }
                .whenever(rule.appWidgetManager)
                .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
            call =
                backgroundScope.async(start = CoroutineStart.LAZY) {
                    rule
                        .delegate()
                        .setPreview(widgetName = "test_widget", previewData = TestTemplate())
                }

            val error = runCatching { call.await() }.exceptionOrNull()

            assertThat(error).isInstanceOf(CancellationException::class.java)
            verify(rule.appWidgetManager, times(1))
                .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())
        }

    /**
     * Runs [BaseWidgetDelegate.setPreview], expecting it to throw, and returns the exception it
     * threw. Coroutine stack trace recovery may rethrow a copy caused by the original exception, so
     * the original is returned in that case, to inspect what is attached to it.
     */
    private suspend fun setPreviewFailure(): Throwable {
        val error = runCatching {
            rule.delegate().setPreview(widgetName = "test_widget", previewData = TestTemplate())
        }
            .exceptionOrNull()
        assertThat(error).isNotNull()
        val cause = error!!.cause
        return if (
            cause != null && cause.javaClass == error.javaClass && cause.message == error.message
        ) {
            cause
        } else {
            error
        }
    }
}
