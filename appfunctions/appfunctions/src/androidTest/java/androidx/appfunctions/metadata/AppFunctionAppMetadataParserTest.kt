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

import android.content.Context
import androidx.appfunctions.metadata.AppFunctionPackageMetadata.Companion.parseAppFunctionAppMetadata
import androidx.appfunctions.test.R
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionAppMetadataParserTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun parse_instructionsAttribute() {
        val metadata =
            parseAppFunctionAppMetadata(
                context.resources.getXml(R.xml.app_metadata),
                context.resources,
            )

        assertThat(metadata?.instructions).startsWith("* Use noSchema_enabledByDefault")
        assertThat(metadata?.displayDescription).isEqualTo(context.getString(R.string.description))
    }

    @Test
    fun parse_legacyDescriptionAttribute_fallsBackToInstructions() {
        val metadata =
            parseAppFunctionAppMetadata(
                context.resources.getXml(R.xml.legacy_app_metadata),
                context.resources,
            )

        assertThat(metadata)
            .isEqualTo(
                AppFunctionAppMetadata(
                    instructions = "Legacy instructions",
                    displayDescription = "Legacy display description",
                )
            )
    }

    @Test
    fun parse_bothAttributes_prefersInstructions() {
        val metadata =
            parseAppFunctionAppMetadata(
                context.resources.getXml(R.xml.instructions_and_legacy_app_metadata),
                context.resources,
            )

        assertThat(metadata?.instructions).isEqualTo("New instructions")
    }
}
