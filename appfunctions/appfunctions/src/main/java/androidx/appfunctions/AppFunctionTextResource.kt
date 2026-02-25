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

package androidx.appfunctions

/**
 * Represents a text resource in an app function's response/parameters.
 *
 * @param mimeType The MIME type of the text resource, used by the agent to understand the
 *   [content].
 * @param content The text content of the resource.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
public class AppFunctionTextResource(public val mimeType: String, public val content: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionTextResource

        if (mimeType != other.mimeType) return false
        return content == other.content
    }

    override fun hashCode(): Int {
        var result = mimeType.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}
