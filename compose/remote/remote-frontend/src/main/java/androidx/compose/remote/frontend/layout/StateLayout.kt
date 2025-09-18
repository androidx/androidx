/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.toComposeUiLayout
import androidx.compose.remote.frontend.state.RemoteInt
import androidx.compose.remote.frontend.state.rememberRemoteIntValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StateMachineSpec(public var currentState: RemoteInt, public var states: IntArray) {

    public val statesNames: HashMap<String, Int> = HashMap<String, Int>()
    public val values: HashMap<String, RemoteInt> = HashMap<String, RemoteInt>()

    public operator fun component1(): Int {
        return states[0]
    }

    public operator fun component2(): Int {
        return states[1]
    }

    public operator fun component3(): Int {
        return states[2]
    }

    public operator fun component4(): Int {
        return states[3]
    }

    public operator fun component5(): Int {
        return states[4]
    }

    public operator fun component6(): Int {
        return states[5]
    }

    public operator fun component7(): Int {
        return states[6]
    }

    public operator fun component8(): Int {
        return states[7]
    }

    public operator fun component9(): Int {
        return states[8]
    }

    public operator fun component10(): Int {
        return states[9]
    }

    public operator fun component11(): Int {
        return states[10]
    }

    public operator fun component12(): Int {
        return states[11]
    }

    public fun size(): Int {
        return states.size
    }

    //
    //  @RemoteComposable
    //  @Composable
    //  fun transitionToState(name: String): Pair<OrigamiInt, OrigamiInt> {
    //    val transition by rememberOrigami { statesNames[name]!!.toOrigami() }
    //    return Pair(transition, currentState)
    //  }

    public fun nameState(name: String, state: Int) {
        for (i in 0 until states.size) {
            if (states[i] == state) {
                statesNames[name] = i
            }
        }
    }
    //
    //  fun nameValue(name: String, value: OrigamiValue<*>) {
    //    values[name] = value
    //  }
    //
    //  fun value(s: String): OrigamiValue<*> {
    //    return (values[s] as OrigamiValue<*>)
    //  }
}

@RemoteComposable
@Composable
public fun rememberStateMachine(vararg states: Int): StateMachineSpec {
    val currentState = rememberRemoteIntValue { 0 }
    val stateMachine = remember { StateMachineSpec(currentState, states.sortedArray()) }
    return stateMachine
}

@RemoteComposable
@Composable
public fun rememberStateMachine(currentState: RemoteInt, vararg states: Int): StateMachineSpec {
    val stateMachine = remember { StateMachineSpec(currentState, states.sortedArray()) }
    return stateMachine
}

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeStateLayoutModifier(
    public var modifier: RemoteModifier,
    public var horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    public var verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    public var currentState: RemoteInt,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startStateLayout(
                        modifier.toRemoteCompose(),
                        currentState.getIntId(),
                    )
                    //          it.document.startBox(
                    //            modifier.toRemoteCompose(),
                    //            horizontalAlignment.toRemoteCompose(),
                    //            verticalArrangement.toRemoteCompose(),
                    //          )
                    drawContent()
                    //          it.document.endBox()
                    it.document.endStateLayout()
                }
            }
        }
    }
}

@RemoteComposable
@Composable
public fun StateLayout(
    modifier: RemoteModifier = RemoteModifier,
    stateMachine: StateMachineSpec,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable (Int) -> Unit,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    if (captureMode is NoRemoteCompose) {
        val currentState = stateMachine.currentState
        Box(
            modifier.toComposeUi(),
            contentAlignment = boxAlignment(horizontalAlignment, verticalArrangement),
        ) {
            content(currentState.value)
        }
    } else {
        Box(
            RemoteComposeStateLayoutModifier(
                    modifier,
                    horizontalAlignment,
                    verticalArrangement,
                    stateMachine.currentState,
                )
                .then(modifier.toComposeUiLayout())
        ) {
            for (state in stateMachine.states) {
                content(state)
            }
        }
    }
}
