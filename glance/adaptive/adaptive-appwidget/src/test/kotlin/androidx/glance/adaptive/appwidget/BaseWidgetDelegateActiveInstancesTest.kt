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
import android.os.Bundle
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.OtherReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests [BaseWidgetDelegate.getActiveInstances]. */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateActiveInstancesTest {

    @get:Rule val rule = BaseWidgetDelegateTestRule()

    @Test
    fun getActiveInstances_withoutReceivers_throws() = runTest {
        val error = runCatching {
            rule.delegate().getActiveInstances("test_widget")
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun getActiveInstances_withReceiverButNoPlacements_returnsEmptyList() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        assertThat(rule.delegate().getActiveInstances("test_widget")).isEmpty()
    }

    @Test
    fun getActiveInstances_returnsOneInstancePerWidgetId() = runTest {
        rule.setupBoundWidget(601, TestReceiver::class.java.name, widgetId = "instance_1")
        rule.setupBoundWidget(602, TestReceiver::class.java.name, widgetId = "instance_2")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances.map { it.widgetId }).containsExactly("instance_1", "instance_2")
        assertThat(instances.map { it.widgetName }).containsExactly("test_widget", "test_widget")
        instances.forEach { instance ->
            assertThat(instance.placementsPerSurface.size).isEqualTo(1)
            assertThat(
                    instance.placementsPerSurface.getOrDefault(
                        AppWidgetSurface.MOBILE_HOME_SCREEN,
                        0,
                    )
                )
                .isEqualTo(1)
        }
    }

    @Test
    fun getActiveInstances_withWidgetIdPlacedTwice_countsBothPlacements() = runTest {
        rule.setupBoundWidget(611, TestReceiver::class.java.name, widgetId = "shared")
        rule.setupBoundWidget(612, TestReceiver::class.java.name, widgetId = "shared")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances).hasSize(1)
        val instance = instances.single()
        assertThat(instance.widgetId).isEqualTo("shared")
        assertThat(instance.placementsPerSurface.size).isEqualTo(1)
        assertThat(
                instance.placementsPerSurface.getOrDefault(
                    AppWidgetSurface.MOBILE_HOME_SCREEN,
                    0,
                )
            )
            .isEqualTo(2)
    }

    @Test
    fun getActiveInstances_withPlacementsOnSeveralSurfaces_countsEachSurface() = runTest {
        rule.setupBoundWidget(
            621,
            TestReceiver::class.java.name,
            widgetId = "shared",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            622,
            TestReceiver::class.java.name,
            widgetId = "shared",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances).hasSize(1)
        val placementsPerSurface = instances.single().placementsPerSurface
        assertThat(placementsPerSurface.size).isEqualTo(2)
        assertThat(placementsPerSurface.getOrDefault(AppWidgetSurface.MOBILE_HOME_SCREEN, 0))
            .isEqualTo(1)
        assertThat(placementsPerSurface.getOrDefault(AppWidgetSurface.MOBILE_LOCK_SCREEN, 0))
            .isEqualTo(1)
    }

    @Test
    fun getActiveInstances_skipsPlacementsWithoutStoredWidgetId() = runTest {
        rule.setupBoundWidget(631, TestReceiver::class.java.name, widgetId = "instance_1")
        rule.setupBoundWidget(632, TestReceiver::class.java.name, widgetId = null)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances.map { it.widgetId }).containsExactly("instance_1")
    }

    @Test
    fun getActiveInstances_skipsPlacementsWithBlankWidgetId() = runTest {
        rule.setupBoundWidget(633, TestReceiver::class.java.name, widgetId = "instance_1")
        rule.setupBoundWidget(634, TestReceiver::class.java.name, widgetId = "   ")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances.map { it.widgetId }).containsExactly("instance_1")
    }

    @Test
    fun getActiveInstances_excludesOtherWidgetNames() = runTest {
        rule.setupBoundWidget(641, TestReceiver::class.java.name, widgetId = "mine")
        rule.setupBoundWidget(642, OtherReceiver::class.java.name, widgetId = "theirs")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        rule.registerReceiverInManifest(OtherReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances.map { it.widgetId }).containsExactly("mine")
    }

    @Test
    fun getActiveInstances_withWidgetIdPlacedTwice_retainsLowestAppWidgetIdOptions() = runTest {
        // Registered highest-first so that a correct result cannot come from insertion order.
        rule.setupBoundWidget(
            652,
            TestReceiver::class.java.name,
            widgetId = "shared",
            extraOptions = Bundle().apply { putString("config_key", "from_652") },
        )
        rule.setupBoundWidget(
            651,
            TestReceiver::class.java.name,
            widgetId = "shared",
            extraOptions = Bundle().apply { putString("config_key", "from_651") },
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val instances = rule.delegate().getActiveInstances("test_widget")

        assertThat(instances).hasSize(1)
        assertThat(instances.single().options.getString("config_key")).isEqualTo("from_651")
    }

    @Test
    fun getActiveInstances_whenOptionsFailureOccurs_propagatesException() = runTest {
        rule.setupBoundWidget(661, TestReceiver::class.java.name, widgetId = "failing")
        rule.setupBoundWidget(662, TestReceiver::class.java.name, widgetId = "healthy")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val realManager = AppWidgetManager.getInstance(rule.context)
        val mockManager =
            mock<AppWidgetManager> {
                on { getAppWidgetIds(any()) }.thenReturn(intArrayOf(661, 662))
                on { getAppWidgetOptions(eq(661)) }.thenThrow(RuntimeException("IPC failure"))
                on { getAppWidgetOptions(eq(662)) }
                    .thenAnswer { realManager.getAppWidgetOptions(662) }
            }

        val delegate =
            BaseWidgetDelegate(
                context = rule.context,
                repository = WidgetInstanceRepository(rule.context, mockManager),
                appWidgetManager = mockManager,
            )

        val error = runCatching { delegate.getActiveInstances("test_widget") }.exceptionOrNull()

        assertThat(error).isInstanceOf(RuntimeException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("IPC failure")
    }
}
