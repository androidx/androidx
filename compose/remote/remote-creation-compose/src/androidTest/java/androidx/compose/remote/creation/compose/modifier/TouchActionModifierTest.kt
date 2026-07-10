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

package androidx.compose.remote.creation.compose.modifier

import android.view.View
import android.view.ViewGroup
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.RemoteInteractionTestRule
import androidx.compose.remote.player.view.platform.RemoteComposeView
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class TouchActionModifierTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteInteractionTestRule(ApplicationProvider.getApplicationContext())

    @Test
    fun handlesTouchDown() {
        val action = HostAction(DOWN_ACTION.rs, ACTION_VALUE.rs)
        runActionTest(modifier = RemoteModifier.onTouchDown(action)) { element -> element.click() }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair(DOWN_ACTION, ACTION_VALUE))
    }

    @Test
    fun handlesTouchUp() {
        val simpleDownAction = HostAction(DOWN_ACTION.rs, ACTION1_VALUE.rs)
        val upAction = HostAction(UP_ACTION.rs, ACTION2_VALUE.rs)
        // b/500282418: it should not require onTouchDown
        runActionTest(
            modifier = RemoteModifier.onTouchDown(simpleDownAction).onTouchUp(upAction)
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair(DOWN_ACTION, ACTION1_VALUE), Pair(UP_ACTION, ACTION2_VALUE))
    }

    @Test
    fun handlesCombinedTouchDown() {
        val action1 = HostAction(DOWN_ACTION.rs, ACTION1_VALUE.rs)
        val action2 = HostAction(DOWN_ACTION.rs, ACTION2_VALUE.rs)
        runActionTest(modifier = RemoteModifier.onTouchDown(CombinedAction(action1, action2))) {
            element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair(DOWN_ACTION, ACTION1_VALUE), Pair(DOWN_ACTION, ACTION2_VALUE))
    }

    @Test
    fun handlesCombinedTouchUp() {
        val simpleDownAction = HostAction(DOWN_ACTION.rs, ACTION_VALUE.rs)
        val action1 = HostAction(UP_ACTION.rs, ACTION1_VALUE.rs)
        val action2 = HostAction(UP_ACTION.rs, ACTION2_VALUE.rs)
        // b/500282418: it should not require onTouchDown
        runActionTest(
            modifier =
                RemoteModifier.onTouchDown(simpleDownAction)
                    .onTouchUp(CombinedAction(action1, action2))
        ) { element ->
            element.click()
        }
        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(
                Pair(DOWN_ACTION, ACTION_VALUE),
                Pair(UP_ACTION, ACTION1_VALUE),
                Pair(UP_ACTION, ACTION2_VALUE),
            )
    }

    @Test
    fun handlesTouchCancel() {
        val simpleDownAction = HostAction(DOWN_ACTION.rs, ACTION_VALUE.rs)
        val action = HostAction(CANCEL_ACTION.rs, ACTION_VALUE.rs)
        remoteComposeTestRule.setContent {
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .onTouchDown(simpleDownAction)
                        .onTouchCancel(action),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        remoteComposeTestRule.composeTestRule.waitForIdle()

        val androidRule = remoteComposeTestRule.composeTestRule as AndroidComposeTestRule<*, *>
        val activity = androidRule.activity
        val playerView = findRemoteComposeView(activity.window.decorView)!!
        val context = playerView.remoteContext
        val doc = playerView.document.document

        doc.touchDown(context, 100f, 100f)
        doc.touchCancel(context, 100f, 100f, 0f, 0f)

        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(Pair(DOWN_ACTION, ACTION_VALUE), Pair(CANCEL_ACTION, ACTION_VALUE))
    }

    @Test
    fun handlesCombinedTouchCancel() {
        val simpleDownAction = HostAction(DOWN_ACTION.rs, ACTION_VALUE.rs)
        val action1 = HostAction(CANCEL_ACTION.rs, ACTION1_VALUE.rs)
        val action2 = HostAction(CANCEL_ACTION.rs, ACTION2_VALUE.rs)
        remoteComposeTestRule.setContent {
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .onTouchDown(simpleDownAction)
                        .onTouchCancel(CombinedAction(action1, action2)),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        remoteComposeTestRule.composeTestRule.waitForIdle()

        val androidRule = remoteComposeTestRule.composeTestRule as AndroidComposeTestRule<*, *>
        val activity = androidRule.activity
        val playerView = findRemoteComposeView(activity.window.decorView)!!
        val context = playerView.remoteContext
        val doc = playerView.document.document

        doc.touchDown(context, 100f, 100f)
        doc.touchCancel(context, 100f, 100f, 0f, 0f)

        assertThat(remoteComposeTestRule.clickEvents)
            .containsExactly(
                Pair(DOWN_ACTION, ACTION_VALUE),
                Pair(CANCEL_ACTION, ACTION1_VALUE),
                Pair(CANCEL_ACTION, ACTION2_VALUE),
            )
    }

    private fun findRemoteComposeView(view: View): RemoteComposeView? {
        if (view is RemoteComposeView) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findRemoteComposeView(view.getChildAt(i))
                if (result != null) return result
            }
        }
        return null
    }

    private fun runActionTest(modifier: RemoteModifier, block: (element: UiObject2) -> Unit) {
        remoteComposeTestRule.setContent {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().then(modifier),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText(BUTTON_LABEL)
            }
        }

        uiAutomator {
            val element = onElement { text?.toString() == BUTTON_LABEL }
            block(element)
        }
    }

    companion object {
        private const val BUTTON_LABEL = "Touch me!"
        private const val DOWN_ACTION = "touchDownAction"
        private const val UP_ACTION = "touchUpAction"
        private const val CANCEL_ACTION = "touchCancelAction"
        private const val ACTION_VALUE = "actionValue"
        private const val ACTION1_VALUE = "value1"
        private const val ACTION2_VALUE = "value2"
    }
}
