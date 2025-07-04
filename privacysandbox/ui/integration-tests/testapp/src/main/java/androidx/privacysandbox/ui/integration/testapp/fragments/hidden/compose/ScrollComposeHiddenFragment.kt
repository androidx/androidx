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

package androidx.privacysandbox.ui.integration.testapp.fragments.hidden.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.ui.client.compose.SandboxedSdkUi
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.testapp.fragments.hidden.AbstractScrollHiddenFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// OptIn calling the experimental API SandboxedSdkUi(SandboxedUiAdapter, Modifier,
// SandboxedSdkViewEventListener?, Boolean)
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
class ScrollComposeHiddenFragment : AbstractScrollHiddenFragment() {

    private var adapter: SandboxedUiAdapter? by mutableStateOf(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply { setContent { ScrollList() } }
    }

    @Composable
    fun ScrollList() {
        val items = remember {
            List(20) { index ->
                "ClientItem ${index + 1}" to
                    Color(
                        red = (0..255).random(),
                        green = (0..255).random(),
                        blue = (0..255).random(),
                    )
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val modifier = Modifier.Companion.fillMaxWidth().height(200.dp)

            Box(modifier) {
                if (adapter == null) {
                    return
                }

                SandboxedSdkUi(
                    adapter!!,
                    modifier,
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener,
                )
            }

            items.forEach { (text, color) ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(50.dp).background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    override fun loadAd(automatedTestCallbackBundle: Bundle) {
        val coroutineScope = MainScope()
        coroutineScope.launch { adapter = buildAdapter(automatedTestCallbackBundle) }
    }
}
