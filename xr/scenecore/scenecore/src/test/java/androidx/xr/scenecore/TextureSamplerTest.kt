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

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextureSamplerTest {

    @Test
    fun wrapMode_toString() {
        assertThat(TextureSampler.WrapMode.CLAMP_TO_EDGE.toString()).isEqualTo("CLAMP_TO_EDGE")
        assertThat(TextureSampler.WrapMode.REPEAT.toString()).isEqualTo("REPEAT")
        assertThat(TextureSampler.WrapMode.MIRRORED_REPEAT.toString()).isEqualTo("MIRRORED_REPEAT")
    }

    @Test
    fun minificationFilter_toString() {
        assertThat(TextureSampler.MinificationFilter.NEAREST.toString()).isEqualTo("NEAREST")
        assertThat(TextureSampler.MinificationFilter.LINEAR.toString()).isEqualTo("LINEAR")
        assertThat(TextureSampler.MinificationFilter.NEAREST_MIPMAP_NEAREST.toString())
            .isEqualTo("NEAREST_MIPMAP_NEAREST")
        assertThat(TextureSampler.MinificationFilter.LINEAR_MIPMAP_NEAREST.toString())
            .isEqualTo("LINEAR_MIPMAP_NEAREST")
        assertThat(TextureSampler.MinificationFilter.NEAREST_MIPMAP_LINEAR.toString())
            .isEqualTo("NEAREST_MIPMAP_LINEAR")
        assertThat(TextureSampler.MinificationFilter.LINEAR_MIPMAP_LINEAR.toString())
            .isEqualTo("LINEAR_MIPMAP_LINEAR")
    }

    @Test
    fun magnificationFilter_toString() {
        assertThat(TextureSampler.MagnificationFilter.NEAREST.toString()).isEqualTo("NEAREST")
        assertThat(TextureSampler.MagnificationFilter.LINEAR.toString()).isEqualTo("LINEAR")
    }

    @Test
    fun compareMode_toString() {
        assertThat(TextureSampler.CompareMode.NONE.toString()).isEqualTo("NONE")
        assertThat(TextureSampler.CompareMode.COMPARE_TO_TEXTURE.toString())
            .isEqualTo("COMPARE_TO_TEXTURE")
    }

    @Test
    fun compareFunction_toString() {
        assertThat(TextureSampler.CompareFunction.LESSER_OR_EQUAL.toString())
            .isEqualTo("LESSER_OR_EQUAL")
        assertThat(TextureSampler.CompareFunction.GREATER_OR_EQUAL.toString())
            .isEqualTo("GREATER_OR_EQUAL")
        assertThat(TextureSampler.CompareFunction.LESSER.toString()).isEqualTo("LESSER")
        assertThat(TextureSampler.CompareFunction.GREATER.toString()).isEqualTo("GREATER")
        assertThat(TextureSampler.CompareFunction.EQUAL.toString()).isEqualTo("EQUAL")
        assertThat(TextureSampler.CompareFunction.NOT_EQUAL.toString()).isEqualTo("NOT_EQUAL")
        assertThat(TextureSampler.CompareFunction.ALWAYS.toString()).isEqualTo("ALWAYS")
        assertThat(TextureSampler.CompareFunction.NEVER.toString()).isEqualTo("NEVER")
    }
}
