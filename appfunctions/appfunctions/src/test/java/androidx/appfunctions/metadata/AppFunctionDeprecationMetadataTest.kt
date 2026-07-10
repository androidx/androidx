/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionDeprecationMetadataTest {
    @Test
    fun appFunctionDeprecationMetadata_equalsAndHashCode() {
        val deprecated1 = AppFunctionDeprecationMetadata(message = "Deprecated 1")
        val deprecated2 = AppFunctionDeprecationMetadata(message = "Deprecated 1")
        val deprecated3 = AppFunctionDeprecationMetadata(message = "Deprecated 2")

        assertThat(deprecated1).isEqualTo(deprecated2)
        assertThat(deprecated1.hashCode()).isEqualTo(deprecated2.hashCode())

        assertThat(deprecated1).isNotEqualTo(deprecated3)
        assertThat(deprecated1.hashCode()).isNotEqualTo(deprecated3.hashCode())
    }

    @Test
    fun toAppFunctionDeprecationMetadataDocument_returnCorrectDocument() {
        val message = "Deprecation Message"
        val metadata = AppFunctionDeprecationMetadata(message)

        val document = metadata.toAppFunctionDeprecationMetadataDocument()

        assertThat(document).isEqualTo(AppFunctionDeprecationMetadataDocument(message = message))
    }

    @Test
    fun toAppFunctionDeprecationMetadata_returnCorrectMetadata() {
        val message = "Deprecation Message"
        val document = AppFunctionDeprecationMetadataDocument(message = message)

        val metadata = document.toAppFunctionDeprecationMetadata()

        assertThat(metadata).isEqualTo(AppFunctionDeprecationMetadata(message))
    }
}
