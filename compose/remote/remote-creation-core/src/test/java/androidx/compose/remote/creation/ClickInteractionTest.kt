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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClickInteractionTest {
    private lateinit var rcPlatform: RcPlatformServices
    private lateinit var profile: Profile

    val creationDisplayInfo = CreationDisplayInfo(450, 450, (2f * 160).toInt())

    @Before
    fun setUp() {
        rcPlatform = RcPlatformServices.None
        profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                rcPlatform,
            ) { creationDisplayInfo, profile, _ ->
                RemoteComposeWriter(
                    profile,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(
                        Header.DOC_PROFILES,
                        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                    ),
                )
            }
    }

    @Test
    fun testClickInteractions() {
        val context =
            RemoteComposeContext(creationDisplayInfo, "test", profile) {
                val clickCount = addNamedFloat("clickCount", 0f)
                val longPressCount = addNamedFloat("longPressCount", 0f)
                val doubleTapCount = addNamedFloat("doubleTapCount", 0f)

                root {
                    column(
                        Modifier.onClick(
                                ValueFloatExpressionChange(
                                    Utils.idFromNan(clickCount),
                                    Utils.idFromNan((rf(clickCount) + 1f).toFloat()),
                                )
                            )
                            .onLongClick(
                                ValueFloatExpressionChange(
                                    Utils.idFromNan(longPressCount),
                                    Utils.idFromNan((rf(longPressCount) + 1f).toFloat()),
                                )
                            )
                            .onDoubleClick(
                                ValueFloatExpressionChange(
                                    Utils.idFromNan(doubleTapCount),
                                    Utils.idFromNan((rf(doubleTapCount) + 1f).toFloat()),
                                )
                            )
                    ) {
                        text("Click Test")
                    }
                }
            }

        val bufferData = context.buffer()
        val readBuffer = RemoteComposeBuffer()
        readBuffer.buffer.setSystemInfo(
            SystemInfo(CoreDocument.DOCUMENT_API_LEVEL, CoreDocument.PROFILE)
        )
        readBuffer.buffer.reset(bufferData.size)
        for (b in bufferData) {
            readBuffer.buffer.writeByte(b.toInt())
        }
        readBuffer.buffer.setIndex(0)

        val document = CoreDocument()
        document.initFromBuffer(readBuffer)

        val root = document.rootLayoutComponent
        assertThat(root).isNotNull()

        // Find the column component. Note that there might be FloatExpressions in the list
        // created during the evaluation of the click actions.
        val column = root!!.list.filterIsInstance<Component>().first()
        val modifiers = column.list

        // Find click modifiers in the main list or inside ComponentModifiers
        val clickOps = mutableListOf<MultiClickModifier>()
        clickOps.addAll(modifiers.filterIsInstance<MultiClickModifier>())

        modifiers
            .filterIsInstance<
                androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
            >()
            .forEach { clickOps.addAll(it.modifiersList.filterIsInstance<MultiClickModifier>()) }

        assertThat(clickOps).hasSize(2)

        // Verify click types
        val clickTypes = clickOps.map { it.clickTypeForTest }
        assertThat(clickTypes)
            .containsExactly(
                MultiClickModifier.CLICK_TYPE_LONG,
                MultiClickModifier.CLICK_TYPE_DOUBLE,
            )
    }

    @Test
    fun testMultipleActions() {
        val context =
            RemoteComposeContext(creationDisplayInfo, "test", profile) {
                val clickCount = addNamedFloat("clickCount", 0f)
                val resetFlag = addNamedFloat("resetFlag", 0f)

                root {
                    column(
                        Modifier.onClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(clickCount),
                                Utils.idFromNan((rf(clickCount) + 1f).toFloat()),
                            ),
                            ValueFloatExpressionChange(
                                Utils.idFromNan(resetFlag),
                                Utils.idFromNan((rf(0f).toFloat())),
                            ),
                        )
                    ) {
                        text("Multiple Actions Test")
                    }
                }
            }

        val bufferData = context.buffer()
        val readBuffer = RemoteComposeBuffer()
        readBuffer.buffer.setSystemInfo(
            SystemInfo(CoreDocument.DOCUMENT_API_LEVEL, CoreDocument.PROFILE)
        )
        readBuffer.buffer.reset(bufferData.size)
        for (b in bufferData) {
            readBuffer.buffer.writeByte(b.toInt())
        }
        readBuffer.buffer.setIndex(0)

        val document = CoreDocument()
        document.initFromBuffer(readBuffer)

        val root = document.rootLayoutComponent
        assertThat(root).isNotNull()

        val column = root!!.list.filterIsInstance<Component>().first()
        val clickOps = mutableListOf<MultiClickModifier>()
        column.list
            .filterIsInstance<
                androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
            >()
            .forEach { clickOps.addAll(it.modifiersList.filterIsInstance<MultiClickModifier>()) }
        assertThat(clickOps).hasSize(0)
    }

    private val MultiClickModifier.clickTypeForTest: Int
        get() {
            val field = MultiClickModifier::class.java.getDeclaredField("mClickType")
            field.isAccessible = true
            return field.get(this) as Int
        }
}
