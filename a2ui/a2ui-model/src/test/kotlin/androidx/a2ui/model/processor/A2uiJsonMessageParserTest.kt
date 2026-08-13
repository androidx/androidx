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

package androidx.a2ui.model.processor

import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiJsonMessageParserTest {

    @Test
    fun parse_validCreateSurfaceMessage_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_CATALOG_ID, TEST_CATALOG_ID)
                            addProperty(FIELD_SEND_DATA_MODEL, true)
                            add(
                                FIELD_THEME,
                                JsonObject().apply {
                                    addProperty(FIELD_PRIMARY_COLOR, TEST_PRIMARY_COLOR)
                                },
                            )
                        },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("")

        assertThat(message)
            .isEqualTo(
                A2uiCreateSurfaceMessage(
                    surfaceId = TEST_SURFACE_ID,
                    catalogId = TEST_CATALOG_ID,
                    theme = mapOf(FIELD_PRIMARY_COLOR to TEST_PRIMARY_COLOR),
                    shouldSendDataModel = true,
                )
            )
    }

    @Test
    fun parse_validUpdateComponentsMessage_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            add(
                                FIELD_COMPONENTS,
                                JsonArray().apply {
                                    add(
                                        JsonObject().apply {
                                            addProperty(FIELD_ID, TEST_COMPONENT_ID)
                                            addProperty(FIELD_COMPONENT, TEST_COMPONENT_TYPE)
                                            addProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)
                                        }
                                    )
                                },
                            )
                        },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("")

        assertThat(message)
            .isEqualTo(
                A2uiUpdateComponentsMessage(
                    surfaceId = TEST_SURFACE_ID,
                    components =
                        listOf(
                            A2uiComponentPayload(
                                id = TEST_COMPONENT_ID,
                                type = TEST_COMPONENT_TYPE,
                                properties = mapOf(TEST_PROPERTY_KEY to TEST_PROPERTY_VALUE),
                            )
                        ),
                )
            )
    }

    @Test
    fun parse_validUpdateDataModelMessage_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, TEST_DATA_MODEL_VALUE)
                        },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("")

        assertThat(message)
            .isEqualTo(
                A2uiUpdateDataModelMessage(
                    surfaceId = TEST_SURFACE_ID,
                    path = TEST_PATH,
                    value = TEST_DATA_MODEL_VALUE,
                )
            )
    }

    @Test
    fun parse_validDeleteSurfaceMessage_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_DELETE_SURFACE,
                        JsonObject().apply { addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID) },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("")

        assertThat(message).isEqualTo(A2uiDeleteSurfaceMessage(surfaceId = TEST_SURFACE_ID))
    }

    @Test
    fun parse_multipleMessageEnvelopes_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_CATALOG_ID, TEST_CATALOG_ID)
                        },
                    )
                    add(
                        FIELD_DELETE_SURFACE,
                        JsonObject().apply { addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID) },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context).containsEntry(FIELD_PATH, "/")
    }

    @Test
    fun parse_standardExceptionThrown_isWrappedInValidationException() {
        val reader =
            object : FakeA2uiJsonReader("") {
                override fun beginObject() {
                    throw RuntimeException(TEST_EXCEPTION_MESSAGE)
                }
            }

        val parser = A2uiJsonMessageParser { reader }
        assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
    }

    @Test
    fun parse_doubleValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, TEST_DOUBLE_VALUE)
                        },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("")
        assertThat(message)
            .isEqualTo(
                A2uiUpdateDataModelMessage(
                    surfaceId = TEST_SURFACE_ID,
                    path = TEST_PATH,
                    value = TEST_DOUBLE_VALUE,
                )
            )
    }

    @Test
    fun parse_intValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, TEST_INT_VALUE)
                        },
                    )
                }
            )

        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("")
        assertThat(message)
            .isEqualTo(
                A2uiUpdateDataModelMessage(
                    surfaceId = TEST_SURFACE_ID,
                    path = TEST_PATH,
                    value = TEST_INT_VALUE,
                )
            )
    }

    @Test
    fun parse_emptyEnvelope_throwsValidationException() {
        val reader = FakeA2uiJsonReader(JsonObject())
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context).containsEntry(FIELD_PATH, "/")
    }

    @Test
    fun parse_invalidEnvelope_throwsValidationException() {
        val reader = FakeA2uiJsonReader(JsonObject().apply { addProperty("unknownField", 123) })
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context).containsEntry(FIELD_PATH, "/")
    }

    @Test
    fun parse_createSurface_missingSurfaceId_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply { addProperty(FIELD_CATALOG_ID, TEST_CATALOG_ID) },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_CREATE_SURFACE/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_createSurface_missingCatalogId_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply { addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID) },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_CREATE_SURFACE/$FIELD_CATALOG_ID")
    }

    @Test
    fun parse_updateComponents_missingSurfaceId_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply { add(FIELD_COMPONENTS, JsonArray()) },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_COMPONENTS/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_updateComponents_missingComponentId_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            add(
                                FIELD_COMPONENTS,
                                JsonArray().apply {
                                    add(
                                        JsonObject().apply {
                                            addProperty(FIELD_COMPONENT, TEST_COMPONENT_TYPE)
                                        }
                                    )
                                },
                            )
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_COMPONENTS/$FIELD_COMPONENTS/0/$FIELD_ID")
    }

    @Test
    fun parse_updateComponents_missingComponentType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            add(
                                FIELD_COMPONENTS,
                                JsonArray().apply {
                                    add(
                                        JsonObject().apply {
                                            addProperty(FIELD_ID, TEST_COMPONENT_ID)
                                        }
                                    )
                                },
                            )
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(
                FIELD_PATH,
                "/$FIELD_UPDATE_COMPONENTS/$FIELD_COMPONENTS/0/$FIELD_COMPONENT",
            )
    }

    @Test
    fun parse_updateDataModel_missingSurfaceId_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, TEST_DATA_MODEL_VALUE)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_DATA_MODEL/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_deleteSurface_missingSurfaceId_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply { add(FIELD_DELETE_SURFACE, JsonObject().apply {}) }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_DELETE_SURFACE/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_createSurface_invalidSurfaceIdType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, true) // Boolean is not a string
                            addProperty(FIELD_CATALOG_ID, TEST_CATALOG_ID)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_CREATE_SURFACE/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_createSurface_invalidCatalogIdType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_CATALOG_ID, true) // Boolean is not a string
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_CREATE_SURFACE/$FIELD_CATALOG_ID")
    }

    @Test
    fun parse_createSurface_invalidThemeType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_CATALOG_ID, TEST_CATALOG_ID)
                            addProperty(FIELD_THEME, true) // Boolean is not a map
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_CREATE_SURFACE/$FIELD_THEME")
    }

    @Test
    fun parse_createSurface_invalidSendDataModelType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_CREATE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_CATALOG_ID, TEST_CATALOG_ID)
                            addProperty(
                                FIELD_SEND_DATA_MODEL,
                                "not-a-boolean",
                            ) // String is not a boolean
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_CREATE_SURFACE/$FIELD_SEND_DATA_MODEL")
    }

    @Test
    fun parse_updateComponents_invalidSurfaceIdType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, true) // Boolean is not a string
                            add(FIELD_COMPONENTS, JsonArray())
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_COMPONENTS/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_updateComponents_invalidComponentIdType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            add(
                                FIELD_COMPONENTS,
                                JsonArray().apply {
                                    add(
                                        JsonObject().apply {
                                            addProperty(FIELD_ID, true) // Boolean is not a string
                                            addProperty(FIELD_COMPONENT, TEST_COMPONENT_TYPE)
                                        }
                                    )
                                },
                            )
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_COMPONENTS/$FIELD_COMPONENTS/0/$FIELD_ID")
    }

    @Test
    fun parse_updateComponents_invalidComponentType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_COMPONENTS,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            add(
                                FIELD_COMPONENTS,
                                JsonArray().apply {
                                    add(
                                        JsonObject().apply {
                                            addProperty(FIELD_ID, TEST_COMPONENT_ID)
                                            addProperty(
                                                FIELD_COMPONENT,
                                                true,
                                            ) // Boolean is not a string
                                        }
                                    )
                                },
                            )
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(
                FIELD_PATH,
                "/$FIELD_UPDATE_COMPONENTS/$FIELD_COMPONENTS/0/$FIELD_COMPONENT",
            )
    }

    @Test
    fun parse_updateDataModel_invalidSurfaceIdType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, true) // Boolean is not a string
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, TEST_DATA_MODEL_VALUE)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_DATA_MODEL/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_updateDataModel_invalidPathType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, true) // Boolean is not a string
                            addProperty(FIELD_VALUE, TEST_DATA_MODEL_VALUE)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_UPDATE_DATA_MODEL/$FIELD_PATH")
    }

    @Test
    fun parse_deleteSurface_invalidSurfaceIdType_throwsValidationException() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_DELETE_SURFACE,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, true) // Boolean is not a string
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) { parser.parse("") }
        assertThat(exception.context)
            .containsEntry(FIELD_PATH, "/$FIELD_DELETE_SURFACE/$FIELD_SURFACE_ID")
    }

    @Test
    fun parse_longValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, 9999999999L)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("") as A2uiUpdateDataModelMessage
        assertThat(message.value).isEqualTo(9999999999L)
    }

    @Test
    fun parse_booleanValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            addProperty(FIELD_VALUE, true)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("") as A2uiUpdateDataModelMessage
        assertThat(message.value).isEqualTo(true)
    }

    @Test
    fun parse_nullValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            add(FIELD_VALUE, com.google.gson.JsonNull.INSTANCE)
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("") as A2uiUpdateDataModelMessage
        assertThat(message.value).isNull()
    }

    @Test
    fun parse_listValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            add(
                                FIELD_VALUE,
                                JsonArray().apply {
                                    add(1)
                                    add("two")
                                },
                            )
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("") as A2uiUpdateDataModelMessage
        assertThat(message.value).isEqualTo(listOf(1, "two"))
    }

    @Test
    fun parse_nestedMapValue_parsesCorrectly() {
        val reader =
            FakeA2uiJsonReader(
                JsonObject().apply {
                    add(
                        FIELD_UPDATE_DATA_MODEL,
                        JsonObject().apply {
                            addProperty(FIELD_SURFACE_ID, TEST_SURFACE_ID)
                            addProperty(FIELD_PATH, TEST_PATH)
                            add(
                                FIELD_VALUE,
                                JsonObject().apply { addProperty("nestedKey", "nestedValue") },
                            )
                        },
                    )
                }
            )
        val parser = A2uiJsonMessageParser { reader }
        val message = parser.parse("") as A2uiUpdateDataModelMessage
        assertThat(message.value).isEqualTo(mapOf("nestedKey" to "nestedValue"))
    }

    companion object {
        private const val FIELD_CREATE_SURFACE = "createSurface"
        private const val FIELD_SURFACE_ID = "surfaceId"
        private const val FIELD_CATALOG_ID = "catalogId"
        private const val FIELD_SEND_DATA_MODEL = "sendDataModel"
        private const val FIELD_THEME = "theme"
        private const val FIELD_PRIMARY_COLOR = "primaryColor"
        private const val FIELD_UPDATE_COMPONENTS = "updateComponents"
        private const val FIELD_COMPONENTS = "components"
        private const val FIELD_ID = "id"
        private const val FIELD_COMPONENT = "component"
        private const val FIELD_UPDATE_DATA_MODEL = "updateDataModel"
        private const val FIELD_PATH = "path"
        private const val FIELD_VALUE = "value"
        private const val FIELD_DELETE_SURFACE = "deleteSurface"
        private const val TEST_SURFACE_ID = "test_surface"
        private const val TEST_CATALOG_ID = "test_catalog"
        private const val TEST_PRIMARY_COLOR = "#FF0000"
        private const val TEST_COMPONENT_ID = "test_component"
        private const val TEST_COMPONENT_TYPE = "button"
        private const val TEST_PROPERTY_KEY = "test_property"
        private const val TEST_PROPERTY_VALUE = "test_value"
        private const val TEST_PATH = "/test/path"
        private const val TEST_DATA_MODEL_VALUE = "test_value"
        private const val TEST_EXCEPTION_MESSAGE = "test_message"

        private const val TEST_DOUBLE_VALUE = 1.0
        private const val TEST_INT_VALUE = 1
    }
}
