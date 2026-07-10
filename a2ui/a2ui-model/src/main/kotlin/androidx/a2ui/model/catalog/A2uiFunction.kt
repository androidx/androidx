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

@file:JvmName("A2uiFunctionKt")

package androidx.a2ui.model.catalog

/** Represents a function supported by A2UI including its definition and implementation. */
public interface A2uiFunction {
    /** The definition of the function. */
    public val definition: A2uiFunctionDefinition

    /**
     * Executes the function logic.
     *
     * @param args The statically resolved arguments. All dynamic properties should be resolved by
     *   the DynamicEvaluator before populating this argument.
     * @return The raw evaluated result.
     */
    public fun execute(args: Map<String, Any>): Any?
}
