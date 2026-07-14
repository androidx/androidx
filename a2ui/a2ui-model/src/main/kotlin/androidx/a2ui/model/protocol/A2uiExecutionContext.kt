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

/**
 * Provides environment access, dynamic payload evaluation, and function execution capabilities
 * during execution.
 */
public interface A2uiExecutionContext {
    /**
     * Evaluates a dynamic payload.
     *
     * @param dataPath base path used to resolve relative paths
     * @param payload payload to evaluate
     * @return evaluated result, or null if evaluation fails
     */
    public fun evaluatePayload(dataPath: A2uiDataPath, payload: Any?): Any?

    /**
     * Executes a catalog function by name.
     *
     * @param name name of the function to execute
     * @param args arguments to pass to the function
     * @return result of the function execution, or null if execution fails
     */
    public fun executeFunction(name: String, args: Map<String, Any>): Any?

    /**
     * Resolves a value from the data model at the given path.
     *
     * @param path data path to resolve
     * @return resolved value, or null if value resolution fails
     */
    public fun resolveValue(path: A2uiDataPath): Any?
}
