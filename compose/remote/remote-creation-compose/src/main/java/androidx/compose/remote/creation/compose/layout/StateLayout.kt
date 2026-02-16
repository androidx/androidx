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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rememberRemoteIntValue
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.compose.v2.StateLayoutV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.util.fastForEach
import kotlin.enums.enumEntries

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("PrimitiveInCollection")
public class StateMachineSpec(public val currentState: RemoteInt, public var states: List<Int>) {

    public fun size(): Int {
        return states.size
    }
}

@RemoteComposable
@Composable
public fun rememberStateMachine(vararg states: Int): StateMachineSpec {
    val currentState = rememberRemoteIntValue { 0 }
    val stateMachine = remember { StateMachineSpec(currentState, states.sorted()) }
    return stateMachine
}

@RemoteComposable
@Composable
public fun rememberStateMachine(currentState: RemoteInt, vararg states: Int): StateMachineSpec {
    val stateMachine = remember { StateMachineSpec(currentState, states.sorted()) }
    return stateMachine
}

@RemoteComposable
@Composable
public inline fun <reified T : Enum<T>> rememberStateMachine(
    currentState: RemoteInt
): StateMachineSpec {
    val stateMachine = remember {
        StateMachineSpec(currentState, enumEntries<T>().indices.toList())
    }
    return stateMachine
}

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeStateLayoutModifier(
    public var modifier: RemoteModifier,
    public var horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.Start,
    public var verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Top,
    public var currentState: RemoteInt,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.startStateLayout(
                canvas.toRecordingModifier(modifier),
                with(canvas) { currentState.id },
            )
            this@draw.drawContent()
            canvas.document.endStateLayout()
        }
    }
}

@RemoteComposable
@Composable
public fun StateLayout(
    stateMachine: StateMachineSpec,
    modifier: RemoteModifier = RemoteModifier,
    horizontalAlignment: RemoteAlignment.Horizontal = RemoteAlignment.CenterHorizontally,
    verticalArrangement: RemoteArrangement.Vertical = RemoteArrangement.Center,
    content: @Composable (Int) -> Unit,
) {
    if (currentComposer.applier is RemoteComposeApplierV2) {
        StateLayoutV2(stateMachine, modifier, content)
        return
    }
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    Box(
        RemoteComposeStateLayoutModifier(
                modifier,
                horizontalAlignment,
                verticalArrangement,
                stateMachine.currentState,
            )
            .then(modifier.toComposeUiLayout())
    ) {
        stateMachine.states.fastForEach { state -> content(state) }
    }
}
