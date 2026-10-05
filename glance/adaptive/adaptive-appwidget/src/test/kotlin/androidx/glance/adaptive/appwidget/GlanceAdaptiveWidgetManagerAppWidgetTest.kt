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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.WidgetInstanceInfo
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceAdaptiveWidgetManagerAppWidgetTest {

    private class TestTemplate : AdaptiveGlanceTemplate {
        override val templateId: String = "TestTemplate"
    }

    /**
     * Delegate for a surface without AppWidgets, and therefore without dynamic previews or pinning.
     */
    private class OtherSurfaceDelegate : GlanceAdaptiveWidgetDelegate {
        var callCount = 0

        override suspend fun pushUpdate(
            widgetName: String,
            currentData: AdaptiveGlanceTemplate,
            widgetIds: Set<String>?,
        ) {
            callCount++
        }

        override suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo> {
            callCount++
            return emptyList()
        }
    }

    /** Arguments of one [GlanceAdaptiveAppWidgetDelegate.requestPin] call. */
    private data class RequestPinCall(
        val widgetName: String,
        val initialData: AdaptiveGlanceTemplate?,
        val successCallback: PendingIntent?,
    )

    /** AppWidget delegate that records the previews and pin requests it is asked for. */
    private class FakeAppWidgetDelegate : GlanceAdaptiveAppWidgetDelegate {
        val setPreviewCalls = mutableListOf<Pair<String, AdaptiveGlanceTemplate>>()
        val requestPinCalls = mutableListOf<RequestPinCall>()

        override suspend fun pushUpdate(
            widgetName: String,
            currentData: AdaptiveGlanceTemplate,
            widgetIds: Set<String>?,
        ) {}

        override suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo> =
            emptyList()

        override suspend fun setPreview(widgetName: String, previewData: AdaptiveGlanceTemplate) {
            setPreviewCalls += widgetName to previewData
        }

        override suspend fun requestPin(
            widgetName: String,
            initialData: AdaptiveGlanceTemplate?,
            successCallback: PendingIntent?,
        ): Boolean {
            requestPinCalls += RequestPinCall(widgetName, initialData, successCallback)
            return true
        }
    }

    @Test
    fun glanceAdaptiveWidgetManager_withContext_createsManagerWithBaseWidgetDelegate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GlanceAdaptiveWidgetManager(context)

        assertThat(manager.delegate).isInstanceOf(BaseWidgetDelegate::class.java)
    }

    @Test
    fun setPreview_withAppWidgetDelegate_forwardsToDelegate() = runTest {
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val previewData = TestTemplate()

        manager.setPreview(widgetName = "test_widget", previewData = previewData)

        assertThat(delegate.setPreviewCalls).containsExactly("test_widget" to previewData)
        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun setPreview_withNonAppWidgetDelegate_isNoOpAndLogsWarning() = runTest {
        val delegate = OtherSurfaceDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        manager.setPreview(widgetName = "test_widget", previewData = TestTemplate())

        assertThat(delegate.callCount).isEqualTo(0)
        assertThat(fallbackWarnings()).hasSize(1)
    }

    @Test
    fun setPreview_onManagerFromFactory_doesNotLogFallbackWarning() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GlanceAdaptiveWidgetManager(context)

        manager.setPreview(widgetName = "test_widget", previewData = TestTemplate())

        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun requestPin_withAppWidgetDelegate_forwardsToDelegate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)
        val initialData = TestTemplate()
        val successCallback =
            PendingIntent.getBroadcast(
                context,
                /* requestCode= */ 0,
                Intent("test.action.PINNED").setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )

        val result =
            manager.requestPin(
                widgetName = "test_widget",
                initialData = initialData,
                successCallback = successCallback,
            )

        assertThat(result).isTrue()
        assertThat(delegate.requestPinCalls)
            .containsExactly(RequestPinCall("test_widget", initialData, successCallback))
        assertThat(fallbackWarnings()).isEmpty()
    }

    @Test
    fun requestPin_withOnlyWidgetName_forwardsDefaultsToDelegate() = runTest {
        val delegate = FakeAppWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        manager.requestPin(widgetName = "test_widget")

        assertThat(delegate.requestPinCalls)
            .containsExactly(
                RequestPinCall(
                    widgetName = "test_widget",
                    initialData = null,
                    successCallback = null,
                )
            )
    }

    @Test
    fun requestPin_withNonAppWidgetDelegate_returnsFalseAndLogsWarning() = runTest {
        val delegate = OtherSurfaceDelegate()
        val manager = GlanceAdaptiveWidgetManager(delegate)

        val result = manager.requestPin(widgetName = "test_widget")

        assertThat(result).isFalse()
        assertThat(delegate.callCount).isEqualTo(0)
        assertThat(fallbackWarnings()).hasSize(1)
    }

    @Test
    fun glanceAdaptiveWidgetManager_doesNotDeclareAppWidgetOnlyMembers() {
        // A member would always win over the extension of the same name, silently shadowing it
        // and making the operation callable without depending on adaptive-appwidget.
        val memberNames = GlanceAdaptiveWidgetManager::class.java.methods.map { it.name }

        assertThat(memberNames).containsNoneOf("setPreview", "requestPin")
    }

    private fun fallbackWarnings() = ShadowLog.getLogsForTag(LOG_TAG).filter { it.type == Log.WARN }

    private companion object {
        const val LOG_TAG = "GlanceAdaptiveAppWidget"
    }
}
