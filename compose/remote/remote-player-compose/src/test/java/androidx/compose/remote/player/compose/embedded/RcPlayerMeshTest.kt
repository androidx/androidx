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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.DrawMesh2D
import androidx.compose.remote.core.operations.MatrixFromMesh2D
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.draw.UnknownOperationPolicy
import androidx.compose.remote.player.compose.embedded.draw.UnknownOperations
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for 2D vertex meshes in the embedded player: `AddMesh2D` (geometry), `DrawMesh2D`
 * (vertex-coloured or textured draw) and `MatrixFromMesh2D` (canvas matrix from a mesh point).
 * These run through the View player's `AndroidPaintContext`; assertions are on rendered pixels.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerMeshTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun vertexColouredQuadFillsItsBounds() {
        val bitmap = rule.renderDocument {
            val mesh = addQuad(colors = IntArray(4) { AndroidColor.RED })
            writer.drawMesh2D(mesh, DrawMesh2D.BLEND_COLORS_ONLY, DrawMesh2D.NO_IMAGE)
        }

        assertRed(bitmap, 50, 50)
        assertRed(bitmap, 25, 75)
        assertNotRed(bitmap, 10, 10)
        assertNotRed(bitmap, 90, 50)
    }

    @Test
    fun texturedQuadSamplesTheDocumentBitmap() {
        val texture = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        texture.eraseColor(AndroidColor.BLUE)
        val bitmap = rule.renderDocument {
            val image = writer.addBitmap(texture)
            val mesh = addQuad(uv = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
            writer.drawMesh2D(mesh, DrawMesh2D.BLEND_MODULATE, image)
        }

        val inside = bitmap.getPixel(50, 50)
        assertThat(AndroidColor.blue(inside)).isGreaterThan(200)
        assertThat(AndroidColor.red(inside)).isLessThan(60)
        val outside = bitmap.getPixel(10, 10)
        assertThat(AndroidColor.blue(outside) > 200 && AndroidColor.red(outside) < 60).isFalse()
    }

    @Test
    fun texturedQuadSamplesHostImageLoaderAndRedrawsWhenLoaded() {
        val hostImageId = 99
        val document = buildDocument {
            val mesh = addQuad(uv = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
            writer.drawMesh2D(mesh, DrawMesh2D.BLEND_MODULATE, hostImageId)
        }
        val asyncDrawable = mutableStateOf<Drawable?>(null)
        val loader = RcImageLoader { id ->
            if (id == hostImageId) asyncDrawable else mutableStateOf(null)
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, imageLoader = loader)
                }
            }
        }

        // Before the async image arrives, the mesh does not crash or draw blue.
        val before = rule.onRoot().captureToImage().asAndroidBitmap().getPixel(50, 50)
        assertThat(AndroidColor.blue(before) > 200 && AndroidColor.red(before) < 60).isFalse()

        val loadedTexture =
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.BLUE)
            }
        rule.runOnIdle {
            asyncDrawable.value = BitmapDrawable(Resources.getSystem(), loadedTexture)
        }

        val after = rule.onRoot().captureToImage().asAndroidBitmap().getPixel(50, 50)
        assertThat(AndroidColor.blue(after)).isGreaterThan(200)
        assertThat(AndroidColor.red(after)).isLessThan(60)
    }

    @Test
    fun matrixFromMeshMovesOriginToMeshPoint() {
        // The quad spans (20, 20)-(80, 80), so its centre (u = v = 0.5) is (50, 50). A marker
        // drawn at the origin after the matrix lands there.
        val bitmap = rule.renderDocument {
            val mesh = addQuad()
            writer.save()
            writer.matrixFromMesh2D(mesh, 0.5f, 0.5f, MatrixFromMesh2D.FLAG_ORIGIN)
            painter.setColor(AndroidColor.RED).commit()
            writer.drawCircle(0f, 0f, 5f)
            writer.restore()
        }

        assertRed(bitmap, 50, 50)
        assertNotRed(bitmap, 2, 2)
    }

    @Test
    fun matrixFromMeshIsScopedBySaveRestore() {
        val bitmap = rule.renderDocument {
            val mesh = addQuad()
            writer.save()
            writer.matrixFromMesh2D(mesh, 0.5f, 0.5f, MatrixFromMesh2D.FLAG_ORIGIN)
            writer.restore()
            painter.setColor(AndroidColor.RED).commit()
            writer.drawCircle(10f, 10f, 5f)
        }

        assertRed(bitmap, 10, 10)
        assertNotRed(bitmap, 60, 60)
    }

    @Test
    fun explicitFeatureSetWithoutMeshFeatureSkipsMeshes() {
        UnknownOperations.policy = UnknownOperationPolicy.Log
        val bitmap = rule.renderDocument(redQuadDocument(), RcPlayerFeatureSet.Empty)

        assertNotRed(bitmap, 50, 50)
    }

    @Test
    fun explicitFeatureSetWithMeshFeatureDrawsMeshes() {
        val bitmap = rule.renderDocument(redQuadDocument(), RcPlayerFeatureSet(MeshFeature))

        assertRed(bitmap, 50, 50)
    }

    @Test
    fun defaultFeaturesIncludeMeshesForTheExperimentalProfile() {
        val experimental = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL

        assertThat(RcPlayerFeatures.forProfiles(experimental).features).contains(MeshFeature)
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_ANDROIDX).features)
            .doesNotContain(MeshFeature)
        val unsupported = RcPlayerFeatureSet(MeshFeature).unsupportedOpcodes(experimental)
        assertThat(Operations.DRAW_MESH_2D in unsupported).isFalse()
    }

    private fun redQuadDocument(): CoreDocument = buildDocument {
        val mesh = addQuad(colors = IntArray(4) { AndroidColor.RED })
        writer.drawMesh2D(mesh, DrawMesh2D.BLEND_COLORS_ONLY, DrawMesh2D.NO_IMAGE)
    }

    /** A 2x2 grid quad spanning (20, 20)-(80, 80), as two triangles. */
    private fun RemoteComposeContextAndroid.addQuad(
        uv: FloatArray? = null,
        colors: IntArray? = null,
    ): Int =
        writer.addMesh2DValues(
            intArrayOf(0, 1, 2, 1, 3, 2),
            floatArrayOf(20f, 20f, 80f, 20f, 20f, 80f, 80f, 80f),
            uv,
            colors,
            false,
            Mesh2DGenerator.LAYOUT_GRID,
            2,
            2,
        )
}
