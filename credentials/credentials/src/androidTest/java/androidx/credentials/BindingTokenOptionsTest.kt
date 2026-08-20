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

package androidx.credentials

import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalDigitalCredentialApi::class)
class BindingTokenOptionsTest {

    @Test
    fun constructor_defaultAlgorithm_sha256() {
        val proofingToken = byteArrayOf(1, 2, 3)
        val options = BindingTokenOptions(proofingToken)

        assertThat(options.proofingToken).isEqualTo(proofingToken)
        assertThat(options.bindingAlgorithm).isEqualTo(BindingTokenOptions.ALGORITHM_SHA_256)
    }

    @Test
    fun constructor_customAlgorithm_valid() {
        val proofingToken = byteArrayOf(1, 2, 3, 4, 5)
        val options = BindingTokenOptions(proofingToken, BindingTokenOptions.ALGORITHM_SHA_384)

        assertThat(options.proofingToken).isEqualTo(proofingToken)
        assertThat(options.bindingAlgorithm).isEqualTo(BindingTokenOptions.ALGORITHM_SHA_384)
    }

    @Test
    fun equals_and_hashCode() {
        val token1 = byteArrayOf(1, 2, 3)
        val token2 = byteArrayOf(1, 2, 3)
        val token3 = byteArrayOf(4, 5, 6)

        val options1 = BindingTokenOptions(token1, BindingTokenOptions.ALGORITHM_SHA_256)
        val options2 = BindingTokenOptions(token2, BindingTokenOptions.ALGORITHM_SHA_256)
        val options3 = BindingTokenOptions(token3, BindingTokenOptions.ALGORITHM_SHA_256)
        val options4 = BindingTokenOptions(token1, BindingTokenOptions.ALGORITHM_SHA_384)

        assertThat(options1).isEqualTo(options2)
        assertThat(options1.hashCode()).isEqualTo(options2.hashCode())

        assertThat(options1).isNotEqualTo(options3)
        assertThat(options1).isNotEqualTo(options4)
    }
}
