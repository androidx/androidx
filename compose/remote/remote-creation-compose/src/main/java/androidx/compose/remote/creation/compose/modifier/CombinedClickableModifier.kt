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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.ui.semantics.Role

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.combinedClickable(
    onClick: Action? = null,
    onLongClick: Action? = null,
    onDoubleClick: Action? = null,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): RemoteModifier =
    then(
            if (enabled) {
                var modifier: RemoteModifier = RemoteModifier

                fun Action?.getActions(): List<Action> =
                    when (this) {
                        is CombinedAction -> actions.toList()
                        else -> listOfNotNull(this)
                    }

                val onClickActions = onClick.getActions()
                if (onClickActions.isNotEmpty()) {
                    modifier =
                        modifier.then(
                            ClickableModifier(
                                onClickActions,
                                clickType = MultiClickModifier.CLICK_TYPE_SINGLE,
                            )
                        )
                }

                val onLongClickActions = onLongClick.getActions()
                if (onLongClickActions.isNotEmpty()) {
                    modifier =
                        modifier.then(
                            ClickableModifier(
                                onLongClickActions,
                                clickType = MultiClickModifier.CLICK_TYPE_LONG,
                            )
                        )
                }

                val onDoubleClickActions = onDoubleClick.getActions()
                if (onDoubleClickActions.isNotEmpty()) {
                    modifier =
                        modifier.then(
                            ClickableModifier(
                                onDoubleClickActions,
                                clickType = MultiClickModifier.CLICK_TYPE_DOUBLE,
                            )
                        )
                }

                modifier
            } else {
                RemoteModifier
            }
        )
        .then(
            if (role != null)
                RemoteModifier.semantics {
                    this.role = role
                    this.enabled = enabled
                }
            else RemoteModifier
        )
