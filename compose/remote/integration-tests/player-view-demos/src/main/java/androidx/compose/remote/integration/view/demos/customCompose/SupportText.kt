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

package androidx.compose.remote.integration.view.demos.customCompose

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.custom.ComposeCustomSupport
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@SuppressLint("RestrictedApiAndroidX")
public object SupportText {
    public const val PROP_TEXT: Int = 1
    public const val PROP_TEXT_COLOR: Int = 2
    public const val PROP_TEXT_SIZE: Int = 3
    public const val PROP_BACKGROUND_COLOR: Int = 4

    @SuppressLint("RestrictedApiAndroidX")
    @Composable
    public fun Content(state: ComposeCustomSupport.ComponentState, remoteContext: RemoteContext?) {
        val text = state.stringProps[PROP_TEXT] ?: ""
        val textColor = state.intProps[PROP_TEXT_COLOR]?.let { Color(it) } ?: Color.Unspecified
        val textSize =
            state.floatProps[PROP_TEXT_SIZE] ?: (state.intProps[PROP_TEXT_SIZE]?.toFloat() ?: 14f)
        val bgColor = state.intProps[PROP_BACKGROUND_COLOR]?.let { Color(it) } ?: Color.Transparent

        Box(
            modifier = Modifier.background(bgColor).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = textColor, fontSize = textSize.sp)
        }
    }
}
