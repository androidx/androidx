/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.internal

import android.content.Context
import android.content.ContextWrapper
import androidx.appfunctions.AppFunctionConfiguration
import androidx.appfunctions.internal.ConfigurableAppFunctionFactory.AppFunctionInstantiationException
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ConfigurableAppFunctionFactoryTest {

    private lateinit var noConfigContext: Context

    private lateinit var configContext: Context

    @Before
    fun setup() {
        noConfigContext = FakeNoConfigContext(mock(Context::class.java))
        configContext = FakeConfigProviderContext(mock(Context::class.java))
    }

    @Test
    fun testCreateNoArgEnclosingClass_withoutConfig() {
        val factory = ConfigurableAppFunctionFactory<NoArgEnclosingClass>(noConfigContext)
        factory.createEnclosingClass(NoArgEnclosingClass::class.java)
    }

    @Test
    fun testCreateNoArgEnclosingClass_withConfig() {
        val creator = ConfigurableAppFunctionFactory<NoArgEnclosingClass>(configContext)
        creator.createEnclosingClass(NoArgEnclosingClass::class.java)
    }

    @Test
    fun testCreateNoArgEnclosingClass_ifProvideFactory() {
        val creator = ConfigurableAppFunctionFactory<NoArgWithFactoryEnclosingClass>(configContext)
        creator.createEnclosingClass(NoArgWithFactoryEnclosingClass::class.java)
    }

    @Test
    fun testCreateEnclosingClassRequiredArgs_withoutFactory() {
        val creator = ConfigurableAppFunctionFactory<RequiredArgsEnclosingClass>(noConfigContext)
        assertThrows(AppFunctionInstantiationException::class.java) {
            creator.createEnclosingClass(RequiredArgsEnclosingClass::class.java)
        }
    }

    @Test
    fun testCreateEnclosingClassRequiredArgs_withFactory() {
        val creator = ConfigurableAppFunctionFactory<RequiredArgsEnclosingClass>(configContext)
        creator.createEnclosingClass(RequiredArgsEnclosingClass::class.java)
    }

    @Test
    fun testCreateEnclosingClassWithPrivateConstructor() {
        val creator =
            ConfigurableAppFunctionFactory<PrivateConstructorEnclosingClass>(noConfigContext)
        assertThrows(AppFunctionInstantiationException::class.java) {
            creator.createEnclosingClass(PrivateConstructorEnclosingClass::class.java)
        }
    }

    @Test
    fun testCreateEnclosingClass_thatThrowErrorDuringInvocation() {
        val creator = ConfigurableAppFunctionFactory<InvocationErrorEnclosingClass>(noConfigContext)
        assertThrows(AppFunctionInstantiationException::class.java) {
            creator.createEnclosingClass(InvocationErrorEnclosingClass::class.java)
        }
    }

    @Test
    fun testCreateEnclosingClass_thatThrowErrorDuringInitialization() {
        val creator = ConfigurableAppFunctionFactory<InitializeErrorEnclosingClass>(noConfigContext)
        assertThrows(AppFunctionInstantiationException::class.java) {
            creator.createEnclosingClass(InitializeErrorEnclosingClass::class.java)
        }
    }

    // Fake context
    class FakeConfigProviderContext(baseContext: Context) :
        ContextWrapper(baseContext), AppFunctionConfiguration.Provider {
        override fun getApplicationContext(): Context? = this

        override val appFunctionConfiguration: AppFunctionConfiguration
            get() =
                AppFunctionConfiguration.Builder()
                    .addEnclosingClassFactory(
                        NoArgWithFactoryEnclosingClass::class.java,
                    ) {
                        NoArgWithFactoryEnclosingClass()
                    }
                    .addEnclosingClassFactory(
                        RequiredArgsEnclosingClass::class.java,
                    ) {
                        RequiredArgsEnclosingClass(0)
                    }
                    .build()
    }

    class FakeNoConfigContext(baseContext: Context) : ContextWrapper(baseContext) {
        override fun getApplicationContext(): Context? = this
    }

    // Test enclosing classes
    class NoArgEnclosingClass()

    class NoArgWithFactoryEnclosingClass()

    class RequiredArgsEnclosingClass(val x: Int)

    class PrivateConstructorEnclosingClass private constructor()

    class InvocationErrorEnclosingClass private constructor(val x: Int) {
        constructor() : this(0) {
            throw RuntimeException()
        }
    }

    class InitializeErrorEnclosingClass() {
        init {
            throw RuntimeException()
        }
    }
}
