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

package androidx.xr.glimmer.remote

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createRemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.painterRemoteColor
import androidx.compose.remote.creation.compose.painter.painterRemoteImageBitmap
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.asRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // captureToImage
class RemoteIconTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val displayInfo = createRemoteCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun localRemoteIconSize_defaultValue() = runTest {
        var defaultSize: RemoteDp? = null
        captureCoreDocument { defaultSize = LocalRemoteIconSize.current }

        assertThat(defaultSize?.value?.constantValue).isEqualTo(40f)
    }

    @Test
    fun localRemoteIconSize_themeProvidesMediumIconSize() = runTest {
        var localIconSize: RemoteDp? = null
        var mediumIconSize: RemoteDp? = null
        captureCoreDocument {
            RemoteGlimmerTheme {
                localIconSize = LocalRemoteIconSize.current
                mediumIconSize = RemoteGlimmerTheme.iconSizes.medium
            }
        }

        assertThat(localIconSize?.value).isEqualTo(mediumIconSize?.value)
    }

    @Test
    fun vector_defaultSize() = runTest {
        val vector = createTestVector(width = 35.rdp, height = 83.rdp)
        val doc = captureCoreDocument {
            RemoteIcon(imageVector = vector, contentDescription = null)
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.widthModifier?.value).isEqualTo(40f)
        assertThat(iconBox.heightModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.heightModifier?.value).isEqualTo(40f)
    }

    @Test
    fun vector_localRemoteIconSize() = runTest {
        val vector = createTestVector(width = 35.rdp, height = 83.rdp)
        val doc = captureCoreDocument {
            CompositionLocalProvider(LocalRemoteIconSize provides 50.rdp) {
                RemoteIcon(imageVector = vector, contentDescription = null)
            }
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(50f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(50f)
    }

    @Test
    fun vector_explicitModifierSize_overridesDefault() = runTest {
        val vector = createTestVector(width = 35.rdp, height = 83.rdp)
        val doc = captureCoreDocument {
            RemoteIcon(
                imageVector = vector,
                contentDescription = null,
                modifier = RemoteModifier.size(32.rdp),
            )
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(32f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(32f)
    }

    @Test
    fun bitmap_defaultSize() = runTest {
        val doc = captureCoreDocument {
            val bitmap = createTestBitmap(width = 35, height = 83)
            RemoteIcon(bitmap = bitmap, contentDescription = null)
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.widthModifier?.value).isEqualTo(40f)
        assertThat(iconBox.heightModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.heightModifier?.value).isEqualTo(40f)
    }

    @Test
    fun bitmap_localRemoteIconSize() = runTest {
        val doc = captureCoreDocument {
            CompositionLocalProvider(LocalRemoteIconSize provides 50.rdp) {
                val bitmap = createTestBitmap(width = 35, height = 83)
                RemoteIcon(bitmap = bitmap, contentDescription = null)
            }
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(50f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(50f)
    }

    @Test
    fun bitmap_explicitModifierSize_overridesDefault() = runTest {
        val doc = captureCoreDocument {
            val bitmap = createTestBitmap(width = 35, height = 83)
            RemoteIcon(
                bitmap = bitmap,
                contentDescription = null,
                modifier = RemoteModifier.size(48.rdp),
            )
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(48f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(48f)
    }

    @Test
    fun painter_noIntrinsicSize_defaultSize() = runTest {
        val doc = captureCoreDocument {
            RemoteIcon(painter = painterRemoteColor(Color.Red.rc), contentDescription = null)
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.widthModifier?.value).isEqualTo(40f)
        assertThat(iconBox.heightModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.heightModifier?.value).isEqualTo(40f)
    }

    @Test
    fun painter_noIntrinsicSize_localRemoteIconSize() = runTest {
        val doc = captureCoreDocument {
            CompositionLocalProvider(LocalRemoteIconSize provides 50.rdp) {
                RemoteIcon(painter = painterRemoteColor(Color.Red.rc), contentDescription = null)
            }
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(50f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(50f)
    }

    @Test
    fun painter_noIntrinsicSize_explicitModifierSize_overridesDefault() = runTest {
        val doc = captureCoreDocument {
            RemoteIcon(
                painter = painterRemoteColor(Color.Red.rc),
                contentDescription = null,
                modifier = RemoteModifier.size(30.rdp),
            )
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(30f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(30f)
    }

    @Test
    fun painter_intrinsicSize_defaultSize() = runTest {
        val doc = captureCoreDocument {
            val bitmap = createTestBitmap(width = 35, height = 83)
            RemoteIcon(painter = painterRemoteImageBitmap(bitmap), contentDescription = null)
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.widthModifier?.value).isEqualTo(40f)
        assertThat(iconBox.heightModifier?.type).isEqualTo(DimensionModifierOperation.Type.EXACT_DP)
        assertThat(iconBox.heightModifier?.value).isEqualTo(40f)
    }

    @Test
    fun painter_intrinsicSize_localRemoteIconSize() = runTest {
        val doc = captureCoreDocument {
            CompositionLocalProvider(LocalRemoteIconSize provides 50.rdp) {
                val bitmap = createTestBitmap(width = 35, height = 83)
                RemoteIcon(painter = painterRemoteImageBitmap(bitmap), contentDescription = null)
            }
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(50f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(50f)
    }

    @Test
    fun painter_intrinsicSize_explicitModifierSize_overridesDefault() = runTest {
        val doc = captureCoreDocument {
            val bitmap = createTestBitmap(width = 35, height = 83)
            RemoteIcon(
                painter = painterRemoteImageBitmap(bitmap),
                contentDescription = null,
                modifier = RemoteModifier.size(48.rdp),
            )
        }

        val iconBox = doc.findFirstIconBox()
        assertThat(iconBox.widthModifier?.value).isEqualTo(48f)
        assertThat(iconBox.heightModifier?.value).isEqualTo(48f)
    }

    @Test
    fun vector_semantics_whenContentDescriptionProvided() = runTest {
        val vector = createTestVector()
        val doc = captureCoreDocument {
            RemoteIcon(imageVector = vector, contentDescription = "Favorite".rs)
        }

        val semantics = doc.findAllOperations<CoreSemantics>()
        assertThat(semantics).hasSize(1)
        assertThat(semantics.single().role).isEqualTo(AccessibleComponent.Role.IMAGE)
        assertThat(semantics.single().mContentDescriptionId).isNotEqualTo(0)
    }

    @Test
    fun vector_noSemantics_whenContentDescriptionNull() = runTest {
        val vector = createTestVector()
        val doc = captureCoreDocument {
            RemoteIcon(imageVector = vector, contentDescription = null)
        }

        val semantics = doc.findAllOperations<CoreSemantics>()
        assertThat(semantics).isEmpty()
    }

    @Test
    fun bitmap_semantics_whenContentDescriptionProvided() = runTest {
        val doc = captureCoreDocument {
            val bitmap = createTestBitmap()
            RemoteIcon(bitmap = bitmap, contentDescription = "Avatar".rs)
        }

        val semantics = doc.findAllOperations<CoreSemantics>()
        assertThat(semantics).hasSize(1)
        assertThat(semantics.single().role).isEqualTo(AccessibleComponent.Role.IMAGE)
        assertThat(semantics.single().mContentDescriptionId).isNotEqualTo(0)
    }

    @Test
    fun bitmap_noSemantics_whenContentDescriptionNull() = runTest {
        val doc = captureCoreDocument {
            val bitmap = createTestBitmap()
            RemoteIcon(bitmap = bitmap, contentDescription = null)
        }

        val semantics = doc.findAllOperations<CoreSemantics>()
        assertThat(semantics).isEmpty()
    }

    @Test
    fun painter_semantics_whenContentDescriptionProvided() = runTest {
        val doc = captureCoreDocument {
            RemoteIcon(
                painter = painterRemoteColor(Color.Red.rc),
                contentDescription = "Indicator".rs,
            )
        }

        val semantics = doc.findAllOperations<CoreSemantics>()
        assertThat(semantics).hasSize(1)
        assertThat(semantics.single().role).isEqualTo(AccessibleComponent.Role.IMAGE)
        assertThat(semantics.single().mContentDescriptionId).isNotEqualTo(0)
    }

    @Test
    fun painter_noSemantics_whenContentDescriptionNull() = runTest {
        val doc = captureCoreDocument {
            RemoteIcon(painter = painterRemoteColor(Color.Red.rc), contentDescription = null)
        }

        val semantics = doc.findAllOperations<CoreSemantics>()
        assertThat(semantics).isEmpty()
    }

    @Test
    fun vector_defaultTint_appliesDefaultContentColor() {
        // TODO: b/560206798 - Test content color from RemoteGlimmerTheme once available.
        val vector = createTestVector(color = Color.Red)
        val doc = setRemoteContent { RemoteIcon(imageVector = vector, contentDescription = null) }

        assertThat(doc.paintBundleStrings().any { it.contains(colorFilterString(Color.White)) })
            .isTrue()
        captureRoot().assertIconCenterColor(Color.White)
    }

    @Test
    fun vector_explicitTint_appliesColorFilter() {
        val vector = createTestVector(color = Color.Red)
        val doc = setRemoteContent {
            RemoteIcon(imageVector = vector, contentDescription = null, tint = Color.Blue.rc)
        }

        assertThat(doc.paintBundleStrings().any { it.contains(colorFilterString(Color.Blue)) })
            .isTrue()
        captureRoot().assertIconCenterColor(Color.Blue)
    }

    @Test
    fun vector_unspecifiedTint_omitsColorFilterAndPreservesOriginalColors() {
        val vector = createTestVector(color = Color.Red)
        val doc = setRemoteContent {
            RemoteIcon(imageVector = vector, contentDescription = null, tint = Color.Unspecified.rc)
        }

        assertThat(doc.paintBundleStrings().none { it.contains("ColorFilter(") }).isTrue()
        captureRoot().assertIconCenterColor(Color.Red)
    }

    @Test
    @Ignore("b/588377603 - Transparent tint is ignored since RC has no separate Unspecified value")
    fun vector_transparentTint_hidesIcon() {
        val vector = createTestVector(color = Color.Red)
        setRemoteContent {
            RemoteIcon(imageVector = vector, contentDescription = null, tint = Color.Transparent.rc)
        }

        captureRoot().assertIconHidden()
    }

    @Test
    fun vector_tint_doesNotLeakToSubsequentSibling() {
        val vector = createTestVector(color = Color.Red)
        setRemoteContent {
            val siblingBitmap = createTestBitmap(color = Color.Green)
            RemoteColumn {
                RemoteIcon(imageVector = vector, contentDescription = null, tint = Color.Magenta.rc)
                RemoteImage(
                    bitmap = siblingBitmap,
                    contentDescription = null,
                    modifier = RemoteModifier.size(40.rdp),
                )
            }
        }

        val rendered = captureRoot()
        rendered.assertIconCenterColor(Color.Magenta)
        rendered.assertIconCenterColor(Color.Green, offsetY = defaultIconSizePx())
    }

    @Test
    fun bitmap_defaultTint_appliesDefaultContentColor() {
        // TODO: b/560206798 - Test content color from RemoteGlimmerTheme once available.
        val doc = setRemoteContent {
            val bitmap = createTestBitmap(color = Color.Red)
            RemoteIcon(bitmap = bitmap, contentDescription = null)
        }

        assertThat(doc.paintBundleStrings().any { it.contains(colorFilterString(Color.White)) })
            .isTrue()
        captureRoot().assertIconCenterColor(Color.White)
    }

    @Test
    fun bitmap_explicitTint_appliesColorFilter() {
        val doc = setRemoteContent {
            val bitmap = createTestBitmap(color = Color.Red)
            RemoteIcon(bitmap = bitmap, contentDescription = null, tint = Color.Blue.rc)
        }

        assertThat(doc.paintBundleStrings().any { it.contains(colorFilterString(Color.Blue)) })
            .isTrue()
        captureRoot().assertIconCenterColor(Color.Blue)
    }

    @Test
    fun bitmap_unspecifiedTint_omitsColorFilterAndPreservesOriginalColors() {
        val doc = setRemoteContent {
            val bitmap = createTestBitmap(color = Color.Red)
            RemoteIcon(bitmap = bitmap, contentDescription = null, tint = Color.Unspecified.rc)
        }

        assertThat(doc.paintBundleStrings().none { it.contains("ColorFilter(") }).isTrue()
        captureRoot().assertIconCenterColor(Color.Red)
    }

    @Test
    @Ignore("b/588377603 - Transparent tint is ignored since RC has no separate Unspecified value")
    fun bitmap_transparentTint_hidesIcon() {
        setRemoteContent {
            val bitmap = createTestBitmap(color = Color.Red)
            RemoteIcon(bitmap = bitmap, contentDescription = null, tint = Color.Transparent.rc)
        }

        captureRoot().assertIconHidden()
    }

    @Test
    fun bitmap_tint_doesNotLeakToSubsequentSibling() {
        setRemoteContent {
            val iconBitmap = createTestBitmap(color = Color.Red)
            val siblingBitmap = createTestBitmap(color = Color.Green)
            RemoteColumn {
                RemoteIcon(bitmap = iconBitmap, contentDescription = null, tint = Color.Magenta.rc)
                RemoteImage(
                    bitmap = siblingBitmap,
                    contentDescription = null,
                    modifier = RemoteModifier.size(40.rdp),
                )
            }
        }

        val rendered = captureRoot()
        rendered.assertIconCenterColor(Color.Magenta)
        rendered.assertIconCenterColor(Color.Green, offsetY = defaultIconSizePx())
    }

    private suspend fun captureCoreDocument(content: @Composable () -> Unit): CoreDocument {
        val captured = captureSingleRemoteDocument(context, displayInfo, content = content)
        return CoreDocument().apply {
            initFromBuffer(
                RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(captured.bytes))
            )
        }
    }

    /** Captures [content] and plays it in the embedded player. */
    private fun setRemoteContent(content: @Composable @RemoteComposable () -> Unit): CoreDocument =
        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo,
            playComposableWrapper = { player -> RootBox { player() } },
            content = content,
        )

    @Composable
    private fun RootBox(content: @Composable () -> Unit) {
        Box(Modifier.size(displayInfo.widthDp, displayInfo.heightDp).testTag(ROOT_TAG)) {
            content()
        }
    }

    private fun captureRoot(): Bitmap =
        rule.onNodeWithTag(ROOT_TAG).captureToImage().asAndroidBitmap()

    /**
     * Checks the color of the pixel in the center of an icon whose top edge is at [offsetY]. The
     * center isn't affected by anti-aliasing of the icon edges.
     */
    private fun Bitmap.assertIconCenterColor(expectedColor: Color, offsetY: Int = 0) {
        val center = defaultIconSizePx() / 2
        assertThat(Color(getPixel(center, offsetY + center))).isEqualTo(expectedColor)
    }

    /** Checks that the center of an icon at the top has the same color as the root background. */
    private fun Bitmap.assertIconHidden() {
        assertIconCenterColor(Color(getPixel(width - 1, height - 1)))
    }

    private fun defaultIconSizePx(): Int =
        (40f * context.resources.displayMetrics.density).roundToInt()

    private fun colorFilterString(color: Color): String =
        "ColorFilter(color=0x${Integer.toHexString(color.toArgb())}"

    private fun CoreDocument.paintBundleStrings(): List<String> =
        findAllOperations<PaintData>().map { it.mPaintData.toString() }

    @Composable
    private fun createTestBitmap(
        width: Int = 24,
        height: Int = 24,
        color: Color = Color.Red,
    ): RemoteImageBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(color.toArgb()) }
            .asImageBitmap()
            .asRemoteImageBitmap()

    private fun createTestVector(
        width: RemoteDp = 24.rdp,
        height: RemoteDp = 24.rdp,
        color: Color = Color.Red,
    ): RemoteImageVector =
        RemoteImageVector.Builder(
                name = "TestIcon",
                defaultWidth = width,
                defaultHeight = height,
                viewportWidth = 24f.rf,
                viewportHeight = 24f.rf,
                tintColor = Color.Unspecified.rc,
            )
            .apply {
                path(fill = RemoteBrush.solidColor(color.rc)) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                    lineTo(0f.rf, 24f.rf)
                    close()
                }
            }
            .build()

    private fun CoreDocument.findFirstIconBox(): BoxLayout = findAllOperations<BoxLayout>().first()

    private inline fun <reified T : Operation> CoreDocument.findAllOperations(): List<T> =
        collectAllOperations().filterIsInstance<T>()

    private fun CoreDocument.collectAllOperations(): List<Operation> {
        val result = mutableListOf<Operation>()
        fun visit(op: Operation) {
            result.add(op)
            if (op is Container) {
                op.list.forEach(::visit)
            }
            if (op is LayoutComponent) {
                op.componentModifiers.list.forEach(::visit)
                op.canvasOperations?.let(::visit)
            }
        }
        operations.forEach(::visit)
        return result
    }

    private companion object {
        const val ROOT_TAG = "root"
    }
}
