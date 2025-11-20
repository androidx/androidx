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
     * Natural language description guiding the LLM on how to use the app's functions.
     *
     * Corresponds to the `description` attribute in the
     * [&lt;AppFunctionAppMetadata&gt;](androidx.appfunctions.R.styleable.AppFunctionAppMetadata)
     * styleable. Defaults to empty string if not specified.
     */
    public val description: String = "",
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
                this.description == other.description &&
                this.displayDescription == other.displayDescription

    override fun hashCode(): Int = Objects.hash(description, displayDescription)

    override fun toString(): String {
        return "AppFunctionAppMetadata(description='$description', displayDescription='$displayDescription')"
    }
}
