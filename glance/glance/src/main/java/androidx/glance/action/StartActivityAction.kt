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

package androidx.glance.action

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.glance.ExperimentalGlanceApi

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StartActivityAction : Action {
    val parameters: ActionParameters
    val activityOptions: Bundle?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StartActivityComponentAction(
    val componentName: ComponentName,
    override val parameters: ActionParameters,
    override val activityOptions: Bundle?,
) : StartActivityAction

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StartActivityClassAction(
    val activityClass: Class<out Activity>,
    override val parameters: ActionParameters,
    override val activityOptions: Bundle?,
) : StartActivityAction

/**
 * Creates an [Action] that launches the [Activity] specified by the given [ComponentName].
 *
 * @param componentName component of the activity to launch
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 */
fun actionStartActivity(
    componentName: ComponentName,
    parameters: ActionParameters = actionParametersOf(),
): Action = StartActivityComponentAction(componentName, parameters, null)

/**
 * Creates an [Action] that launches the [Activity] specified by the given [ComponentName].
 *
 * @param componentName component of the activity to launch
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 * an activity start.
 */
@ExperimentalGlanceApi
fun actionStartActivity(
    componentName: ComponentName,
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null,
): Action = StartActivityComponentAction(componentName, parameters, activityOptions)

/**
 * Creates an [Action] that launches the specified [Activity] when triggered.
 *
 * @param activity class of the activity to launch
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 */
fun <T : Activity> actionStartActivity(
    activity: Class<T>,
    parameters: ActionParameters = actionParametersOf(),
): Action = StartActivityClassAction(activity, parameters, null)

/**
 * Creates an [Action] that launches the specified [Activity] when triggered.
 *
 * @param activity class of the activity to launch
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 * an activity start.
 */
@ExperimentalGlanceApi
fun <T : Activity> actionStartActivity(
    activity: Class<T>,
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null,
): Action = StartActivityClassAction(activity, parameters, activityOptions)

@Suppress("MissingNullability")
/* Shouldn't need to specify @NonNull. b/199284086 */
/**
 * Creates an [Action] that launches the specified [Activity] when triggered.
 *
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 */
inline fun <reified T : Activity> actionStartActivity(
    parameters: ActionParameters = actionParametersOf(),
): Action = actionStartActivity(T::class.java, parameters)

@Suppress("MissingNullability")
/* Shouldn't need to specify @NonNull. b/199284086 */
/**
 * Creates an [Action] that launches the specified [Activity] when triggered.
 *
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 * an activity start.
 */
@ExperimentalGlanceApi
inline fun <reified T : Activity> actionStartActivity(
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null,
): Action = actionStartActivity(T::class.java, parameters, activityOptions)
