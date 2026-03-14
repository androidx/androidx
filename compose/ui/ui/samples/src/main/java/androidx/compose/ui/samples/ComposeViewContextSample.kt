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

package androidx.compose.ui.samples

import android.content.Context
import android.view.View
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ComposeViewContext
import androidx.compose.ui.platform.ExperimentalComposeViewContextApi
import androidx.compose.ui.platform.findViewTreeComposeViewContext

@OptIn(ExperimentalComposeViewContextApi::class)
@Sampled
fun ComposeViewContextPrewarmSample() {
    // The developer will call this when the user is expected to need to see this content within
    // a few frames. It allows the content to be composed before it is shown. When it is attached
    // to the hierarchy, the composition will have been complete, so it only needs to be laid out
    // and drawn.
    fun prefetchComposeViewContent(
        context: Context,
        composeViewContext: ComposeViewContext,
    ): ComposeView {
        val composeView = ComposeView(context)
        composeView.setContent {
            // Sample content
            Text("This is composed without being attached to hierarchy")
        }
        // Start composing right now
        composeView.createComposition(composeViewContext)
        // The composeView can be attached to the hierarchy when it is needed.
        return composeView
    }
}

@OptIn(ExperimentalComposeViewContextApi::class)
@Sampled
fun ComposeViewContextUnattachedSample(attachedView: View) {
    val composeView = ComposeView(attachedView.context)
    composeView.setContent { Box(Modifier.fillMaxSize()) }
    // If a ComposeViewContext hasn't been attached to the hierarchy, create a new one for this view
    val composeViewContext =
        attachedView.findViewTreeComposeViewContext() ?: ComposeViewContext(attachedView)
    // start composing while composeView isn't attached
    composeView.createComposition(composeViewContext)
}
