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
import androidx.compose.remote.player.compose.embedded.demos.embedded.SupportSpannableStringPlugin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.LinkAnnotation
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
                RcPlayer(document = document, customPlugins = CustomPluginRegistry(plugin))
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
                RcPlayer(document = document, customPlugins = CustomPluginRegistry(returnPlugin))
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
                RcPlayer(document = document, customPlugins = registry)
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
                RcPlayer(document = document, customPlugins = registry)
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
                RcPlayer(document = document, customPlugins = registry)
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
                            Color.Red.toArgb(),
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
                RcPlayer(document = document, customPlugins = registry)
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

    @Test
    fun supportSpannableStringRendersAnnotatedStringWithLinks() {
        val fullText = "Please review our Terms of Service and Privacy Policy."
        val termsUrl = "https://example.com/terms"
        val privacyUrl = "https://example.com/privacy"

        val docContext =
            RemoteComposeContextAndroid(
                300,
                100,
                "custom-spannable",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            SupportSpannableStringPlugin.CONFIG,
                            listOf(
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_TEXT,
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId(fullText),
                                ),
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_LINK_COUNT,
                                    Custom.CustomProperty.INT_PROP,
                                    2,
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 0).toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId(termsUrl),
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_START_BASE + 0)
                                        .toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    18,
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_END_BASE + 0).toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    34,
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 1).toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId(privacyUrl),
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_START_BASE + 1)
                                        .toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    39,
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_END_BASE + 1).toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    53,
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

        val registry = CustomPluginRegistry(SupportSpannableStringPlugin)

        rule.setContent {
            Box(modifier = Modifier.size(300.dp)) {
                RcPlayer(document = document, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        val node = rule.onNodeWithText(fullText).fetchSemanticsNode()
        val textList = node.config[SemanticsProperties.Text]
        assertThat(textList).hasSize(1)

        val renderedAnnotatedString = textList.first()
        assertThat(renderedAnnotatedString.text).isEqualTo(fullText)

        val linkAnnotations = renderedAnnotatedString.getLinkAnnotations(0, fullText.length)
        assertThat(linkAnnotations).hasSize(2)

        val link0 = linkAnnotations[0]
        assertThat(link0.start).isEqualTo(18)
        assertThat(link0.end).isEqualTo(34)
        assertThat((link0.item as LinkAnnotation.Url).url).isEqualTo(termsUrl)

        val link1 = linkAnnotations[1]
        assertThat(link1.start).isEqualTo(39)
        assertThat(link1.end).isEqualTo(53)
        assertThat((link1.item as LinkAnnotation.Url).url).isEqualTo(privacyUrl)

        // Sub-range queries
        val termsOnly = renderedAnnotatedString.getLinkAnnotations(18, 34)
        assertThat(termsOnly).hasSize(1)
        assertThat((termsOnly.first().item as LinkAnnotation.Url).url).isEqualTo(termsUrl)

        val privacyOnly = renderedAnnotatedString.getLinkAnnotations(39, 53)
        assertThat(privacyOnly).hasSize(1)
        assertThat((privacyOnly.first().item as LinkAnnotation.Url).url).isEqualTo(privacyUrl)
    }

    @Test
    fun supportSpannableStringRendersStyledTextAndHandlesOutOfBoundsRanges() {
        val sampleText = "Hello World"
        val testUrl = "https://example.com"

        val docContext =
            RemoteComposeContextAndroid(
                200,
                100,
                "custom-spannable-styled",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            SupportSpannableStringPlugin.CONFIG,
                            listOf(
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_TEXT,
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId(sampleText),
                                ),
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_TEXT_COLOR,
                                    Custom.CustomProperty.INT_PROP,
                                    Color.Blue.toArgb(),
                                ),
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_TEXT_SIZE,
                                    Custom.CustomProperty.FLOAT_PROP,
                                    18f,
                                ),
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_LINK_COUNT,
                                    Custom.CustomProperty.INT_PROP,
                                    1,
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 0).toShort(),
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId(testUrl),
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_START_BASE + 0)
                                        .toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    -5,
                                ),
                                Custom.CustomProperty(
                                    (SupportSpannableStringPlugin.PROP_LINK_END_BASE + 0).toShort(),
                                    Custom.CustomProperty.INT_PROP,
                                    100,
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

        val registry = CustomPluginRegistry(SupportSpannableStringPlugin)

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = document, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        val node = rule.onNodeWithText(sampleText).fetchSemanticsNode()
        val textList = node.config[SemanticsProperties.Text]
        assertThat(textList).hasSize(1)

        val renderedAnnotatedString = textList.first()
        assertThat(renderedAnnotatedString.text).isEqualTo(sampleText)

        val linkAnnotations = renderedAnnotatedString.getLinkAnnotations(0, sampleText.length)
        assertThat(linkAnnotations).hasSize(1)

        val link = linkAnnotations[0]
        // Since start was -5 and end was 100, coerceIn clamped them to 0 and sampleText.length
        assertThat(link.start).isEqualTo(0)
        assertThat(link.end).isEqualTo(sampleText.length)
        assertThat((link.item as LinkAnnotation.Url).url).isEqualTo(testUrl)
    }

    @Test
    fun supportSpannableStringRendersTextWithoutLinks() {
        val plainText = "Plain text without any links"

        val docContext =
            RemoteComposeContextAndroid(
                200,
                100,
                "custom-spannable-nolinks",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                writer.root {
                    writer.column(Modifier, 1, 1) {
                        writer.startCustom(
                            Modifier,
                            SupportSpannableStringPlugin.CONFIG,
                            listOf(
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_TEXT,
                                    Custom.CustomProperty.STRING_PROP,
                                    writer.textCreateId(plainText),
                                ),
                                Custom.CustomProperty(
                                    SupportSpannableStringPlugin.PROP_LINK_COUNT,
                                    Custom.CustomProperty.INT_PROP,
                                    0,
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

        val registry = CustomPluginRegistry(SupportSpannableStringPlugin)

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = document, customPlugins = registry)
            }
        }
        rule.mainClock.advanceTimeBy(100)

        val node = rule.onNodeWithText(plainText).fetchSemanticsNode()
        val textList = node.config[SemanticsProperties.Text]
        val renderedAnnotatedString = textList.first()

        assertThat(renderedAnnotatedString.text).isEqualTo(plainText)
        assertThat(renderedAnnotatedString.getLinkAnnotations(0, plainText.length)).isEmpty()
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
