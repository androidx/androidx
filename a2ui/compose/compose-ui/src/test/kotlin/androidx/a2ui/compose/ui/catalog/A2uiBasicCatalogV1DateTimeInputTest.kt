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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiBooleanSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1DateTimeInputTest {

    @Test
    fun interfaceDefaults_haveExpectedValues() {
        val dateTimeInputComponent =
            object : A2uiBasicCatalogV1.DateTimeInput {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    value: Long?,
                    onValueChange: ((Long?) -> Unit)?,
                    enableDate: Boolean,
                    enableTime: Boolean,
                    min: Long?,
                    max: Long?,
                    label: String?,
                    modifier: Modifier,
                ) {}
            }

        assertThat(dateTimeInputComponent.name).isEqualTo("DateTimeInput")
        assertThat(dateTimeInputComponent.description)
            .isEqualTo("Allows the user to select a date and/or time.")
        assertThat(dateTimeInputComponent.properties)
            .containsExactly(
                A2uiBasicCatalogV1.WeightProperty,
                A2uiBasicCatalogV1.DateTimeInput.ValueProperty,
                A2uiBasicCatalogV1.DateTimeInput.EnableDateProperty,
                A2uiBasicCatalogV1.DateTimeInput.EnableTimeProperty,
                A2uiBasicCatalogV1.DateTimeInput.MinProperty,
                A2uiBasicCatalogV1.DateTimeInput.MaxProperty,
                A2uiBasicCatalogV1.DateTimeInput.LabelProperty,
            )
            .inOrder()
    }

    @Test
    fun companionProperties_haveExpectedSchema() {
        assertThat(A2uiBasicCatalogV1.DateTimeInput.ValueProperty.key).isEqualTo("value")
        assertThat(A2uiBasicCatalogV1.DateTimeInput.ValueProperty.isRequired).isTrue()
        assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.DateTimeInput.ValueProperty.schema)

        assertThat(A2uiBasicCatalogV1.DateTimeInput.EnableDateProperty.key).isEqualTo("enableDate")
        assertThat(A2uiBasicCatalogV1.DateTimeInput.EnableDateProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.DateTimeInput.EnableDateProperty.schema.keywords)
            .containsExactly(A2uiSchemaKeyword.Default(false))
        assertIs<A2uiBooleanSchema>(A2uiBasicCatalogV1.DateTimeInput.EnableDateProperty.schema)

        assertThat(A2uiBasicCatalogV1.DateTimeInput.EnableTimeProperty.key).isEqualTo("enableTime")
        assertThat(A2uiBasicCatalogV1.DateTimeInput.EnableTimeProperty.isRequired).isFalse()
        assertThat(A2uiBasicCatalogV1.DateTimeInput.EnableTimeProperty.schema.keywords)
            .containsExactly(A2uiSchemaKeyword.Default(false))
        assertIs<A2uiBooleanSchema>(A2uiBasicCatalogV1.DateTimeInput.EnableTimeProperty.schema)

        assertThat(A2uiBasicCatalogV1.DateTimeInput.MinProperty.key).isEqualTo("min")
        assertThat(A2uiBasicCatalogV1.DateTimeInput.MinProperty.isRequired).isFalse()
        assertDateTimeFormatConstraints(A2uiBasicCatalogV1.DateTimeInput.MinProperty.schema)

        assertThat(A2uiBasicCatalogV1.DateTimeInput.MaxProperty.key).isEqualTo("max")
        assertThat(A2uiBasicCatalogV1.DateTimeInput.MaxProperty.isRequired).isFalse()
        assertDateTimeFormatConstraints(A2uiBasicCatalogV1.DateTimeInput.MaxProperty.schema)

        assertThat(A2uiBasicCatalogV1.DateTimeInput.LabelProperty.key).isEqualTo("label")
        assertThat(A2uiBasicCatalogV1.DateTimeInput.LabelProperty.isRequired).isFalse()
        assertIs<A2uiDynamicStringSchema>(A2uiBasicCatalogV1.DateTimeInput.LabelProperty.schema)
    }

    private fun assertDateTimeFormatConstraints(schema: A2uiSchema) {
        assertIs<A2uiAnySchema>(schema)
        val allOf = schema.keywords.single()
        assertIs<A2uiSchemaKeyword.AllOf>(allOf)
        assertThat(allOf.schemas).hasSize(2)
        assertIs<A2uiDynamicStringSchema>(allOf.schemas.first())

        val formatConstraintSchema = allOf.schemas.last()
        assertIs<A2uiAnySchema>(formatConstraintSchema)
        val ifThen = formatConstraintSchema.keywords.single()
        assertIs<A2uiSchemaKeyword.IfThen>(ifThen)
        assertIs<A2uiStringSchema>(ifThen.ifSchema)
        assertThat(ifThen.elseSchema).isNull()

        val thenSchema = ifThen.thenSchema
        assertIs<A2uiAnySchema>(thenSchema)
        val oneOf = thenSchema.keywords.single()
        assertIs<A2uiSchemaKeyword.OneOf>(oneOf)
        val formats =
            oneOf.schemas.map {
                assertIs<A2uiAnySchema>(it)
                val formatKeyword = it.keywords.single()
                assertIs<A2uiSchemaKeyword.Format>(formatKeyword)
                formatKeyword.format
            }
        assertThat(formats).containsExactly("date", "time", "date-time").inOrder()
    }

    @Test
    fun parseIsoDateTimeToUtcMillis_supportedFormats_parsedCorrectly() {
        assertThat(parseIsoDateTimeToUtcMillis("2026-03-24T15:25:00Z")).isEqualTo(1774365900000L)
        assertThat(parseIsoDateTimeToUtcMillis("2026-03-24T15:25:00.123Z"))
            .isEqualTo(1774365900123L)
        assertThat(parseIsoDateTimeToUtcMillis("2026-03-24T15:25:00")).isEqualTo(1774365900000L)
        assertThat(parseIsoDateTimeToUtcMillis("2026-03-24T15:25:00.123")).isEqualTo(1774365900123L)
        assertThat(parseIsoDateTimeToUtcMillis("2026-03-24T15:25")).isEqualTo(1774365900000L)
        assertThat(parseIsoDateTimeToUtcMillis("2026-03-24")).isEqualTo(1774310400000L)
        assertThat(parseIsoDateTimeToUtcMillis("15:25:00")).isEqualTo(55500000L)
        assertThat(parseIsoDateTimeToUtcMillis("15:25:00.123")).isEqualTo(55500123L)
        assertThat(parseIsoDateTimeToUtcMillis("15:25")).isEqualTo(55500000L)

        assertThat(parseIsoDateTimeToUtcMillis("")).isNull()
        assertThat(parseIsoDateTimeToUtcMillis("not-a-date")).isNull()
        assertThat(parseIsoDateTimeToUtcMillis(null)).isNull()
    }

    @Test
    fun formatUtcMillisToIso_formatsWithExpectedPatterns() {
        val millis = 1774365900000L // 2026-03-24T15:25:00 UTC
        assertThat(formatUtcMillisToIso(millis, enableDate = true, enableTime = true))
            .isEqualTo("2026-03-24T15:25:00Z")
        assertThat(formatUtcMillisToIso(millis, enableDate = true, enableTime = false))
            .isEqualTo("2026-03-24")
        assertThat(formatUtcMillisToIso(millis, enableDate = false, enableTime = true))
            .isEqualTo("15:25:00")
        assertThat(formatUtcMillisToIso(null, enableDate = true, enableTime = true)).isNull()
    }
}
