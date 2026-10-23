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

package androidx.credentials.registry.digitalcredentials.openid4vci.filter

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONObject

/** Filters OpenID4VCI issuance requests based on issuance metadata. */
public sealed class OpenId4VciFilter {
    /** Serializes the filter to JSON. */
    internal abstract fun asJson(): JSONObject

    /**
     * Combines this filter with another using logical AND.
     *
     * @param other the other filter to combine with
     * @return a filter that passes only if both filters pass
     */
    public infix fun and(other: OpenId4VciFilter): OpenId4VciFilter {
        val left = if (this is AllOf) this.filters else listOf(this)
        val right = if (other is AllOf) other.filters else listOf(other)
        return AllOf(left + right)
    }

    /**
     * Combines this filter with another using logical OR.
     *
     * @param other the other filter to combine with
     * @return a filter that passes if either filter passes
     */
    public infix fun or(other: OpenId4VciFilter): OpenId4VciFilter {
        val left = if (this is AnyOf) this.filters else listOf(this)
        val right = if (other is AnyOf) other.filters else listOf(other)
        return AnyOf(left + right)
    }

    /**
     * Inverts this filter.
     *
     * @return a filter that passes if this filter fails
     */
    public open operator fun not(): OpenId4VciFilter = Not(this)
}

/**
 * Passes all issuance requests.
 *
 * This filter remains internal to the library group and is not exposed in the public API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PassFilter() : OpenId4VciFilter() {
    internal override fun asJson(): JSONObject = JSONObject().apply { put("Pass", JSONObject()) }

    override fun equals(other: Any?): Boolean = this === other || other is PassFilter

    override fun hashCode(): Int = PassFilter::class.hashCode()
}

/**
 * Passes only if all child filters pass.
 *
 * @property filters list of filters that must all pass
 * @throws IllegalArgumentException if [filters] is empty
 */
public class AllOf(public val filters: List<OpenId4VciFilter>) : OpenId4VciFilter() {
    init {
        require(filters.isNotEmpty()) { "filters must not be empty." }
    }

    internal override fun asJson(): JSONObject =
        JSONObject().apply {
            put(
                "And",
                JSONObject().apply {
                    put(
                        "filters",
                        JSONArray().apply {
                            for (f in filters) {
                                put(f.asJson())
                            }
                        },
                    )
                },
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AllOf) return false
        return this.filters == other.filters
    }

    override fun hashCode(): Int = filters.hashCode()
}

/**
 * Passes if at least one child filter passes.
 *
 * @property filters list of filters where at least one must pass
 * @throws IllegalArgumentException if [filters] is empty
 */
public class AnyOf(public val filters: List<OpenId4VciFilter>) : OpenId4VciFilter() {
    init {
        require(filters.isNotEmpty()) { "filters must not be empty." }
    }

    internal override fun asJson(): JSONObject =
        JSONObject().apply {
            put(
                "Or",
                JSONObject().apply {
                    put(
                        "filters",
                        JSONArray().apply {
                            for (f in filters) {
                                put(f.asJson())
                            }
                        },
                    )
                },
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyOf) return false
        return this.filters == other.filters
    }

    override fun hashCode(): Int = filters.hashCode()
}

/**
 * Inverts the result of the child filter.
 *
 * @property filter the filter to invert
 */
public class Not(public val filter: OpenId4VciFilter) : OpenId4VciFilter() {
    internal override fun asJson(): JSONObject =
        JSONObject().apply { put("Not", JSONObject().apply { put("filter", filter.asJson()) }) }

    override fun not(): OpenId4VciFilter = filter

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Not) return false
        return this.filter == other.filter
    }

    override fun hashCode(): Int = filter.hashCode()
}

/**
 * Filters issuance requests by allowed credential issuers.
 *
 * See
 * [Credential Issuer Identifier](https://openid.github.io/OpenID4VCI/openid-4-verifiable-credential-issuance-1_1-wg-draft.html#section-4.1.1-2.1)
 * in the OpenID4VCI specification.
 *
 * @property issuers set of allowed credential issuers (e.g., "https://issuer.example.com")
 * @throws IllegalArgumentException if [issuers] is empty or contains blank strings
 */
public class AllowedIssuers(public val issuers: Set<String>) : OpenId4VciFilter() {
    init {
        require(issuers.isNotEmpty()) { "issuers must not be empty." }
        require(issuers.all { it.isNotBlank() }) { "issuers must not contain blank elements." }
    }

    internal override fun asJson(): JSONObject =
        JSONObject().apply {
            put("AllowedIssuers", JSONObject().apply { put("issuers", JSONArray(issuers)) })
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AllowedIssuers) return false
        return this.issuers == other.issuers
    }

    override fun hashCode(): Int = issuers.hashCode()
}

/**
 * Filters issuance requests by allowed credential configuration identifiers.
 *
 * See
 * [Credential Configuration Identifier](https://openid.github.io/OpenID4VCI/openid-4-verifiable-credential-issuance-1_1-wg-draft.html#section-4.1.1-2.2)
 * in the OpenID4VCI specification.
 *
 * @property configurationIds set of allowed credential configuration identifiers
 * @throws IllegalArgumentException if [configurationIds] is empty or contains blank strings
 */
public class AllowedConfigurationIds(public val configurationIds: Set<String>) :
    OpenId4VciFilter() {
    init {
        require(configurationIds.isNotEmpty()) { "configurationIds must not be empty." }
        require(configurationIds.all { it.isNotBlank() }) {
            "configurationIds must not contain blank elements."
        }
    }

    internal override fun asJson(): JSONObject =
        JSONObject().apply {
            put(
                "AllowedConfigurationIds",
                JSONObject().apply { put("configuration_ids", JSONArray(configurationIds)) },
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AllowedConfigurationIds) return false
        return this.configurationIds == other.configurationIds
    }

    override fun hashCode(): Int = configurationIds.hashCode()
}

/**
 * Filters issuance requests by allowed ISO mdoc document types.
 *
 * @property doctypes set of allowed document types (e.g., "org.iso.18013.5.1.mDL")
 * @throws IllegalArgumentException if [doctypes] is empty or contains blank strings
 */
public class AllowedMdocDoctypes(public val doctypes: Set<String>) : OpenId4VciFilter() {
    init {
        require(doctypes.isNotEmpty()) { "doctypes must not be empty." }
        require(doctypes.all { it.isNotBlank() }) { "doctypes must not contain blank elements." }
    }

    internal override fun asJson(): JSONObject =
        JSONObject().apply {
            put("AllowedMdocDoctypes", JSONObject().apply { put("doctypes", JSONArray(doctypes)) })
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AllowedMdocDoctypes) return false
        return this.doctypes == other.doctypes
    }

    override fun hashCode(): Int = doctypes.hashCode()
}

/**
 * Filters issuance requests by allowed SD-JWT Verifiable Credential Types.
 *
 * @property vcts set of allowed verifiable credential types
 * @throws IllegalArgumentException if [vcts] is empty or contains blank strings
 */
public class AllowedSdJwtVcts(public val vcts: Set<String>) : OpenId4VciFilter() {
    init {
        require(vcts.isNotEmpty()) { "vcts must not be empty." }
        require(vcts.all { it.isNotBlank() }) { "vcts must not contain blank elements." }
    }

    internal override fun asJson(): JSONObject =
        JSONObject().apply {
            put("AllowedSdJwtVcts", JSONObject().apply { put("vcts", JSONArray(vcts)) })
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AllowedSdJwtVcts) return false
        return this.vcts == other.vcts
    }

    override fun hashCode(): Int = vcts.hashCode()
}
