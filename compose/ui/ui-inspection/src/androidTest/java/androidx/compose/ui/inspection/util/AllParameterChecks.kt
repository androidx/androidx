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

package androidx.compose.ui.inspection.util

import androidx.compose.ui.inspection.rules.sendCommand
import androidx.inspection.testing.InspectorTester
import com.google.common.truth.Truth.assertThat
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse

suspend fun createAllParametersChecks(
    inspectorTester: InspectorTester,
    rootId: Long,
    generation: Int,
    skipSystemComposables: Boolean = false,
): AllParametersChecks {
    val composableResponse =
        inspectorTester
            .sendCommand(
                GetComposablesCommand(
                    rootViewId = rootId,
                    skipSystemComposables = skipSystemComposables,
                    extractAllParameters = true,
                    generation = generation,
                )
            )
            .getComposablesResponse

    val parametersResponse =
        inspectorTester
            .sendCommand(
                GetAllParametersCommand(
                    rootViewId = rootId,
                    skipSystemComposables = skipSystemComposables,
                    generation = generation,
                )
            )
            .getAllParametersResponse
    return AllParametersChecks(composableResponse, parametersResponse)
}

/** Utility class for checking results from GetComposables and GetAllParameters. */
class AllParametersChecks(
    val composableResponse: GetComposablesResponse,
    val parametersResponse: GetAllParametersResponse,
) {
    private val composableStrings = composableResponse.stringsList.toMap()
    private val parametersStrings = parametersResponse.stringsList.toMap()

    fun assertIconNode(composable: ComposableNode) {
        assertNode(composable, "Icon")
    }

    fun assertTextNode(composable: ComposableNode, expectedText: String) {
        assertNode(composable, "Text", "text", expectedText)
    }

    fun assertNoNode(composableList: List<ComposableNode>, unwantedNodeName: String) {
        assertThat(composableList.any { composableStrings[it.name] == unwantedNodeName }).isFalse()
    }

    fun assertNode(composable: ComposableNode, expectedName: String) {
        assertThat(composableStrings[composable.name]).isEqualTo(expectedName)
    }

    fun assertNode(
        composable: ComposableNode,
        expectedName: String,
        parameterName: String,
        expectedParameterValue: String,
    ) {
        assertThat(composableStrings[composable.name]).isEqualTo(expectedName)
        assertThat(parameterStringValue(composable, parameterName))
            .isEqualTo(expectedParameterValue)
    }

    fun parameterTextValue(composable: ComposableNode): String? {
        return parameterStringValue(composable, "text")
    }

    fun parameterStringValue(composable: ComposableNode, parameterName: String): String? {
        val group =
            parametersResponse.parameterGroupsList.single { it.composableId == composable.id }
        val parameter = group.parameterList.find { parametersStrings[it.name] == parameterName }
        if (parameter == null) {
            val params = group.parameterList.associateBy { parametersStrings[it.name] }
            error("$parameterName not found in parameters. Found: ${params.keys.joinToString()}")
        }
        return parametersStrings[parameter.int32Value]
    }
}
