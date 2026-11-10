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

package androidx.appfunctions.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionBehaviorHintsMetadataTest {

    @Test
    fun appFunctionBehaviorHintsMetadata_defaultValues() {
        val hints = AppFunctionBehaviorHintsMetadata()

        assertThat(hints.isReadOnly).isFalse()
        assertThat(hints.isDestructive).isTrue()
        assertThat(hints.isIdempotent).isFalse()
        assertThat(hints.isOpenWorld).isTrue()
    }

    @Test
    fun appFunctionBehaviorHintsMetadata_equalsAndHashCode() {
        val hints1 =
            AppFunctionBehaviorHintsMetadata(
                isReadOnly = true,
                isDestructive = false,
                isIdempotent = true,
                isOpenWorld = false,
            )
        val hints2 =
            AppFunctionBehaviorHintsMetadata(
                isReadOnly = true,
                isDestructive = false,
                isIdempotent = true,
                isOpenWorld = false,
            )
        val hints3 =
            AppFunctionBehaviorHintsMetadata(
                isReadOnly = false,
                isDestructive = true,
                isIdempotent = false,
                isOpenWorld = true,
            )

        assertThat(hints1).isEqualTo(hints2)
        assertThat(hints1.hashCode()).isEqualTo(hints2.hashCode())

        assertThat(hints1).isNotEqualTo(hints3)
        assertThat(hints1.hashCode()).isNotEqualTo(hints3.hashCode())
    }

    @Test
    fun toAppFunctionBehaviorHintsMetadataDocument_returnCorrectDocument() {
        val metadata =
            AppFunctionBehaviorHintsMetadata(
                isReadOnly = true,
                isDestructive = false,
                isIdempotent = true,
                isOpenWorld = false,
            )

        val document = metadata.toAppFunctionBehaviorHintsMetadataDocument()

        assertThat(document)
            .isEqualTo(
                AppFunctionBehaviorHintsMetadataDocument(
                    isReadOnly = true,
                    isDestructive = false,
                    isIdempotent = true,
                    isOpenWorld = false,
                )
            )
    }

    @Test
    fun toAppFunctionBehaviorHintsMetadata_returnCorrectMetadata() {
        val document =
            AppFunctionBehaviorHintsMetadataDocument(
                isReadOnly = true,
                isDestructive = false,
                isIdempotent = true,
                isOpenWorld = false,
            )

        val metadata = document.toAppFunctionBehaviorHintsMetadata()

        assertThat(metadata)
            .isEqualTo(
                AppFunctionBehaviorHintsMetadata(
                    isReadOnly = true,
                    isDestructive = false,
                    isIdempotent = true,
                    isOpenWorld = false,
                )
            )
    }

    @Test
    fun toAppFunctionBehaviorHintsMetadata_nullProperties_returnsDefaultMetadata() {
        val document = AppFunctionBehaviorHintsMetadataDocument()

        val metadata = document.toAppFunctionBehaviorHintsMetadata()

        assertThat(metadata)
            .isEqualTo(
                AppFunctionBehaviorHintsMetadata(
                    isReadOnly = false,
                    isDestructive = true,
                    isIdempotent = false,
                    isOpenWorld = true,
                )
            )
    }
}
