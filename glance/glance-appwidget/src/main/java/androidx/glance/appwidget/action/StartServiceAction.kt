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
import androidx.annotation.RestrictTo
import androidx.glance.action.Action

internal sealed interface StartServiceAction : Action {
    val isForegroundService: Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StartServiceComponentAction(
    val componentName: ComponentName,
    override val isForegroundService: Boolean
) : StartServiceAction

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StartServiceClassAction(
    val serviceClass: Class<out Service>,
    override val isForegroundService: Boolean
) : StartServiceAction

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StartServiceIntentAction(
    val intent: Intent,
    override val isForegroundService: Boolean
) : StartServiceAction

/**
 * Creates an [Action] that launches a [Service] from the given [Intent] when triggered. The
 * intent should specify a component with [Intent.setClass] or [Intent.setComponent].
 *
 * @param intent the intent used to launch the activity
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently
 */
fun actionStartService(intent: Intent, isForegroundService: Boolean = false): Action =
    StartServiceIntentAction(intent, isForegroundService)

/**
 * Creates an [Action] that launches the [Service] specified by the given [ComponentName].
 *
 * @param componentName component of the Service to launch
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently
 */
fun actionStartService(
    componentName: ComponentName,
    isForegroundService: Boolean = false
): Action = StartServiceComponentAction(componentName, isForegroundService)

/**
 * Creates an [Action] that launches the specified [Service] when triggered.
 *
 * @param service class of the Service to launch
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently
 */
fun <T : Service> actionStartService(
    service: Class<T>,
    isForegroundService: Boolean = false
): Action =
    StartServiceClassAction(service, isForegroundService)

/**
 * Creates an [Action] that launches the specified [Service] when triggered.
 *
 * @param isForegroundService set to true when the provided [Service] runs in foreground. This flag
 * is only used for device versions after [android.os.Build.VERSION_CODES.O] that requires
 * foreground service to be launched differently.
 */
@Suppress("MissingNullability")
/* Shouldn't need to specify @NonNull. b/199284086 */
inline fun <reified T : Service> actionStartService(
    isForegroundService: Boolean = false
): Action = actionStartService(T::class.java, isForegroundService)
