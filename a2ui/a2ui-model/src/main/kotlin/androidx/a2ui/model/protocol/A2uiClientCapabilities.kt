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
 * Represents the client capabilities advertisement sent to the agent during system initialization.
 *
 * @property supportedCatalogIds Ids of component/function catalogs supported by this client
 *   renderer.
 */
public class A2uiClientCapabilities(public val supportedCatalogIds: List<String>) {
    /** Converts this capability object to the A2UI JSON-compatible Map structure. */
    public fun toPayloadMap(): Map<String, Map<String, Any?>> {
        val versionPayload = mapOf<String, Any?>("supportedCatalogIds" to supportedCatalogIds)
        return mapOf("a2uiClientCapabilities" to mapOf(PROTOCOL_VERSION to versionPayload))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiClientCapabilities) return false
        return supportedCatalogIds == other.supportedCatalogIds
    }

    override fun hashCode(): Int {
        return supportedCatalogIds.hashCode()
    }

    override fun toString(): String {
        return "A2uiClientCapabilities(supportedCatalogIds=$supportedCatalogIds)"
    }

    internal companion object {
        private const val PROTOCOL_VERSION = "v0.9"
    }
}
