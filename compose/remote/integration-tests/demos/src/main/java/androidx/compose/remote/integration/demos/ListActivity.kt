/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.demos

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.integration.demos.widget.listWidget
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Suppress("RestrictedApiAndroidX")
class ListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val bytes = listWidget(applicationContext, "ListActivity")

            val frameLayout =
                FrameLayout(this@ListActivity).apply {
                    setBackgroundColor(Color.LightGray.toArgb())
                    setPadding(20, 200, 20, 200)
                }

            val player =
                RemoteComposePlayer(this@ListActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setDocument(bytes)
                }

            frameLayout.addView(player)

            setContentView(frameLayout)
        }
    }
}

@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
fun ScrollableList(name: String, modifier: RemoteModifier = RemoteModifier) {
    val scrollState = rememberRemoteScrollState()
    RemoteColumn(
        modifier = modifier.verticalScroll(scrollState).background(Color.Yellow.rc),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        repeat(50) {
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .height(96.rdp)
                        .border(1.rdp, Color.LightGray.rc)
                        .clickable(HostAction("abc".rs))
                        // Must be direct child of the scrollable item
                        .semantics(mergeDescendants = true) {},
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(
                    if (it == 0) name else "Item $it",
                    color = RemoteColor(Color.Black),
                    fontSize = 36.rsp,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Preview
@Composable
fun ScrollableListPreview() {
    RemotePreview { ScrollableList("ScrollableListPreview") }
}
