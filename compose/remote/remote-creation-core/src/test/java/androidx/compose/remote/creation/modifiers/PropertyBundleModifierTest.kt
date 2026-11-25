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

package androidx.compose.remote.creation.modifiers

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.PropertyBundleModifierOperation
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PropertyBundleModifierTest {

    private val displayInfo = CreationDisplayInfo(450, 450, 320)
    private val experimentalMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL

    @Test
    fun recordingModifier_propertyBundle_returnsSelfAndAddsElement() {
        val modifier = RecordingModifier()

        val returned =
            modifier.propertyBundle(
                /* bundleId = */ 12,
                /* parentBundleId = */ 34,
                /* propertyIds = */ intArrayOf(15, 20),
                /* values = */ intArrayOf(1, 2),
            )

        assertThat(returned).isSameInstanceAs(modifier)
        assertThat(modifier.list).hasSize(1)
        val element = modifier.list.single() as PropertyBundleModifier
        assertThat(element.bundleId).isEqualTo(12)
        assertThat(element.parentBundleId).isEqualTo(34)
    }

    @Test
    fun propertyBundleModifier_serializesAndInflatesOnComponent() {
        val writer = createExperimentalWriter()
        writer.root {
            writer.startBox(
                RecordingModifier()
                    .propertyBundle(
                        /* bundleId = */ 5,
                        /* parentBundleId = */ 9,
                        /* propertyIds = */ intArrayOf(15, 20),
                        /* values = */ intArrayOf(1, -7),
                    )
            )
            writer.endBox()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val box = root.list.filterIsInstance<BoxLayout>().single()

        val constantsById = box.list.filterIsInstance<IntegerConstant>().associateBy { it.id }
        val op =
            box.componentModifiers.modifiersList
                .filterIsInstance<PropertyBundleModifierOperation>()
                .single()
        assertThat(box.list.filterIsInstance<PropertyBundleModifierOperation>()).isEmpty()
        assertThat(op.bundleId).isEqualTo(5)
        assertThat(op.parentBundleId).isEqualTo(9)
        assertThat(op.hasProperty(15)).isTrue()
        assertThat(op.hasProperty(20)).isTrue()
        assertThat(op.hasProperty(99)).isFalse()
        assertThat(op.getValue(99)).isNull()

        val id15 = op.getValue(15)!!
        val id20 = op.getValue(20)!!
        assertThat(constantsById[id15].toString()).isEqualTo("IntegerConstant[$id15] = 1")
        assertThat(constantsById[id20].toString()).isEqualTo("IntegerConstant[$id20] = -7")

        val bundle = box.propertyBundle
        assertThat(bundle).isSameInstanceAs(op)
        assertThat(bundle!!.bundleId).isEqualTo(5)
        assertThat(bundle.parentBundleId).isEqualTo(9)
        assertThat(bundle.getValue(15)).isEqualTo(id15)
        assertThat(bundle.getValue(20)).isEqualTo(id20)
        assertThat(bundle.getValue(99, -1)).isEqualTo(-1)
    }

    @Test
    fun propertyBundleModifier_longValues_reusesVariableIdsAndMaterializesLiterals() {
        val writer = createExperimentalWriter()
        val sharedVar = writer.addInteger(77)
        val rawSharedId = (sharedVar - 0x100000000L).toInt()

        writer.root {
            writer.startBox(
                RecordingModifier()
                    .propertyBundle(
                        /* bundleId = */ 8,
                        /* parentBundleId = */ 4,
                        /* propertyIds = */ intArrayOf(10, 20),
                        /* values = */ longArrayOf(sharedVar, -15L),
                    )
            )
            writer.endBox()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val box = root.list.filterIsInstance<BoxLayout>().single()

        val op = box.propertyBundle!!
        assertThat(op.bundleId).isEqualTo(8)
        assertThat(op.parentBundleId).isEqualTo(4)
        // Tagged variable ID is stored directly without emitting an extra IntegerConstant inside
        // box
        assertThat(op.getValue(10)).isEqualTo(rawSharedId)

        // Untagged literal is materialized as an IntegerConstant inside box
        val boxConstants = box.list.filterIsInstance<IntegerConstant>()
        assertThat(boxConstants).hasSize(1)
        val constOp = boxConstants.single()
        assertThat(op.getValue(20)).isEqualTo(constOp.id)
        assertThat(constOp.toString()).isEqualTo("IntegerConstant[${constOp.id}] = -15")
    }

    @Test
    fun propertyBundleModifier_chainsInOrderWithOtherModifiers() {
        val writer = createExperimentalWriter()
        writer.root {
            writer.startBox(
                RecordingModifier()
                    .padding(8)
                    .propertyBundle(3, 1, intArrayOf(42), intArrayOf(99))
                    .background(0xFF00FF00.toInt())
            )
            writer.endBox()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val box = root.list.filterIsInstance<BoxLayout>().single()

        assertThat(box.componentModifiers.modifiersList.map { it.javaClass })
            .containsExactly(
                PaddingModifierOperation::class.java,
                PropertyBundleModifierOperation::class.java,
                BackgroundModifierOperation::class.java,
            )
            .inOrder()
        assertThat(box.propertyBundle!!.bundleId).isEqualTo(3)
        assertThat(box.propertyBundle!!.parentBundleId).isEqualTo(1)
        val constOp = box.list.filterIsInstance<IntegerConstant>().single()
        assertThat(box.propertyBundle!!.getValue(42)).isEqualTo(constOp.id)
        assertThat(constOp.toString()).isEqualTo("IntegerConstant[${constOp.id}] = 99")
    }

    @Test
    fun writer_addPropertyBundleModifierOperation_preservesBundleAndParentIds() {
        val writer = createExperimentalWriter()
        writer.root {
            writer.startBox(
                RecordingModifier().then { w ->
                    w.addPropertyBundleModifierOperation(
                        /* bundleId = */ 7,
                        /* parentBundleId = */ 3,
                        /* propertyIds = */ intArrayOf(10, 20),
                        /* values = */ intArrayOf(100, 200),
                    )
                }
            )
            writer.endBox()
        }

        val doc = parseDocument(writer)
        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val box = root.list.filterIsInstance<BoxLayout>().single()

        val op =
            box.componentModifiers.modifiersList
                .filterIsInstance<PropertyBundleModifierOperation>()
                .single()
        assertThat(op.bundleId).isEqualTo(7)
        assertThat(op.parentBundleId).isEqualTo(3)
        assertThat(op.getValue(10)).isEqualTo(100)
        assertThat(op.getValue(20)).isEqualTo(200)
    }

    @Test
    fun propertyBundleModifier_throwsOnMismatchedArrayLengthsOrExcessiveCount() {
        val writer = createExperimentalWriter()

        val mismatchError =
            assertThrows(IllegalArgumentException::class.java) {
                writer.root {
                    writer.startBox(
                        RecordingModifier().propertyBundle(0, 0, intArrayOf(1, 2), intArrayOf(10))
                    )
                    writer.endBox()
                }
            }
        assertThat(mismatchError)
            .hasMessageThat()
            .contains("Parallel arrays must have identical lengths")

        val writer2 = createExperimentalWriter()
        val tooMany = IntArray(Limits.MAX_PROPERTY_BUNDLE_ENTRIES + 1)
        val limitError =
            assertThrows(IllegalArgumentException::class.java) {
                writer2.root {
                    writer2.startBox(RecordingModifier().propertyBundle(0, 0, tooMany, tooMany))
                    writer2.endBox()
                }
            }
        assertThat(limitError).hasMessageThat().contains("Too many property bundle entries")
    }

    private fun createExperimentalWriter(): RemoteComposeWriter {
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, experimentalMask, RcPlatformServices.None) {
                info,
                prof,
                _ ->
                RemoteComposeWriter(
                    prof,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, info.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, info.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, experimentalMask),
                )
            }
        return profile.create(displayInfo, "test")
    }

    private fun parseDocument(writer: RemoteComposeWriter): CoreDocument {
        val bytes = writer.encodeToByteArray()
        val readBuffer = RemoteComposeBuffer()
        readBuffer.buffer.setSystemInfo(SystemInfo(writer.apiLevel, experimentalMask))
        readBuffer.buffer.reset(bytes.size)
        for (b in bytes) {
            readBuffer.buffer.writeByte(b.toInt())
        }
        readBuffer.buffer.setIndex(0)
        return CoreDocument().apply { initFromBuffer(readBuffer) }
    }
}
