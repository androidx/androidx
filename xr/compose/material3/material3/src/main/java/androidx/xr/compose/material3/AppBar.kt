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

package androidx.xr.compose.material3

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.DefaultSingleRowTopAppBarOverride
import androidx.compose.material3.DefaultTwoRowsTopAppBarOverride
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.SingleRowTopAppBarOverride
import androidx.compose.material3.SingleRowTopAppBarOverrideScope
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TwoRowsTopAppBarOverride
import androidx.compose.material3.TwoRowsTopAppBarOverrideScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.XrSingleRowTopAppBarOverride.SingleRowTopAppBar
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape

/** [SingleRowTopAppBarOverride] that uses the XR-specific [SingleRowTopAppBar]. */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class, ExperimentalMaterial3Api::class)
internal object XrSingleRowTopAppBarOverride : SingleRowTopAppBarOverride {
    @Composable
    override fun SingleRowTopAppBarOverrideScope.SingleRowTopAppBar() {
        HorizontalOrbiter(LocalSingleRowTopAppBarOrbiterProperties.current) {
            // TODO(kmost): Expose as XR components that do not require the use of overrides.
            //  Currently, this is not possible, as delegating to the default override impl requires
            //  a SingleRowTopAppBarOverrideScope instance (which has an internal constructor).
            with(DefaultSingleRowTopAppBarOverride) { SingleRowTopAppBar() }
        }
    }
}

/**
 * The default [HorizontalOrbiterProperties] used by XR [TopAppBar] if none is specified in
 * [LocalSingleRowTopAppBarOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultSingleRowTopAppBarOrbiterProperties: HorizontalOrbiterProperties =
    HorizontalOrbiterProperties(
        position = ContentEdge.Horizontal.Top,
        offset = XrSingleRowTopAppBarTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterHorizontally,
        shape = SpatialRoundedCornerShape(CornerSize(50)),
    )

/** The [HorizontalOrbiterProperties] used by XR [TopAppBar]. */
@ExperimentalMaterial3XrApi
public val LocalSingleRowTopAppBarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultSingleRowTopAppBarOrbiterProperties
    }

/** [TwoRowsTopAppBarOverride] that uses the XR-specific [TwoRowsTopAppBar]. */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class, ExperimentalMaterial3Api::class)
internal object XrTwoRowsTopAppBarOverride : TwoRowsTopAppBarOverride {
    @Composable
    override fun TwoRowsTopAppBarOverrideScope.TwoRowsTopAppBar() {
        HorizontalOrbiter(LocalTwoRowsTopAppBarOrbiterProperties.current) {
            // TODO(kmost): Expose as XR components that do not require the use of overrides.
            //  Currently, this is not possible, as delegating to the default override impl requires
            //  a TwoRowsTopAppBarOverrideScope instance (which has an internal constructor).
            with(DefaultTwoRowsTopAppBarOverride) { TwoRowsTopAppBar() }
        }
    }
}

/**
 * The default [HorizontalOrbiterProperties] used by XR [TopAppBar] if none is specified in
 * [LocalTwoRowsTopAppBarOrbiterProperties].
 */
@ExperimentalMaterial3XrApi
public val DefaultTwoRowsTopAppBarOrbiterProperties: HorizontalOrbiterProperties =
    HorizontalOrbiterProperties(
        position = ContentEdge.Horizontal.Top,
        offset = XrTwoRowsTopAppBarTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterHorizontally,
        shape = SpatialRoundedCornerShape(CornerSize(50)),
    )

/** The [HorizontalOrbiterProperties] used by XR [TopAppBar]. */
@ExperimentalMaterial3XrApi
public val LocalTwoRowsTopAppBarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultTwoRowsTopAppBarOrbiterProperties
    }

private object XrSingleRowTopAppBarTokens {
    /** The [OrbiterOffset] for SingleRowTopAppBar Orbiters in Full Space Mode (FSM). */
    val OrbiterOffset = 24.dp
}

private object XrTwoRowsTopAppBarTokens {
    /** The [OrbiterOffset] for TwoRowsTopAppBar Orbiters in Full Space Mode (FSM). */
    val OrbiterOffset = 24.dp
}
