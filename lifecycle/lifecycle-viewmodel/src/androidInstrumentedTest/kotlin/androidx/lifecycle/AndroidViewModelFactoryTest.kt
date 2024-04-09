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
import androidx.kruth.assertThat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
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
    fun testEmptyConstructorAndEmptyExtrasAndroidViewModel() {
        val factory = AndroidViewModelFactory()
        try {
            factory.create(VM::class.java, CreationExtras.Empty)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun testEmptyConstructorAndEmptyExtrasSimpleViewModel() {
        val factory = AndroidViewModelFactory()
        val vm = factory.create(TestVM::class.java, CreationExtras.Empty)
        assertThat(vm).isNotNull()
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

    @Test
    fun testAppConstructorCreateIsCalled() {
        val factory = VMFactory(queryApplication())
        val vm = factory.create(VM::class.java, CreationExtras.Empty)
        assertThat(vm).isNotNull()
        assertThat(factory.calledCreateWithNoExtras).isTrue()
    }
}

class VMFactory(application: Application) : AndroidViewModelFactory(application) {
    var calledCreateWithNoExtras = false
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        calledCreateWithNoExtras = true
        return super.create(modelClass)
    }
}

class VM(application: Application) : AndroidViewModel(application)

class TestVM() : ViewModel()

private fun queryApplication(): Application {
    val context = InstrumentationRegistry.getInstrumentation().context.applicationContext
    return context as? Application ?: throw IllegalStateException("Failed to get an application")
}
