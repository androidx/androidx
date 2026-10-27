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

package androidx.glance.adaptive.appwidget.ui.selection

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.glance.adaptive.core.ui.selection.Dimensions
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class AppWidgetSurfaceTest {

    @Test
    fun surfaceDetector_fromHostCategory_resolvesCorrectly() {
        assertThat(
                AppWidgetSurfaceDetector.fromHostCategory(
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
                )
            )
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)
        assertThat(
                AppWidgetSurfaceDetector.fromHostCategory(
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
                )
            )
            .isEqualTo(AppWidgetSurface.MOBILE_LOCK_SCREEN)
        assertThat(
                AppWidgetSurfaceDetector.fromHostCategory(
                    AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD
                )
            )
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)
        assertThat(
                AppWidgetSurfaceDetector.fromHostCategory(
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD or
                        AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD
                )
            )
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)
        assertThat(AppWidgetSurfaceDetector.fromHostCategory(0))
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)
    }

    @Test
    fun surfaceDetector_fromAppWidgetOptions_resolvesCorrectly() {
        assertThat(AppWidgetSurfaceDetector.fromAppWidgetOptions(null))
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)

        val homeOptions =
            Bundle().apply {
                putInt(
                    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                )
            }
        assertThat(AppWidgetSurfaceDetector.fromAppWidgetOptions(homeOptions))
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)

        val notKeyguardOptions =
            Bundle().apply {
                putInt(
                    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD,
                )
            }
        assertThat(AppWidgetSurfaceDetector.fromAppWidgetOptions(notKeyguardOptions))
            .isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)

        val lockOptions =
            Bundle().apply {
                putInt(
                    AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
                )
            }
        assertThat(AppWidgetSurfaceDetector.fromAppWidgetOptions(lockOptions))
            .isEqualTo(AppWidgetSurface.MOBILE_LOCK_SCREEN)
    }

    @Test
    fun surfaceDetector_resolvePreviewCategories_resolvesAllApplicableSurfaces() {
        // Home screen only (offers keyguard by default since not_keyguard is false)
        val homeTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
            )
        assertThat(homeTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                AppWidgetSurface.MOBILE_LOCK_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
            )

        // Home screen with Not Keyguard (excludes keyguard)
        val homeAndNotKeyguardTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD
            )
        assertThat(homeAndNotKeyguardTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
            )

        // Keyguard only
        val lockTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            )
        assertThat(lockTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_LOCK_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            )

        // Not keyguard only (defaults to home screen, excludes keyguard)
        val notKeyguardTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD
            )
        assertThat(notKeyguardTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
            )

        // Both Home Screen and Keyguard
        val multiTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            )
        assertThat(multiTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                AppWidgetSurface.MOBILE_LOCK_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
            )

        // Both Not Keyguard and Keyguard (not_keyguard suppresses keyguard)
        val notKeyguardAndLockTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            )
        assertThat(notKeyguardAndLockTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
            )

        // Unspecified (0) defaults to home screen, which offers keyguard as well
        val zeroTargets = AppWidgetSurfaceDetector.resolvePreviewCategories(0)
        assertThat(zeroTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                AppWidgetSurface.MOBILE_LOCK_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
            )

        // Custom / searchbox flag
        val searchTargets =
            AppWidgetSurfaceDetector.resolvePreviewCategories(
                AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
            )
        assertThat(searchTargets)
            .containsExactly(
                AppWidgetSurface.MOBILE_HOME_SCREEN to
                    AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
            )
    }

    @Test
    fun appWidgetSurface_tags_resolveCorrectly() {
        assertThat(AppWidgetSurface.MOBILE_HOME_SCREEN.tag).isEqualTo("home_screen")
        assertThat(AppWidgetSurface.MOBILE_LOCK_SCREEN.tag).isEqualTo("keyguard")
    }

    @Test
    fun appWidgetSurface_constants_haveUnspecifiedDimensions() {
        val surfaces = AppWidgetSurface.ALL
        for (i in surfaces.indices) {
            assertThat(surfaces[i].dimensions).isEqualTo(Dimensions(0, 0))
        }
    }

    @Test
    fun appWidgetSurface_withDimensions_carriesDimensionsAndKeepsIdentity() {
        val placed = AppWidgetSurface.MOBILE_LOCK_SCREEN.withDimensions(Dimensions(348, 88))

        assertThat(placed.dimensions).isEqualTo(Dimensions(348, 88))
        assertThat(placed.tag).isEqualTo(AppWidgetSurface.MOBILE_LOCK_SCREEN.tag)
        assertThat(placed).isEqualTo(AppWidgetSurface.MOBILE_LOCK_SCREEN)
        assertThat(placed.hashCode()).isEqualTo(AppWidgetSurface.MOBILE_LOCK_SCREEN.hashCode())
        assertThat(placed).isNotEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)
        // The constant itself is left untouched.
        assertThat(AppWidgetSurface.MOBILE_LOCK_SCREEN.dimensions).isEqualTo(Dimensions(0, 0))
    }
}
