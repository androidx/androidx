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

package androidx.xr.runtime.testing

import androidx.xr.runtime.SessionConfigureResult
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.testing.internal.FakeSessionResultProvider
import androidx.xr.runtime.testing.internal.FakeSessionResultProviderFactory
import org.junit.rules.ExternalResource

/**
 * A JUnit TestRule for controlling the results of [androidx.xr.runtime.Session] operations within
 * tests. This rule intercepts calls to create and configure sessions, allowing tests to inject
 * specific success or failure results.
 *
 * To use this rule, include it in your test class:
 * ```kotlin
 * @RunWith(JUnit4::class)
 * class MySessionTest {
 *     @get:Rule
 *     val sessionTestRule = SessionTestRule()
 *
 *     @Test
 *     fun testSessionCreationFails() = runTest {
 *         sessionTestRule.createResult = SessionCreateResult.Failure(IllegalArgumentException("Test"))
 *         // ... code that calls Session.create() ...
 *     }
 *
 *     @Test
 *     fun testSessionConfigurationSuccess() = runTest {
 *         sessionTestRule.configureResult = SessionConfigureResult.Success
 *         // ... code that calls Session.configure() ...
 *     }
 * }
 * ```
 */
public class SessionTestRule : ExternalResource() {

    /**
     * The result to be returned when [androidx.xr.runtime.Session.create] is called. If null, the
     * default behavior of the runtime will be used. While this will likely be
     * [androidx.xr.runtime.SessionCreateSuccess], other results are possible, depending on the
     * underlying selected runtime implementation.
     */
    public var createResult: SessionCreateResult? = null

    /**
     * The result to be returned when [androidx.xr.runtime.Session.configure] is called. If null,
     * the default behavior of the runtime will be used. While this will likely be
     * [androidx.xr.runtime.SessionConfigureSuccess], other results are possible, depending on the
     * underlying selected runtime implementation.
     */
    public var configureResult: SessionConfigureResult? = null
        set(value) {
            field = value
            if (::sessionResultProvider.isInitialized) {
                sessionResultProvider.configureResult = value
            }
        }

    internal lateinit var sessionResultProvider: FakeSessionResultProvider

    override fun before() {
        FakeSessionResultProviderFactory.sessionTestRule = this
    }

    override fun after() {
        FakeSessionResultProviderFactory.sessionTestRule = null
    }

    internal fun registerWithProvider(provider: FakeSessionResultProvider) {
        sessionResultProvider = provider

        sessionResultProvider.createResult = createResult
        sessionResultProvider.configureResult = configureResult
    }
}
