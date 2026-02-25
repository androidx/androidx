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

package androidx.xr.arcore.projected.testapp.tiltgesture

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ExperimentalGesturesApi
import androidx.xr.arcore.TiltGesture
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TiltGestureTrackingActivity : ComponentActivity() {
    companion object {
        private const val TAG = "TiltGestureTrackingActivity"
    }

    private lateinit var session: Session
    private val sessionInitialized = CompletableDeferred<Unit>()
    @OptIn(ExperimentalGesturesApi::class)
    private var tiltFlow by mutableStateOf<Flow<TiltGesture.State>?>(null)

    @OptIn(ExperimentalGesturesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(4000)
            tryCreateSession()
            lifecycleScope.launch {
                Log.i(TAG, "before sessionInitialized.await()")
                sessionInitialized.await()
                Log.i(TAG, "sessionInitialized.await()")
                tiltFlow = TiltGesture.detect(session = session)
            }
        }
        ComposeView(this)
            .also { setContentView(it) }
            .setContent {
                GlimmerTheme {
                    val state = tiltFlow?.collectAsState(TiltGesture.State())
                    TiltDemoApp(state = state?.value ?: TiltGesture.State())
                }
            }
    }

    @OptIn(ExperimentalGesturesApi::class)
    @Composable
    private fun TiltDemoApp(state: TiltGesture.State) {
        Column(
            modifier = Modifier.fillMaxSize().background(GlimmerTheme.colors.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { finish() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(text = "Tilt - ${state.tilt}, progress - ${state.progress}")
            }
            MessageCard(
                sender = "Lorem ipsum",
                message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                tilt = state.tilt,
            )
        }
    }

    private fun tryCreateSession() {
        Log.i(TAG, "Session.create($this)")
        when (val result = Session.create(this)) {
            is SessionCreateSuccess -> {
                session = result.session
                try {
                    val configResult =
                        session.configure(Config(deviceTracking = DeviceTrackingMode.LAST_KNOWN))
                    when (configResult) {
                        is SessionConfigureSuccess -> {
                            Log.i(TAG, "Session created successfully!!")
                        }

                        else -> {
                            Log.e(TAG, "Session creation error")
                        }
                    }
                } catch (e: UnsupportedOperationException) {
                    Log.e(TAG, "Session configuration not supported.", e)
                } finally {
                    sessionInitialized.complete(Unit)
                }
            }
            is SessionCreateApkRequired -> {
                Log.e(TAG, "Can't create session due to apk missing")
            }
            is SessionCreateUnsupportedDevice -> {
                Log.e(TAG, "Can't create session, unsupported device")
                finish()
            }

            else -> {
                Log.e(TAG, "Unexpected ${result::class.simpleName}")
            }
        }
    }
}
