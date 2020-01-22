/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.startup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppInitializerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun basicUsageTest() {
        AppInitializer.initialize(context, listOf<Class<*>>(InitializerNoDependencies::class.java))
        assertThat(AppInitializer.sInitialized.get(), `is`(true))
    }

    @Test
    fun basicInitializationTest() {
        val initializing = mutableSetOf<Class<*>>()
        val initialized = mutableSetOf<Class<*>>()
        val components = listOf<Class<*>>(InitializerNoDependencies::class.java)
        AppInitializer.doInitialize(context, components, initializing, initialized)
        assertThat(initializing.size, `is`(0))
        assertThat(initialized.size, `is`(1))
        assertThat<Collection<Class<*>>>(
            initialized,
            containsInAnyOrder<Class<*>>(*components.toTypedArray())
        )
    }

    @Test
    fun initializationWithDependencies() {
        val initializing = mutableSetOf<Class<*>>()
        val initialized = mutableSetOf<Class<*>>()
        val components = listOf<Class<*>>(InitializerWithDependency::class.java)
        AppInitializer.doInitialize(context, components, initializing, initialized)
        assertThat(initializing.size, `is`(0))
        assertThat(initialized.size, `is`(2))
        assertThat<Collection<Class<*>>>(
            initialized,
            containsInAnyOrder<Class<*>>(
                InitializerNoDependencies::class.java,
                InitializerWithDependency::class.java
            )
        )
    }

    @Test
    fun initializationWithCyclicDependencies() {
        val initializing = mutableSetOf<Class<*>>()
        val initialized = mutableSetOf<Class<*>>()
        val components = listOf<Class<*>>(CyclicDependencyInitializer::class.java)
        try {
            AppInitializer.doInitialize(context, components, initializing, initialized)
            fail()
        } catch (exception: StartupException) {
            assertThat(exception.localizedMessage, containsString("Cycle detected."))
        }
    }
}
