/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.xr.adaptive

import androidx.compose.material3.xr.XrStubs
import androidx.compose.material3.xr.spatial.HorizontalOrbiterProperties
import androidx.compose.material3.xr.spatial.VerticalOrbiterProperties
import androidx.compose.material3.xr.stub.XrHorizontalOrbiterStub
import androidx.compose.material3.xr.stub.XrVerticalOrbiterStub
import androidx.compose.runtime.Composable
import androidx.xr.compose.platform.LocalSpatialCapabilities

internal object XrStubsImpl : XrStubs {
    @Composable
    override fun horizontalOrbiterStub(): XrHorizontalOrbiterStub? =
        XrHorizontalOrbiterStubImpl.takeIf { LocalSpatialCapabilities.current.isSpatialUiEnabled }

    @Composable
    override fun verticalOrbiterStub(): XrVerticalOrbiterStub? =
        XrVerticalOrbiterStubImpl.takeIf { LocalSpatialCapabilities.current.isSpatialUiEnabled }
}

private object XrHorizontalOrbiterStubImpl : XrHorizontalOrbiterStub {
    @Composable
    override fun Orbiter(
        properties: HorizontalOrbiterProperties,
        content: @Composable (() -> Unit),
    ) {
        androidx.xr.compose.spatial.Orbiter(
            position = properties.position.toXrPositionHorizontal(),
            offset = properties.offset,
            offsetType = properties.offsetType.toXrOrbiterOffsetType(),
            alignment = properties.alignment,
            shape = properties.shape.toXrSpatialShape(),
            elevation = properties.elevation,
            shouldRenderInNonSpatial = false,
            content = content,
        )
    }
}

private object XrVerticalOrbiterStubImpl : XrVerticalOrbiterStub {
    @Composable
    override fun Orbiter(properties: VerticalOrbiterProperties, content: @Composable () -> Unit) {
        androidx.xr.compose.spatial.Orbiter(
            position = properties.position.toXrPositionVertical(),
            offset = properties.offset,
            offsetType = properties.offsetType.toXrOrbiterOffsetType(),
            alignment = properties.alignment,
            shape = properties.shape.toXrSpatialShape(),
            elevation = properties.elevation,
            shouldRenderInNonSpatial = false,
            content = content,
        )
    }
}
