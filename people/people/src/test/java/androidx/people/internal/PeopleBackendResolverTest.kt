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
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PeopleBackendResolverTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun resolve_withThePlatform_prefersItWithoutAskingGooglePlayServices() = runTest {
        val platform = AvailableBackend()
        var playServicesProbes = 0
        val resolver =
            BackendResolver(
                platform = { platform },
                playServices = {
                    playServicesProbes++
                    AvailableBackend()
                },
            )

        val result = resolver.resolve(context)

        assertThat(result).isSameInstanceAs(platform)
        assertThat(playServicesProbes).isEqualTo(0)
    }

    @Test
    fun resolve_withoutThePlatform_usesGooglePlayServices() = runTest {
        val playServices = AvailableBackend()
        val resolver = BackendResolver(platform = { null }, playServices = { playServices })

        val result = resolver.resolve(context)

        assertThat(result).isSameInstanceAs(playServices)
    }

    @Test
    fun resolve_withNeitherPeopleProvider_isUnavailable() = runTest {
        val resolver = BackendResolver(platform = { null }, playServices = { null })

        val result = resolver.resolve(context)

        assertThat(result).isSameInstanceAs(UnavailablePeopleBackend)
    }

    @Test
    fun resolve_whenCalledRepeatedly_probesOnce() = runTest {
        var probes = 0
        val resolver =
            BackendResolver(
                platform = { null },
                playServices = {
                    probes++
                    null
                },
            )

        val first = resolver.resolve(context)
        val second = resolver.resolve(context)

        assertThat(second).isSameInstanceAs(first)
        assertThat(probes).isEqualTo(1)
    }

    @Test
    fun resolve_afterATransientFailure_probesAgain() = runTest {
        val playServices = AvailableBackend()
        var fail = true
        val resolver =
            BackendResolver(
                platform = { null },
                playServices = {
                    if (fail) throw IllegalStateException("Binder died") else playServices
                },
            )

        val first = resolver.resolve(context)
        fail = false
        val second = resolver.resolve(context)

        assertThat(first).isSameInstanceAs(UnavailablePeopleBackend)
        assertThat(second).isSameInstanceAs(playServices)
    }

    @Test
    fun resolve_whenCancelled_cachesNothing() = runTest {
        val platform = AvailableBackend()
        var cancel = true
        val resolver =
            BackendResolver(
                platform = { if (cancel) throw CancellationException("Cancelled") else platform },
                playServices = { null },
            )

        val first = runCatching { resolver.resolve(context) }
        cancel = false
        val second = resolver.resolve(context)

        assertThat(first.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(second).isSameInstanceAs(platform)
    }

    @Test
    fun peopleBackendResolver_withoutAnyPeopleProvider_isUnavailable() = runTest {
        // Robolectric runs below Android 37.2, and people-play-services is not on the classpath.
        val result = PeopleBackendResolver.resolve(context)

        assertThat(result).isSameInstanceAs(UnavailablePeopleBackend)
    }

    @Test
    fun loadPeopleBackendFactory_withAFactory_returnsANewInstance() {
        val factory = loadPeopleBackendFactory(TestFactory::class.java.name)

        assertThat(factory).isInstanceOf(TestFactory::class.java)
    }

    @Test
    fun loadPeopleBackendFactory_withoutTheClass_returnsNull() {
        val factory = loadPeopleBackendFactory("androidx.people.playservices.DoesNotExist")

        assertThat(factory).isNull()
    }

    @Test
    fun loadPeopleBackendFactory_withoutANoArgumentConstructor_returnsNull() {
        val factory = loadPeopleBackendFactory(FactoryWithoutNoArgumentConstructor::class.java.name)

        assertThat(factory).isNull()
    }

    @Test
    fun loadPeopleBackendFactory_withAClassThatIsNotAFactory_returnsNull() {
        val factory = loadPeopleBackendFactory(NotAFactory::class.java.name)

        assertThat(factory).isNull()
    }

    /** A backend distinct from [UnavailablePeopleBackend], for identity checks. */
    private class AvailableBackend : PeopleBackend by UnavailablePeopleBackend {
        override val isAvailable: Boolean = true
    }

    class TestFactory : PeopleBackendFactory {
        override suspend fun create(context: Context): PeopleBackend? = null
    }

    class FactoryWithoutNoArgumentConstructor(@Suppress("unused") private val name: String) :
        PeopleBackendFactory {
        override suspend fun create(context: Context): PeopleBackend? = null
    }

    class NotAFactory
}
