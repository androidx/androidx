/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.lifecycle

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidViewModelFactoryTest {

    @Test
    fun testEmptyConstructorAndExtras() {
        val factory = AndroidViewModelFactory()
        val extras = MutableCreationExtras()
        extras[AndroidViewModelFactory.APPLICATION_KEY] = queryApplication()
        val vm = factory.create(VM::class.java, extras)
        assertThat(vm).isNotNull()
    }

    @Test
    fun testEmptyConstructorAndEmptyExtras() {
        val factory = AndroidViewModelFactory()
        try {
            factory.create(VM::class.java, CreationExtras.Empty)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun testEmptyConstructorAndNoExtras() {
        val factory = AndroidViewModelFactory()
        try {
            factory.create(VM::class.java)
            Assert.fail()
        } catch (e: UnsupportedOperationException) {
        }
    }

    @Test
    fun testAppConstructorAndEmptyExtras() {
        val factory = AndroidViewModelFactory(queryApplication())
        val vm = factory.create(VM::class.java, CreationExtras.Empty)
        assertThat(vm).isNotNull()
    }

    @Test
    fun testAppConstructorAndNoExtras() {
        val factory = AndroidViewModelFactory(queryApplication())
        val vm = factory.create(VM::class.java)
        assertThat(vm).isNotNull()
    }
}

class VM(application: Application) : AndroidViewModel(application)

private fun queryApplication(): Application {
    val context = InstrumentationRegistry.getInstrumentation().context.applicationContext
    return context as? Application ?: throw IllegalStateException("Failed to get an application")
}