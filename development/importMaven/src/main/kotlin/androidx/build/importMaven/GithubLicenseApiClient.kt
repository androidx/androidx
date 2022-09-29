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

package androidx.build.importMaven

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.kotlin.com.google.common.annotations.VisibleForTesting

/**
 * Downloads the license for a given Github project URL.
 */
class GithubLicenseApiClient {
    private val logger = logger("GithubLicenseApiClient")
    private val client = OkHttpClient()

    /**
     * Returns the license for the given [githubUrl] if it is a Github url and the project has a
     * license file.
     */
    fun getProjectLicense(githubUrl: String): String? {
        val (owner, repo) = githubUrl.extractGithubOwnerAndRepo() ?: return null
        logger.trace {
            "Getting license for $githubUrl"
        }
        val request = Request.Builder().url(
            "https://api.github.com/repos/$owner/$repo/license"
        ).addHeader(
            "Accept", "application/vnd.github.v3.raw"
        ).build()
        val response = client.newCall(request).execute()
        if (response.code == 404) {
            logger.warn {
                """
                Failed to get license from github for $githubUrl
                API response: ${response.body?.string()}
                """.trimIndent()
            }
            return null
        }
        return response.body?.use {
            it.string()
        }
    }

    @VisibleForTesting
    internal fun String.extractGithubOwnerAndRepo(): Pair<String, String>? {
        val httpUrl = this.toHttpUrlOrNull() ?: return null
        httpUrl.host.split('.').let {
            if (it.size < 2) return null
            if (it.last() != "com") return null
            if (it[it.size - 2] != "github") return null
        }
        val pathSegments = httpUrl.pathSegments.filter { it.isNotBlank() }
        if (pathSegments.size != 2) {
            return null
        }
        return pathSegments[0] to pathSegments[1]
    }
}