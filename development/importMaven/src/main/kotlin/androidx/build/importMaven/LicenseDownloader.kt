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

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.logging.log4j.kotlin.logger
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Pulls the license for a given project.
 * A license might be fetched:
 * * directly, if it is a txt url
 * * from github, if it is a github project
 * * via license server, as the fallback.
 */
class LicenseDownloader(
    /**
     * If set, we'll also query github API to get the license.
     * Note that, even though this provides better license files, it might potentially fetch the
     * wrong license if the project changed its license.
     */
    private val enableGithubApi: Boolean = false
) {
    private val logger = logger("LicenseDownloader")
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val licenseEndpoint = "https://fetch-licenses.appspot.com/convert/licenses"
    private val githubLicenseApiClient = GithubLicenseApiClient()
    private val licenseXPath = XPathFactory.newInstance().newXPath()
        .compile("/project/licenses/license/url")
    private val scmUrlXPath = XPathFactory.newInstance().newXPath()
        .compile("/project/scm/url")
    private val client = OkHttpClient()

    /**
     * Fetches license information for external dependencies.
     */
    fun fetchLicenseFromPom(bytes: ByteArray): ByteArray? {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = bytes.inputStream().use {
            builder.parse(it)
        }
        val licenseUrl = (licenseXPath.evaluate(document, XPathConstants.NODE) as? Node)
            ?.textContent
        val scmUrl = (scmUrlXPath.evaluate(document, XPathConstants.NODE) as? Node)?.textContent
        val fetchers = listOf(
            {
                // directly download if it is a txt file
                licenseUrl?.let(this::tryFetchTxtLicense)
            },
            {
                // download via github API if it is hosted on github
                if (enableGithubApi) {
                    scmUrl?.let(githubLicenseApiClient::getProjectLicense)
                } else {
                    null
                }
            },
            {
                // fallback to license server
                licenseUrl?.let(this::fetchViaLicenseProxy)
            }
        )
        val licenseContents = fetchers.firstNotNullOfOrNull {
            it()
        } ?: return null
        // get rid of any windows style line endings or extra newlines
        val cleanedUp = licenseContents.replace("\r", "").dropLastWhile {
            it == '\n'
        } + "\n"
        return cleanedUp.toByteArray(Charsets.UTF_8)
    }

    private fun tryFetchTxtLicense(url: String): String? {
        if (!url.endsWith(".txt")) {
            return null
        }
        logger.trace {
            "Fetching license directly from $url"
        }
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use {
            it.body?.string()
        }
    }

    private fun fetchViaLicenseProxy(url: String): String? {
        logger.trace {
            "Fetching license ($url) via license server"
        }
        val payload = "{\"url\": \"$url\"}".toRequestBody(mediaType)
        val request = Request.Builder().url(licenseEndpoint).post(payload).build()
        return client.newCall(request).execute().use {
            it.body?.string()
        }
    }
}