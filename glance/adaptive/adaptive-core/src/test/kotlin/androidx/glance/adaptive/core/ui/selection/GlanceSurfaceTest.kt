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

package androidx.glance.adaptive.core.ui.selection

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceSurfaceTest {

    @Test
    fun surfaceDetector_fromHostCategory_resolvesCorrectly() {
        assertThat(
                SurfaceDetector.fromHostCategory(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            )
            .isEqualTo(GlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(SurfaceDetector.fromHostCategory(AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD))
            .isEqualTo(GlanceSurface.MOBILE_LOCK_SCREEN)
        assertThat(SurfaceDetector.fromHostCategory(0)).isEqualTo(GlanceSurface.MOBILE_HOME_SCREEN)
    }

    @Test
    fun surfaceDetector_fromAppWidgetOptions_resolvesCorrectly() {
        assertThat(SurfaceDetector.fromAppWidgetOptions(null))
            .isEqualTo(GlanceSurface.MOBILE_HOME_SCREEN)

        val homeOptions =
            Bundle().apply {
                putInt(
                    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                )
            }
        assertThat(SurfaceDetector.fromAppWidgetOptions(homeOptions))
            .isEqualTo(GlanceSurface.MOBILE_HOME_SCREEN)

        val lockOptions =
            Bundle().apply {
                putInt(
                    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
                )
            }
        assertThat(SurfaceDetector.fromAppWidgetOptions(lockOptions))
            .isEqualTo(GlanceSurface.MOBILE_LOCK_SCREEN)
    }

    @Test
    fun hostConstraints_equalsHashCodeToString() {
        val constraints1 = HostConstraints(Dimensions(100, 200), GlanceSurface.MOBILE_HOME_SCREEN)
        val constraints2 = HostConstraints(Dimensions(100, 200), GlanceSurface.MOBILE_HOME_SCREEN)
        val diffConstraints =
            HostConstraints(Dimensions(100, 200), GlanceSurface.MOBILE_LOCK_SCREEN)

        assertThat(constraints1).isEqualTo(constraints2)
        assertThat(constraints1.hashCode()).isEqualTo(constraints2.hashCode())
        assertThat(constraints1).isNotEqualTo(diffConstraints)
        assertThat(constraints1.toString())
            .isEqualTo(
                "HostConstraints(dimensions=Dimensions(widthDp=100, heightDp=200), surface=MOBILE_HOME_SCREEN)"
            )
    }
}
