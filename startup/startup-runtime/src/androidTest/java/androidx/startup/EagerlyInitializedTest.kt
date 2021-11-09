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
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class EagerlyInitializedTest {
    class A : Initializer<Unit> {
        override fun create(context: Context) {
            val initializer = AppInitializer.getInstance(context)
            assertTrue(initializer.isEagerlyInitialized(javaClass))
        }

        override fun dependencies() = listOf(B::class.java)
    }

    class B : Initializer<Unit> {
        override fun create(context: Context) {
            val initializer = AppInitializer.getInstance(context)
            assertTrue(initializer.isEagerlyInitialized(javaClass))
        }

        override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
    }

    private lateinit var context: Context
    private lateinit var appInitializer: AppInitializer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appInitializer = AppInitializer(context)
        AppInitializer.setDelegate(appInitializer)
    }

    @Test
    fun testEagerlyInitialize() {
        val metadata = Bundle()
        metadata.putString(A::class.java.name, STARTUP)
        metadata.putString(B::class.java.name, STARTUP)
        appInitializer.discoverAndInitialize(metadata)
    }

    companion object {
        const val STARTUP = "androidx.startup"
    }
}
