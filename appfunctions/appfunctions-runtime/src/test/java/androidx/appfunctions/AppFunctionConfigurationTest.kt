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

package androidx.appfunctions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionConfigurationTest {
    @Test
    fun testEmpty() {
        val configuration = AppFunctionConfiguration.Builder().build()

        assertThat(configuration.enclosingClassFactories).isEmpty()
    }

    @Test
    fun testUniqueFactories() {
        val configuration =
            AppFunctionConfiguration.Builder()
                .addEnclosingClassFactory(TestAppFunctionClass1::class.java) {
                    TestAppFunctionClass1()
                }
                .addEnclosingClassFactory(TestAppFunctionClass2::class.java) {
                    TestAppFunctionClass2()
                }
                .build()

        assertThat(configuration.enclosingClassFactories).hasSize(2)
        assertThat(configuration.enclosingClassFactories[TestAppFunctionClass1::class.java])
            .isNotNull()
        assertThat(configuration.enclosingClassFactories[TestAppFunctionClass2::class.java])
            .isNotNull()
        assertThat(
                configuration.enclosingClassFactories[TestAppFunctionClass1::class.java]!!.invoke()
            )
            .isInstanceOf(TestAppFunctionClass1::class.java)
        assertThat(
                configuration.enclosingClassFactories[TestAppFunctionClass2::class.java]!!.invoke()
            )
            .isInstanceOf(TestAppFunctionClass2::class.java)
    }

    @Test
    fun testDuplicatedFactories() {
        val configuration =
            AppFunctionConfiguration.Builder()
                .addEnclosingClassFactory(TestAppFunctionClass1::class.java) {
                    TestAppFunctionClass1()
                }
                .addEnclosingClassFactory(TestAppFunctionClass1::class.java) {
                    TestAppFunctionClass1()
                }
                .build()

        assertThat(configuration.enclosingClassFactories).hasSize(1)
        assertThat(configuration.enclosingClassFactories[TestAppFunctionClass1::class.java])
            .isNotNull()
        assertThat(
                configuration.enclosingClassFactories[TestAppFunctionClass1::class.java]!!.invoke()
            )
            .isInstanceOf(TestAppFunctionClass1::class.java)
    }

    internal class TestAppFunctionClass1

    internal class TestAppFunctionClass2
}
