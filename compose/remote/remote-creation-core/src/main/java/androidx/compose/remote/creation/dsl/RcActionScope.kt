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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.actions.ValueStringChange

/** Scope for recording interaction logic. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcActionScope {
    /** Sets a float variable to a new value. */
    public fun setValue(variable: RcFloat, value: Float)

    /** Sets an integer variable to a new value. */
    public fun setValue(variable: RcInteger, value: Int)

    /** Sets a text variable to a new value. */
    public fun setValue(variable: RcText, value: String)

    /** Sets a float variable to an expression value. */
    public fun setValue(variable: RcFloat, expression: RcFloat)

    /** Triggers a named host action. */
    public fun hostAction(name: String)
}

/** Internal implementation of [RcActionScope] that bridges to the legacy [Action] system. */
internal class RcActionScopeImpl : RcActionScope {
    private val actionBuilders = mutableListOf<(RemoteComposeWriter) -> Action>()

    override fun setValue(variable: RcFloat, value: Float) {
        actionBuilders.add { _ ->
            val id = Utils.idFromNan(variable.toFloat())
            ValueFloatChange(id, value)
        }
    }

    override fun setValue(variable: RcFloat, expression: RcFloat) {
        actionBuilders.add { _ ->
            val id = Utils.idFromNan(variable.toFloat())
            val exprId = Utils.idFromNan(expression.toFloat())
            ValueFloatExpressionChange(id, exprId)
        }
    }

    override fun setValue(variable: RcInteger, value: Int) {
        actionBuilders.add { _ ->
            val id = (variable.id % 0x100000000L).toInt()
            ValueIntegerChange(id, value)
        }
    }

    override fun setValue(variable: RcText, value: String) {
        actionBuilders.add { _ -> ValueStringChange(variable.id, value) }
    }

    override fun hostAction(name: String) {
        actionBuilders.add { _ -> HostAction(name) }
    }

    fun build(writer: RemoteComposeWriter): List<Action> {
        return actionBuilders.map { it(writer) }
    }
}
