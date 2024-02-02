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

package androidx.compose.ui.window

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.test.fail

internal class ViewControllerBasedLifecycleOwner : LifecycleOwner {
    private enum class Action {
        VIEW_WILL_APPEAR,
        VIEW_DID_DISAPPEAR,
        APPLICATION_DID_ENTER_BACKGROUND,
        APPLICATION_WILL_ENTER_FOREGROUND,
        DISPOSE

        // TODO: add actions for Popup and Dialog to behave like Android
    }

    private sealed interface State {
        fun reduce(action: Action): State

        class Created(
            private val isApplicationForeground: Boolean,
            private val lifecycle: LifecycleRegistry
        ) : State {
            override fun reduce(action: Action): State {
                return when (action) {
                    Action.VIEW_WILL_APPEAR -> {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

                        if (isApplicationForeground) {
                            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                            Running(lifecycle = lifecycle)
                        } else {
                            Suspended(lifecycle = lifecycle)
                        }
                    }

                    Action.VIEW_DID_DISAPPEAR -> {
                        if (isApplicationForeground) {
                            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                        }

                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

                        Created(isApplicationForeground = isApplicationForeground, lifecycle = lifecycle)
                    }

                    Action.APPLICATION_DID_ENTER_BACKGROUND -> {
                        Created(isApplicationForeground = false, lifecycle = lifecycle)
                    }

                    Action.APPLICATION_WILL_ENTER_FOREGROUND -> {
                        Created(isApplicationForeground = true, lifecycle = lifecycle)
                    }

                    Action.DISPOSE -> {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

                        Disposed
                    }
                }
            }
        }
        class Running(
            private val lifecycle: LifecycleRegistry
        ) : State {
            override fun reduce(action: Action): State {
                return when (action) {
                    Action.VIEW_WILL_APPEAR -> {
                        this
                    }

                    Action.VIEW_DID_DISAPPEAR -> {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

                        Created(isApplicationForeground = true, lifecycle = lifecycle)
                    }

                    Action.APPLICATION_DID_ENTER_BACKGROUND -> {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

                        Suspended(lifecycle = lifecycle)
                    }

                    Action.APPLICATION_WILL_ENTER_FOREGROUND -> {
                        this
                    }

                    Action.DISPOSE -> {
                        logWarning("'ViewControllerBasedLifecycleOwner' received 'Action.DISPOSE' while in 'State.Running'. Make sure that view controller containment API is used correctly. 'removeFromParent' must be called before 'dispose'")

                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

                        Disposed
                    }
                }
            }
        }

        class Suspended(private val lifecycle: LifecycleRegistry) : State {
            override fun reduce(action: Action): State {
                return when(action) {
                    Action.VIEW_WILL_APPEAR -> {
                        this
                    }

                    Action.VIEW_DID_DISAPPEAR -> {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

                        Created(isApplicationForeground = false, lifecycle = lifecycle)
                    }

                    Action.APPLICATION_DID_ENTER_BACKGROUND -> {
                        this
                    }

                    Action.APPLICATION_WILL_ENTER_FOREGROUND -> {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                        Running(lifecycle = lifecycle)
                    }

                    Action.DISPOSE -> {
                        logWarning("'ViewControllerBasedLifecycleOwner' received 'Action.DISPOSE' while in 'State.Suspended'. Make sure that view controller containment API is used correctly. 'removeFromParent' must be called before 'dispose'")

                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

                        Disposed
                    }
                }
            }
        }

        object Disposed : State {
            override fun reduce(action: Action): State {
                when (action) {
                    Action.VIEW_WILL_APPEAR, Action.VIEW_DID_DISAPPEAR, Action.DISPOSE -> {
                        fail("Invalid '$action' for 'State.Disposed'")
                    }
                    Action.APPLICATION_DID_ENTER_BACKGROUND, Action.APPLICATION_WILL_ENTER_FOREGROUND -> {
                        // no-op
                        return this
                    }
                }
            }
        }
    }

    override val lifecycle = LifecycleRegistry(this)

    private var state: State = State.Created(
        isApplicationForeground = ApplicationStateListener.isApplicationActive,
        lifecycle = lifecycle
    )

    private val applicationStateListener = ApplicationStateListener { isForeground ->
        handleAction(
            if (isForeground) Action.APPLICATION_WILL_ENTER_FOREGROUND
            else Action.APPLICATION_DID_ENTER_BACKGROUND
        )
    }

    init {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun dispose() {
        handleAction(Action.DISPOSE)
        applicationStateListener.dispose()
    }

    fun handleViewWillAppear() {
        handleAction(Action.VIEW_WILL_APPEAR)
    }

    fun handleViewDidDisappear() {
        handleAction(Action.VIEW_DID_DISAPPEAR)
    }

    private fun handleAction(action: Action) {
        state = state.reduce(action)
    }

    companion object {
        fun logWarning(message: String) {
            println("Warning: ViewControllerBasedLifecycleOwner - $message")
        }
    }
}