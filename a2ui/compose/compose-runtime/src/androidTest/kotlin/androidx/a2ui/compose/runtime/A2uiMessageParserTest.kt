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

package androidx.a2ui.compose.runtime

import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A2uiMessageParserTest {

    private val parser = A2uiMessageParser()

    @Test
    fun parse_createSurfaceMessage_parsesCorrectly() {
        val json =
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "surf_1",
                "catalogId": "test_catalog_v1",
                "theme": {
                  "primaryColor": "#FF0000"
                },
                "sendDataModel": true
              }
            }
            """
                .trimIndent()

        val message = assertIs<A2uiCreateSurfaceMessage>(parser.parse(json))

        assertThat(message.surfaceId).isEqualTo("surf_1")
        assertThat(message.catalogId).isEqualTo("test_catalog_v1")
        assertThat(message.theme).isEqualTo(mapOf("primaryColor" to "#FF0000"))
        assertThat(message.shouldSendDataModel).isTrue()
    }

    @Test
    fun parse_updateComponentsMessage_parsesCorrectly() {
        val json =
            """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "surf_1",
                "components": [
                  {
                    "id": "btn_submit",
                    "component": "Button",
                    "variant": "primary",
                    "action": {
                      "event": {
                        "name": "submit_form"
                      }
                    }
                  }
                ]
              }
            }
            """
                .trimIndent()

        val message = assertIs<A2uiUpdateComponentsMessage>(parser.parse(json))

        assertThat(message.surfaceId).isEqualTo("surf_1")
        assertThat(message.components).hasSize(1)
        val component = message.components.first()
        assertThat(component.id).isEqualTo("btn_submit")
        assertThat(component.type).isEqualTo("Button")
        assertThat(component.properties)
            .isEqualTo(
                mapOf(
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "submit_form")),
                )
            )
    }

    @Test
    fun parse_updateDataModelMessage_parsesCorrectly() {
        val json =
            """
            {
              "version": "v0.9",
              "updateDataModel": {
                "surfaceId": "surf_1",
                "path": "/user/profile",
                "value": {
                  "name": "Alice",
                  "age": 30,
                  "balance": 150.75,
                  "isActive": true,
                  "tags": ["admin", "beta_tester", null],
                  "preferences": null
                }
              }
            }
            """
                .trimIndent()

        val message = assertIs<A2uiUpdateDataModelMessage>(parser.parse(json))

        assertThat(message.surfaceId).isEqualTo("surf_1")
        assertThat(message.path).isEqualTo("/user/profile")
        val valueMap = assertIs<Map<*, *>>(message.value)
        assertThat(valueMap["name"]).isEqualTo("Alice")
        assertThat(valueMap["age"]).isEqualTo(30)
        assertThat(valueMap["balance"]).isEqualTo(150.75)
        assertThat(valueMap["isActive"]).isEqualTo(true)
        assertThat(valueMap["tags"]).isEqualTo(listOf("admin", "beta_tester", null))
        assertThat(valueMap.containsKey("preferences")).isTrue()
        assertThat(valueMap["preferences"]).isNull()
    }

    @Test
    fun parse_deleteSurfaceMessage_parsesCorrectly() {
        val json =
            """
            {
              "version": "v0.9",
              "deleteSurface": {
                "surfaceId": "surf_1"
              }
            }
            """
                .trimIndent()

        val message = assertIs<A2uiDeleteSurfaceMessage>(parser.parse(json))

        assertThat(message.surfaceId).isEqualTo("surf_1")
    }

    @Test
    fun parse_emptyJsonString_throwsValidationException() {
        val exception = assertFailsWith<A2uiValidationException> { parser.parse("") }

        assertThat(exception.message).contains("Malformed JSON message")
    }

    @Test
    fun parse_malformedJsonSyntax_throwsValidationException() {
        val json =
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "surf_1"
            """
                .trimIndent()

        val exception = assertFailsWith<A2uiValidationException> { parser.parse(json) }

        assertThat(exception.message).contains("Malformed JSON message")
    }

    @Test
    fun parse_emptyJsonObject_throwsValidationException() {
        val exception =
            assertFailsWith<A2uiValidationException> { parser.parse("{\"version\": \"v0.9\"}") }

        assertThat(exception.context["path"]).isEqualTo("/")
        assertThat(exception.message).contains("Empty or invalid A2UI message envelope")
    }

    @Test
    fun parse_multipleEnvelopes_throwsValidationException() {
        val json =
            """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "surf_1",
                "catalogId": "cat_1"
              },
              "deleteSurface": {
                "surfaceId": "surf_1"
              }
            }
            """
                .trimIndent()

        val exception = assertFailsWith<A2uiValidationException> { parser.parse(json) }

        assertThat(exception.context["path"]).isEqualTo("/")
        assertThat(exception.message).contains("Multiple message envelopes found")
    }

    @Test
    fun parse_missingVersion_throwsValidationException() {
        val json =
            """
            {
              "createSurface": {
                "surfaceId": "surf_1",
                "catalogId": "test_catalog_v1"
              }
            }
            """
                .trimIndent()

        val exception = assertFailsWith<A2uiValidationException> { parser.parse(json) }

        assertThat(exception.context["path"]).isEqualTo("/version")
        assertThat(exception.message).contains("Missing or empty 'version' in message envelope")
    }

    @Test
    fun parse_unsupportedVersion_throwsValidationException() {
        val json =
            """
            {
              "version": "v9.9",
              "createSurface": {
                "surfaceId": "surf_1",
                "catalogId": "test_catalog_v1"
              }
            }
            """
                .trimIndent()

        val exception = assertFailsWith<A2uiValidationException> { parser.parse(json) }

        assertThat(exception.context["path"]).isEqualTo("/version")
        assertThat(exception.message).contains("Unsupported protocol version: v9.9")
    }
}
