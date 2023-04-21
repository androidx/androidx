/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core

import androidx.privacysandbox.tools.core.proto.PrivacySandboxToolsProtocol.ToolMetadata
import kotlin.io.path.Path

/** Privacy Sandbox Tool metadata constants. */
object Metadata {
    /** Tool metadata message. It's serialized and stored in every SDK API descriptor. */
    val toolMetadata: ToolMetadata =
        ToolMetadata.newBuilder()
            .setCodeGenerationVersion(3)
            .build()

    /** Relative path to metadata file in SDK API descriptor jar. */
    val filePath = Path("META-INF/privacysandbox/tool-metadata.pb")
}