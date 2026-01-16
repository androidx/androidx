/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.fail

class AndroidNavigatorProviderTest {

    @Test
    fun addWithExplicitNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator("name", navigator)

        assertThat(provider.getNavigator<EmptyNavigator>("name")).isEqualTo(navigator)
        try {
            provider.getNavigator(EmptyNavigator::class.java)
            fail("getNavigator(Class) with an invalid name should cause an IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun addWithExplicitNameGetWithMissingAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        try {
            provider.getNavigator(NoNameNavigator::class.java)
            fail(
                "getNavigator(Class) with no @Navigator.Name should cause an " +
                    "IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithAnnotationNameGetWithAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertThat(provider.getNavigator(EmptyNavigator::class.java)).isEqualTo(navigator)
    }
}
