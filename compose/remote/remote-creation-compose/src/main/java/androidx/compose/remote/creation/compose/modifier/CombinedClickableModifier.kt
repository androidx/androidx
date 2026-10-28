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
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.RemoteAction

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.combinedClickable(
    onClick: RemoteAction = RemoteAction.Empty,
    onLongClick: RemoteAction = RemoteAction.Empty,
    onDoubleClick: RemoteAction = RemoteAction.Empty,
    enabled: Boolean = true,
    role: RemoteRole? = RemoteRole.Button,
): RemoteModifier =
    then(
            if (enabled) {
                var modifier: RemoteModifier = RemoteModifier

                fun RemoteAction?.getActions(): List<RemoteAction> =
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
                    this.remoteRole = role
                    this.enabled = enabled
                }
            else RemoteModifier
        )
