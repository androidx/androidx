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

package androidx.ink.brush.behavior

import androidx.ink.brush.ExperimentalInkCustomBrushApi
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class ConstantNodeTest {

    @Test
    fun constantNodeConstructor_throwsForNonFiniteValue() {
        assertFailsWith<IllegalArgumentException> { ConstantNode(Float.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { ConstantNode(Float.NaN) }
    }

    @Test
    fun constantNodeInputs_isEmpty() {
        assertThat(ConstantNode(42f).inputs).isEmpty()
    }

    @Test
    fun constantNodeToString() {
        assertThat(ConstantNode(42f).toString()).isEqualTo("ConstantNode(42.0)")
    }

    @Test
    fun constantNodeEquals_checksEqualityOfValues() {
        val node1 = ConstantNode(1f)
        val node2 = ConstantNode(1f)
        val node3 = ConstantNode(2f)
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun constantNodeHashCode_withIdenticalValues_match() {
        val node1 = ConstantNode(1f)
        val node2 = ConstantNode(1f)
        val node3 = ConstantNode(2f)
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }
}
