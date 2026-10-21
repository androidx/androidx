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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteStrokeJoin
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.test.Test
import kotlin.test.assertEquals

class DrawHelpersTest {

    @Test
    fun remoteGraphicsConversions_toInt() {
        assertEquals(PaintBundle.BLEND_MODE_SRC_OVER, RemoteBlendMode.SrcOver.value)
        assertEquals(0, RemoteTileMode.Clamp.value)
        assertEquals(1, RemoteTileMode.Repeated.value)
        assertEquals(2, RemoteTileMode.Mirror.value)
        assertEquals(3, RemoteTileMode.Decal.value)
        assertEquals(0, RemotePathFillType.NonZero.value)
        assertEquals(1, RemotePathFillType.EvenOdd.value)
        assertEquals(0, RemoteStrokeCap.Butt.value)
        assertEquals(1, RemoteStrokeCap.Round.value)
        assertEquals(2, RemoteStrokeCap.Square.value)
        assertEquals(0, RemoteStrokeJoin.Miter.value)
        assertEquals(1, RemoteStrokeJoin.Round.value)
        assertEquals(2, RemoteStrokeJoin.Bevel.value)
    }

    @Test
    fun composeToRemote_roundTrip() {
        assertEquals(RemoteBlendMode.SrcOver, BlendMode.SrcOver.toRemoteBlendMode())
        assertEquals(BlendMode.SrcOver, RemoteBlendMode.SrcOver.toComposeBlendMode())
        assertEquals(RemoteBlendMode.Clear, BlendMode.Clear.toRemoteBlendMode())
        assertEquals(BlendMode.Clear, RemoteBlendMode.Clear.toComposeBlendMode())

        assertEquals(RemoteTileMode.Mirror, TileMode.Mirror.toRemoteTileMode())
        assertEquals(RemotePathFillType.NonZero, PathFillType.NonZero.toRemotePathFillType())
        assertEquals(RemotePathFillType.EvenOdd, PathFillType.EvenOdd.toRemotePathFillType())
        assertEquals(RemoteStrokeCap.Butt, StrokeCap.Butt.toRemoteStrokeCap())
        assertEquals(RemoteStrokeCap.Round, StrokeCap.Round.toRemoteStrokeCap())
        assertEquals(RemoteStrokeCap.Square, StrokeCap.Square.toRemoteStrokeCap())
        assertEquals(RemoteStrokeJoin.Miter, StrokeJoin.Miter.toRemoteStrokeJoin())
        assertEquals(RemoteStrokeJoin.Round, StrokeJoin.Round.toRemoteStrokeJoin())
        assertEquals(RemoteStrokeJoin.Bevel, StrokeJoin.Bevel.toRemoteStrokeJoin())

        assertEquals(RemoteFontStyle.Italic, FontStyle.Italic.toRemoteFontStyle())
        assertEquals(RemoteFontWeight.Bold, FontWeight.Bold.toRemoteFontWeight())
    }

    @Test
    fun composeGraphicsConversions_toInt() {
        assertEquals(PaintBundle.BLEND_MODE_SRC_OVER, BlendMode.SrcOver.toInt())
        assertEquals(PaintBundle.STYLE_FILL, PaintingStyle.Fill.toInt())
        assertEquals(PaintBundle.STYLE_STROKE, PaintingStyle.Stroke.toInt())
        assertEquals(0, TileMode.Clamp.toTileModeInt())
        assertEquals(1, TileMode.Repeated.toTileModeInt())
        assertEquals(2, TileMode.Mirror.toTileModeInt())
        assertEquals(3, TileMode.Decal.toTileModeInt())
        assertEquals(ImageScaling.SCALE_CROP, ContentScale.Crop.toImageScalingInt())
        assertEquals(TextLayout.TEXT_ALIGN_CENTER, TextAlign.Center.encode())
        assertEquals(TextLayout.OVERFLOW_ELLIPSIS, TextOverflow.Ellipsis.encode())
        assertEquals(1, FontStyle.Italic.encode())
        assertEquals(2, LineBreak.Heading.encode())
        assertEquals(2, Hyphens.Auto.encode())
    }
}
