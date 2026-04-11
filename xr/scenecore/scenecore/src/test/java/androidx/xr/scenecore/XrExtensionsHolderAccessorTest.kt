/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import androidx.xr.runtime.TypeHolder
import androidx.xr.runtime.XrExtensionsHolder
import androidx.xr.scenecore.runtime.extensions.XrExtensionsHolderAccessor
import androidx.xr.scenecore.testing.FakeXrExtensionsHolderProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XrExtensionsHolderAccessorTest {
    @After
    fun tearDown() {
        // Clean up the static state of the fake provider after each test to ensure isolation.
        FakeXrExtensionsHolderProvider.fakeHolder = null
        FakeXrExtensionsHolderProvider.fakeHolderLegacy = null
    }

    @Test
    fun getHolder_whenNull_returnsNull() {
        FakeXrExtensionsHolderProvider.fakeHolder = null

        assertThat(XrExtensionsHolderAccessor.holder).isNull()
    }

    @Test
    fun getHolderLegacy_whenNull_returnsNull() {
        FakeXrExtensionsHolderProvider.fakeHolderLegacy = null

        assertThat(XrExtensionsHolderAccessor.holderLegacy).isNull()
    }

    @Test
    fun getHolder_whenHasValueButLegacyNot_returnNull() {
        FakeXrExtensionsHolderProvider.fakeHolder = XrExtensionsHolder("str", String::class.java)

        assertThat(XrExtensionsHolderAccessor.holder).isNull()
    }

    @Test
    fun getHolder_whenHasValue_returnsHolder() {
        FakeXrExtensionsHolderProvider.fakeHolderLegacy =
            XrExtensionsHolder("str", String::class.java)
        FakeXrExtensionsHolderProvider.fakeHolder = XrExtensionsHolder("str", String::class.java)

        assertThat(XrExtensionsHolderAccessor.holderLegacy).isNotNull()
        assertThat(XrExtensionsHolderAccessor.holder).isNotNull()

        val holder = XrExtensionsHolderAccessor.holder!!
        val instance = TypeHolder.assertGetValue(holder, String::class.java)

        assertThat(instance).isNotNull()
        assertThat(instance).isEqualTo("str")
    }

    @Test
    fun getHolderLegacy_whenHasValue_returnsHolderLegacy() {
        FakeXrExtensionsHolderProvider.fakeHolderLegacy =
            XrExtensionsHolder("str", String::class.java)

        assertThat(XrExtensionsHolderAccessor.holderLegacy).isNotNull()

        val holderLegacy = XrExtensionsHolderAccessor.holderLegacy!!
        val instance = TypeHolder.assertGetValue(holderLegacy, String::class.java)

        assertThat(instance).isNotNull()
        assertThat(instance).isEqualTo("str")
    }
}
