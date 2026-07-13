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

package androidx.compose.remote.player.compose.embedded

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Fail-fast guard for the embedded player's reflective access to package-private remote-core
 * fields.
 *
 * The player reads ~50 package-private fields on core operations by reflection (no public accessors
 * yet). A core rename would otherwise break the player *silently at runtime*, op by op. This test
 * is the central registry of that contract: it resolves every (class, field) pair and fails loudly
 * here if any is gone — turning a silent runtime regression into a build failure.
 *
 * **When you add a new reflective `getDeclaredField` in the embedded package, add the pair here.**
 * (Keep in sync with the `getDeclaredField` call sites in RcPlayerDrawing, ClickModifier,
 * WidthModifier, RcPlayerRowLayout, RcPlayerColumnLayout.)
 */
@RunWith(JUnit4::class)
class CoreReflectionGuardTest {

    private val ops = "androidx.compose.remote.core.operations"
    private val managers = "$ops.layout.managers"
    private val modifiers = "$ops.layout.modifiers"

    /**
     * (fully-qualified class name) -> list of package-private field names accessed by reflection.
     */
    private val reflectiveFields: Map<String, List<String>> =
        mapOf(
            "$ops.ClipPath" to listOf("mId", "mRegionOp"),
            "$ops.DrawTextOnPath" to listOf("mPathId", "mOutHOffset", "mOutVOffset"),
            "$ops.DrawTextAnchored" to
                listOf("mTextID", "mOutX", "mOutY", "mOutPanX", "mOutPanY", "mFlags"),
            "$ops.DrawTextOnCircle" to
                listOf(
                    "mCenterX",
                    "mCenterY",
                    "mRadius",
                    "mStartAngle",
                    "mWarpRadiusOffset",
                    "mAlignment",
                    "mPlacement",
                ),
            "$ops.DrawBitmapInt" to
                listOf(
                    "mImageId",
                    "mSrcLeft",
                    "mSrcTop",
                    "mSrcRight",
                    "mSrcBottom",
                    "mDstLeft",
                    "mDstTop",
                    "mDstRight",
                    "mDstBottom",
                ),
            "$ops.DrawTweenPath" to
                listOf("mPath1Id", "mPath2Id", "mOutTween", "mOutStart", "mOutStop"),
            "$ops.DrawToBitmap" to listOf("mBitmapId", "mMode", "mColor"),
            "$ops.DrawBitmapFontText" to
                listOf(
                    "mTextID",
                    "mBitmapFontID",
                    "mStart",
                    "mEnd",
                    "mOutX",
                    "mOutY",
                    "mOutGlyphSpacing",
                ),
            "$ops.BitmapFontData" to listOf("mKerningTable"),
            "$ops.DrawBitmapFontTextOnPath" to
                listOf(
                    "mTextID",
                    "mBitmapFontID",
                    "mPathID",
                    "mStart",
                    "mEnd",
                    "mOutYAdj",
                    "mOutGlyphSpacing",
                ),
            "$ops.DrawBitmapTextAnchored" to
                listOf(
                    "mTextID",
                    "mBitmapFontID",
                    "mOutStart",
                    "mOutEnd",
                    "mOutX",
                    "mOutY",
                    "mOutPanX",
                    "mOutPanY",
                    "mOutGlyphSpacing",
                ),
            "$modifiers.HostNamedActionOperation" to listOf("mTextId", "mType", "mValueId"),
            "$modifiers.DimensionConstraintsModifierOperation" to listOf("mType"),
            "$modifiers.DimensionModifierOperation" to listOf("mValue"),
            "$managers.RowLayout" to listOf("mSpacedBy"),
            "$managers.ColumnLayout" to listOf("mSpacedBy"),
            "$managers.Custom" to listOf("mConfig", "mConfigId", "mProperties"),
            // Particle rendering is bridged to the core paint() implementations; only the
            // loop's source is read reflectively (for the once-per-document seeding).
            "$ops.ParticlesLoop" to listOf("mParticlesSource"),
            "$ops.ConditionalOperations" to listOf("mVarAOut", "mVarBOut", "mType"),
            "$ops.layout.LoopOperation" to
                listOf("mFromOut", "mUntilOut", "mStepOut", "mIndexVariableId"),
            "$ops.FloatFunctionCall" to listOf("mFunction", "mOutArgs"),
        )

    @Test
    fun allReflectivelyAccessedCoreFieldsExist() {
        val missing = mutableListOf<String>()
        for ((className, fields) in reflectiveFields) {
            val clazz =
                try {
                    Class.forName(className)
                } catch (e: ClassNotFoundException) {
                    missing += "MISSING CLASS: $className"
                    continue
                }
            for (field in fields) {
                try {
                    clazz.getDeclaredField(field)
                } catch (e: NoSuchFieldException) {
                    missing += "$className#$field"
                }
            }
        }
        assertThat(missing).isEmpty()
    }
}
