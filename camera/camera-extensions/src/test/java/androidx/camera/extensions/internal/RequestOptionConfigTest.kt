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

package androidx.camera.extensions.internal

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.core.impl.Config.Option
import androidx.camera.core.impl.MutableOptionsBundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = arrayOf("androidx.camera.extensions.internal")
)
class RequestOptionConfigTest {
    @Test
    fun canBuildWithCaptureRequestOptions() {
        val config =
            RequestOptionConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO
                )
                .setCaptureRequestOption(CaptureRequest.JPEG_ORIENTATION, 90)
                .build()

        assertThat(config.listOptions().size).isEqualTo(2)
        assertThat(
                config.retrieveOption(
                    RequestOptionConfig.createOptionFromKey(CaptureRequest.CONTROL_AF_MODE)
                )
            )
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO)
        assertThat(
                config.retrieveOption(
                    RequestOptionConfig.createOptionFromKey(CaptureRequest.JPEG_ORIENTATION)
                )
            )
            .isEqualTo(90)
    }

    @Test
    fun canBuildFromConfig() {
        val mutableOptionConfig = MutableOptionsBundle.create()
        mutableOptionConfig.insertOption(
            Option.create("NonCaptureOption", String::class.java, null),
            "value1"
        )
        mutableOptionConfig.insertOption(
            Option.create("NonCaptureOption2", Integer::class.java, null),
            99
        )
        mutableOptionConfig.insertOption(
            RequestOptionConfig.createOptionFromKey(CaptureRequest.CONTROL_AF_MODE),
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )

        val requestOptionConfig =
            RequestOptionConfig.Builder.from(mutableOptionConfig)
                .setCaptureRequestOption(CaptureRequest.JPEG_ORIENTATION, 180)
                .build()
        assertThat(requestOptionConfig.listOptions().size).isEqualTo(2)
        assertThat(
                requestOptionConfig.retrieveOption(
                    RequestOptionConfig.createOptionFromKey(CaptureRequest.CONTROL_AF_MODE)
                )
            )
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        assertThat(
                requestOptionConfig.retrieveOption(
                    RequestOptionConfig.createOptionFromKey(CaptureRequest.JPEG_ORIENTATION)
                )
            )
            .isEqualTo(180)
    }
}
