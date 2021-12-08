/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.action

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import androidx.glance.action.Action

internal sealed interface StartBroadcastReceiverAction : Action

internal class StartBroadcastReceiverActionAction(
    val action: String,
    val componentName: ComponentName? = null,
) : StartBroadcastReceiverAction

internal class StartBroadcastReceiverComponentAction(
    val componentName: ComponentName,
) : StartBroadcastReceiverAction

internal class StartBroadcastReceiverClassAction(
    val receiverClass: Class<out BroadcastReceiver>,
) : StartBroadcastReceiverAction

internal class StartBroadcastReceiverIntentAction(
    val intent: Intent,
) : StartBroadcastReceiverAction

/**
 * Creates an [Action] that launches the [BroadcastReceiver] specified by the given action.
 *
 * @param action of the BroadcastReceiver to launch
 * @param componentName optional [ComponentName] of the target BroadcastReceiver
 */
public fun actionStartBroadcastReceiver(
    action: String,
    componentName: ComponentName? = null
): Action = StartBroadcastReceiverActionAction(action, componentName)

/**
 * Creates an [Action] that launches a [BroadcastReceiver] from the given [Intent] when triggered.
 * The intent should specify a component with [Intent.setClass] or [Intent.setComponent].
 *
 * @param intent the [Intent] used to launch the [BroadcastReceiver]
 */
public fun actionStartBroadcastReceiver(intent: Intent): Action =
    StartBroadcastReceiverIntentAction(intent)

/**
 * Creates an [Action] that launches the [BroadcastReceiver] specified by the given [ComponentName].
 *
 * @param componentName component of the [BroadcastReceiver] to launch
 */
public fun actionStartBroadcastReceiver(componentName: ComponentName): Action =
    StartBroadcastReceiverComponentAction(componentName)

/**
 * Creates an [Action] that launches the specified [BroadcastReceiver] when triggered.
 *
 * @param receiver class of the [BroadcastReceiver] to launch
 */
public fun <T : BroadcastReceiver> actionStartBroadcastReceiver(receiver: Class<T>): Action =
    StartBroadcastReceiverClassAction(receiver)

/**
 * Creates an [Action] that launches the specified [BroadcastReceiver] when triggered.
 */
@Suppress("MissingNullability") // Shouldn't need to specify @NonNull. b/199284086
public inline fun <reified T : BroadcastReceiver> actionStartBroadcastReceiver(): Action =
    actionStartBroadcastReceiver(T::class.java)
