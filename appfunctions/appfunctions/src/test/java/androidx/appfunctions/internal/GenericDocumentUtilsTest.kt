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

package androidx.appfunctions.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 31)
class GenericDocumentUtilsTest {

    @Test
    fun testFromPlatformToJetpackGenericDocument_singlePropertiesConverted() {
        val platformDocument =
            android.app.appsearch.GenericDocument.Builder<
                    android.app.appsearch.GenericDocument.Builder<*>
                >(
                    "namespace",
                    "id",
                    "schemaType",
                )
                .setScore(42)
                .setTtlMillis(1000L)
                .setCreationTimestampMillis(2000L)
                .setPropertyLong("longProp", 1L)
                .setPropertyDouble("doubleProp", 3.0)
                .setPropertyBoolean("booleanProp", true)
                .setPropertyBytes("bytesProp", byteArrayOf(1, 2))
                .build()

        val jetpackDocument =
            GenericDocumentUtils.fromPlatformToJetpackGenericDocument(platformDocument)

        assertThat(jetpackDocument.namespace).isEqualTo("namespace")
        assertThat(jetpackDocument.id).isEqualTo("id")
        assertThat(jetpackDocument.schemaType).isEqualTo("schemaType")
        assertThat(jetpackDocument.score).isEqualTo(42)
        assertThat(jetpackDocument.ttlMillis).isEqualTo(1000L)
        assertThat(jetpackDocument.creationTimestampMillis).isEqualTo(2000L)

        assertThat(jetpackDocument.getPropertyLongArray("longProp")?.toList()).containsExactly(1L)
        assertThat(jetpackDocument.getPropertyDoubleArray("doubleProp")?.toList())
            .containsExactly(3.0)
        assertThat(jetpackDocument.getPropertyBooleanArray("booleanProp")?.toList())
            .containsExactly(true)

        val bytesProp = jetpackDocument.getPropertyBytesArray("bytesProp")
        assertThat(bytesProp).isNotNull()
        assertThat(bytesProp!!.size).isEqualTo(1)
        assertThat(bytesProp[0]).isEqualTo(byteArrayOf(1, 2))
    }

    @Test
    fun testFromPlatformToJetpackGenericDocument_allPropertiesConverted() {
        val platformDocument =
            android.app.appsearch.GenericDocument.Builder<
                    android.app.appsearch.GenericDocument.Builder<*>
                >(
                    "namespace",
                    "id",
                    "schemaType",
                )
                .setScore(42)
                .setTtlMillis(1000L)
                .setCreationTimestampMillis(2000L)
                .setPropertyString("stringProp", "hello", "world")
                .setPropertyLong("longProp", 1L, 2L)
                .setPropertyDouble("doubleProp", 3.0, 4.0)
                .setPropertyBoolean("booleanProp", true, false)
                .setPropertyBytes("bytesProp", byteArrayOf(1, 2), byteArrayOf(3, 4))
                .setPropertyDocument(
                    "documentProp",
                    android.app.appsearch.GenericDocument.Builder<
                            android.app.appsearch.GenericDocument.Builder<*>
                        >(
                            "subNamespace",
                            "subId",
                            "subSchemaType",
                        )
                        .setPropertyString("subStringProp", "subValue")
                        .build(),
                )
                .build()

        val jetpackDocument =
            GenericDocumentUtils.fromPlatformToJetpackGenericDocument(platformDocument)

        assertThat(jetpackDocument.namespace).isEqualTo("namespace")
        assertThat(jetpackDocument.id).isEqualTo("id")
        assertThat(jetpackDocument.schemaType).isEqualTo("schemaType")
        assertThat(jetpackDocument.score).isEqualTo(42)
        assertThat(jetpackDocument.ttlMillis).isEqualTo(1000L)
        assertThat(jetpackDocument.creationTimestampMillis).isEqualTo(2000L)

        assertThat(jetpackDocument.getPropertyStringArray("stringProp")?.toList())
            .containsExactly("hello", "world")
            .inOrder()
        assertThat(jetpackDocument.getPropertyLongArray("longProp")?.toList())
            .containsExactly(1L, 2L)
            .inOrder()
        assertThat(jetpackDocument.getPropertyDoubleArray("doubleProp")?.toList())
            .containsExactly(3.0, 4.0)
            .inOrder()
        assertThat(jetpackDocument.getPropertyBooleanArray("booleanProp")?.toList())
            .containsExactly(true, false)
            .inOrder()

        val bytesProp = jetpackDocument.getPropertyBytesArray("bytesProp")
        assertThat(bytesProp).isNotNull()
        assertThat(bytesProp!!.size).isEqualTo(2)
        assertThat(bytesProp[0]).isEqualTo(byteArrayOf(1, 2))
        assertThat(bytesProp[1]).isEqualTo(byteArrayOf(3, 4))

        val subDocuments = jetpackDocument.getPropertyDocumentArray("documentProp")
        assertThat(subDocuments).isNotNull()
        assertThat(subDocuments!!.size).isEqualTo(1)
        val subDocument = subDocuments[0]
        assertThat(subDocument.namespace).isEqualTo("subNamespace")
        assertThat(subDocument.id).isEqualTo("subId")
        assertThat(subDocument.schemaType).isEqualTo("subSchemaType")
        assertThat(subDocument.getPropertyString("subStringProp")).isEqualTo("subValue")
    }
}
