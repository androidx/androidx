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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.compose.embedded.demos.SupportEditTextData
import androidx.compose.remote.player.compose.embedded.demos.SupportEditTextPlugin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Custom (host-extension) components: the document declares a `Custom` component with a
 * config string + typed properties, and the host's `customContent` composable renders it (the
 * embedded equivalent of the View player's setCustomSupport / CustomContext).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCustomComponentTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun customComponentRendersHostContentWithResolvedProperties() {
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "test:badge",
                            listOf(
                                Custom.CustomProperty(
                                    1.toShort(),
                                    Custom.CustomProperty.FLOAT_PROP,
                                    42f,
                                ),
                                Custom.CustomProperty(
                                    2.toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    7,
                                ),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        val plugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:badge"

                @Composable
                override fun extract(component: RcCustomComponent): Unit? =
                    if (component.config == "test:badge") Unit else null

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    val floatProp = FloatProperty(1)
                    val intProp = IntProperty(2)
                    BasicText(
                        "custom:${component.config}:${component.floatState(floatProp).value.toInt()}:${component.intState(intProp).value}"
                    )
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    document = document,
                    autoUpdate = false,
                    customPlugins = CustomPluginRegistry(plugin),
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)

        // The host content composed inside the Custom component, with the config name and both
        // properties resolved by type.
        rule.onNodeWithText("custom:test:badge:42:7").assertExists()
    }

    /**
     * Return channels flow host values back into the document: the host writes via
     * returnFloat/returnText and the bound document variable/text holds the value — available to
     * the rest of the document on the same recomposition, not a later frame.
     */
    @Suppress("UNCHECKED_CAST")
    @Test
    fun customReturnChannelsWriteBackIntoTheDocument() {
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                val floatTargetId = writer.addNamedFloat("returnTarget", 0f)
                val textTargetId = writer.textCreateId("")
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "test:return",
                            listOf(
                                Custom.CustomProperty(
                                    1.toShort(),
                                    Custom.CustomProperty.FLOAT_RETURN,
                                    floatTargetId,
                                ),
                                Custom.CustomProperty(
                                    2.toShort(),
                                    Custom.CustomProperty.TEXT_RETURN,
                                    textTargetId,
                                ),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        val returnPlugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:return"

                @Composable
                override fun extract(component: RcCustomComponent): Unit? =
                    if (component.config == "test:return") Unit else null

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    val floatHandler = component.returnFloatHandler(FloatReturnProperty(1))
                    val textHandler = component.returnTextHandler(TextReturnProperty(2))
                    floatHandler(77f)
                    textHandler("from-host")
                    BasicText("returned")
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    document = document,
                    autoUpdate = false,
                    customPlugins = CustomPluginRegistry(returnPlugin),
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("returned").assertExists()

        // The write-back landed in the document store, at the ids the Custom op declared.
        val custom = requireNotNull(findCustom(document.getOperationsReflection()))
        val props = custom.readData().properties as List<Custom.CustomProperty>
        val floatId =
            Utils.idFromNan(
                props.first { it.mDataType == Custom.CustomProperty.FLOAT_RETURN }.mFloatValue
            )
        val textId = props.first { it.mDataType == Custom.CustomProperty.TEXT_RETURN }.mIntValue
        assertThat(document.remoteComposeState.getFloat(floatId)).isEqualTo(77f)
        assertThat(document.remoteComposeState.getFromId(textId)).isEqualTo("from-host")
    }

    @Test
    fun customComponentRendersViaCustomPluginRegistry() {
        data class BadgeDataConfig(val scoreProp: FloatProperty, val countProp: IntProperty)

        val badgePlugin =
            object : CustomComposablePlugin<BadgeDataConfig> {
                override val name: String = "test:badge"

                @Composable
                override fun extract(component: RcCustomComponent): BadgeDataConfig? {
                    if (component.config != "test:badge") return null
                    val scoreProp = FloatProperty(1)
                    val countProp = IntProperty(2)
                    if (!component.hasProperty(scoreProp)) return null
                    return BadgeDataConfig(scoreProp = scoreProp, countProp = countProp)
                }

                @Composable
                override fun Content(
                    data: BadgeDataConfig,
                    component: RcCustomComponent,
                    modifier: Modifier,
                ) {
                    val score by component.floatState(data.scoreProp)
                    val count by component.intState(data.countProp)
                    BasicText("plugin:${score.toInt()}:$count")
                }
            }

        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "test:badge",
                            listOf(
                                Custom.CustomProperty(
                                    1.toShort(),
                                    Custom.CustomProperty.FLOAT_PROP,
                                    42f,
                                ),
                                Custom.CustomProperty(
                                    2.toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    7,
                                ),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        val registry = CustomPluginRegistry(badgePlugin)

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(document = document, autoUpdate = false, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("plugin:42:7").assertExists()
    }

    @Test
    fun customComponentRendersViaDataClassSchema() {
        val badgePlugin =
            object : CustomComposablePlugin<BadgeDataSchema> {
                override val name: String = "test:badge"

                @Composable
                override fun extract(component: RcCustomComponent): BadgeDataSchema? {
                    if (component.config != "test:badge") return null
                    if (!component.hasProperty(BadgeDataSchema.SCORE)) return null
                    return BadgeDataSchema()
                }

                @Composable
                override fun Content(
                    data: BadgeDataSchema,
                    component: RcCustomComponent,
                    modifier: Modifier,
                ) {
                    val score by component.floatState(BadgeDataSchema.SCORE)
                    val count by component.intState(BadgeDataSchema.COUNT)
                    val title by component.textState(BadgeDataSchema.TITLE)
                    BasicText("schema:$title:${score.toInt()}:$count")
                }
            }

        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "test:badge",
                            listOf(
                                Custom.CustomProperty(
                                    1.toShort(),
                                    Custom.CustomProperty.FLOAT_PROP,
                                    42f,
                                ),
                                Custom.CustomProperty(
                                    2.toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    7,
                                ),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        val registry = CustomPluginRegistry(badgePlugin)

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(document = document, autoUpdate = false, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("schema:default-title:42:7").assertExists()
    }

    @Test
    fun supportEditTextRendersAndHandlesReturnChannel() {
        var textTargetId = -1
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                textTargetId = writer.textCreateId("")
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "support:edit-text",
                            listOf(
                                Custom.CustomProperty(
                                    SupportEditTextData.TEXT.id.toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId("initial-text"),
                                ),
                                Custom.CustomProperty(
                                    SupportEditTextData.HINT.id.toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId("enter-text"),
                                ),
                                Custom.CustomProperty(
                                    SupportEditTextData.RET_TEXT.id.toShort(),
                                    Custom.CustomProperty.TEXT_RETURN,
                                    textTargetId,
                                ),
                            ),
                        )
                        writer.endCustom()
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        val registry = CustomPluginRegistry(SupportEditTextPlugin)

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(document = document, autoUpdate = false, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("initial-text").assertExists()
    }

    @Test
    fun supportEditTextUpdatesSharedTextState() {
        var textTargetId = -1
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "custom",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                textTargetId = writer.textCreateId("")
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            "support:edit-text",
                            listOf(
                                Custom.CustomProperty(
                                    SupportEditTextData.TEXT.id.toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId("initial-text"),
                                ),
                                Custom.CustomProperty(
                                    SupportEditTextData.HINT.id.toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId("enter-text"),
                                ),
                                Custom.CustomProperty(
                                    SupportEditTextData.RET_TEXT.id.toShort(),
                                    Custom.CustomProperty.TEXT_RETURN,
                                    textTargetId,
                                ),
                            ),
                        )
                        writer.endCustom()

                        writer.textComponent(
                            Modifier,
                            textTargetId,
                            android.graphics.Color.RED,
                            12f,
                            0,
                            400f,
                            null,
                            0,
                            0,
                            1,
                        ) {}
                    }
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        val registry = CustomPluginRegistry(SupportEditTextPlugin)

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = document, autoUpdate = true, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("initial-text").assertExists()

        rule.onNode(hasSetTextAction()).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("-updated")
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performImeAction()

        rule.mainClock.advanceTimeBy(100)

        // Both the editable field and the shared text component display the updated value.
        rule.onAllNodesWithText("initial-text-updated").assertCountEquals(2)
    }

    private fun findCustom(operations: Collection<Operation>): Custom? {
        for (op in operations) {
            if (op is Custom) return op
            if (op is Container)
                findCustom(op.list)?.let {
                    return it
                }
        }
        return null
    }

    private data class BadgeDataSchema(val label: String = "default-label") {
        companion object {
            val SCORE = FloatProperty(1.toShort(), default = 0f)
            val COUNT = IntProperty(2.toShort(), default = 0)
            val TITLE = StringProperty(3.toShort(), default = "default-title")
        }
    }
}
