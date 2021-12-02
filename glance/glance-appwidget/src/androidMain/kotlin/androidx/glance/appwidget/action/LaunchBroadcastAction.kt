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

internal sealed interface LaunchBroadcastReceiverAction : Action

internal class LaunchBroadcastReceiverActionAction(
    val action: String,
    val componentName: ComponentName? = null,
) : LaunchBroadcastReceiverAction

internal class LaunchBroadcastReceiverComponentAction(
    val componentName: ComponentName,
) : LaunchBroadcastReceiverAction

internal class LaunchBroadcastReceiverClassAction(
    val receiverClass: Class<out BroadcastReceiver>,
) : LaunchBroadcastReceiverAction

internal class LaunchBroadcastReceiverIntentAction(
    val intent: Intent,
) : LaunchBroadcastReceiverAction

/**
 * Creates an [Action] that launches the [BroadcastReceiver] specified by the given action.
 *
 * @param action of the BroadcastReceiver to launch
 * @param componentName optional [ComponentName] of the target BroadcastReceiver
 */
public fun actionLaunchBroadcastReceiver(
    action: String,
    componentName: ComponentName? = null
): Action = LaunchBroadcastReceiverActionAction(action, componentName)

/**
 * Creates an [Action] that launches a [BroadcastReceiver] from the given [Intent] when triggered.
 * The intent should specify a component with [Intent.setClass] or [Intent.setComponent].
 *
 * @param intent the [Intent] used to launch the [BroadcastReceiver]
 */
public fun actionLaunchBroadcastReceiver(intent: Intent): Action =
    LaunchBroadcastReceiverIntentAction(intent)

/**
 * Creates an [Action] that launches the [BroadcastReceiver] specified by the given [ComponentName].
 *
 * @param componentName component of the [BroadcastReceiver] to launch
 */
public fun actionLaunchBroadcastReceiver(componentName: ComponentName): Action =
    LaunchBroadcastReceiverComponentAction(componentName)

/**
 * Creates an [Action] that launches the specified [BroadcastReceiver] when triggered.
 *
 * @param receiver class of the [BroadcastReceiver] to launch
 */
public fun <T : BroadcastReceiver> actionLaunchBroadcastReceiver(receiver: Class<T>): Action =
    LaunchBroadcastReceiverClassAction(receiver)

/**
 * Creates an [Action] that launches the specified [BroadcastReceiver] when triggered.
 */
@Suppress("MissingNullability") // Shouldn't need to specify @NonNull. b/199284086
public inline fun <reified T : BroadcastReceiver> actionLaunchBroadcastReceiver(): Action =
    actionLaunchBroadcastReceiver(T::class.java)
