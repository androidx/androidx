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
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
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
    fun setPreview_whenSetWidgetPreviewFailsForOneEntry_stillSetsRemainingPreviews() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        // Fail whichever picker entry is delivered first, then behave normally.
        doThrow(IllegalStateException("Simulated host failure"))
            .doCallRealMethod()
            .whenever(rule.appWidgetManager)
            .setWidgetPreview(any<ComponentName>(), any<Int>(), any<RemoteViews>())

        rule.delegate().setPreview(widgetName = "test_widget", previewData = TestTemplate())

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
}
