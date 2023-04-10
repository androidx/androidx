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
package androidx.camera.core.impl

import android.graphics.ImageFormat
import android.util.Range
import android.util.Size
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test

class AttachedSurfaceInfoTest {
    private var mAttachedSurfaceInfo: AttachedSurfaceInfo? = null
    private val mSurfaceConfig = SurfaceConfig.create(
        SurfaceConfig.ConfigType.JPEG,
        SurfaceConfig.ConfigSize.PREVIEW
    )
    private val mImageFormat = ImageFormat.JPEG
    private val mSize = Size(1920, 1080)
    private val mTargetFramerate = Range(10, 20)
    @Before
    fun setup() {
        mAttachedSurfaceInfo = AttachedSurfaceInfo.create(
            mSurfaceConfig, mImageFormat, mSize,
            mTargetFramerate
        )
    }

    @Test
    fun canGetSurfaceConfig() {
        Truth.assertThat(mAttachedSurfaceInfo!!.surfaceConfig).isEqualTo(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.PREVIEW
            )
        )
    }

    @Test
    fun canGetImageFormat() {
        Truth.assertThat(mAttachedSurfaceInfo!!.imageFormat).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun canGetSize() {
        Truth.assertThat(mAttachedSurfaceInfo!!.size).isEqualTo(mSize)
    }

    @Test
    fun canGetTargetFrameRate() {
        Truth.assertThat(mAttachedSurfaceInfo!!.targetFrameRate).isEqualTo(mTargetFramerate)
    }

    @Test
    fun nullGetTargetFrameRateReturnsNull() {
        val attachedSurfaceInfo2 = AttachedSurfaceInfo.create(
            mSurfaceConfig,
            mImageFormat, mSize, null
        )
        Truth.assertThat(attachedSurfaceInfo2.targetFrameRate).isNull()
    }
}