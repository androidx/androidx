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

package androidx.compose.remote.integration.view.demos.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.remote.creation.compose.widgets.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalRemoteCreationComposeApi::class)
@SuppressLint("RestrictedApiAndroidX")
class MyWidget : RemoteComposeWidget() {
    init {
        RemoteComposeCreationComposeFlags.isRemoteApplierEnabled = false
    }

    @RemoteComposable
    @Composable
    fun Button(text: String, modifier: RemoteModifier = RemoteModifier, onClick: () -> Unit) {
        RemoteBox(
            modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.LightGray)
                .padding(20.dp)
                .onClick(onClick),
            RemoteAlignment.CenterHorizontally,
            RemoteArrangement.Center,
        ) {
            RemoteText(text, fontSize = 32.rsp, color = RemoteColor(Color.White))
        }
    }

    @RemoteComposable
    @Composable
    override fun Content(context: Context, widgetId: Int) {
        val counter = readCounter(context, widgetId)
        RemoteRow(
            RemoteModifier.background(Color.White).fillMaxSize(),
            horizontalArrangement = RemoteArrangement.CenterHorizontally,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            Button("-", RemoteModifier.weight(1f)) { writeCounter(context, widgetId, -1) }
            RemoteText("$counter", fontSize = 48.rsp)
            Button("+", RemoteModifier.weight(1f)) { writeCounter(context, widgetId, 1) }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Data management
    ///////////////////////////////////////////////////////////////////////////////////////////////

    val COUNTER = "counter"

    fun getDataStore(context: Context, widgetId: Int): SharedPreferences {
        return context.getSharedPreferences("WIDGET_$widgetId", Context.MODE_PRIVATE)
    }

    fun readCounter(context: Context, widgetId: Int): Int {
        return getDataStore(context, widgetId).getInt(COUNTER, 0)
    }

    fun writeCounter(context: Context, widgetId: Int, value: Int) {
        val store = getDataStore(context, widgetId)
        val counter = store.getInt(COUNTER, 0)
        with(store.edit()) {
            putInt(COUNTER, counter + value)
            apply()
        }
    }
}
