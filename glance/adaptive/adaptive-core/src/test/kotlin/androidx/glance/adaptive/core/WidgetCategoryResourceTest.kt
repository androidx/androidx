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

package androidx.glance.adaptive.core

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class WidgetCategoryResourceTest {

    @Test
    fun categoryAllKnownResource_matchesPlatformCategoryBitmask() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val allKnown =
            context.resources.getInteger(R.integer.glance_adaptive_widget_category_all_known)

        val expectedBitmask =
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD or
                WIDGET_CATEGORY_XR

        assertThat(allKnown).isEqualTo(expectedBitmask)
        assertThat(allKnown).isEqualTo(0x13)
    }

    private companion object {
        // SDK 38 constants, not yet available in AndroidX
        const val WIDGET_CATEGORY_XR = 16 // 0x10
    }
}
