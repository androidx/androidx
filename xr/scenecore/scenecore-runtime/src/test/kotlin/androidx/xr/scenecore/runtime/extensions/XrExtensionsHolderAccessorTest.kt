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

// TODO (b/502381626) - Use SceneCoreTestRule instead
@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.runtime.extensions

import android.extensions.xr.XrExtensions
import androidx.xr.scenecore.runtime.TypeHolder
import androidx.xr.scenecore.runtime.XrExtensionsHolder
import androidx.xr.scenecore.testing.FakeXrExtensionsHolderProvider
import com.android.extensions.xr.XrExtensions as XrExtensionsLegacy
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
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
        val extensionsLegacy = XrExtensionsLegacy()
        FakeXrExtensionsHolderProvider.fakeHolder =
            XrExtensionsHolder(extensionsLegacy, XrExtensionsLegacy::class.java)

        assertThat(XrExtensionsHolderAccessor.holder).isNull()
    }

    @Test
    fun getHolder_whenHasValue_returnsHolder() {
        val extensionsLegacy = XrExtensionsLegacy()
        val extensions = extensionsLegacy.underlyingObject
        FakeXrExtensionsHolderProvider.fakeHolderLegacy =
            XrExtensionsHolder(extensionsLegacy, XrExtensionsLegacy::class.java)
        FakeXrExtensionsHolderProvider.fakeHolder =
            XrExtensionsHolder(extensions, XrExtensions::class.java)

        assertThat(XrExtensionsHolderAccessor.holderLegacy).isNotNull()
        assertThat(XrExtensionsHolderAccessor.holder).isNotNull()

        val holder = XrExtensionsHolderAccessor.holder!!
        val instance = TypeHolder.assertGetValue(holder, XrExtensions::class.java)

        assertThat(instance).isNotNull()
        assertThat(instance).isEqualTo(extensions)
    }

    @Test
    fun getHolderLegacy_whenHasValue_returnsHolderLegacy() {
        val extensionsLegacy = XrExtensionsLegacy()
        FakeXrExtensionsHolderProvider.fakeHolderLegacy =
            XrExtensionsHolder(extensionsLegacy, XrExtensionsLegacy::class.java)

        assertThat(XrExtensionsHolderAccessor.holderLegacy).isNotNull()

        val holderLegacy = XrExtensionsHolderAccessor.holderLegacy!!
        val instance = TypeHolder.assertGetValue(holderLegacy, XrExtensionsLegacy::class.java)

        assertThat(instance).isNotNull()
        assertThat(instance).isEqualTo(extensionsLegacy)
    }
}
