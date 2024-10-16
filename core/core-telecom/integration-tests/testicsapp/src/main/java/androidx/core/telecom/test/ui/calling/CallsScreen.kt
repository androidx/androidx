/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.test.ui.calling

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.core.telecom.test.R
import androidx.core.telecom.test.services.AudioRoute
import androidx.core.telecom.test.services.CallState
import androidx.core.telecom.test.services.CallType
import androidx.core.telecom.test.services.Direction
import androidx.core.telecom.test.ui.calling.OngoingCallsViewModel.Companion.UnknownAudioUiState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The main screen of the application, which allows the user to view and manage ongoing calls on the
 * device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    context: Context = LocalContext.current,
    ongoingCallsViewModel: OngoingCallsViewModel,
    onShowAudioRouting: () -> Unit,
    onMoveToSettings: () -> Unit
) {
    DisposableEffect(lifecycleOwner, context) {
        ongoingCallsViewModel.connectService(context)
        // When the effect leaves the Composition, teardown
        onDispose { ongoingCallsViewModel.disconnectService() }
    }
    val callDataState: List<CallUiState> by
        ongoingCallsViewModel
            .streamCallData(LocalContext.current)
            .collectAsStateWithLifecycle(emptyList())
    val isMuted: Boolean by
        ongoingCallsViewModel.streamMuteData().collectAsStateWithLifecycle(false)
    val currentAudioRoute: AudioEndpointUiState by
        ongoingCallsViewModel
            .streamCurrentEndpointAudioData()
            .collectAsStateWithLifecycle(UnknownAudioUiState)
    Scaffold(
        topBar = {
            TopAppBar(
                colors =
                    topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                title = { Text("Ongoing Calls") },
                actions = {
                    IconButton(onClick = onMoveToSettings) {
                        Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Settings ")
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        ServiceConnectedCallContent(
            modifier = Modifier.padding(scaffoldPadding),
            callDataState,
            isMuted,
            currentAudioRoute,
            onChangeMuteState = ongoingCallsViewModel::onChangeMuteState,
            onShowAudioRouteDialog = onShowAudioRouting
        )
    }
}

@Composable
fun ServiceConnectedCallContent(
    modifier: Modifier = Modifier,
    calls: List<CallUiState>,
    isMuted: Boolean,
    currentAudioRoute: AudioEndpointUiState,
    onChangeMuteState: (Boolean) -> Unit,
    onShowAudioRouteDialog: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (calls.isNotEmpty()) {
            DeviceStatusCard(
                isMuted,
                currentAudioRoute,
                onShowAudioRouteDialog = onShowAudioRouteDialog,
                onMuteStateChange = onChangeMuteState
            )
        }
        calls.forEach { caller -> CallCard(caller = caller) }
    }
}

@Preview(showBackground = true, wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE)
@Composable
fun CallerCard(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    name: String = "Abraham Lincoln",
    number: String = "555-1212",
    photo: Uri? = null,
    direction: Direction = Direction.INCOMING,
    callType: CallType = CallType.AUDIO,
    callState: CallState = CallState.UNKNOWN
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (photo == null) {
            Icon(
                Icons.Rounded.Face,
                modifier = Modifier.size(48.dp),
                contentDescription = "Caller Icon"
            )
        } else {
            val context = LocalContext.current
            val bitmap: Bitmap by remember {
                mutableStateOf(
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, photo)
                    )
                )
            }
            Image(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = "Caller Icon"
            )
        }
        Column(modifier = Modifier.padding(6.dp)) {
            if (name.isNotEmpty()) {
                Text(text = name)
            }
            Text(text = number)
            if (isExpanded) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Text(
                        text =
                            when (callType) {
                                CallType.AUDIO -> "Audio Call"
                                CallType.VIDEO -> "Video Call"
                            }
                    )
                    VerticalDivider(modifier = Modifier.padding(horizontal = 6.dp))
                    Text(
                        text =
                            when (direction) {
                                Direction.INCOMING -> "Incoming"
                                Direction.OUTGOING -> "Outgoing"
                            }
                    )
                    VerticalDivider(modifier = Modifier.padding(horizontal = 6.dp))
                    Text(
                        text =
                            when (callState) {
                                CallState.UNKNOWN -> "Unknown"
                                CallState.INCOMING -> "Incoming"
                                CallState.DIALING -> "Dialing"
                                CallState.ACTIVE -> "Active"
                                CallState.HELD -> "Held"
                                CallState.DISCONNECTING -> "Disconnecting"
                                CallState.DISCONNECTED -> "Disconnected"
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(
    isMuted: Boolean = false,
    currentAudioRoute: AudioEndpointUiState,
    onMuteStateChange: (Boolean) -> Unit,
    onShowAudioRouteDialog: () -> Unit,
) {
    ElevatedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        modifier = Modifier.fillMaxWidth().padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
        ) {
            Text("Device State")
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Global Mute:")
                OutlinedIconButton(onClick = { onMuteStateChange(!isMuted) }) {
                    if (!isMuted) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = "device unmuted globally"
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.mic_off_24px),
                            contentDescription = "device muted globally"
                        )
                    }
                }
                Text("Current Audio Route:")
                OutlinedIconButton(
                    enabled = currentAudioRoute.audioRoute != AudioRoute.UNKNOWN,
                    onClick = onShowAudioRouteDialog
                ) {
                    Icon(
                        painter =
                            painterResource(getResourceForAudioRoute(currentAudioRoute.audioRoute)),
                        contentDescription = "current audio route"
                    )
                }
            }
        }
    }
}

@Composable
fun CallCard(caller: CallUiState, defaultExpandedState: Boolean = false) {
    var isExpanded by remember { mutableStateOf(defaultExpandedState) }
    val expandedColor =
        when (isExpanded) {
            true -> MaterialTheme.colorScheme.surfaceContainerHigh
            false -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    val padding =
        when (isExpanded) {
            true -> 6.dp
            false -> 12.dp
        }
    ElevatedCard(
        colors = CardDefaults.cardColors(containerColor = expandedColor),
        modifier =
            Modifier.animateContentSize()
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .padding(padding)
                .clickable { isExpanded = !isExpanded }
    ) {
        Column {
            Column(modifier = Modifier.padding(6.dp)) {
                CallerCard(
                    isExpanded = isExpanded,
                    name = caller.name,
                    number = caller.number,
                    photo = caller.photo,
                    direction = caller.direction,
                    callType = caller.callType,
                    callState = caller.state
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val isCallTransitionPossible =
                        caller.validTransition != CallStateTransition.NONE &&
                            caller.validTransition != CallStateTransition.DISCONNECT
                    if (isCallTransitionPossible) {
                        OutlinedButton(
                            onClick = { caller.onStateChanged(caller.validTransition) },
                        ) {
                            val stateTransitionText =
                                when (caller.validTransition) {
                                    CallStateTransition.UNHOLD -> "Unhold"
                                    CallStateTransition.HOLD -> "Hold"
                                    CallStateTransition.ANSWER -> "Answer"
                                    CallStateTransition.NONE -> "None"
                                    CallStateTransition.DISCONNECT -> "Disconnect"
                                }
                            Text(text = stateTransitionText)
                        }
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    OutlinedButton(
                        onClick = { caller.onStateChanged(CallStateTransition.DISCONNECT) },
                    ) {
                        val disconnectText =
                            if (caller.state == CallState.INCOMING) {
                                "Reject"
                            } else {
                                "Hangup"
                            }
                        Text(disconnectText)
                    }
                }
            }
            AnimatedVisibility(isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ExtensionsContent(
                        ExtensionUiState(caller.localCallSilenceUiState, caller.participantUiState)
                    )
                }
            }
        }
    }
}
