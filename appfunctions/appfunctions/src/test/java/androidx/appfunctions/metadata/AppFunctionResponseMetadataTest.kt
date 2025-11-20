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

import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata.Companion.TYPE_INT
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionResponseMetadataTest {

    @Test
    fun appFunctionResponseMetadata_toAppFunctionResponseMetadataDocument_returnsCorrectDocument() {
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionIntTypeMetadata(isNullable = false),
                description = "Test response description",
            )

        val responseMetadataDocument = responseMetadata.toAppFunctionResponseMetadataDocument()

        assertThat(responseMetadataDocument)
            .isEqualTo(
                AppFunctionResponseMetadataDocument(
                    valueType =
                        AppFunctionDataTypeMetadataDocument(
                            type = AppFunctionDataTypeMetadata.TYPE_INT,
                            isNullable = false,
                        ),
                    description = "Test response description",
                )
            )
    }

    @Test
    fun appFunctionResponseMetadataDocument_toAppFunctionResponseMetadata_returnsCorrectMetadata() {
        val responseMetadataDocument =
            AppFunctionResponseMetadataDocument(
                valueType =
                    AppFunctionDataTypeMetadataDocument(type = TYPE_INT, isNullable = false),
                description = "Test response description",
            )

        val responseMetadata = responseMetadataDocument.toAppFunctionResponseMetadata()

        assertThat(responseMetadata)
            .isEqualTo(
                AppFunctionResponseMetadata(
                    valueType = AppFunctionIntTypeMetadata(false),
                    description = "Test response description",
                )
            )
    }
}
