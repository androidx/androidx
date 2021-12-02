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
import androidx.annotation.RestrictTo
import android.content.ComponentName

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface LaunchActivityAction : Action {
    abstract val parameters: ActionParameters
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LaunchActivityComponentAction(
    public val componentName: ComponentName,
    public override val parameters: ActionParameters
) : LaunchActivityAction

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LaunchActivityClassAction(
    public val activityClass: Class<out Activity>,
    public override val parameters: ActionParameters
) : LaunchActivityAction

/**
 * Creates an [Action] that launches the [Activity] specified by the given [ComponentName].
 *
 * @param componentName component of the activity to launch
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 */
public fun actionLaunchActivity(
    componentName: ComponentName,
    parameters: ActionParameters = actionParametersOf()
): Action = LaunchActivityComponentAction(componentName, parameters)

/**
 * Creates an [Action] that launches the specified [Activity] when triggered.
 *
 * @param activity class of the activity to launch
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 */
public fun <T : Activity> actionLaunchActivity(
    activity: Class<T>,
    parameters: ActionParameters = actionParametersOf()
): Action = LaunchActivityClassAction(activity, parameters)

@Suppress("MissingNullability") /* Shouldn't need to specify @NonNull. b/199284086 */
/**
 * Creates an [Action] that launches the specified [Activity] when triggered.
 *
 * @param parameters the parameters associated with the action. Parameter values will be added to
 * the activity intent, keyed by the parameter key name string.
 */
public inline fun <reified T : Activity> actionLaunchActivity(
    parameters: ActionParameters = actionParametersOf()
): Action = actionLaunchActivity(T::class.java, parameters)
