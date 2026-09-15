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

package androidx.credentials.agesignals

import android.content.Context
import android.os.CancellationSignal
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AgeSignalProviderFactoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var factory: AgeSignalProviderFactory

    @Before
    fun setUp() {
        factory = AgeSignalProviderFactory(context)
    }

    @Test
    fun getBestAvailableProvider_testModeWithAvailableProvider_returnsProvider() {
        val fakeProvider = FakeAgeSignalProvider(context, isAvailableResponse = true)
        factory.testMode = true
        factory.testProvider = fakeProvider

        val provider = factory.getBestAvailableProvider()

        assertThat(provider).isNotNull()
        assertThat(provider).isSameInstanceAs(fakeProvider)
    }

    @Test
    fun getBestAvailableProvider_testModeWithUnavailableProvider_returnsNull() {
        val fakeProvider = FakeAgeSignalProvider(context, isAvailableResponse = false)
        factory.testMode = true
        factory.testProvider = fakeProvider

        val provider = factory.getBestAvailableProvider()

        assertThat(provider).isNull()
    }

    @Test
    fun getBestAvailableProvider_testModeWithNullProvider_returnsNull() {
        factory.testMode = true
        factory.testProvider = null

        val provider = factory.getBestAvailableProvider()

        assertThat(provider).isNull()
    }

    @Test
    fun getBestAvailableProvider_noManifestProvider_returnsNull() {
        factory.testMode = false

        val provider = factory.getBestAvailableProvider()

        assertThat(provider).isNull()
    }

    @Test
    fun instantiateProvider_validClassName_instantiatesProviderViaReflection() {
        val provider = factory.instantiateProvider(listOf(FakeAgeSignalProvider::class.java.name))

        assertThat(provider).isNotNull()
        assertThat(provider).isInstanceOf(FakeAgeSignalProvider::class.java)
    }

    class UnavailableProvider(context: Context) : AgeSignalProvider {
        override fun onGetAgeRange(
            context: Context,
            request: GetAgeRangeRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
        ) {}

        override fun isAvailable(): Boolean = false
    }

    class SecondValidProvider(context: Context) : AgeSignalProvider {
        override fun onGetAgeRange(
            context: Context,
            request: GetAgeRangeRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
        ) {}

        override fun isAvailable(): Boolean = true
    }

    @Test
    fun instantiateProvider_multipleActiveProviders_returnsNull() {
        val provider =
            factory.instantiateProvider(
                listOf(
                    FakeAgeSignalProvider::class.java.name,
                    SecondValidProvider::class.java.name,
                )
            )

        assertThat(provider).isNull()
    }

    @Test
    fun instantiateProvider_unavailableProvider_returnsNull() {
        val provider = factory.instantiateProvider(listOf(UnavailableProvider::class.java.name))

        assertThat(provider).isNull()
    }

    @Test
    fun instantiateProvider_invalidClassName_returnsNull() {
        val provider = factory.instantiateProvider(listOf("com.nonexistent.InvalidProviderClass"))

        assertThat(provider).isNull()
    }

    @Test
    fun instantiateProvider_classWithoutMatchingConstructor_returnsNull() {
        val provider = factory.instantiateProvider(listOf(String::class.java.name))

        assertThat(provider).isNull()
    }
}
