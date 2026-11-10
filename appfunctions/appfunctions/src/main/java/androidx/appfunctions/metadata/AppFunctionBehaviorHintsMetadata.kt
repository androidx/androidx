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

package androidx.appfunctions.metadata

import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document
import java.util.Objects

/**
 * Advisory hints about the runtime behavior of an app function.
 *
 * The properties and their default values conform to the
 * [Model Context Protocol (MCP) ToolAnnotations specification](https://modelcontextprotocol.io/specification/2025-11-25/schema#toolannotations):
 * [isReadOnly], [isDestructive], [isIdempotent], and [isOpenWorld].
 *
 * Callers must account for the following semantics when interpreting these hints:
 * - **No binding contract:** Hints are purely advisory and do not prescribe how callers should
 *   react. Turning hints into a policy (for example, prompting for confirmation before calling a
 *   function with [isDestructive] set to `true`) is entirely up to the caller.
 * - **Untrusted by default:** Hints are self-reported by the providing app and are not verified to
 *   match the function's actual implementation. Callers should not rely on hints from untrusted
 *   apps for security-critical decisions.
 * - **No validation:** The library does not validate hint values or combinations at declaration
 *   time, construction time, or reading time. Behavior hints are bespoke signals between the
 *   providing app (server) and caller (client) that the library does not mediate; the values are
 *   passed through as-is for the caller and providing app to interpret.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionBehaviorHintsMetadata
@JvmOverloads
public constructor(
    /**
     * Whether the app function does not modify its environment.
     *
     * Defaults to `false`.
     */
    public val isReadOnly: Boolean = false,
    /**
     * Whether the app function may perform destructive updates to its environment, as opposed to
     * purely additive updates.
     *
     * Destructive updates include deleting or overwriting existing data, whereas additive updates
     * only create new records. Meaningful only when the function modifies its environment (that is,
     * when [isReadOnly] is `false`).
     *
     * Defaults to `true`.
     */
    public val isDestructive: Boolean = true,
    /**
     * Whether calling the app function repeatedly with the same arguments has no additional effect
     * on its environment.
     *
     * Meaningful only when the function modifies its environment (that is, when [isReadOnly] is
     * `false`).
     *
     * Defaults to `false`.
     */
    public val isIdempotent: Boolean = false,
    /**
     * Whether the app function may interact with an external "open world" of entities, as opposed
     * to operating strictly within a closed domain.
     *
     * Examples include searching the web, sending messages to external recipients, or calling
     * third-party services, as opposed to operating strictly within local app storage.
     *
     * Defaults to `true`.
     */
    public val isOpenWorld: Boolean = true,
) {
    internal fun toAppFunctionBehaviorHintsMetadataDocument():
        AppFunctionBehaviorHintsMetadataDocument {
        return AppFunctionBehaviorHintsMetadataDocument(
            isReadOnly = isReadOnly,
            isDestructive = isDestructive,
            isIdempotent = isIdempotent,
            isOpenWorld = isOpenWorld,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionBehaviorHintsMetadata

        if (isReadOnly != other.isReadOnly) return false
        if (isDestructive != other.isDestructive) return false
        if (isIdempotent != other.isIdempotent) return false
        if (isOpenWorld != other.isOpenWorld) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(isReadOnly, isDestructive, isIdempotent, isOpenWorld)
    }

    override fun toString(): String {
        return "AppFunctionBehaviorHintsMetadata(" +
            "isReadOnly=$isReadOnly, " +
            "isDestructive=$isDestructive, " +
            "isIdempotent=$isIdempotent, " +
            "isOpenWorld=$isOpenWorld" +
            ")"
    }
}

@Document
internal data class AppFunctionBehaviorHintsMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    @Document.BooleanProperty val isReadOnly: Boolean? = null,
    @Document.BooleanProperty val isDestructive: Boolean? = null,
    @Document.BooleanProperty val isIdempotent: Boolean? = null,
    @Document.BooleanProperty val isOpenWorld: Boolean? = null,
) {
    fun toAppFunctionBehaviorHintsMetadata(): AppFunctionBehaviorHintsMetadata {
        return AppFunctionBehaviorHintsMetadata(
            isReadOnly = isReadOnly ?: false,
            isDestructive = isDestructive ?: true,
            isIdempotent = isIdempotent ?: false,
            isOpenWorld = isOpenWorld ?: true,
        )
    }
}
