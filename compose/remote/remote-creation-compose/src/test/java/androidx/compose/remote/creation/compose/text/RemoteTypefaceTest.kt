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

package androidx.compose.remote.creation.compose.text

import android.graphics.Typeface
import android.os.Build
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLegacyTypeface

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteTypefaceTest {

    @Test
    fun create_withNullFontName_returnsNamedWithDefault() {
        val remote = RemoteTypeface.create(null, RemoteTypeface.Style.Normal)
        assertThat(remote).isInstanceOf(RemoteTypeface.Named::class.java)
        remote as RemoteTypeface.Named
        assertThat(remote.name).isEqualTo("default")
        assertThat(remote.weight).isEqualTo(400)
        assertThat(remote.isItalic).isFalse()
    }

    @Test
    fun create_withFontName_returnsNamedWithFontName() {
        val remote = RemoteTypeface.create("my-custom-font", RemoteTypeface.Style.Normal)
        assertThat(remote).isInstanceOf(RemoteTypeface.Named::class.java)
        remote as RemoteTypeface.Named
        assertThat(remote.name).isEqualTo("my-custom-font")
        assertThat(remote.weight).isEqualTo(400)
        assertThat(remote.isItalic).isFalse()
    }

    @Test
    fun named_toAndroidTypeface() {
        val remote = RemoteTypeface.Named("sans-serif", 700, true)
        val typeface = remote.toAndroidTypeface()
        assertThat(typeface).isNotNull()
    }

    @Test
    fun named_equalsAndHashCode() {
        val remote1 = RemoteTypeface.Named("my-font", 700, true)
        val remote2 = RemoteTypeface.Named("my-font", 700, true)
        val remote3 = RemoteTypeface.Named("my-font", 400, true)
        val remote4 = RemoteTypeface.Named("other-font", 700, true)

        assertThat(remote1).isEqualTo(remote2)
        assertThat(remote1.hashCode()).isEqualTo(remote2.hashCode())

        assertThat(remote1).isNotEqualTo(remote3)
        assertThat(remote1).isNotEqualTo(remote4)
    }

    @Test
    fun fromAndroidTypeface_null_returnsDefault() {
        val remote = RemoteTypeface.fromAndroidTypeface(null)
        assertThat(remote).isEqualTo(RemoteTypeface.Default)
    }

    @Test
    fun fromAndroidTypeface_default_returnsDefault() {
        val remote = RemoteTypeface.fromAndroidTypeface(Typeface.DEFAULT)
        assertThat(remote).isEqualTo(RemoteTypeface.Default)
    }

    @Test
    fun fromAndroidTypeface_defaultBold_returnsDefaultBold() {
        val remote = RemoteTypeface.fromAndroidTypeface(Typeface.DEFAULT_BOLD)
        assertThat(remote).isEqualTo(RemoteTypeface.DefaultBold)
    }

    @Test
    fun fromAndroidTypeface_sansSerif_returnsSansSerif() {
        val remote = RemoteTypeface.fromAndroidTypeface(Typeface.SANS_SERIF)
        assertThat(remote).isEqualTo(RemoteTypeface.SansSerif)
    }

    @Test
    fun fromAndroidTypeface_serif_returnsSerif() {
        val remote = RemoteTypeface.fromAndroidTypeface(Typeface.SERIF)
        assertThat(remote).isEqualTo(RemoteTypeface.Serif)
    }

    @Test
    fun fromAndroidTypeface_monospace_returnsMonospace() {
        val remote = RemoteTypeface.fromAndroidTypeface(Typeface.MONOSPACE)
        assertThat(remote).isEqualTo(RemoteTypeface.Monospace)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fromAndroidTypeface_sdk34OrAbove_withoutSystemFontFamilyName_returnsDefault() {
        val tf = Typeface.create("roboto-flex", Typeface.BOLD)
        assertThat(tf.systemFontFamilyName).isNull()

        val remote = RemoteTypeface.fromAndroidTypeface(tf)
        assertThat(remote).isEqualTo(RemoteTypeface.Default)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun fromAndroidTypeface_sdkBelow34_returnsDefault() {
        val tf = Typeface.create("roboto-flex", Typeface.BOLD)
        val remote = RemoteTypeface.fromAndroidTypeface(tf)
        assertThat(remote).isEqualTo(RemoteTypeface.Default)
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
        shadows = [ShadowTypefaceWithSystemName::class],
    )
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fromAndroidTypeface_sdk34OrAbove_withSystemFontFamilyName_returnsNamed() {
        val tf = Typeface.create("serif", Typeface.BOLD)
        assertThat(tf.systemFontFamilyName).isEqualTo("my-custom-system-font")
        assertThat(tf.weight).isEqualTo(700)
        assertThat(tf.isItalic).isTrue()

        val remote = RemoteTypeface.fromAndroidTypeface(tf)
        assertThat(remote).isInstanceOf(RemoteTypeface.Named::class.java)
        remote as RemoteTypeface.Named
        assertThat(remote.name).isEqualTo("my-custom-system-font")
        assertThat(remote.weight).isEqualTo(700)
        assertThat(remote.isItalic).isTrue()
    }

    @Implements(Typeface::class)
    class ShadowTypefaceWithSystemName : ShadowLegacyTypeface() {
        @Implementation fun getSystemFontFamilyName(): String = "my-custom-system-font"

        @Implementation fun getWeight(): Int = 700

        @Implementation fun isItalic(): Boolean = true
    }
}
