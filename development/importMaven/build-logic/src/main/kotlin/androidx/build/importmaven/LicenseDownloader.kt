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

package androidx.build.importmaven

import java.util.concurrent.TimeUnit.MINUTES
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.logging.log4j.kotlin.logger
import org.w3c.dom.Node

/**
 * Pulls the license for a given project. A license might be fetched:
 * * directly, if it is a txt url
 * * from github, if it is a github project
 * * via license server, as the fallback.
 */
class LicenseDownloader(
    /**
     * If set, we'll also query github API to get the license. Note that, even though this provides
     * better license files, it might potentially fetch the wrong license if the project changed its
     * license.
     */
    private val enableGithubApi: Boolean = false
) {
    private val logger = logger("LicenseDownloader")
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val licenseEndpoint = "https://fetch-licenses.appspot.com/convert/licenses"
    private val githubLicenseApiClient = GithubLicenseApiClient()
    private val licenseUrlXPath =
        XPathFactory.newInstance().newXPath().compile("/project/licenses/license/url")
    private val licenseNameXPath =
        XPathFactory.newInstance().newXPath().compile("/project/licenses/license/name")
    private val scmUrlXPath = XPathFactory.newInstance().newXPath().compile("/project/scm/url")
    private val client = OkHttpClient.Builder().readTimeout(1, MINUTES).build()

    private val apacheLicenseNames =
        setOf(
            "The Apache Software License, Version 2.0",
            "The Apache License, Version 2.0",
            "Apache License, Version 2.0",
            "Apache 2.0",
            "apache-2.0",
        )
    private val apacheUrls =
        setOf(
            "http://www.apache.org/licenses/LICENSE-2.0.txt",
            "https://www.apache.org/licenses/LICENSE-2.0.txt",
            "http://www.apache.org/licenses/LICENSE-2.0",
            "http://www.apache.org/licenses/LICENSE-2.0.html",
            "https://opensource.org/licenses/Apache-2.0",
        )
    private val mitLicenseNames = setOf("MIT License", "The MIT License")
    private val mitUrls =
        setOf(
            "https://opensource.org/licenses/MIT",
            "http://opensource.org/licenses/MIT",
            "http://www.opensource.org/licenses/mit-license.php",
        )
    private val bsd3LicenseNames = setOf("BSD-3-Clause", "3-Clause BSD License")
    private val bsd3Urls =
        setOf(
            "https://asm.ow2.io/license.html",
            "https://opensource.org/licenses/BSD-3-Clause",
            "http://opensource.org/licenses/BSD-3-Clause",
        )
    private val androidSoftwareLicenseNames =
        setOf(
            "Android Software Development Kit License",
            "Android Software Development Kit License Agreement",
        )
    private val androidSoftwareUrls =
        setOf(
            "https://developer.android.com/studio/terms",
            "https://developer.android.com/studio/terms.html",
        )

    /** Fetches license information for external dependencies. */
    fun fetchLicenseFromPom(bytes: ByteArray): ByteArray? {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = bytes.inputStream().use { builder.parse(it) }
        val licenseName =
            (licenseNameXPath.evaluate(document, XPathConstants.NODE) as? Node)?.textContent
        val licenseUrl =
            (licenseUrlXPath.evaluate(document, XPathConstants.NODE) as? Node)?.textContent
        val scmUrl = (scmUrlXPath.evaluate(document, XPathConstants.NODE) as? Node)?.textContent
        val fetchers =
            listOf(
                {
                    // short-circuit common license
                    if (licenseName != null && licenseUrl != null) {
                        tryReturnKnownLicense(licenseName, licenseUrl)
                    } else null
                },
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
                },
            )
        val licenseContents = fetchers.firstNotNullOfOrNull { it() } ?: return null
        // get rid of any windows style line endings or extra newlines
        val cleanedUp = licenseContents.replace("\r", "").dropLastWhile { it == '\n' } + "\n"
        return cleanedUp.toByteArray(Charsets.UTF_8)
    }

    private fun tryReturnKnownLicense(name: String, url: String): String? {
        fun getLicenseByName(name: String): String {
            println("Getting $name")
            return LicenseDownloader::class.java.getResourceAsStream(name)!!.bufferedReader().use {
                it.readText()
            }
        }
        return when {
            name in apacheLicenseNames && url in apacheUrls -> getLicenseByName("/apache-2.0.txt")
            name in mitLicenseNames && url in mitUrls -> getLicenseByName("/mit.txt")
            name in bsd3LicenseNames && url in bsd3Urls -> getLicenseByName("/bsd-3.txt")
            name in androidSoftwareLicenseNames && url in androidSoftwareUrls ->
                getLicenseByName("/android-software-license.txt")
            name == "Eclipse Public License 1.0" &&
                url == "http://www.eclipse.org/legal/epl-v10.html" ->
                getLicenseByName("/epl-1.0.txt")
            name == "Eclipse Public License v2.0" &&
                url == "https://www.eclipse.org/legal/epl-v20.html" ->
                getLicenseByName("/epl-2.0.txt")
            else -> null
        }
    }

    private fun tryFetchTxtLicense(url: String): String? {
        if (!url.endsWith(".txt")) {
            return null
        }
        logger.trace { "Fetching license directly from $url" }
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() }
    }

    private fun fetchViaLicenseProxy(url: String): String? {
        logger.trace { "Fetching license ($url) via license server" }
        val payload = "{\"url\": \"$url\"}".toRequestBody(mediaType)
        val request = Request.Builder().url(licenseEndpoint).post(payload).build()
        return client.newCall(request).execute().use { it.body?.string() }
    }
}
