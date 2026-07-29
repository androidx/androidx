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

package androidx.credentials.registry.digitalcredentials.openid4vci

import android.content.Context
import androidx.credentials.DigitalCredential.Companion.TYPE_DIGITAL_CREDENTIAL
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.registry.provider.RegisterCreationOptionsRequest
import androidx.credentials.registry.provider.RegistryManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONArray
import org.json.JSONObject

/**
 * Holds display metadata for OpenID4VCI credentials.
 *
 * Contains holder information and individual credential entries.
 *
 * @property entries list of credential entries containing subtitle and explainer
 * @property holderDisplayData optional display metadata for the credential holder. Only privileged
 *   apps can set this to override the default app name and icon.
 */
public class OpenId4VciDisplayData(
    public val entries: List<Entry> = emptyList(),
    public val holderDisplayData: HolderDisplayData? = null,
) {
    /**
     * Explains the credential issuance request to the user.
     *
     * Provides issuer-specific or default terms and information.
     *
     * The explainer text supports markdown formatting, but only links (e.g., `[link text](url)`)
     * are supported. When rendered, the text must not exceed 150 characters.
     *
     * @property perIssuer map of issuer origin to issuer-specific explainer text. The text must not
     *   exceed 150 characters when rendered and only link markdown is supported.
     * @property default default explainer text used if no issuer-specific text matches. The text
     *   must not exceed 150 characters when rendered and only link markdown is supported.
     */
    public class Explainer(
        public val perIssuer: Map<String, String> = emptyMap(),
        public val default: String? = null,
    ) {
        init {
            require(perIssuer.isNotEmpty() || default != null) {
                "Explainer must have at least one issuer-specific or default terms."
            }
            require(perIssuer.keys.all { it.isNotBlank() }) { "Issuer origins must not be blank." }
            require(perIssuer.values.all { it.isNotBlank() }) {
                "Explainer text must not be blank."
            }
            if (default != null) {
                require(default.isNotBlank()) { "Default explainer text must not be blank." }
            }
        }

        internal fun asJson(): JSONObject =
            JSONObject().apply {
                put("per_issuer", JSONObject(perIssuer))
                if (default != null) {
                    put("default", default)
                }
            }
    }

    /**
     * Holds display metadata for a single credential entry.
     *
     * @property subtitle optional subtitle showing user identity or account info
     * @property explainer optional explainer text for this entry
     */
    public class Entry(public val subtitle: String? = null, public val explainer: Explainer? = null)

    /**
     * Holds display metadata for the credential holder.
     *
     * Only privileged apps can use this to override the default app name and icon.
     *
     * @property name optional name of the holder or wallet app
     * @property icon optional icon bytes of the holder or wallet app
     */
    public class HolderDisplayData(
        public val name: String? = null,
        public val icon: ByteArray? = null,
    )
}

/**
 * Registers OpenID4VCI credential creation options.
 *
 * Use [create] to build an instance.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
public class OpenId4VciRegistry
private constructor(
    id: String,
    creationOptions: ByteArray,
    matcher: ByteArray,
    intentAction: String,
) :
    RegisterCreationOptionsRequest(
        TYPE_DIGITAL_CREDENTIAL,
        id,
        creationOptions,
        matcher,
        intentAction,
    ) {
    public companion object {
        private const val MATCHER_BINARY = "issuance.wasm"
        private const val NUM_BYTES_PER_INT32 = 4

        private fun readMatcher(context: Context): ByteArray =
            context.assets.open(MATCHER_BINARY).use { it.readBytes() }

        /**
         * Creates an [OpenId4VciRegistry] request.
         *
         * Example usage:
         * ```
         * val registry = OpenId4VciRegistry.create(
         *     context = context,
         *     id = "issuer_id",
         *     filter = AllowedIssuers(setOf("https://issuer.example.com")),
         *     displayData = OpenId4VciDisplayData(
         *         entries = listOf(
         *             OpenId4VciDisplayData.Entry(
         *                 subtitle = "user@example.com",
         *                 explainer = OpenId4VciDisplayData.Explainer(
         *                     perIssuer = mapOf(
         *                         "https://issuer.example.com" to
         *                             "[Issuer Terms of Service](https://issuer.example.com/tos)"
         *                     ),
         *                     default = "Default explainer"
         *                 )
         *             )
         *         )
         *     )
         * )
         * ```
         *
         * @param context the application context
         * @param id unique identifier for this registry request
         * @param filter filter to match against issuance metadata
         * @param displayData optional display metadata for the credentials. If omitted, a single
         *   entry with the app name and icon will appear in the selection list on match.
         * @param preferredProtocols optional list of preferred protocols
         * @param intentAction optional intent action for creation
         * @return a new [OpenId4VciRegistry] instance
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            context: Context,
            id: String,
            filter: OpenId4VciFilter,
            displayData: OpenId4VciDisplayData? = null,
            preferredProtocols: List<String> = emptyList(),
            intentAction: String = RegistryManager.ACTION_CREATE_CREDENTIAL,
        ): OpenId4VciRegistry =
            OpenId4VciRegistry(
                id,
                buildCreationOptions(id, filter, displayData, preferredProtocols),
                readMatcher(context),
                intentAction,
            )

        private fun buildCreationOptions(
            id: String,
            filter: OpenId4VciFilter,
            displayData: OpenId4VciDisplayData?,
            preferredProtocols: List<String>,
        ): ByteArray {
            val icon = displayData?.holderDisplayData?.icon
            val hasIcon = icon != null && icon.isNotEmpty()

            val jsonEntries = JSONArray()
            val entries = displayData?.entries ?: emptyList()

            if (entries.isEmpty()) {
                val jsonEntry =
                    JSONObject().apply {
                        displayData?.holderDisplayData?.name?.let { put("title", it) }
                        if (hasIcon) {
                            put(
                                "icon",
                                JSONArray().apply {
                                    put(NUM_BYTES_PER_INT32)
                                    put(NUM_BYTES_PER_INT32 + icon!!.size)
                                },
                            )
                        }
                    }
                jsonEntries.put(jsonEntry)
            } else {
                for (entry in entries) {
                    val jsonEntry =
                        JSONObject().apply {
                            displayData?.holderDisplayData?.name?.let { put("title", it) }
                            entry.subtitle?.let { put("subtitle", it) }
                            entry.explainer?.let { put("explainer", it.asJson()) }
                            if (hasIcon) {
                                put(
                                    "icon",
                                    JSONArray().apply {
                                        put(NUM_BYTES_PER_INT32)
                                        put(NUM_BYTES_PER_INT32 + icon!!.size)
                                    },
                                )
                            }
                        }
                    jsonEntries.put(jsonEntry)
                }
            }

            val jsonBytes =
                JSONObject()
                    .apply {
                        put("entry_id", id)
                        put("entries", jsonEntries)
                        put("filter", filter.asJson())
                        if (preferredProtocols.isNotEmpty()) {
                            put("preferred_protocols", JSONArray(preferredProtocols))
                        }
                    }
                    .toString()
                    .toByteArray(Charsets.UTF_8)

            return packOptions(icon, jsonBytes)
        }

        /**
         * Packs the optional icon and JSON metadata into a single byte array.
         *
         * The format of the returned byte array is:
         * - 4 bytes: JSON offset (Int32, Little Endian). This points to the start of the JSON
         *   bytes.
         * - X bytes: Icon bytes (optional). If present, they start at offset 4.
         * - Y bytes: JSON metadata bytes.
         *
         * If no icon is present, the JSON offset is 4, and JSON bytes start immediately.
         */
        private fun packOptions(icon: ByteArray?, jsonBytes: ByteArray): ByteArray {
            val hasIcon = icon != null && icon.isNotEmpty()
            val jsonOffset = NUM_BYTES_PER_INT32 + (if (hasIcon) icon!!.size else 0)
            val result = ByteArray(jsonOffset + jsonBytes.size)

            ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(jsonOffset)
                if (hasIcon) {
                    put(icon)
                }
                put(jsonBytes)
            }
            return result
        }
    }
}
