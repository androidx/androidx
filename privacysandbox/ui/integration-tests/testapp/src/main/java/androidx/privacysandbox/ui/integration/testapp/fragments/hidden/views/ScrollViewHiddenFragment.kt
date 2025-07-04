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

package androidx.privacysandbox.ui.integration.testapp.fragments.hidden.views

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.integration.testapp.R
import androidx.privacysandbox.ui.integration.testapp.fragments.hidden.AbstractScrollHiddenFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// OptIn calling the experimental API SandboxedSdkView#orderProviderUiAboveClientUi
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
class ScrollViewHiddenFragment : AbstractScrollHiddenFragment() {
    private lateinit var sandboxedSdkView: SandboxedSdkView
    private lateinit var inflatedView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_scroll_hidden, container, false)
        sandboxedSdkView = inflatedView.findViewById(R.id.scrollableRemoteView)
        fillScrollView()
        sandboxedSdkView.orderProviderUiAboveClientUi(providerUiOnTop)
        sandboxedSdkView.setEventListener(eventListener)
        return inflatedView
    }

    internal fun fillScrollView() {
        val scrollViewContent: LinearLayout = inflatedView.findViewById(R.id.scrollViewContent)
        for (i in 0..19) {
            val textView = TextView(requireActivity())
            textView.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 200)
            textView.text = "ClientItem " + (i + 1)
            textView.setBackgroundColor(
                Color.rgb(
                    (Math.random() * 256).toInt(),
                    (Math.random() * 256).toInt(),
                    (Math.random() * 256).toInt(),
                )
            )
            textView.gravity = Gravity.CENTER
            scrollViewContent.addView(textView)
        }
    }

    override fun loadAd(automatedTestCallbackBundle: Bundle) {
        sandboxedSdkView.setEventListener(eventListener)
        CoroutineScope(Dispatchers.Main).launch {
            sandboxedSdkView.setAdapter(buildAdapter(automatedTestCallbackBundle))
        }
    }
}
