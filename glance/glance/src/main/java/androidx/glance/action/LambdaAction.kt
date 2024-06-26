/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.action

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LambdaAction(
    val key: String,
    val block: () -> Unit,
) : Action {
    override fun toString() = "LambdaAction($key, ${block.hashCode()})"
}

/**
 * Create an [Action] that runs [block] when triggered.
 *
 * @param key A stable and unique key that identifies this action. This key is saved in the
 *   PendingIntent for the UI element, and used to trigger this action when the element is clicked.
 *   If not provided we use [androidx.compose.runtime.currentCompositeKeyHash] as the key. Since
 *   that key is based on the location within the composition, it will be identical for lambdas
 *   generated in a loop (if not using [androidx.compose.runtime.key]). To avoid this, prefer
 *   setting explicit keys for your lambdas.
 * @param block the function to be run when this action is triggered.
 */
@Composable
fun action(
    key: String? = null,
    block: () -> Unit,
): Action {
    val finalKey = if (!key.isNullOrEmpty()) key else currentCompositeKeyHash.toString()
    return LambdaAction(finalKey, block)
}
