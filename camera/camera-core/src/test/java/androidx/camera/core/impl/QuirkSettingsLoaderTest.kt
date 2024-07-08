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

package androidx.camera.core.impl

import android.content.Context
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.camera.core.impl.QuirkSettingsLoader.KEY_DEFAULT_QUIRK_ENABLED
import androidx.camera.core.impl.QuirkSettingsLoader.KEY_QUIRK_FORCE_DISABLED
import androidx.camera.core.impl.QuirkSettingsLoader.KEY_QUIRK_FORCE_ENABLED
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QuirkSettingsLoaderTest {

    companion object {
        private val DEFAULT_SETTINGS = QuirkSettings.withDefaultBehavior()
    }

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val quirkSettingsLoader = QuirkSettingsLoader()
    private val quirk1 = Quirk1::class.java
    private val quirk2 = Quirk2::class.java
    private val quirk3 = Quirk3::class.java
    private val quirk4 = Quirk4::class.java

    @Test
    fun testLoad_withoutMetadataHolderService() {
        val settings = quirkSettingsLoader.apply(appContext)
        assertThat(settings).isNull()
    }

    @Test
    fun testLoad_withEmptyMetadata() {
        val context = setMetadataHolderService(extraMetadata = Bundle())

        val settings = quirkSettingsLoader.apply(context)
        assertThat(settings).isEqualTo(DEFAULT_SETTINGS)
    }

    @Test
    fun testLoad_withUnsupportedKey() {
        // simulate a case when metadata bundle only contains unsupported key
        val metadata = Bundle().apply { putString("InvalidKey", "InvalidValue") }
        val context = setMetadataHolderService(extraMetadata = metadata)

        val settings = quirkSettingsLoader.apply(context)
        assertThat(settings).isEqualTo(DEFAULT_SETTINGS)
    }

    @Test
    fun testLoad_withValidMetadata() {
        // Arrange.
        val context =
            setMetadataHolderService(
                defaultQuirkEnabled = false,
                forceEnabledQuirks = setOf(quirk1.name, quirk2.name),
                forceDisabledQuirks = setOf(quirk3.name, quirk4.name)
            )

        // Act.
        val settings = quirkSettingsLoader.apply(context)

        // Assert - Verify the loaded settings.
        assertThat(settings).isNotNull()
        assertThat(settings!!.isEnabledWhenDeviceHasQuirk).isFalse()
        assertThat(settings.forceEnabledQuirks).containsExactlyElementsIn(setOf(quirk1, quirk2))
        assertThat(settings.forceDisabledQuirks).containsExactlyElementsIn(setOf(quirk3, quirk4))
    }

    @Test
    fun testLoad_withInvalidClassName() {
        // Arrange.
        val context =
            setMetadataHolderService(
                defaultQuirkEnabled = true,
                forceEnabledQuirks = setOf(quirk1.name, quirk2.name, "invalid.class.name"),
                forceDisabledQuirks = setOf(String::class.java.name /*Not a Quirk*/)
            )

        // Act.
        val settings = quirkSettingsLoader.apply(context)

        // Assert - Verify the loaded settings.
        assertThat(settings).isNotNull()
        assertThat(settings!!.isEnabledWhenDeviceHasQuirk).isTrue()
        assertThat(settings.forceEnabledQuirks).containsExactlyElementsIn(setOf(quirk1, quirk2))
        assertThat(settings.forceDisabledQuirks).isEmpty()
    }

    // Helper function to set metadata for the MetadataHolderService
    private fun setMetadataHolderService(
        defaultQuirkEnabled: Boolean? = null,
        forceEnabledQuirks: Set<String>? = null,
        forceDisabledQuirks: Set<String>? = null,
        extraMetadata: Bundle? = null,
    ): Context {
        // Didn't find a way to customize resources in Robolectric. Use Mockito as a workaround.
        val context = spy(appContext)
        val mockResources by lazy {
            mock(Resources::class.java).also { `when`(context.resources).thenReturn(it) }
        }

        // Create metadata.
        val metadata =
            Bundle().apply {
                // Prioritize extraMetadata to avoid overwriting subsequent values.
                extraMetadata?.let { putAll(it) }
                defaultQuirkEnabled?.let { putBoolean(KEY_DEFAULT_QUIRK_ENABLED, it) }
                forceEnabledQuirks?.let {
                    putInt(KEY_QUIRK_FORCE_ENABLED, 123)
                    `when`(mockResources.getStringArray(123)).thenReturn(it.toTypedArray())
                }
                forceDisabledQuirks?.let {
                    putInt(KEY_QUIRK_FORCE_DISABLED, 456)
                    `when`(mockResources.getStringArray(456)).thenReturn(it.toTypedArray())
                }
            }

        // Register MetadataService.
        val packageManager = Shadows.shadowOf(context.packageManager)
        packageManager.addOrUpdateService(
            ServiceInfo().apply {
                name = QuirkSettingsLoader.MetadataHolderService::class.java.name
                packageName = context.packageName
                enabled = false
                exported = false
                metaData = metadata
            }
        )
        return context
    }

    private class Quirk1 : Quirk

    private class Quirk2 : Quirk

    private class Quirk3 : Quirk

    private class Quirk4 : Quirk
}
