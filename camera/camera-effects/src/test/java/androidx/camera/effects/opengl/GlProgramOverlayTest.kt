/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.effects.opengl

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [GlProgramOverlay]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class GlProgramOverlayTest {

    @Test
    fun createWithNoQueue_usesExternalSampler() {
        assertThat(GlProgramOverlay(0).fragmentShader)
            .contains("uniform samplerExternalOES samplerInputTexture")
    }

    @Test
    fun createWithQueue_uses2DSampler() {
        assertThat(GlProgramOverlay(1).fragmentShader)
            .contains("uniform sampler2D samplerInputTexture")
    }
}
