/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people.internal

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.people.internal.platform.PlatformPeopleBackend
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "PeopleBackendResolver"

/**
 * Decides, once per process, which People Provider serves this device, for both client applications
 * and People Directory applications.
 *
 * `androidx.people.PeopleDirectoryContentProvider` and `androidx.people:people-client` both route
 * every call through [resolve], so the two can never disagree about which People Provider is in
 * use.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object PeopleBackendResolver {

    /** The only class name this artifact knows from `androidx.people:people-play-services`. */
    private const val PLAY_SERVICES_FACTORY =
        "androidx.people.playservices.PlayServicesPeopleBackendFactory"

    private val resolver =
        BackendResolver(
            platform = { context -> PlatformPeopleBackend.createIfSupported(context) },
            playServices = { context ->
                loadPeopleBackendFactory(PLAY_SERVICES_FACTORY)?.create(context)
            },
        )

    /**
     * Returns the backend serving this process.
     *
     * Main-safe. The first call probes the device, which may take a round trip to Google Play
     * services; later calls return the cached backend.
     *
     * @param context any [Context]; only the application context is retained.
     */
    public suspend fun resolve(context: Context): PeopleBackend =
        resolver.resolve(context.applicationContext)
}

/**
 * Picks the first People Provider that serves this device — the platform, then Google Play services
 * — and caches the answer for the life of the process.
 *
 * Each probe returns the backend for its People Provider, returns `null` if that People Provider
 * definitively does not serve the calling application, or throws if the answer could not be
 * determined. Caching means that a change on the device, such as Google Play services starting to
 * support the application, takes effect the next time the process starts. A probe that throws is
 * not cached, so that a transient failure does not hide an available People Provider until then.
 *
 * @param platform returns the platform backend, or `null` if the platform does not serve this
 *   device.
 * @param playServices returns the Google Play services backend, or `null` if
 *   `androidx.people:people-play-services` is not part of the application or Google Play services
 *   does not support it.
 */
internal class BackendResolver(
    private val platform: suspend (Context) -> PeopleBackend?,
    private val playServices: suspend (Context) -> PeopleBackend?,
) {
    private val mutex = Mutex()

    @Volatile private var resolved: PeopleBackend? = null

    /** Returns the cached backend, probing for one on the first call. */
    suspend fun resolve(context: Context): PeopleBackend =
        resolved ?: mutex.withLock { resolved ?: probe(context) }

    private suspend fun probe(context: Context): PeopleBackend {
        val backend =
            try {
                platform(context) ?: playServices(context) ?: UnavailablePeopleBackend
            } catch (e: CancellationException) {
                // Nothing is cached, so the next call probes again.
                throw e
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Could not determine the People Provider; will retry on the next call",
                    e,
                )
                return UnavailablePeopleBackend
            }
        resolved = backend
        return backend
    }
}

/**
 * Loads the [PeopleBackendFactory] named [className] through its public no-argument constructor, or
 * returns `null` if it cannot be loaded.
 *
 * A missing class means that the artifact providing it is not part of the application. Any other
 * failure, such as a constructor removed because the artifact's keep rule did not apply, cannot fix
 * itself while the process runs, so it is logged and treated the same way rather than retried.
 */
internal fun loadPeopleBackendFactory(className: String): PeopleBackendFactory? =
    try {
        Class.forName(className).getDeclaredConstructor().newInstance() as PeopleBackendFactory
    } catch (e: ClassNotFoundException) {
        null
    } catch (e: ReflectiveOperationException) {
        Log.w(TAG, "Cannot instantiate $className", e)
        null
    } catch (e: ClassCastException) {
        Log.w(TAG, "$className is not a PeopleBackendFactory", e)
        null
    }
