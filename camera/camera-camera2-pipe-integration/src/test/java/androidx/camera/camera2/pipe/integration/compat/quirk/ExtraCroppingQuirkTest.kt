/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.os.Build
import android.util.Size
import androidx.camera.core.impl.SurfaceConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ExtraCroppingQuirkTest {
    private val quirk = ExtraCroppingQuirk()

    @Test
    fun deviceModelNotContained_returnsNull() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "non-existent")
        assert(!ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.PRIV)).isNull()
    }

    @Test
    fun deviceBrandNotSamsung_returnsNull() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "non-existent")
        assert(!ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.PRIV)).isNull()
    }

    @Test
    fun osVersionNotInRange_returnsNull() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J710MN")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 20)
        assert(!ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.PRIV)).isNull()
    }

    @Test
    fun rawConfigType_returnsNull() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J710MN")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 22)
        assert(ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.RAW)).isNull()
    }

    @Test
    fun privConfigType_returnsSize() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J710MN")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 22)
        assert(ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.PRIV))
            .isEqualTo(Size(1920, 1080))
    }

    @Test
    fun yuvConfigType_returnsSize() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J710MN")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 22)
        assert(ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.YUV))
            .isEqualTo(Size(1280, 720))
    }

    @Test
    fun jpegConfigType_returnsSize() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J710MN")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 22)
        assert(ExtraCroppingQuirk.isEnabled())
        Truth.assertThat(quirk.getVerifiedResolution(SurfaceConfig.ConfigType.JPEG))
            .isEqualTo(Size(3264, 1836))
    }
}
