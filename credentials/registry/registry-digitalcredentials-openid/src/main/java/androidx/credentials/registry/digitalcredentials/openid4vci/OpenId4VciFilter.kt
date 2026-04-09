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

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONObject

/** Filters OpenID4VCI issuance requests based on issuance metadata. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class OpenId4VciFilter {
    /** Serializes the filter to JSON. */
    public abstract fun asJson(): JSONObject

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
    override fun asJson(): JSONObject = JSONObject().apply { put("Pass", JSONObject()) }
}

/**
 * Passes only if all child filters pass.
 *
 * @property filters list of filters that must all pass
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AllOf(public val filters: List<OpenId4VciFilter>) : OpenId4VciFilter() {
    override fun asJson(): JSONObject =
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
}

/**
 * Passes if at least one child filter passes.
 *
 * @property filters list of filters where at least one must pass
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnyOf(public val filters: List<OpenId4VciFilter>) : OpenId4VciFilter() {
    override fun asJson(): JSONObject =
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
}

/**
 * Inverts the result of the child filter.
 *
 * @property filter the filter to invert
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Not(public val filter: OpenId4VciFilter) : OpenId4VciFilter() {
    override fun asJson(): JSONObject =
        JSONObject().apply { put("Not", JSONObject().apply { put("filter", filter.asJson()) }) }

    override fun not(): OpenId4VciFilter = filter
}
