/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.material3

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.hasHeight
import androidx.wear.protolayout.testing.hasWidth
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class CircularProgressIndicatorTest {
    @Test
    fun containerSize_default() {
        LayoutElementAssertionsProvider(DEFAULT_PROGRESS_INDICATOR)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(expand()))
    }

    @Test
    fun containerSize_wrap_failure() {
        assertThrows(IllegalArgumentException::class.java) {
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                circularProgressIndicator(
                    staticProgress = STATIC_PROGRESS,
                    size = wrapWithMinTapTargetDimension(),
                )
            }
        }
    }

    // TODO: b/372916396 - More tests when the testing framework also deals with ArcLayoutElements

    companion object {
        private val CONTEXT = getApplicationContext() as Context

        private val DEVICE_CONFIGURATION =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .setRendererSchemaVersion(VersionInfo.Builder().setMajor(1).setMinor(403).build())
                .build()

        private const val STATIC_PROGRESS = 0.5F

        private val DEFAULT_PROGRESS_INDICATOR =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                circularProgressIndicator(staticProgress = STATIC_PROGRESS)
            }
    }
}
