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

package androidx.appfunctions.metadata

import java.util.Objects

/**
 * Metadata describing how an app exposes its functions for use by an AI agent or large language
 * model (LLM).
 *
 * This class corresponds to the
 * [&lt;AppFunctionAppMetadata&gt;](androidx.appfunctions.R.styleable.AppFunctionAppMetadata)
 * styleable in the XML linked to property `android.app.appfunctions.app_metadata` in the app's
 * manifest.
 */
// TODO: b/429149071 - Link to the dev site explaining the attributes.
public class AppFunctionAppMetadata(
    /**
     * App-provided instructions for the LLM on how to use the app's functions together.
     *
     * Agents should use these instructions as context when planning calls to the app's functions,
     * for example to follow a required order across functions (such as searching for an entity
     * before updating or deleting it) or to respect constraints that apply across functions.
     * Instructions complement, and do not replace, the descriptions of individual functions.
     *
     * Corresponds to the `instructions` attribute in the
     * [&lt;AppFunctionAppMetadata&gt;](androidx.appfunctions.R.styleable.AppFunctionAppMetadata)
     * styleable. Defaults to empty string if not specified.
     */
    public val instructions: String = "",
    /**
     * A short, user-visible description of what the app functions enable the agent to do.
     *
     * Corresponds to the `displayDescription` attribute in the
     * [&lt;AppFunctionAppMetadata&gt;](androidx.appfunctions.R.styleable.AppFunctionAppMetadata)
     * styleable. Defaults to empty string if not specified.
     */
    public val displayDescription: String = "",
) {

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is AppFunctionAppMetadata &&
                this.instructions == other.instructions &&
                this.displayDescription == other.displayDescription

    override fun hashCode(): Int = Objects.hash(instructions, displayDescription)

    override fun toString(): String {
        return "AppFunctionAppMetadata(instructions='$instructions', displayDescription='$displayDescription')"
    }
}
