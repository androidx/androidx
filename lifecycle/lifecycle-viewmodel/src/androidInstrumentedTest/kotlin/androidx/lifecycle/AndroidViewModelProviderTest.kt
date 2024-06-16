/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.reflect.KClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidViewModelProviderTest {

    /** @see androidx.lifecycle.viewmodel.createViewModel */
    @Test
    fun get_throwsNotImplementedError_shouldFallbackToAvoidDesugaringIssues() {
        val extras = MutableCreationExtras()
        val factory = NotImplementedErrorFactory()
        val provider = ViewModelProvider(ViewModelStore(), factory, extras)
        val expectedModelClass = ViewModel::class

        assertThrows<IllegalArgumentException> { provider[expectedModelClass.java] }
        assertThat(factory.createInvocations).hasSize(expectedSize = 3)

        val (actualModelClass1, actualCreationExtras1) = factory.createInvocations[0]
        assertThat(actualModelClass1).isEqualTo(expectedModelClass)
        assertThat(actualCreationExtras1).isInstanceOf<CreationExtras>()

        val (actualViewModelClass2, actualCreationExtras2) = factory.createInvocations[1]
        assertThat(actualViewModelClass2).isEqualTo(expectedModelClass.java)
        assertThat(actualCreationExtras2).isInstanceOf<CreationExtras>()

        val (actualModelClass3, actualCreationExtras3) = factory.createInvocations[2]
        assertThat(actualModelClass3).isEqualTo(expectedModelClass.java)
        assertThat(actualCreationExtras3).isNull()
    }

    private class NotImplementedErrorFactory : ViewModelProvider.Factory {

        val createInvocations = mutableListOf<List<*>>()

        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            createInvocations += listOf(modelClass, extras)
            throw AbstractMethodError("abstract method `ViewModelProvider.Factory.create`")
        }

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            createInvocations += listOf(modelClass, extras)
            throw AbstractMethodError("abstract method `ViewModelProvider.Factory.create`")
        }

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            createInvocations += listOf(modelClass, null)
            throw IllegalArgumentException("Cannot create an instance of $modelClass")
        }
    }
}
