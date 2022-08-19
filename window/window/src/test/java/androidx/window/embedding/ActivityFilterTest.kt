/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.embedding

import android.content.ComponentName
import androidx.window.core.ExperimentalWindowApi
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test

/**
 * The unit tests for [ActivityFilter] that will test construction.
 */
@OptIn(ExperimentalWindowApi::class)
class ActivityFilterTest {

    @Test(expected = IllegalArgumentException::class)
    fun packageNameMustNotBeEmpty() {
        val component = componentName("", "class")
        ActivityFilter(component, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameMustNotBeEmpty() {
        val component = componentName("package", "")
        ActivityFilter(component, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun packageNameCannotContainWildcard() {
        val component = componentName("fake.*.package", "class")
        ActivityFilter(component, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classNameCannotContainWildcard() {
        val component = componentName("package", "fake.*.class")
        ActivityFilter(component, null)
    }

    private fun componentName(pkg: String, cls: String?): ComponentName {
        return mock<ComponentName>().apply {
            whenever(this.packageName).thenReturn(pkg)
            whenever(this.className).thenReturn(cls)
        }
    }
}