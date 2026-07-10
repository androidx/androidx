/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation

import androidx.navigation.internal.InternalUri

public actual abstract class NavUri {
    public actual abstract fun getFragment(): String?

    public actual abstract fun getQuery(): String?

    public actual abstract fun getPathSegments(): List<String>

    public actual open fun getQueryParameters(key: String): List<String> =
        error("Abstract implementation")

    public actual open fun getQueryParameterNames(): Set<String> = error("Abstract implementation")

    actual abstract override fun toString(): String
}

internal actual object NavUriUtils {
    actual fun encode(s: String, allow: String?): String = InternalUri.encode(s, allow)

    actual fun decode(s: String): String = InternalUri.decode(s)

    actual fun parse(uriString: String): NavUri = ActualUri(uriString)
}

private class ActualUri(private val uriString: String) : NavUri() {

    private companion object {
        private val QUERY_PATTERN = Regex("^[^?#]+\\?([^#]*).*")
        private val FRAGMENT_PATTERN = Regex("#(.+)")
    }

    private val _query: String? by lazy { QUERY_PATTERN.find(uriString)?.groups?.get(1)?.value }

    private val _fragment: String? by lazy {
        FRAGMENT_PATTERN.find(uriString)?.groups?.get(1)?.value
    }

    private val schemeSeparatorIndex by lazy { uriString.indexOf(':') }

    private val _pathSegments: List<String> by lazy {
        val ssi = schemeSeparatorIndex
        if (ssi > -1) {
            if (ssi + 1 == uriString.length) return@lazy emptyList()
            if (uriString.getOrNull(ssi + 1) != '/') return@lazy emptyList()
        }

        val path = InternalUri.parsePath(uriString, ssi)

        path.split('/').map { InternalUri.decode(it) }
    }

    override fun getFragment(): String? = _fragment

    override fun getQuery(): String? = _query

    override fun getPathSegments(): List<String> = _pathSegments

    private fun isHierarchical(): Boolean {
        if (schemeSeparatorIndex == -1) return true // All relative URIs are hierarchical.
        if (uriString.length == schemeSeparatorIndex + 1) return false // No ssp.

        // If the ssp starts with a '/', this is hierarchical.
        return uriString[schemeSeparatorIndex + 1] == '/'
    }

    override fun getQueryParameters(key: String): List<String> {
        require(isHierarchical())
        val query = _query ?: return emptyList()
        val encodedKey = InternalUri.encode(key)

        return query.split('&').mapNotNull {
            val i = it.indexOf('=')
            when {
                i == -1 -> if (it == encodedKey) "" else null
                it.substring(0, i) == encodedKey -> {
                    InternalUri.decode(it.substring(i + 1))
                }
                else -> null
            }
        }
    }

    override fun getQueryParameterNames(): Set<String> {
        require(isHierarchical())
        val query = _query ?: return emptySet()

        return query
            .split('&')
            .map {
                val index = it.indexOf('=')
                if (index == -1) return@map it else InternalUri.decode(it.substring(0, index))
            }
            .toSet()
    }

    override fun toString(): String = uriString
}
