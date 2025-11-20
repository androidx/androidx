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

package androidx.glance.wear.tiles.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action

internal class RunCallbackAction(public val callbackClass: Class<out ActionCallback>) : Action {
    companion object {

        public suspend fun run(context: Context, className: String, glanceId: GlanceId) {
            val workClass = Class.forName(className)

            if (!ActionCallback::class.java.isAssignableFrom(workClass)) {
                error("Provided class must implement ActionCallback.")
            }

            val actionCallback = workClass.getDeclaredConstructor().newInstance() as ActionCallback
            actionCallback.onAction(context, glanceId)
        }
    }
}

/**
 * A callback executed in response to the user action, before the content is updated. The
 * implementing class must have a public zero argument constructor, this is used to instantiate the
 * class at runtime.
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public interface ActionCallback {
    /**
     * Performs the work associated with this action. Called when the action is triggered.
     *
     * @param context the calling context
     * @param glanceId the [GlanceId] that triggered this action
     */
    public suspend fun onAction(context: Context, glanceId: GlanceId)
}

/**
 * Creates an [Action] that executes a given [ActionCallback] implementation
 *
 * @param callbackClass the class that implements [ActionCallback]
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun <T : ActionCallback> actionRunCallback(callbackClass: Class<T>): Action =
    RunCallbackAction(callbackClass)

/** Creates an [Action] that executes a given [ActionCallback] implementation */
@Suppress("MissingNullability") // Shouldn't need to specify @NonNull. b/199284086
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public inline fun <reified T : ActionCallback> actionRunCallback(): Action =
    actionRunCallback(T::class.java)
