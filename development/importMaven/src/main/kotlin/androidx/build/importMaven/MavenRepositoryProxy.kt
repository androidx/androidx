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

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.path
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.toByteArray
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.net.URI
import java.net.URL

/**
 * Creates a local proxy server for given the artifactory url.
 *
 * @see MavenRepositoryProxy.Companion.startAll
 */
class MavenRepositoryProxy private constructor(
    delegateHost: String,
    val downloadObserver: DownloadObserver?
) {
    init {
        check(delegateHost.startsWith("http")) {
            "Unsupported url: $delegateHost. Only http(s) urls are supported"
        }
    }

    private val logger = logger("MavenProxy[$delegateHost]")

    private val delegateHost = delegateHost.trimEnd {
        it == '/'
    }

    fun <T> start(block: (URI) -> T): T {
        val client = HttpClient(OkHttp)
        val server = embeddedServer(Netty, port = 0 /*random port*/) {
            routing {
                get("/{...}") {
                    val path = this.call.request.path()
                    val incomingHeaders = this.call.request.headers
                    logger.trace {
                        "Request($path)"
                    }

                    try {
                        val (clientResponse, responseBytes) = requestFromDelegate(
                            path,
                            client,
                            incomingHeaders
                        )
                        call.respondBytes(
                            bytes = responseBytes,
                            contentType = clientResponse.contentType(),
                            status = clientResponse.status
                        ).also {
                            logger.trace {
                                "Success ($path)"
                            }
                        }
                    } catch (ex: Throwable) {
                        logger.error(ex) {
                            "Failed ($path): ${ex.message}"
                        }
                        throw ex
                    }
                }
            }
        }
        return try {
            server.start(wait = false)
            val url = runBlocking {
                server.resolvedConnectors().first().let {
                    URL(
                        it.type.name.lowercase(),
                        it.host,
                        it.port,
                        ""
                    )
                    URI("${it.type.name.lowercase()}://${it.host}:${it.port}")
                }
            }
            block(url)
        } finally {
            runCatching {
                client.close()
            }
            runCatching {
                server.stop()
            }
        }
    }

    private suspend fun requestFromDelegate(
        path: String,
        client: HttpClient,
        incomingHeaders: Headers
    ): Pair<HttpResponse, ByteArray> {
        val delegatedUrl = "$delegateHost$path"
        val clientResponse = client.request(delegatedUrl) {
            incomingHeaders.forEach { key, value ->
                // don't copy host header since we are proxying from localhost.
                if (key != "Host") {
                    header(key, value)
                }
            }
            method = HttpMethod.Get
        }
        val responseBytes = clientResponse.bodyAsChannel().toByteArray()
        if (clientResponse.status.isSuccess()) {
            downloadObserver?.onDownload(
                path = path.dropWhile { it == '/' },
                bytes = responseBytes
            )
        }
        return Pair(clientResponse, responseBytes)
    }

    companion object {
        /**
         * Creates proxy servers for all given artifactory urls.
         *
         * It will call the given [block] with local servers that can be provided to gradle as maven
         * repositories.
         */
        fun <T> startAll(
            repositoryUrls: List<String>,
            downloadObserver: DownloadObserver?,
            block: (List<URI>) -> T
        ): T {
            val proxies = repositoryUrls.map { url ->
                MavenRepositoryProxy(
                    delegateHost = url,
                    downloadObserver = downloadObserver
                )
            }
            return startAll(
                proxies = proxies,
                previousUris = emptyList(),
                block = block
            )
        }

        /**
         * Recursively start all proxy servers
         */
        private fun <T> startAll(
            proxies: List<MavenRepositoryProxy>,
            previousUris: List<URI>,
            block: (List<URI>) -> T
        ): T {
            if (proxies.isEmpty()) {
                return block(previousUris)
            }
            val first = proxies.first()
            return first.start { myUri ->
                startAll(
                    proxies = proxies.drop(1),
                    previousUris = previousUris + myUri,
                    block = block
                )
            }
        }
    }
}