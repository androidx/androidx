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

package androidx.camera.viewfinder.impl

import android.os.Build
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationMode
import androidx.camera.viewfinder.core.impl.ImplementationModeCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument // Needed for Robolectric to correctly instrument classes in the same module
@Config(sdk = [Config.ALL_SDKS])
class ImplementationModeCompatTest {

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    fun defaultMode_preAppliedSource_usesEmbedded_belowApi29() {
        assertThat(
                ImplementationModeCompat.resolveImplementationMode(
                    implementationMode = null,
                    transformationMode = TransformationMode.PRE_APPLIED,
                )
            )
            .isEqualTo(ImplementationMode.EMBEDDED)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun defaultMode_preAppliedSource_usesExternal_onApi29AndAbove() {
        assertThat(
                ImplementationModeCompat.resolveImplementationMode(
                    implementationMode = null,
                    transformationMode = TransformationMode.PRE_APPLIED,
                )
            )
            .isEqualTo(ImplementationMode.EXTERNAL)
    }

    @Test
    fun defaultMode_deferredSource_matchesChooseCompatibleMode() {
        assertThat(
                ImplementationModeCompat.resolveImplementationMode(
                    implementationMode = null,
                    transformationMode = TransformationMode.DEFERRED,
                )
            )
            .isEqualTo(ImplementationModeCompat.chooseCompatibleMode())
    }

    @Test
    fun explicitMode_isPreservedForAllTransformationModes() {
        for (mode in listOf(ImplementationMode.EXTERNAL, ImplementationMode.EMBEDDED)) {
            for (transformationMode in
                listOf(TransformationMode.DEFERRED, TransformationMode.PRE_APPLIED)) {
                assertThat(
                        ImplementationModeCompat.resolveImplementationMode(mode, transformationMode)
                    )
                    .isEqualTo(mode)
            }
        }
    }
}
