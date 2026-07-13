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

package androidx.compose.remote.creation.compose.action

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.actions.HostAction as CreationHostAction
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode

/**
 * Creates an [Action] that triggers a lambda on the host.
 *
 * @param content The lambda to execute.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun lambdaAction(content: () -> Unit): Action {
    // Using hashCode as recommended for Int conversion of CompositeKeyHashCode
    val actionId = currentCompositeKeyHashCode.hashCode()
    return LambdaAction(actionId, content)
}

/**
 * An [Action] that triggers a lambda on the host.
 *
 * @property actionId The unique ID of the action, derived from Compose composition key.
 * @property content The lambda to execute.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LambdaAction(public val actionId: Int, public val content: () -> Unit) :
    RemoteAction() {

    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val action = CreationHostAction(LambdaAction.actionName(actionId))
        val writerCallback = document.writerCallback
        if (writerCallback is WriterEvents) {
            writerCallback.storeLambda(actionId, content)
        } else {
            error("A WriterEvents is required for writing a LambdaAction.")
        }
        return action
    }

    public companion object {
        public const val PREFIX: String = "lambda:0x"

        public fun actionName(actionId: Int): String = PREFIX + Integer.toHexString(actionId)

        public fun parseId(name: String): Int? {
            if (!name.startsWith(PREFIX)) return null
            val hex = name.removePrefix(PREFIX)
            return try {
                hex.toLong(16).toInt()
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}
