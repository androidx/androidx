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

package androidx.pdf

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RenderParamsTest {

    @Test
    fun testParcelable() {
        val renderParams =
            RenderParams(
                renderMode = RenderParams.RENDER_MODE_FOR_PRINT,
                renderFlags =
                    RenderParams.FLAG_RENDER_TEXT_ANNOTATIONS or
                        RenderParams.FLAG_RENDER_HIGHLIGHT_ANNOTATIONS,
                renderFormContentMode = RenderParams.RENDER_FORM_CONTENT_DISABLED,
            )

        val parcel = Parcel.obtain()
        renderParams.writeToParcel(parcel, renderParams.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = RenderParams.CREATOR.createFromParcel(parcel)

        assertThat(createdFromParcel.renderMode).isEqualTo(renderParams.renderMode)
        assertThat(createdFromParcel.renderFlags).isEqualTo(renderParams.renderFlags)
        assertThat(createdFromParcel.renderFormContentMode)
            .isEqualTo(renderParams.renderFormContentMode)
    }

    @Test
    fun testParcelableWithDefaultConstructorValues() {
        val renderParams = RenderParams(renderMode = RenderParams.RENDER_MODE_FOR_DISPLAY)

        val parcel = Parcel.obtain()
        renderParams.writeToParcel(parcel, renderParams.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = RenderParams.CREATOR.createFromParcel(parcel)

        assertThat(createdFromParcel.renderMode).isEqualTo(RenderParams.RENDER_MODE_FOR_DISPLAY)
        assertThat(createdFromParcel.renderFlags).isEqualTo(RenderParams.FLAG_RENDER_NONE)
        assertThat(createdFromParcel.renderFormContentMode)
            .isEqualTo(RenderParams.RENDER_FORM_CONTENT_ENABLED)
    }
}
