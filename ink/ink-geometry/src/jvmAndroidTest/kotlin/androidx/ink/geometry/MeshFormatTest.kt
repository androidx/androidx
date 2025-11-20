/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MeshFormatTest {

    private fun DefaultMeshFormat() = MeshFormat.wrapNative(nativeDefaultMeshFormat())

    private fun FullMeshFormat() = MeshFormat.wrapNative(nativeFullMeshFormat())

    @UsedByNative private external fun nativeDefaultMeshFormat(): Long

    @UsedByNative private external fun nativeFullMeshFormat(): Long

    @Test
    fun isPackedEquivalent_withSameInstance_returnsTrue() {
        val meshFormat = Mesh().format
        assertThat(meshFormat.isPackedEquivalent(meshFormat)).isTrue()
    }

    @Test
    fun isPackedEquivalent_withEquivalent_returnsTrue() {
        val meshFormat = Mesh().format
        assertThat(meshFormat.isPackedEquivalent(Mesh().format)).isTrue()
    }

    @Test
    fun isUnpackedEquivalent_withSameInstance_returnsTrue() {
        val meshFormat = Mesh().format
        assertThat(meshFormat.isUnpackedEquivalent(meshFormat)).isTrue()
    }

    @Test
    fun isUnpackedEquivalent_withEquivalent_returnsTrue() {
        val meshFormat = Mesh().format
        assertThat(meshFormat.isUnpackedEquivalent(Mesh().format)).isTrue()
    }

    @Test
    fun attributeCount_returnsCorrectCount() {
        val meshFormat = Mesh().format
        assertThat(meshFormat.attributeCount()).isEqualTo(1)
    }
}
