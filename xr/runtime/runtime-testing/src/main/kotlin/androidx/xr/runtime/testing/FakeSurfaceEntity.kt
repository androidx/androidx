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

package androidx.xr.runtime.testing

import android.graphics.ImageFormat
import android.media.ImageReader
import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.PerceivedResolutionResult
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.internal.SurfaceEntity
import androidx.xr.runtime.internal.TextureResource

// TODO: b/405218432 - Implement this correctly instead of stubbing it out.
/** Test-only implementation of [SurfaceEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSurfaceEntity : SurfaceEntity, FakeEntity() {
    override var stereoMode: Int = 0

    override var canvasShape: SurfaceEntity.CanvasShape = SurfaceEntity.CanvasShape.Quad(1f, 1f)

    override val dimensions: Dimensions = Dimensions(0.0f, 0.0f, 0.0f)

    override val surface: Surface =
        ImageReader.newInstance(1, 1, ImageFormat.YUV_420_888, 1).surface

    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {}

    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {}

    override var edgeFeather: SurfaceEntity.EdgeFeather = SurfaceEntity.EdgeFeather.SolidEdge()

    override fun getPerceivedResolution(): PerceivedResolutionResult {
        return PerceivedResolutionResult.Success(PixelDimensions(0, 0))
    }

    override val contentColorMetadataSet: Boolean = false

    override var colorSpace: Int = 0

    override var colorTransfer: Int = 0

    override var colorRange: Int = 0

    override var maxCLL: Int = 0

    override fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxCLL: Int,
    ) {}

    override fun resetContentColorMetadata() {}
}
