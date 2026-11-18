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

import android.os.Bundle
import android.os.Parcel
import android.os.PatternMatcher
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionDataTypeMetadataBundleTest {

    @Test
    fun roundTrip_int() =
        assertRoundTrip(
            AppFunctionIntTypeMetadata(
                isNullable = true,
                description = DESCRIPTION,
                enumValues = setOf(1, 2, 3),
            )
        )

    @Test
    fun roundTrip_long() =
        assertRoundTrip(AppFunctionLongTypeMetadata(isNullable = true, description = DESCRIPTION))

    @Test
    fun roundTrip_float() =
        assertRoundTrip(AppFunctionFloatTypeMetadata(isNullable = true, description = DESCRIPTION))

    @Test
    fun roundTrip_double() =
        assertRoundTrip(AppFunctionDoubleTypeMetadata(isNullable = true, description = DESCRIPTION))

    @Test
    fun roundTrip_boolean() =
        assertRoundTrip(
            AppFunctionBooleanTypeMetadata(isNullable = true, description = DESCRIPTION)
        )

    @Test
    fun roundTrip_bytes() =
        assertRoundTrip(AppFunctionBytesTypeMetadata(isNullable = true, description = DESCRIPTION))

    @Test
    fun roundTrip_unit() =
        assertRoundTrip(AppFunctionUnitTypeMetadata(isNullable = true, description = DESCRIPTION))

    @Test
    fun roundTrip_string() =
        assertRoundTrip(
            AppFunctionStringTypeMetadata(
                isNullable = true,
                description = DESCRIPTION,
                enumValues = setOf("content://a", "content://b"),
                patternMatchers =
                    listOf(
                        PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
                        PatternMatcher("file:", PatternMatcher.PATTERN_PREFIX),
                    ),
                format = AppFunctionStringTypeMetadata.FORMAT_URI,
            )
        )

    @Test
    fun roundTrip_parcelable() =
        assertRoundTrip(
            AppFunctionParcelableTypeMetadata(
                qualifiedName = "android.net.Uri",
                isNullable = true,
                description = DESCRIPTION,
            )
        )

    @Test
    fun roundTrip_reference() =
        assertRoundTrip(
            AppFunctionReferenceTypeMetadata(
                referenceDataType = "com.example.Note",
                isNullable = true,
                description = DESCRIPTION,
            )
        )

    @Test
    fun roundTrip_array() =
        assertRoundTrip(
            AppFunctionArrayTypeMetadata(
                itemType = AppFunctionIntTypeMetadata(isNullable = true, enumValues = setOf(1)),
                isNullable = true,
                description = DESCRIPTION,
            )
        )

    @Test
    fun roundTrip_object_preservesPropertiesAndOrder() {
        val metadata =
            AppFunctionObjectTypeMetadata(
                properties =
                    linkedMapOf(
                        "zebra" to AppFunctionStringTypeMetadata(true, enumValues = setOf("a")),
                        "alpha" to AppFunctionIntTypeMetadata(false, enumValues = setOf(1, 2)),
                        "middle" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionStringTypeMetadata(false),
                                isNullable = true,
                            ),
                    ),
                required = listOf("alpha"),
                qualifiedName = "com.example.Note",
                isNullable = true,
                description = DESCRIPTION,
            )

        val result = AppFunctionDataTypeMetadata.fromBundle(metadata.toBundle().parceled())

        assertThat(result).isEqualTo(metadata)
        assertThat((result as AppFunctionObjectTypeMetadata).properties.keys.toList())
            .containsExactly("zebra", "alpha", "middle")
            .inOrder()
    }

    @Test
    fun roundTrip_emptyObject() =
        assertRoundTrip(
            AppFunctionObjectTypeMetadata(
                properties = emptyMap(),
                required = emptyList(),
                qualifiedName = null,
                isNullable = false,
            )
        )

    @Test
    fun roundTrip_allOf() =
        assertRoundTrip(
            AppFunctionAllOfTypeMetadata(
                matchAll =
                    listOf(
                        AppFunctionReferenceTypeMetadata("com.example.Base", false),
                        AppFunctionObjectTypeMetadata(
                            properties = mapOf("id" to AppFunctionLongTypeMetadata(false)),
                            required = listOf("id"),
                            qualifiedName = null,
                            isNullable = false,
                        ),
                    ),
                qualifiedName = "com.example.Derived",
                isNullable = true,
                description = DESCRIPTION,
            )
        )

    @Test
    fun roundTrip_oneOf() =
        assertRoundTrip(
            AppFunctionOneOfTypeMetadata(
                matchOneOf =
                    listOf(
                        AppFunctionReferenceTypeMetadata("com.example.Dog", false),
                        AppFunctionReferenceTypeMetadata("com.example.Cat", false),
                    ),
                qualifiedName = "com.example.Animal",
                isNullable = true,
                description = DESCRIPTION,
            )
        )

    @Test
    fun fromBundle_missingType_throws() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionDataTypeMetadata.fromBundle(Bundle())
        }
    }

    @Test
    fun fromBundle_unknownType_throws() {
        val bundle = Bundle().apply { putInt("type", Int.MAX_VALUE) }

        assertFailsWith<IllegalArgumentException> {
            AppFunctionDataTypeMetadata.fromBundle(bundle)
        }
    }

    @Test
    fun fromBundle_wrongTypeForTypeKey_throws() {
        val bundle = Bundle().apply { putString("type", "notAnInt") }

        assertFailsWith<IllegalArgumentException> {
            AppFunctionDataTypeMetadata.fromBundle(bundle)
        }
    }

    @Test
    fun toBundle_nestedTooDeep_throws() {
        var metadata: AppFunctionDataTypeMetadata = AppFunctionIntTypeMetadata(isNullable = false)
        repeat(70) {
            metadata = AppFunctionArrayTypeMetadata(itemType = metadata, isNullable = false)
        }

        assertFailsWith<IllegalArgumentException> { metadata.toBundle() }
    }

    @Test
    fun fromBundle_nestedTooDeep_throws() {
        var bundle = Bundle().apply { putInt("type", AppFunctionDataTypeMetadata.TYPE_INT) }
        repeat(70) {
            val child = bundle
            bundle =
                Bundle().apply {
                    putInt("type", AppFunctionDataTypeMetadata.TYPE_ARRAY)
                    putBundle("itemType", child)
                }
        }

        assertFailsWith<IllegalArgumentException> {
            AppFunctionDataTypeMetadata.fromBundle(bundle)
        }
    }

    @Test
    fun fromBundle_nestedWithinLimit_succeeds() {
        var metadata: AppFunctionDataTypeMetadata = AppFunctionIntTypeMetadata(isNullable = false)
        repeat(32) {
            metadata = AppFunctionArrayTypeMetadata(itemType = metadata, isNullable = false)
        }

        val result = AppFunctionDataTypeMetadata.fromBundle(metadata.toBundle().parceled())

        assertThat(result).isEqualTo(metadata)
    }

    @Test
    fun fromBundle_stringNullEnumValue_throws() {
        val bundle =
            Bundle().apply {
                putInt("type", AppFunctionDataTypeMetadata.TYPE_STRING)
                putStringArray("enumValues", arrayOf("a", null))
            }

        assertFailsWith<IllegalArgumentException> {
            AppFunctionDataTypeMetadata.fromBundle(bundle)
        }
    }

    @Test
    fun fromBundle_objectNullRequiredProperty_throws() {
        val bundle =
            Bundle().apply {
                putInt("type", AppFunctionDataTypeMetadata.TYPE_OBJECT)
                putStringArray("propertyNames", emptyArray())
                putBundle("properties", Bundle())
                putStringArray("required", arrayOf("a", null))
            }

        assertFailsWith<IllegalArgumentException> {
            AppFunctionDataTypeMetadata.fromBundle(bundle)
        }
    }

    private fun assertRoundTrip(metadata: AppFunctionDataTypeMetadata) {
        val result = AppFunctionDataTypeMetadata.fromBundle(metadata.toBundle().parceled())

        assertThat(result).isEqualTo(metadata)
    }

    private fun Bundle.parceled(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val DESCRIPTION = "A description"
    }
}
