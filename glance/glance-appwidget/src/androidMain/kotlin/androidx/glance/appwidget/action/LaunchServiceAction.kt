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

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import androidx.glance.action.Action

internal sealed interface LaunchServiceAction : Action {
    val isForegroundService: Boolean
}

internal class LaunchServiceComponentAction(
    val componentName: ComponentName,
    override val isForegroundService: Boolean
) : LaunchServiceAction

internal class LaunchServiceClassAction(
    val serviceClass: Class<out Service>,
    override val isForegroundService: Boolean
) : LaunchServiceAction

internal class LaunchServiceIntentAction(
    val intent: Intent,
    override val isForegroundService: Boolean
) : LaunchServiceAction

/**
 * Creates an [Action] that launches a [Service] from the given [Intent] when triggered. The
 * intent should specify a component with [Intent.setClass] or [Intent.setComponent].
 *
 * @param intent the intent used to launch the activity
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently
 */
public fun actionLaunchService(intent: Intent, isForegroundService: Boolean = false): Action =
    LaunchServiceIntentAction(intent, isForegroundService)

/**
 * Creates an [Action] that launches the [Service] specified by the given [ComponentName].
 *
 * @param componentName component of the Service to launch
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently
 */
public fun actionLaunchService(
    componentName: ComponentName,
    isForegroundService: Boolean = false
): Action = LaunchServiceComponentAction(componentName, isForegroundService)

/**
 * Creates an [Action] that launches the specified [Service] when triggered.
 *
 * @param service class of the Service to launch
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently
 */
public fun <T : Service> actionLaunchService(
    service: Class<T>,
    isForegroundService: Boolean = false
): Action =
    LaunchServiceClassAction(service, isForegroundService)

/**
 * Creates an [Action] that launches the specified [Service] when triggered.
 *
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently.
 */
@Suppress("MissingNullability") /* Shouldn't need to specify @NonNull. b/199284086 */
public inline fun <reified T : Service> actionLaunchService(
    isForegroundService: Boolean = false
): Action = actionLaunchService(T::class.java, isForegroundService)
