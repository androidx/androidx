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

package androidx.compose.remote.player.compose.embedded.demos

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking

private const val PENDING_INTENT_ACTION =
    "androidx.compose.remote.player.compose.embedded.demos.HOST_ACTION"

/**
 * Demonstrates each kind of host action, played with the embedded [RcPlayer]:
 * - named actions with no payload, and with float, int, string and boolean payloads,
 * - a named action whose name is a dynamic `RemoteString`,
 * - id actions with a stable id, with and without metadata built from remote state,
 * - a `pendingIntentAction` whose mutable `PendingIntent` is filled in with a dynamic extra.
 *
 * Tap a row to see what the host receives.
 */
@Composable
@Preview(showBackground = true)
public fun HostActionsDemo() {
    val context = LocalContext.current
    val events = remember { mutableStateListOf<String>() }
    fun log(event: String) {
        events.add(0, event)
        if (events.size > 8) events.removeAt(events.lastIndex)
    }

    // A receiver for the PendingIntent, standing in for an app's BroadcastReceiver or Activity.
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val screen = intent.getStringExtra("screen")
                    val taps = intent.getStringExtra("taps")
                    log("PendingIntent screen=$screen taps=$taps")
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PENDING_INTENT_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val pendingIntent = remember {
        PendingIntent.getBroadcast(
            context,
            0,
            // Constant extras belong in the Intent. Dynamic ones, like "taps", must not also be set
            // here, because filled in extras never replace existing ones.
            Intent(PENDING_INTENT_ACTION)
                .setPackage(context.packageName)
                .putExtra("screen", "home"),
            // Mutable so that the player can fill in the dynamic extras.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    val remoteContent: @Composable @RemoteComposable () -> Unit = {
        val taps = remember { MutableRemoteInt(0) }
        RemoteColumn(modifier = RemoteModifier.fillMaxWidth()) {
            DemoRow("Named, no payload", hostAction("refresh".rs))
            DemoRow("Named, float", hostAction("volume".rs, 0.5f.rf))
            DemoRow("Named, int", hostAction("page".rs, 3.ri))
            DemoRow("Named, string", hostAction("open".rs, "inbox".rs))
            DemoRow("Named, boolean", hostAction("toggle".rs, true.rb))
            DemoRow("Named, dynamic name", hostAction("item_".rs + 7.ri.toRemoteString()))
            DemoRow("Id 42", hostAction(id = 42))
            DemoRow(
                "Id 43 + metadata",
                combinedAction(
                    valueChange(taps, taps + 1),
                    hostAction(
                        id = 43,
                        metadata = "screen=home&taps=".rs + taps.toRemoteString(),
                    ),
                ),
            )
            DemoRow(
                "PendingIntent + metadata",
                pendingIntentAction("taps" to taps.toRemoteString()) {
                    pendingIntent
                },
            )
        }
    }

    val capturedDoc = remember {
        runBlocking { captureSingleRemoteDocument(context = context, content = remoteContent) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        RcPlayer(
            capturedDocument = capturedDoc,
            modifier = Modifier.fillMaxWidth(),
            onAction = { id, metadata ->
                log("onAction id=$id metadata=$metadata")
            },
            onNamedAction = { name, value, _ ->
                val type = value?.let { it::class.simpleName } ?: "none"
                log("onNamedAction name=$name value=$value ($type)")
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        events.forEach { Text(it) }
    }
}

@Composable
@RemoteComposable
private fun DemoRow(label: String, action: RemoteAction) {
    RemoteText(
        text = label.rs,
        modifier =
            RemoteModifier.fillMaxWidth()
                .padding(2.rdp)
                .background(Color(0xFFE3F2FD).rc)
                .clickable(action)
                .padding(12.rdp),
    )
}
