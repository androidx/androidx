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

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.embedded.demos.SupportEditTextData
import androidx.compose.remote.player.compose.embedded.demos.SupportEditTextPlugin
import androidx.compose.remote.player.compose.embedded.demos.embedded.SupportSpannableStringPlugin
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.LinkAnnotation
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Custom (host-extension) components recorded with [RemoteCustomComponent] and played by
 * the embedded player (`RcPlayer`): typed properties, return channels, remote children, and
 * plugin-handled clicks ([CustomComposablePlugin.handlesClick]).
 */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCustomComponentTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val experimentalProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    private fun setCustomContent(
        customPlugins: CustomPluginRegistry? = null,
        widthDp: Float = 200f,
        heightDp: Float = 200f,
        onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit =
            { _, _, _ ->
            },
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        return rule.setRemoteContent(
            customPlugins = customPlugins,
            profile = experimentalProfile,
            onNamedAction = onNamedAction,
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(widthDp * density, heightDp * density),
                ),
            content = content,
        )
    }

    @Test
    fun customComponentRendersHostContentWithResolvedProperties() {
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

        setCustomContent(customPlugins = CustomPluginRegistry(plugin)) {
            RemoteColumn {
                RemoteCustomComponent(name = "test:badge") {
                    property(1, 42f)
                    property(2, 7)
                }
            }
        }

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

        val document =
            setCustomContent(customPlugins = CustomPluginRegistry(returnPlugin)) {
                val floatState = remember { MutableRemoteFloat(0f) }
                val textState = remember { MutableRemoteString("") }
                RemoteColumn {
                    RemoteCustomComponent(name = "test:return") {
                        bindReturn(1, floatState)
                        bindReturn(2, textState)
                    }
                }
            }
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

        setCustomContent(customPlugins = CustomPluginRegistry(badgePlugin)) {
            RemoteColumn {
                RemoteCustomComponent(name = "test:badge") {
                    property(1, 42f)
                    property(2, 7)
                }
            }
        }

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

        setCustomContent(customPlugins = CustomPluginRegistry(badgePlugin)) {
            RemoteColumn {
                RemoteCustomComponent(name = "test:badge") {
                    property(1, 42f)
                    property(2, 7)
                }
            }
        }

        rule.onNodeWithText("schema:default-title:42:7").assertExists()
    }

    @Test
    fun supportEditTextRendersAndHandlesReturnChannel() {
        setCustomContent(customPlugins = CustomPluginRegistry(SupportEditTextPlugin)) {
            val textState = remember { MutableRemoteString("") }
            RemoteColumn {
                RemoteCustomComponent(name = "support:edit-text") {
                    property(SupportEditTextData.TEXT.id, "initial-text")
                    property(SupportEditTextData.HINT.id, "enter-text")
                    bindReturn(SupportEditTextData.RET_TEXT.id, textState)
                }
            }
        }

        rule.onNodeWithText("initial-text").assertExists()
    }

    @Test
    fun supportEditTextUpdatesSharedTextState() {
        setCustomContent(customPlugins = CustomPluginRegistry(SupportEditTextPlugin)) {
            val textState = remember { MutableRemoteString("") }
            RemoteColumn {
                RemoteCustomComponent(name = "support:edit-text") {
                    property(SupportEditTextData.TEXT.id, "initial-text")
                    property(SupportEditTextData.HINT.id, "enter-text")
                    bindReturn(SupportEditTextData.RET_TEXT.id, textState)
                }
                RemoteText(textState, color = Color.Red.rc)
            }
        }

        rule.onNodeWithText("initial-text").assertExists()

        rule.onNode(hasSetTextAction()).performTextReplacement("initial-text-updated")
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

        setCustomContent(
            customPlugins = CustomPluginRegistry(SupportSpannableStringPlugin),
            widthDp = 300f,
            heightDp = 100f,
        ) {
            RemoteColumn {
                RemoteCustomComponent(name = SupportSpannableStringPlugin.CONFIG) {
                    property(SupportSpannableStringPlugin.PROP_TEXT.toInt(), fullText)
                    property(SupportSpannableStringPlugin.PROP_LINK_COUNT.toInt(), 2)
                    property(SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 0, termsUrl)
                    property(SupportSpannableStringPlugin.PROP_LINK_START_BASE + 0, 18)
                    property(SupportSpannableStringPlugin.PROP_LINK_END_BASE + 0, 34)
                    property(SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 1, privacyUrl)
                    property(SupportSpannableStringPlugin.PROP_LINK_START_BASE + 1, 39)
                    property(SupportSpannableStringPlugin.PROP_LINK_END_BASE + 1, 53)
                }
            }
        }

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

        setCustomContent(customPlugins = CustomPluginRegistry(SupportSpannableStringPlugin)) {
            RemoteColumn {
                RemoteCustomComponent(name = SupportSpannableStringPlugin.CONFIG) {
                    property(SupportSpannableStringPlugin.PROP_TEXT.toInt(), sampleText)
                    property(
                        SupportSpannableStringPlugin.PROP_TEXT_COLOR.toInt(),
                        Color.Blue.toArgb(),
                    )
                    property(SupportSpannableStringPlugin.PROP_TEXT_SIZE.toInt(), 18f)
                    property(SupportSpannableStringPlugin.PROP_LINK_COUNT.toInt(), 1)
                    property(SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 0, testUrl)
                    property(SupportSpannableStringPlugin.PROP_LINK_START_BASE + 0, -5)
                    property(SupportSpannableStringPlugin.PROP_LINK_END_BASE + 0, 100)
                }
            }
        }

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

        setCustomContent(customPlugins = CustomPluginRegistry(SupportSpannableStringPlugin)) {
            RemoteColumn {
                RemoteCustomComponent(name = SupportSpannableStringPlugin.CONFIG) {
                    property(SupportSpannableStringPlugin.PROP_TEXT.toInt(), plainText)
                    property(SupportSpannableStringPlugin.PROP_LINK_COUNT.toInt(), 0)
                }
            }
        }

        val node = rule.onNodeWithText(plainText).fetchSemanticsNode()
        val textList = node.config[SemanticsProperties.Text]
        val renderedAnnotatedString = textList.first()

        assertThat(renderedAnnotatedString.text).isEqualTo(plainText)
        assertThat(renderedAnnotatedString.getLinkAnnotations(0, plainText.length)).isEmpty()
    }

    @Test
    fun pluginHandlingClicksReceivesClickActions() {
        var capturedDoubleClick: (() -> Unit)? = {}
        var capturedLongClick: (() -> Unit)? = {}
        val plugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:button"
                override val handlesClick: Boolean = true

                @Composable override fun extract(component: RcCustomComponent): Unit = Unit

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    capturedDoubleClick = component.onDoubleClick
                    capturedLongClick = component.onLongClick
                    BasicText("label", Modifier.clickable { component.onClick?.invoke() })
                }
            }

        var fired: String? = null
        setCustomContent(
            customPlugins = CustomPluginRegistry(plugin),
            onNamedAction = { name, _, _ -> fired = name },
        ) {
            RemoteCustomComponent(
                name = "test:button",
                modifier = RemoteModifier.clickable(hostAction("tap".rs), role = null),
            )
        }

        // Unused doubleClick and longClick remain null.
        assertThat(capturedDoubleClick).isNull()
        assertThat(capturedLongClick).isNull()
        // Only the plugin's clickable exists; the player did not also wrap the component in one.
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        rule.onNodeWithText("label").performClick()
        rule.waitForIdle()

        assertThat(fired).isEqualTo("tap")
    }

    @Test
    fun pluginHandlingClicks_withTouchActions_firesTouchUpNotCancelOnTap() {
        val plugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:button"
                override val handlesClick: Boolean = true

                @Composable override fun extract(component: RcCustomComponent): Unit = Unit

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    BasicText("label", Modifier.clickable { component.onClick?.invoke() })
                }
            }

        val fired = mutableListOf<String>()
        setCustomContent(
            customPlugins = CustomPluginRegistry(plugin),
            onNamedAction = { name, _, _ -> fired.add(name) },
        ) {
            RemoteCustomComponent(
                name = "test:button",
                modifier =
                    RemoteModifier.clickable(hostAction("tap".rs), role = null)
                        .onTouchDown(hostAction("down".rs))
                        .onTouchUp(hostAction("up".rs))
                        .onTouchCancel(hostAction("cancel".rs)),
            )
        }

        rule.onNodeWithText("label").performClick()
        rule.waitForIdle()

        // The plugin's clickable consumes the release; that is a click, not a cancelled gesture.
        assertThat(fired).containsExactly("down", "up", "tap")
    }

    @Test
    fun pluginHandlingClicksReceivesDoubleAndLongClickActions() {
        val plugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:button"
                override val handlesClick: Boolean = true

                @Composable override fun extract(component: RcCustomComponent): Unit = Unit

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    BasicText(
                        "label",
                        Modifier.combinedClickable(
                            onClick = { component.onClick?.invoke() },
                            onDoubleClick = component.onDoubleClick,
                            onLongClick = component.onLongClick,
                        ),
                    )
                }
            }

        val fired = mutableListOf<String>()
        setCustomContent(
            customPlugins = CustomPluginRegistry(plugin),
            onNamedAction = { name, _, _ -> fired.add(name) },
        ) {
            RemoteCustomComponent(
                name = "test:button",
                modifier =
                    RemoteModifier.combinedClickable(
                        onClick = hostAction("single".rs),
                        onDoubleClick = hostAction("double".rs),
                        onLongClick = hostAction("long".rs),
                        role = null,
                    ),
            )
        }

        rule.onAllNodes(hasClickAction()).assertCountEquals(1)

        rule.onNodeWithText("label").performClick()
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        assertThat(fired).contains("single")

        rule.onNodeWithText("label").performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        assertThat(fired).contains("double")

        rule.onNodeWithText("label").performTouchInput { longClick() }
        rule.waitForIdle()
        assertThat(fired).contains("long")
    }

    @Test
    fun pluginHandlingClicksLeavesUnusedClickHandlersNull() {
        var capturedClick: (() -> Unit)? = {}
        var capturedDoubleClick: (() -> Unit)? = {}
        var capturedLongClick: (() -> Unit)? = {}
        val plugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:button"
                override val handlesClick: Boolean = true

                @Composable override fun extract(component: RcCustomComponent): Unit = Unit

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    capturedClick = component.onClick
                    capturedDoubleClick = component.onDoubleClick
                    capturedLongClick = component.onLongClick
                    val clickModifier =
                        if (
                            component.onClick != null ||
                                component.onDoubleClick != null ||
                                component.onLongClick != null
                        ) {
                            Modifier.combinedClickable(
                                onClick = { component.onClick?.invoke() },
                                onDoubleClick = component.onDoubleClick,
                                onLongClick = component.onLongClick,
                            )
                        } else {
                            Modifier
                        }
                    BasicText("label", clickModifier)
                }
            }

        setCustomContent(customPlugins = CustomPluginRegistry(plugin)) {
            RemoteCustomComponent(name = "test:button")
        }

        assertThat(capturedClick).isNull()
        assertThat(capturedDoubleClick).isNull()
        assertThat(capturedLongClick).isNull()
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun pluginNotHandlingClicksIsWrappedByThePlayer() {
        var onClick: (() -> Unit)? = {}
        var onDoubleClick: (() -> Unit)? = {}
        var onLongClick: (() -> Unit)? = {}
        val plugin =
            object : CustomComposablePlugin<Unit> {
                override val name: String = "test:label"

                @Composable override fun extract(component: RcCustomComponent): Unit = Unit

                @Composable
                override fun Content(data: Unit, component: RcCustomComponent, modifier: Modifier) {
                    onClick = component.onClick
                    onDoubleClick = component.onDoubleClick
                    onLongClick = component.onLongClick
                    BasicText("label")
                }
            }

        var fired: String? = null
        setCustomContent(
            customPlugins = CustomPluginRegistry(plugin),
            onNamedAction = { name, _, _ -> fired = name },
        ) {
            RemoteCustomComponent(
                name = "test:label",
                modifier = RemoteModifier.clickable(hostAction("tap".rs), role = null),
            )
        }

        // The player applies the click modifier itself and does not expose it to the plugin.
        assertThat(onClick).isNull()
        assertThat(onDoubleClick).isNull()
        assertThat(onLongClick).isNull()
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        rule.onNodeWithText("label").performClick()
        rule.waitForIdle()

        assertThat(fired).isEqualTo("tap")
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
