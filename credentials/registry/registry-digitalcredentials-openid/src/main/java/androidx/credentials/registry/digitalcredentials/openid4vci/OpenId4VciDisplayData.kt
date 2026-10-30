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

import androidx.annotation.RequiresPermission
import org.json.JSONObject

/**
 * Holds display metadata for OpenID4VCI credentials.
 *
 * Contains holder information and individual credential entries.
 *
 * @property entries list of credential entries containing subtitle and explainer. Must not be
 *   empty.
 * @property holderDisplayData optional display metadata for the credential holder. Overriding the
 *   default application name and icon requires the registering app to hold
 *   `android.permission.SUBSTITUTE_SHARE_TARGET_APP_NAME_AND_ICON`; otherwise, the system ignores
 *   the override and falls back to the default app name and icon.
 * @throws IllegalArgumentException if [entries] is empty
 */
public class OpenId4VciDisplayData(
    public val entries: List<Entry>,
    public val holderDisplayData: HolderDisplayData? = null,
) {
    init {
        require(entries.isNotEmpty()) { "Entries must not be empty." }
    }

    /**
     * Explains the credential issuance request to the user.
     *
     * Provides issuer-specific or default terms and information.
     *
     * The explainer text supports markdown formatting, but only links (e.g., `[link text](url)`)
     * are supported. When rendered, the text must not exceed [MAX_EXPLAINER_LENGTH] (150)
     * characters.
     *
     * @property perIssuer map of issuer origin to issuer-specific explainer text. The text must not
     *   exceed [MAX_EXPLAINER_LENGTH] characters when rendered and only link markdown is supported.
     * @property default default explainer text used if no issuer-specific text matches. The text
     *   must not exceed [MAX_EXPLAINER_LENGTH] characters when rendered and only link markdown is
     *   supported.
     * @throws IllegalArgumentException if [perIssuer] is empty and [default] is null, if issuer
     *   origins or explainer texts are blank, or if rendered text exceeds [MAX_EXPLAINER_LENGTH]
     *   characters
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
            perIssuer.forEach { (issuer, text) ->
                validateExplainerText(text, "Explainer text for issuer '$issuer'")
            }
            if (default != null) {
                validateExplainerText(default, "Default explainer text")
            }
        }

        private fun validateExplainerText(text: String, label: String) {
            require(text.isNotBlank()) { "$label must not be blank." }
            val renderedLength = ExplainerTextParser.computeRenderedLength(text)
            require(renderedLength <= MAX_EXPLAINER_LENGTH) {
                "$label exceeds maximum allowed length of $MAX_EXPLAINER_LENGTH characters " +
                    "(rendered length: $renderedLength)."
            }
        }

        internal fun asJson(): JSONObject =
            JSONObject().apply {
                put("per_issuer", JSONObject(perIssuer))
                if (default != null) {
                    put("default", default)
                }
            }

        public companion object {
            /** Maximum allowed character length for rendered explainer text. */
            public const val MAX_EXPLAINER_LENGTH: Int = 150
        }
    }

    /**
     * Holds display metadata for a single credential entry.
     *
     * @property id a unique identifier for this entry within the registration request
     * @property subtitle optional subtitle showing user identity or account info
     * @property explainer optional explainer text for this entry
     * @throws IllegalArgumentException if [id] is blank
     */
    public class Entry(
        public val id: String,
        public val subtitle: String? = null,
        public val explainer: Explainer? = null,
    ) {
        init {
            require(id.isNotBlank()) { "Entry ID must not be blank." }
        }
    }

    /**
     * Holds display metadata for the credential holder.
     *
     * Overriding the default application name and icon requires the registering app to hold
     * `android.permission.SUBSTITUTE_SHARE_TARGET_APP_NAME_AND_ICON`; otherwise, the system ignores
     * the override and falls back to the default app name and icon.
     *
     * @property name optional name of the holder or wallet app
     * @property icon optional icon bytes of the holder or wallet app
     */
    public class HolderDisplayData
    @RequiresPermission(
        value = "android.permission.SUBSTITUTE_SHARE_TARGET_APP_NAME_AND_ICON",
        conditional = true,
    )
    constructor(public val name: String? = null, public val icon: ByteArray? = null)
}
