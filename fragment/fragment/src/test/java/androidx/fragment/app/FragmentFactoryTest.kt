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

package androidx.fragment.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FragmentFactoryTest {
    private val factory = FragmentFactory()

    @Test
    fun `Fragment classes are cached`() {
        val classLoader = CountingClassLoader()
        factory.instantiate(classLoader, TestFragment::class.java.name)
        factory.instantiate(classLoader, TestFragment::class.java.name)
        assertEquals(1, classLoader.loadCount)
    }

    @Test
    fun `Cached fragment classes are not shared across different class loaders`() {
        val firstClassLoader = CountingClassLoader()
        val secondClassLoader = CountingClassLoader()
        factory.instantiate(firstClassLoader, TestFragment::class.java.name)
        factory.instantiate(secondClassLoader, TestFragment::class.java.name)
        assertEquals(1, firstClassLoader.loadCount)
        assertEquals(1, secondClassLoader.loadCount)
    }

    class TestFragment : Fragment()

    class CountingClassLoader : ClassLoader() {
        var loadCount = 0

        override fun loadClass(name: String?): Class<*> {
            loadCount++
            return super.loadClass(name)
        }
    }
}
