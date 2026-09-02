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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class WidgetInstanceRepositoryTest {

    private class MatchingReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "matching_widget"
    }

    private class OtherReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "other_widget"
    }

    private lateinit var context: Context
    private lateinit var repository: WidgetInstanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = WidgetInstanceRepository(context)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withEmptyWidgetIds_returnsEmptyMap() {
        setupBoundWidget(101, MatchingReceiver::class.java.name)
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val result =
            repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = emptySet())

        assertThat(result).isEmpty()
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withNoReceivers_returnsEmptyMap() {
        val result = repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = null)

        assertThat(result).isEmpty()
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withNullWidgetIds_returnsAllMatchingAppWidgetIds() {
        setupBoundWidget(201, MatchingReceiver::class.java.name)
        setupBoundWidget(202, MatchingReceiver::class.java.name)
        setupBoundWidget(203, OtherReceiver::class.java.name)

        registerReceiverInManifest(MatchingReceiver::class.java.name)
        registerReceiverInManifest(OtherReceiver::class.java.name)

        val result = repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = null)

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result).containsKey(componentName)
        assertThat(result[componentName]?.toList()).containsExactly(201, 202)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withWidgetIds_filtersAppWidgetIdsByOptionsExtra() {
        setupBoundWidget(301, MatchingReceiver::class.java.name, widgetId = "instance_1")
        setupBoundWidget(302, MatchingReceiver::class.java.name, widgetId = "instance_2")
        setupBoundWidget(303, MatchingReceiver::class.java.name, widgetId = "instance_3")

        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val result =
            repository.findAppWidgetIdsForWidgetName(
                "matching_widget",
                widgetIds = setOf("instance_1", "instance_3"),
            )

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result).containsKey(componentName)
        assertThat(result[componentName]?.toList()).containsExactly(301, 303)
    }

    @Test
    fun findAppWidgetIdsForWidgetName_withManifestMetaData_matchesWidgetName() {
        setupBoundWidget(401, MatchingReceiver::class.java.name)
        registerReceiverInManifestWithMetaData(MatchingReceiver::class.java.name, "matching_widget")

        val result = repository.findAppWidgetIdsForWidgetName("matching_widget", widgetIds = null)

        val componentName = ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(result).containsKey(componentName)
        assertThat(result[componentName]?.toList()).containsExactly(401)
    }

    @Test
    fun findReceiverComponentsForWidgetName_withZeroBoundWidgets_returnsMatchingComponent() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val components = repository.findReceiverComponentsForWidgetName("matching_widget")

        val expectedComponent =
            ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(components).containsExactly(expectedComponent)
    }

    @Test
    fun findReceiverComponentsForWidgetName_withManifestMetaData_matchesWidgetName() {
        registerReceiverInManifestWithMetaData(MatchingReceiver::class.java.name, "matching_widget")

        val components = repository.findReceiverComponentsForWidgetName("matching_widget")

        val expectedComponent =
            ComponentName(context.packageName, MatchingReceiver::class.java.name)
        assertThat(components).containsExactly(expectedComponent)
    }

    @Test
    fun findReceiverComponentsForWidgetName_withUnmatchedWidgetName_returnsEmpty() {
        registerReceiverInManifest(MatchingReceiver::class.java.name)

        val components = repository.findReceiverComponentsForWidgetName("unmatched_widget")

        assertThat(components).isEmpty()
    }

    private fun setupBoundWidget(appWidgetId: Int, receiverName: String, widgetId: String? = null) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowManager = shadowOf(appWidgetManager)
        val componentName = ComponentName(context.packageName, receiverName)
        val info = AppWidgetProviderInfo().apply { provider = componentName }
        shadowManager.addBoundWidget(appWidgetId, info)

        if (widgetId != null) {
            appWidgetManager.updateAppWidgetOptions(
                appWidgetId,
                Bundle().apply {
                    putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId)
                },
            )
        }
    }

    private fun registerReceiverInManifest(receiverName: String) {
        val componentName = ComponentName(context.packageName, receiverName)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForReceiver(
            componentName,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
    }

    private fun registerReceiverInManifestWithMetaData(receiverName: String, widgetName: String) {
        val componentName = ComponentName(context.packageName, receiverName)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForReceiver(
            componentName,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
        val updateIntent =
            android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                setPackage(context.packageName)
            }
        val resolveInfos =
            context.packageManager.queryBroadcastReceivers(
                updateIntent,
                android.content.pm.PackageManager.GET_META_DATA,
            )
        val resolveInfo = resolveInfos.firstOrNull { it.activityInfo?.name == receiverName }
        resolveInfo?.activityInfo?.metaData =
            Bundle().apply {
                putString(GlanceAdaptiveWidgetReceiver.META_DATA_WIDGET_NAME, widgetName)
            }
    }
}
