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
package androidx.compose.remote.player.core.state

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.compose.remote.player.core.platform.ThreadUtil
import androidx.compose.remote.player.core.state.PlayerState.MapScope
import androidx.compose.remote.player.core.state.PlayerState.PlayerStateMap

/**
 * Represents the state of a remote Compose player.
 *
 * This class manages the state data for a remote Compose player, allowing updates and providing
 * callbacks when the state changes. It also includes a utility method to apply the state to a
 * [StateUpdater].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListenablePlayerState(initialValues: PlayerStateMap = PlayerStateMap.empty()) :
    PlayerState {

    /** The state map for the player state. */
    private var stateMap: PlayerStateMap = initialValues

    /** The callbacks to be called when the player state is updated. */
    private val onStateUpdateCallbacks: MutableList<Runnable> = ArrayList<Runnable>()

    /** A constructor that initializes the player state with the provided values. */
    public constructor(
        init: MapScope.() -> Unit
    ) : this(PlayerStateMap(MapScope(mutableMapOf()).apply { init() }.values.toMap()))

    /** The values for the player state. */
    override val values: Map<String, RcValue>
        get() = stateMap.values

    @MainThread
    override fun updateState(edit: MapScope.() -> Unit) {
        val scope = MapScope(stateMap.values.toMutableMap())
        edit.invoke(scope)
        if (scope.values != stateMap.values) {
            stateMap = PlayerStateMap(scope.values)
            notifyCallbacks()
        }
    }

    /**
     * Add a callback to be called when the player state is updated.
     *
     * The callback will be called immediately.
     */
    @MainThread
    public fun addOnUpdateCallback(callback: Runnable) {
        ThreadUtil.ensureMainThread()

        onStateUpdateCallbacks.add(callback)
        callback.run()
    }

    /** Remove a callback to be called when the player state is updated. */
    @MainThread
    public fun removeOnUpdateCallback(callback: Runnable?) {
        ThreadUtil.ensureMainThread()
        onStateUpdateCallbacks.remove(callback)
    }

    /** Notify the callbacks that the player state has been updated. */
    private fun notifyCallbacks() {
        for (r in onStateUpdateCallbacks) {
            r.run()
        }
    }
}
