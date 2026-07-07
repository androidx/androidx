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

package androidx.a2ui.engine.model

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiExecutionContext

/**
 * Represents the execution environment of a specific component.
 *
 * @param componentId unique identifier of the component associated with this context
 * @param catalog catalog containing registered client-side components and functions
 * @param dispatchError callback to handle errors dispatched from this execution context
 * @param valueResolver resolver to retrieve state synchronously from the data model
 * @param dynamicEvaluator evaluator for resolving dynamic bindings and client functions
 */
internal class A2uiCoreExecutionContext(
    private val componentId: String,
    private val catalog: A2uiCoreCatalog,
    private val dispatchError: (String, A2uiException) -> Unit,
    private val valueResolver: A2uiCoreValueResolver,
    private val dynamicEvaluator: A2uiCoreDynamicEvaluator,
) : A2uiExecutionContext {
    override fun evaluatePayload(dataPath: A2uiDataPath, payload: Any?): Any? =
        dynamicEvaluator.evaluate(dataPath, payload, this)

    override fun executeFunction(name: String, args: Map<String, Any>): Any? {
        val catalogFunction = catalog.getFunction(name)
        if (catalogFunction == null) {
            val exception =
                A2uiException.A2uiRuntimeException("Function '$name' not found in catalog.")
            dispatchError(componentId, exception)
            return null
        }
        return try {
            catalogFunction.execute(args, this)
        } catch (e: A2uiException) {
            dispatchError(componentId, e)
            null
        } catch (e: Exception) {
            val exception =
                A2uiException.A2uiRuntimeException(
                    message = "Function '$name' execution failed",
                    context = mapOf("originalErrorMessage" to e.message),
                )
            dispatchError(componentId, exception)
            null
        }
    }

    override fun resolveValue(path: A2uiDataPath): Any? = valueResolver.resolve(path)
}
