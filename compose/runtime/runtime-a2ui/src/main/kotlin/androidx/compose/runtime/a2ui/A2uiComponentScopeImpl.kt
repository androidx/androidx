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

package androidx.compose.runtime.a2ui

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.engine.model.A2uiCoreValueResolver
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope

@Stable
internal class A2uiComponentScopeImpl(
    private val id: String,
    private val baseDataPath: A2uiDataPath,
    private val surface: A2uiCoreSurfaceModel,
    private val surfaceScope: CoroutineScope,
) : A2uiComponentScope {

    private val resolver = A2uiCoreValueResolver { path -> surface.dataModel[path] }

    @Composable
    override fun observeA2uiComponentState(id: String, dataScopePath: String?): A2uiComponentState {
        TODO("Not implemented yet")
    }

    override fun dispatchAction(actionPayload: Map<String, Any?>) {
        TODO("Not implemented yet")
    }

    override fun reportError(exception: A2uiException) {
        surface.dispatchError(exception, id)
    }

    @Composable
    override fun <T : Any> A2uiComponentProperties.bind(property: DynamicA2uiProperty<T>): T? {
        val payload = this[property.key] ?: return null
        val (evaluatedValue, evaluationError) = resolvePayload(payload)

        // The error was already reported in `resolvePayload`, just return null.
        if (evaluationError != null) return null
        if (evaluatedValue == null) return null

        val castedValue = property.safeCast(evaluatedValue)
        if (castedValue == null) {
            // Type mismatch detected, report the error to the agent for self-correction.
            SideEffect(evaluatedValue, property.key) {
                reportError(
                    A2uiRuntimeException(
                        "Type mismatch for key '${property.key}' in component '${id}'. " +
                            "Received value: $evaluatedValue",
                        mapOf("path" to property.key),
                    )
                )
            }
            return null
        }

        return castedValue
    }

    @Composable
    override fun A2uiComponentProperties.bindChildReferences(
        property: ChildListA2uiProperty
    ): List<A2uiComponentReference>? {
        val childrenProperty = this[property.key] ?: return null
        return when (childrenProperty) {
            is List<*> -> {
                // Static list of child IDs.
                remember(childrenProperty) {
                    val result = ArrayList<A2uiComponentReference>(childrenProperty.size)
                    for (i in childrenProperty.indices) {
                        val item =
                            childrenProperty[i] as? String
                                ?: throw IllegalStateException(
                                    "ChildList static array must only contain strings."
                                )
                        result.add(A2uiComponentReference(item))
                    }
                    result
                }
            }
            is Map<*, *> -> {
                // Dynamic child list template.
                val path =
                    childrenProperty["path"] as? String
                        ?: throw IllegalStateException(
                            "ChildList template is missing required 'path' property."
                        )

                val componentId =
                    childrenProperty["componentId"] as? String
                        ?: throw IllegalStateException(
                            "ChildList template is missing required 'componentId' property."
                        )

                val absolutePath = remember(path, baseDataPath.path) { baseDataPath / path }
                val resolvedList = surface.dataModel[absolutePath]

                if (resolvedList != null && resolvedList !is List<*>) {
                    SideEffect(resolvedList, property.key) {
                        reportError(
                            A2uiRuntimeException(
                                "Type mismatch for child template '${property.key}' in component " +
                                    "'${id}'. Expected a List but received: $resolvedList",
                                mapOf("path" to property.key),
                            )
                        )
                    }
                    return null
                }

                val dataList = resolvedList ?: return null
                val separator = if (path.endsWith("/")) "" else "/"

                // Create references linking the templated component IDs to array elements.
                remember(dataList.size, path, componentId) {
                    List(dataList.size) { index ->
                        val itemPath = "$path$separator$index"
                        A2uiComponentReference(componentId, itemPath)
                    }
                }
            }
            else ->
                throw IllegalStateException(
                    "ChildList property '${property.key}' must be a List or a Map. " +
                        "Received: ${childrenProperty::class.simpleName}"
                )
        }
    }

    @Composable
    override fun <T : Any> A2uiComponentProperties.bindUpdater(
        property: DynamicA2uiProperty<T>
    ): ((T?) -> Unit)? {
        val payload = this[property.key]
        return remember(this@A2uiComponentScopeImpl, payload, property.key) {
            val isWritablePath = payload is Map<*, *> && payload.containsKey("path")
            if (isWritablePath) {
                val path =
                    payload["path"] as? String
                        ?: throw IllegalStateException(
                            "DataBinding 'path' property must be a string."
                        )
                val absolutePath = baseDataPath / path
                { newValue: T? -> surface.dataModel.update(absolutePath, newValue) }
            } else {
                null
            }
        }
    }

    @Composable
    private fun resolvePayload(payload: Any?): Pair<Any?, A2uiException?> {
        if (payload == null) return null to null

        // Caches evaluation and establishes snapshot observation reactivity.
        val evaluationState =
            remember(this, payload) {
                derivedStateOf {
                    try {
                        surface.evaluatePayload(id, resolver, baseDataPath, payload) to null
                    } catch (e: Exception) {
                        null to
                            (e as? A2uiException
                                ?: A2uiRuntimeException(e.message ?: "Evaluation failed"))
                    }
                }
            }

        val resultPair = evaluationState.value
        val error = resultPair.second

        SideEffect(error) {
            if (error != null) {
                reportError(error)
            }
        }

        return resultPair
    }
}
