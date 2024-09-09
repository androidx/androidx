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

package androidx.camera.core.internal.compat.workaround

import android.graphics.BitmapFactory
import android.os.Build
import androidx.camera.core.internal.compat.quirk.DeviceQuirks
import androidx.camera.testing.impl.TestImageUtil
import androidx.camera.testing.impl.TestImageUtil.createA24ProblematicJpegByteArray
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

private const val WIDTH = 640
private const val HEIGHT = 480

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.O)
class JpegMetadataCorrectorTest {

    @Test
    fun needCorrectJpegMetadataOnSamsungA24() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "a24")
        assertThat(JpegMetadataCorrector(DeviceQuirks.getAll()).needCorrectJpegMetadata()).isTrue()
    }

    @Test
    fun needCorrectJpegMetadataOnSamsungS10e() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "beyond0")
        assertThat(JpegMetadataCorrector(DeviceQuirks.getAll()).needCorrectJpegMetadata()).isTrue()
    }

    @Test
    fun needCorrectJpegMetadataOnSamsungS10Plus() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "beyond2")
        assertThat(JpegMetadataCorrector(DeviceQuirks.getAll()).needCorrectJpegMetadata()).isTrue()
    }

    @Test
    fun doesNotNeedCorrectJpegMetadataOnSamsungA23() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "a23")
        assertThat(JpegMetadataCorrector(DeviceQuirks.getAll()).needCorrectJpegMetadata()).isFalse()
    }

    @Test
    fun canCorrectHeaderData_whenJpegMetadataIsIncorrect() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "a24")

        val brokenJpegByteArray = createA24ProblematicJpegByteArray(WIDTH, HEIGHT)
        assertThrows<RuntimeException> {
            BitmapFactory.decodeByteArray(brokenJpegByteArray, 0, brokenJpegByteArray.size)
        }

        val fakeImageProxy =
            TestImageUtil.createJpegFakeImageProxy(
                FakeImageInfo(),
                brokenJpegByteArray,
                WIDTH,
                HEIGHT
            )
        val correctedJpegByteArray =
            JpegMetadataCorrector(DeviceQuirks.getAll()).jpegImageToJpegByteArray(fakeImageProxy)
        assertThat(
                BitmapFactory.decodeByteArray(
                    correctedJpegByteArray,
                    0,
                    correctedJpegByteArray.size
                )
            )
            .isNotNull()
    }

    @Test
    fun canKeepData_whenJpegMetadataIsCorrect() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "a24")

        val jpegByteArray = TestImageUtil.createJpegBytes(WIDTH, HEIGHT)
        val fakeImageProxy =
            TestImageUtil.createJpegFakeImageProxy(FakeImageInfo(), jpegByteArray, WIDTH, HEIGHT)
        val resultJpegByteArray =
            JpegMetadataCorrector(DeviceQuirks.getAll()).jpegImageToJpegByteArray(fakeImageProxy)
        assertThat(BitmapFactory.decodeByteArray(resultJpegByteArray, 0, resultJpegByteArray.size))
            .isNotNull()
    }
}
