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

package androidx.compose.remote.core.operations.loom

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.TextStyle
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PatternInflationIdAndScopeTest {

    private fun createExperimentalWriter(apiLevel: Int = 8): RemoteComposeWriter {
        val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val profile =
            Profile(apiLevel, profileMask, RcPlatformServices.None) { info, p, _ ->
                RemoteComposeWriter(
                    p,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, info.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, info.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
                )
            }
        return profile.create(CreationDisplayInfo(400, 400, 160), "loom-test")
    }

    private fun parseDocument(writer: RemoteComposeWriter): CoreDocument {
        val bytes = writer.encodeToByteArray()
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes))
        val doc = CoreDocument()
        doc.initFromBuffer(buffer)
        return doc
    }

    private fun <T : Operation> collectOps(ops: List<Operation>, clazz: Class<T>): List<T> {
        val found = mutableListOf<T>()
        fun visit(list: List<Operation>) {
            for (op in list) {
                if (clazz.isInstance(op)) {
                    found.add(clazz.cast(op))
                }
                if (op is Container) {
                    visit(op.list)
                }
            }
        }
        visit(ops)
        return found
    }

    private inline fun <reified T : Operation> collectOps(ops: List<Operation>): List<T> =
        collectOps(ops, T::class.java)

    @Test
    fun coreText_inflatedMultipleTimes_getsDistinctComponentAnimationAndAxisIds() {
        val writer = createExperimentalWriter()
        val labelParam = writer.definePatternParameter("label")

        val templateComponentId = 1001
        val templateAnimationId = 1002

        val patternId = writer.definePattern("TextCard", intArrayOf(labelParam))
        // Explicit componentId + animationId via buffer, plus a generated-id CoreText via writer
        val templateAxisTagId = writer.addText("wght")
        writer.buffer.addTextComponentStart(
            templateComponentId,
            templateAnimationId,
            labelParam,
            -1,
            0xFF000000.toInt(),
            -1,
            18f,
            -1f,
            -1f,
            0,
            400f,
            -1,
            CoreText.TEXT_ALIGN_START,
            CoreText.OVERFLOW_CLIP,
            Int.MAX_VALUE,
            0f,
            0f,
            1f,
            0,
            0,
            0,
            false,
            false,
            intArrayOf(templateAxisTagId),
            floatArrayOf(600f),
            false,
            0,
        )
        writer.buffer.addContentStart()
        writer.endTextComponent()

        writer.startTextComponent(
            RecordingModifier(),
            labelParam,
            -1,
            0xFF000000.toInt(),
            -1,
            16f,
            -1f,
            -1f,
            0,
            400f,
            null,
            CoreText.TEXT_ALIGN_START,
            CoreText.OVERFLOW_CLIP,
            Int.MAX_VALUE,
            0f,
            0f,
            1f,
            0,
            0,
            0,
            false,
            false,
            null,
            null,
            false,
            0,
        )
        writer.endTextComponent()
        writer.endPatternDefine()

        val textA = writer.addText("Alpha")
        val textB = writer.addText("Beta")

        writer.startRoot()
        writer.startColumn(RecordingModifier(), 0, 0)
        writer.addPatternInflation(patternId, intArrayOf(textA))
        writer.addPatternInflation(patternId, intArrayOf(textB))
        writer.endColumn()
        writer.endRoot()

        val doc = parseDocument(writer)
        val context = HeadlessRemoteContext()
        doc.initializeContext(context)

        val coreTexts = collectOps<CoreText>(doc.operations)
        assertThat(coreTexts).hasSize(4)

        // Every inflated CoreText must have its own distinct component ID
        val componentIds = coreTexts.map { it.componentId }
        assertThat(componentIds.toSet()).hasSize(4)
        assertThat(componentIds).doesNotContain(templateComponentId)

        // Explicit animation IDs must also be uniqueified per inflation
        val animatedTexts = listOf(coreTexts[0], coreTexts[2])
        assertThat(animatedTexts[0].animationId).isNotEqualTo(templateAnimationId)
        assertThat(animatedTexts[1].animationId).isNotEqualTo(templateAnimationId)
        assertThat(animatedTexts[0].animationId).isNotEqualTo(animatedTexts[1].animationId)

        // Font axis tag text IDs defined inside the pattern must resolve to each inflation's text
        val axis0 = animatedTexts[0].fontAxis!![0]
        val axis1 = animatedTexts[1].fontAxis!![0]
        assertThat(axis0).isNotEqualTo(templateAxisTagId)
        assertThat(axis1).isNotEqualTo(templateAxisTagId)
        assertThat(axis0).isNotEqualTo(axis1)
        assertThat(context.getText(axis0)).isEqualTo("wght")
        assertThat(context.getText(axis1)).isEqualTo("wght")

        // Each inflated CoreText must be independently addressable in CoreDocument's component map
        for (ct in coreTexts) {
            assertThat(doc.getComponent(ct.componentId)).isSameInstanceAs(ct)
        }
    }

    @Test
    fun textStyle_inflatedMultipleTimes_getsDistinctIdsAndAppliesPerInstance() {
        val writer = createExperimentalWriter()
        val labelParam = writer.definePatternParameter("label")
        val colorParam = writer.definePatternParameter("color")

        val patternId = writer.definePattern("StyledText", intArrayOf(labelParam, colorParam))
        val styleId =
            writer.addTextStyle(
                null,
                colorParam,
                20f,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                arrayOf("wght"),
                floatArrayOf(500f),
                null,
                -1,
            )
        writer.startTextComponent(RecordingModifier(), labelParam, styleId, 0)
        writer.endTextComponent()
        writer.endPatternDefine()

        val label1 = writer.addText("First")
        val label2 = writer.addText("Second")
        val redId = writer.addColor(0xFFFF0000.toInt())
        val greenId = writer.addColor(0xFF00FF00.toInt())

        writer.startRoot()
        writer.startColumn(RecordingModifier(), 0, 0)
        writer.addPatternInflation(patternId, intArrayOf(label1, redId))
        writer.addPatternInflation(patternId, intArrayOf(label2, greenId))
        writer.endColumn()
        writer.endRoot()

        val doc = parseDocument(writer)
        val context = HeadlessRemoteContext()
        doc.initializeContext(context)

        val styles = collectOps<TextStyle>(doc.operations)
        assertThat(styles).hasSize(2)
        assertThat(styles[0].id).isNotEqualTo(styles[1].id)

        val coreTexts = collectOps<CoreText>(doc.operations)
        assertThat(coreTexts).hasSize(2)
        assertThat(coreTexts[0].componentId).isNotEqualTo(coreTexts[1].componentId)

        // Apply styles and update variables on each CoreText to verify each got its own TextStyle
        coreTexts[0].updateVariables(context)
        coreTexts[1].updateVariables(context)
        assertThat(coreTexts[0].colorValue).isEqualTo(0xFFFF0000.toInt())
        assertThat(coreTexts[1].colorValue).isEqualTo(0xFF00FF00.toInt())

        // Axis tag text IDs inside each inflated TextStyle must resolve cleanly
        assertThat(context.getText(coreTexts[0].fontAxis!![0])).isEqualTo("wght")
        assertThat(context.getText(coreTexts[1].fontAxis!![0])).isEqualTo("wght")
    }

    @Test
    fun canvasContent_inflatedMultipleTimes_getsDistinctComponentIds() {
        val writer = createExperimentalWriter(apiLevel = 7)
        val patternId = writer.definePattern("BadgeCanvas", intArrayOf())
        writer.startCanvas(RecordingModifier())
        writer.drawRect(0f, 0f, 10f, 10f)
        writer.endCanvas()
        writer.endPatternDefine()

        writer.startRoot()
        writer.startColumn(RecordingModifier(), 0, 0)
        writer.addPatternInflation(patternId, intArrayOf())
        writer.addPatternInflation(patternId, intArrayOf())
        writer.endColumn()
        writer.endRoot()

        val doc = parseDocument(writer)
        val canvases = collectOps<CanvasLayout>(doc.operations)
        val contents = collectOps<CanvasContent>(doc.operations)

        assertThat(canvases).hasSize(2)
        assertThat(contents).hasSize(2)
        assertThat(canvases[0].componentId).isNotEqualTo(canvases[1].componentId)
        assertThat(contents[0].componentId).isNotEqualTo(contents[1].componentId)
    }

    @Test
    fun addText_insidePattern_doesNotPoisonOuterScopeOrSiblingPatterns() {
        val writer = createExperimentalWriter()

        // Pattern 1 defines "save" first (and uses it twice to verify intra-pattern deduplication)
        val pattern1Id = writer.definePattern("SaveButtonA", intArrayOf())
        val p1SaveFirst = writer.addText("save")
        val p1SaveSecond = writer.addText("save")
        assertThat(p1SaveFirst).isEqualTo(p1SaveSecond)
        writer.startTextComponent(RecordingModifier(), p1SaveFirst, -1, 0)
        writer.endTextComponent()
        writer.endPatternDefine()

        // Pattern 2 also uses "save" — must not inherit Pattern 1's private ID
        val pattern2Id = writer.definePattern("SaveButtonB", intArrayOf())
        val p2Save = writer.addText("save")
        assertThat(p2Save).isNotEqualTo(p1SaveFirst)
        writer.startTextComponent(RecordingModifier(), p2Save, -1, 0)
        writer.endTextComponent()
        writer.endPatternDefine()

        // Outer top-level code uses "save" — must emit a top-level TextData with a fresh ID
        val outerSave = writer.addText("save")
        assertThat(outerSave).isNotEqualTo(p1SaveFirst)
        assertThat(outerSave).isNotEqualTo(p2Save)

        writer.startRoot()
        writer.startColumn(RecordingModifier(), 0, 0)
        writer.startTextComponent(RecordingModifier(), outerSave, -1, 0)
        writer.endTextComponent()
        writer.addPatternInflation(pattern1Id, intArrayOf())
        writer.addPatternInflation(pattern2Id, intArrayOf())
        writer.endColumn()
        writer.endRoot()

        val doc = parseDocument(writer)
        val context = HeadlessRemoteContext()
        doc.initializeContext(context)

        // Outer "save" must be defined at top level and resolve cleanly
        assertThat(context.getText(outerSave)).isEqualTo("save")
        val topLevelTextIds = doc.operations.filterIsInstance<TextData>().map { it.mTextId }
        assertThat(topLevelTextIds).contains(outerSave)

        // All three CoreTexts (outer + Pattern 1 + Pattern 2) must resolve to "save"
        val coreTexts = collectOps<CoreText>(doc.operations)
        assertThat(coreTexts).hasSize(3)
        for (ct in coreTexts) {
            assertThat(context.getText(ct.textId!!)).isEqualTo("save")
        }
    }

    @Test
    fun addText_definedBeforePattern_isReusedAndSupportsNestedPatternCallByName() {
        val writer = createExperimentalWriter()

        val sharedOuterId = writer.addText("SharedTitle")
        val innerPatternId = writer.definePattern("InnerBadge", intArrayOf())
        val innerLabelId = writer.addText("Inner")
        writer.startTextComponent(RecordingModifier(), innerLabelId, -1, 0)
        writer.endTextComponent()
        writer.endPatternDefine()

        val outerPatternId = writer.definePattern("OuterCard", intArrayOf())
        // Outer-defined string should be reused inside the pattern
        assertThat(writer.addText("SharedTitle")).isEqualTo(sharedOuterId)
        writer.startTextComponent(RecordingModifier(), sharedOuterId, -1, 0)
        writer.endTextComponent()
        // Calling another pattern by name inside a pattern relies on seeing its outer name ID
        val lookedUpInnerId = writer.addText("InnerBadge")
        assertThat(lookedUpInnerId).isEqualTo(innerPatternId)
        writer.addPatternInflation(lookedUpInnerId, intArrayOf())
        writer.endPatternDefine()

        writer.startRoot()
        writer.startColumn(RecordingModifier(), 0, 0)
        writer.addPatternInflation(outerPatternId, intArrayOf())
        writer.endColumn()
        writer.endRoot()

        val doc = parseDocument(writer)
        val context = HeadlessRemoteContext()
        doc.initializeContext(context)

        val coreTexts = collectOps<CoreText>(doc.operations)
        assertThat(coreTexts).hasSize(2)
        assertThat(context.getText(coreTexts[0].textId!!)).isEqualTo("SharedTitle")
        assertThat(context.getText(coreTexts[1].textId!!)).isEqualTo("Inner")
    }

    @Test
    fun referencedOperations_pushesAndPopsDataScope() {
        val writer = createExperimentalWriter()

        val refId = writer.nextId()
        writer.startReferencedOperations(refId)
        val refTextId = writer.addText("only-in-ref-first")
        writer.endReferencedOperations()

        val outerTextId = writer.addText("only-in-ref-first")
        assertThat(outerTextId).isNotEqualTo(refTextId)

        writer.startRoot()
        writer.startTextComponent(RecordingModifier(), outerTextId, -1, 0)
        writer.endTextComponent()
        writer.endRoot()

        val doc = parseDocument(writer)
        val context = HeadlessRemoteContext()
        doc.initializeContext(context)

        assertThat(context.getText(outerTextId)).isEqualTo("only-in-ref-first")
    }
}
