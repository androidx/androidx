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

package androidx.compose.remote.player.compose.custom

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.LocalDensity

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalRemotePlayerApi::class)
@Suppress("HiddenSuperclass")
@SuppressLint("PrimitiveInCollection")
public class ComposeCustomSupport : ComposeCustomContext {
    public class ComponentState(public val componentId: Int, public var config: String) {
        public var x: Float by mutableFloatStateOf(0f)
        public var y: Float by mutableFloatStateOf(0f)
        public var width: Float by mutableFloatStateOf(300f)
        public var height: Float by mutableFloatStateOf(100f)

        public val stringProps: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, String> =
            mutableStateMapOf()
        public val intProps: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Int> =
            mutableStateMapOf()
        public val floatProps: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float> =
            mutableStateMapOf()
    }

    private val instances = mutableStateMapOf<Int, ComponentState>()
    private val delegates =
        mutableMapOf<
            String,
            @Composable
            (state: ComponentState, remoteContext: RemoteContext?) -> Unit,
        >()
    private var remoteContext: RemoteContext? = null

    public fun addDelegate(
        config: String,
        content: @Composable (state: ComponentState, remoteContext: RemoteContext?) -> Unit,
    ) {
        delegates[config] = content
    }

    override fun setRemoteContext(remoteContext: RemoteContext?) {
        this.remoteContext = remoteContext
    }

    public fun getRemoteContext(): RemoteContext? = remoteContext

    override fun setContext(context: Context?) {}

    override fun setCanvas(canvas: Canvas?) {}

    override fun createCustom(id: Int, config: String) {
        if (instances[id] == null) {
            instances[id] = ComponentState(id, config)
        } else {
            instances[id]?.config = config
        }
    }

    override fun configureCustom(id: Int, type: Int, value: String) {
        instances[id]?.stringProps?.put(type, value)
    }

    override fun configureCustom(id: Int, type: Int, value: Int) {
        instances[id]?.intProps?.put(type, value)
    }

    override fun configureCustom(id: Int, type: Int, value: Float) {
        instances[id]?.floatProps?.put(type, value)
    }

    override fun measureCustom(id: Int, bounds: FloatArray) {
        val state = instances[id] ?: return
        val maxWidth = bounds[1]
        val maxHeight = bounds[3]

        val w = if (maxWidth != Float.MAX_VALUE) maxWidth else 300f
        val h = if (maxHeight != Float.MAX_VALUE) maxHeight else 100f

        state.width = w
        state.height = h

        bounds[0] = 0f
        bounds[1] = 0f
        bounds[2] = w
        bounds[3] = h
    }

    public fun updateBounds(id: Int, x: Float, y: Float) {
        instances[id]?.let { state ->
            state.x = x
            state.y = y
        }
    }

    override fun layoutCustom(id: Int, bounds: FloatArray) {
        instances[id]?.let { state ->
            state.width = bounds[2]
            state.height = bounds[3]
        }
    }

    override fun touchCustom(id: Int, type: Int, x: Float, y: Float): Boolean = false

    override fun drawCustom(id: Int) {}

    @Composable
    public fun RenderComponents() {
        val density = LocalDensity.current
        for ((_, state) in instances) {
            val config = state.config
            val content = delegates[config]
            if (content != null) {
                Box(
                    modifier =
                        Modifier.offset(
                                x = with(density) { state.x.toDp() },
                                y = with(density) { state.y.toDp() },
                            )
                            .size(
                                width = with(density) { state.width.toDp() },
                                height = with(density) { state.height.toDp() },
                            )
                ) {
                    content(state, remoteContext)
                }
            }
        }
    }
}
