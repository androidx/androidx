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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createRemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w200dp-h200dp-xhdpi")
class RcPlayerClickModifierTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun click_hostNamedAction_readsComputedGraphValuesAndDynamicName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val sizePx = 120f * density
        val namedActions = mutableListOf<Pair<String, Any?>>()

        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(sizePx, sizePx)),
            onNamedAction = { name, value, _ -> namedActions.add(name to value) },
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(120.dp).testTag("player")) { content() }
            },
        ) {
            val count = remember { MutableRemoteInt(1) }
            val scrollState = rememberRemoteScrollState(notches = 0)
            val computedInt = count * 3.ri + 2.ri
            val computedFloat = count.toRemoteFloat() * 1.5f.rf + scrollState.positionState
            val computedString = "v=".rs + count.toRemoteString()
            val dynamicName = "item_".rs + count.toRemoteString()

            RemoteColumn(modifier = RemoteModifier.size(120.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(30.rdp)
                            .semantics { contentDescription = "increment".rs }
                            .clickable(valueChangeAction(count, count + 1.ri))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(30.rdp)
                            .semantics { contentDescription = "dispatch".rs }
                            .clickable(
                                combinedAction(
                                    hostAction(dynamicName, computedInt),
                                    hostAction("float".rs, computedFloat),
                                    hostAction("str".rs, computedString),
                                )
                            )
                )
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth().height(60.rdp).verticalScroll(scrollState)
                ) {
                    RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(180.rdp))
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithContentDescription("dispatch").performClick()
        rule.waitForIdle()

        assertThat(namedActions)
            .containsExactly("item_1" to 5, "float" to 1.5f, "str" to "v=1")
            .inOrder()
        namedActions.clear()

        rule.onNodeWithContentDescription("increment").performClick()
        rule.onNodeWithTag("player").performTouchInput {
            swipeUp(
                startY = bottom - 5f * density,
                endY = bottom - 35f * density,
                durationMillis = 120,
            )
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("dispatch").performClick()
        rule.waitForIdle()

        assertThat(namedActions.map { it.first })
            .containsExactly("item_2", "float", "str")
            .inOrder()
        assertThat(namedActions[0].second).isEqualTo(8)
        assertThat(namedActions[1].second as Float).isGreaterThan(3.0f)
        assertThat(namedActions[2].second).isEqualTo("v=2")
    }

    @Test
    fun click_valueExpressionChangeAction_evaluatesComputedGraphExpressions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val sizePx = 120f * density
        val namedActions = mutableListOf<Pair<String, Any?>>()

        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createRemoteCreationDisplayInfo(context, Size(sizePx, sizePx)),
            onNamedAction = { name, value, _ -> namedActions.add(name to value) },
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(120.dp).testTag("player")) { content() }
            },
        ) {
            val count = remember { MutableRemoteInt(1) }
            val savedInt = remember { MutableRemoteInt(0) }
            val savedFloat = remember { MutableRemoteFloat(0f) }
            val scrollState = rememberRemoteScrollState(notches = 0)
            val derivedInt = count * 10.ri + 5.ri
            val derivedFloat = count.toRemoteFloat() * 2.5f.rf + scrollState.positionState

            RemoteColumn(modifier = RemoteModifier.size(120.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(25.rdp)
                            .semantics { contentDescription = "increment".rs }
                            .clickable(valueChangeAction(count, count + 1.ri))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(25.rdp)
                            .semantics { contentDescription = "save".rs }
                            .clickable(
                                combinedAction(
                                    valueChangeAction(savedInt, derivedInt),
                                    valueChangeAction(savedFloat, derivedFloat),
                                )
                            )
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(25.rdp)
                            .semantics { contentDescription = "read".rs }
                            .clickable(
                                combinedAction(
                                    hostAction("saved_int".rs, savedInt),
                                    hostAction("saved_float".rs, savedFloat),
                                )
                            )
                )
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth().height(45.rdp).verticalScroll(scrollState)
                ) {
                    RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(150.rdp))
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithContentDescription("increment").performClick()
        rule.onNodeWithContentDescription("increment").performClick()
        rule.onNodeWithTag("player").performTouchInput {
            swipeUp(
                startY = bottom - 5f * density,
                endY = bottom - 30f * density,
                durationMillis = 120,
            )
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("save").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("read").performClick()
        rule.waitForIdle()

        assertThat(namedActions).hasSize(2)
        assertThat(namedActions[0]).isEqualTo("saved_int" to 35)
        assertThat(namedActions[1].first).isEqualTo("saved_float")
        assertThat(namedActions[1].second as Float).isGreaterThan(7.5f)
    }

    @Test
    fun click_hostIdAction_readsComputedMetadataFromGraphContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val sizePx = 100f * density
        val idActions = mutableListOf<Pair<Int, String?>>()

        val captured = runBlocking {
            captureSingleRemoteDocument(
                context = context,
                creationDisplayInfo =
                    createRemoteCreationDisplayInfo(context, Size(sizePx, sizePx)),
            ) {
                val count = remember { MutableRemoteInt(1) }
                val metadata = "count=".rs + count.toRemoteString()
                RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth()
                                .height(40.rdp)
                                .semantics { contentDescription = "increment".rs }
                                .clickable(valueChangeAction(count, count + 1.ri))
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth()
                                .height(40.rdp)
                                .semantics { contentDescription = "send_id".rs }
                                .clickable(hostAction(id = 42, metadata = metadata))
                    )
                }
            }
        }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    capturedDocument = captured,
                    onAction = { id, metadata -> idActions.add(id to metadata) },
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("increment").performClick()
        rule.onNodeWithContentDescription("send_id").performClick()
        rule.waitForIdle()

        assertThat(idActions).containsExactly(42 to "count=2")
    }
}
