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

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class A2UiServerToClientMessageTest {

    @Test
    fun createSurfaceMessage_allArgumentsProvided_allPropertiesAreStored() {
        val full =
            A2uiCreateSurfaceMessage(
                TEST_SURFACE_ID,
                TEST_CATALOG_ID,
                TEST_THEME,
                shouldSendDataModel = true,
            )
        assertThat(full.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(full.catalogId).isEqualTo(TEST_CATALOG_ID)
        assertThat(full.theme).isEqualTo(TEST_THEME)
        assertThat(full.shouldSendDataModel).isTrue()
    }

    @Test
    fun createSurfaceMessage_onlyRequiredArgumentsProvided_defaultValuesAreUsed() {
        val default = A2uiCreateSurfaceMessage(TEST_SURFACE_ID, TEST_CATALOG_ID)
        assertThat(default.theme).isEmpty()
        assertThat(default.shouldSendDataModel).isFalse()
    }

    @Test
    fun createSurfaceMessage_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID,
                    TEST_THEME,
                    shouldSendDataModel = true,
                ),
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID,
                    TEST_THEME,
                    shouldSendDataModel = true,
                ),
            )
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID_2,
                    TEST_CATALOG_ID,
                    TEST_THEME,
                    shouldSendDataModel = true,
                )
            )
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID_2,
                    TEST_THEME,
                    shouldSendDataModel = true,
                )
            )
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID,
                    TEST_THEME_2,
                    shouldSendDataModel = true,
                )
            )
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID,
                    TEST_THEME,
                    shouldSendDataModel = false,
                )
            )
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID,
                    emptyMap(),
                    shouldSendDataModel = true,
                )
            )
            .addEqualityGroup(
                A2uiCreateSurfaceMessage(
                    TEST_SURFACE_ID,
                    TEST_CATALOG_ID,
                    mapOf("t" to null),
                    shouldSendDataModel = false,
                )
            )
            .testEquals()
    }

    @Test
    fun createSurfaceMessage_toString_returnsExpectedFormat() {
        val msg =
            A2uiCreateSurfaceMessage(
                TEST_SURFACE_ID,
                TEST_CATALOG_ID,
                TEST_THEME,
                shouldSendDataModel = true,
            )
        assertThat(msg.toString())
            .isEqualTo(
                "A2uiCreateSurfaceMessage(surfaceId=$TEST_SURFACE_ID, catalogId=$TEST_CATALOG_ID, theme=$TEST_THEME, shouldSendDataModel=true)"
            )
    }

    @Test
    fun updateComponentsMessage_componentsListProvided_propertiesAreStored() {
        val components = listOf(A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, emptyMap()))
        val update = A2uiUpdateComponentsMessage(TEST_SURFACE_ID, components)
        assertThat(update.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(update.components).isEqualTo(components)
    }

    @Test
    fun updateComponentsMessage_equalsAndHashCode_contracts() {
        val payload1 = A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES)
        val payload2 = A2uiComponentPayload(TEST_COMPONENT_ID_2, TEST_TYPE, TEST_PROPERTIES)
        EqualsTester()
            .addEqualityGroup(
                A2uiUpdateComponentsMessage(TEST_SURFACE_ID, listOf(payload1)),
                A2uiUpdateComponentsMessage(TEST_SURFACE_ID, listOf(payload1)),
            )
            .addEqualityGroup(A2uiUpdateComponentsMessage(TEST_SURFACE_ID_2, listOf(payload1)))
            .addEqualityGroup(A2uiUpdateComponentsMessage(TEST_SURFACE_ID, listOf(payload2)))
            .addEqualityGroup(A2uiUpdateComponentsMessage(TEST_SURFACE_ID, emptyList()))
            .testEquals()
    }

    @Test
    fun updateComponentsMessage_toString_returnsExpectedFormat() {
        val payload = A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES)
        val msg = A2uiUpdateComponentsMessage(TEST_SURFACE_ID, listOf(payload))
        assertThat(msg.toString())
            .isEqualTo(
                "A2uiUpdateComponentsMessage(surfaceId=$TEST_SURFACE_ID, components=[A2uiComponentPayload(id=$TEST_COMPONENT_ID, type=$TEST_TYPE, properties=$TEST_PROPERTIES)])"
            )
    }

    @Test
    fun updateDataModelMessage_explicitValuesProvided_propertiesAreStored() {
        val update = A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, TEST_VALUE)
        assertThat(update.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(update.path).isEqualTo(TEST_PATH)
        assertThat(update.value).isEqualTo(TEST_VALUE)
    }

    @Test
    fun updateDataModelMessage_onlyRequiredArgumentsProvided_defaultValuesAreUsed() {
        val default = A2uiUpdateDataModelMessage(TEST_SURFACE_ID)
        assertThat(default.path).isEqualTo("/")
        assertThat(default.value).isNull()
    }

    @Test
    fun updateDataModelMessage_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, TEST_VALUE),
                A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, TEST_VALUE),
            )
            .addEqualityGroup(A2uiUpdateDataModelMessage(TEST_SURFACE_ID_2, TEST_PATH, TEST_VALUE))
            .addEqualityGroup(A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH_2, TEST_VALUE))
            .addEqualityGroup(A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, TEST_VALUE_2))
            .addEqualityGroup(A2uiUpdateDataModelMessage(TEST_SURFACE_ID, "", TEST_VALUE))
            .addEqualityGroup(A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, null))
            .addEqualityGroup(A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, ""))
            .testEquals()
    }

    @Test
    fun updateDataModelMessage_toString_returnsExpectedFormat() {
        val msg = A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, TEST_VALUE)
        assertThat(msg.toString())
            .isEqualTo(
                "A2uiUpdateDataModelMessage(surfaceId=$TEST_SURFACE_ID, path=$TEST_PATH, value=$TEST_VALUE)"
            )
    }

    @Test
    fun updateDataModelMessage_toString_handlesNulls() {
        val msg = A2uiUpdateDataModelMessage(TEST_SURFACE_ID, TEST_PATH, null)
        assertThat(msg.toString())
            .isEqualTo(
                "A2uiUpdateDataModelMessage(surfaceId=$TEST_SURFACE_ID, path=$TEST_PATH, value=null)"
            )
    }

    @Test
    fun deleteSurfaceMessage_surfaceIdProvided_propertiesAreStored() {
        val delete = A2uiDeleteSurfaceMessage(TEST_SURFACE_ID)
        assertThat(delete.surfaceId).isEqualTo(TEST_SURFACE_ID)
    }

    @Test
    fun deleteSurfaceMessage_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiDeleteSurfaceMessage(TEST_SURFACE_ID),
                A2uiDeleteSurfaceMessage(TEST_SURFACE_ID),
            )
            .addEqualityGroup(A2uiDeleteSurfaceMessage(TEST_SURFACE_ID_2))
            .testEquals()
    }

    @Test
    fun deleteSurfaceMessage_toString_returnsExpectedFormat() {
        val msg = A2uiDeleteSurfaceMessage(TEST_SURFACE_ID)
        assertThat(msg.toString()).isEqualTo("A2uiDeleteSurfaceMessage(surfaceId=$TEST_SURFACE_ID)")
    }

    @Test
    fun handleActionMessage_actionProvided_propertiesAreStored() {
        val action =
            A2uiUserAction(
                TEST_ACTION_TYPE,
                TEST_SURFACE_ID,
                TEST_COMPONENT_ID,
                TEST_TIMESTAMP,
                TEST_CONTEXT,
            )
        val handle = A2uiHandleActionMessage(TEST_SURFACE_ID, action)
        assertThat(handle.surfaceId).isEqualTo(TEST_SURFACE_ID)
        assertThat(handle.action).isEqualTo(action)
    }

    companion object {
        private const val TEST_SURFACE_ID = "test-surface"
        private const val TEST_SURFACE_ID_2 = "test-surface-2"
        private const val TEST_CATALOG_ID = "test-catalog"
        private const val TEST_CATALOG_ID_2 = "test-catalog-2"
        private const val TEST_COMPONENT_ID = "test-component"
        private const val TEST_COMPONENT_ID_2 = "test-component-2"
        private const val TEST_ACTION_TYPE = "test-action"
        private const val TEST_TIMESTAMP = 1778595420000L
        private const val TEST_TYPE = "test-type"
        private const val TEST_PATH = "/test/path"
        private const val TEST_PATH_2 = "/test/path/alt"
        private const val TEST_VALUE = "test-value"
        private const val TEST_VALUE_2 = "test-value-2"

        private val TEST_PROPERTIES = mapOf("prop" to "value")
        private val TEST_THEME = mapOf("theme-prop" to "theme-value")
        private val TEST_THEME_2 = mapOf("theme-prop-2" to "theme-value-2")
        private val TEST_CONTEXT = mapOf("context-prop" to "context-value")
    }
}
