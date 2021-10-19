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
import androidx.glance.GlanceModifier

/**
 * An Action defines the actions a user can take. Implementations specify what operation will be
 * performed in response to the action, eg. [actionLaunchActivity] creates an Action that launches
 * the specified [Activity].
 */
public interface Action

/**
 * Apply an [Action], to be executed in response to a user click.
 */
public fun GlanceModifier.clickable(onClick: Action): GlanceModifier =
    this.then(ActionModifier(onClick))

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ActionModifier(public val action: Action) : GlanceModifier.Element {
    override fun toString(): String {
        return "ActionModifier(action=$action)"
    }
}
