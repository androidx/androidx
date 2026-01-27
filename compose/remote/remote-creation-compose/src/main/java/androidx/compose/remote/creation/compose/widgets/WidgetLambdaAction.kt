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

package androidx.compose.remote.creation.compose.widgets

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.state.RemoteStateScope

/** Keep track of lambda to be run when the corresponding id is sent back by the widget */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WidgetLambdaAction(public val widgetId: Int, public val content: () -> Unit) : Action {

    override fun RemoteStateScope.toRemoteAction():
        androidx.compose.remote.creation.actions.Action {
        val actionId = widgetId * 1000 + counter
        val action = HostAction(actionId, -1) // metadata)
        map[actionId] = this@WidgetLambdaAction
        counter++
        return action
    }

    public companion object {
        public var counter: Int = 0

        public fun clear() {
            counter = 0
            map.clear()
        }

        @SuppressLint("PrimitiveInCollection")
        public val map: HashMap<Int, WidgetLambdaAction> = HashMap<Int, WidgetLambdaAction>()

        public fun run(id: Int) {
            if (map.contains(id)) {
                map[id]?.content?.invoke()
            }
        }
    }
}
