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

package androidx.a2ui.model.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class A2uiInlineCatalogTest {

    @Test
    fun inlineCatalog_contract_returnsExpectedOutputs() {
        val schemaMap = mapOf("id" to "inline1", "components" to emptyMap<String, Any>())
        val inlineCatalog =
            object : A2uiInlineCatalog {
                override val id: String = "inline1"

                override fun toJsonSchemaMap(): Map<String, Any?> = schemaMap

                override fun toJsonSchemaString(): String = schemaMap.toString()
            }

        assertThat(inlineCatalog.id).isEqualTo("inline1")
        assertThat(inlineCatalog.toJsonSchemaMap()).isEqualTo(schemaMap)
        assertThat(inlineCatalog.toJsonSchemaString()).isEqualTo(schemaMap.toString())
    }
}
