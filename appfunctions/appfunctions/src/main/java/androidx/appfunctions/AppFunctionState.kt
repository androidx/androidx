/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions

import android.os.Build
import android.util.ArraySet
import androidx.annotation.RequiresApi
import androidx.appfunctions.metadata.AppFunctionName
import java.util.Objects

/**
 * Runtime state of an app function.
 *
 * This holds properties of an app function that can change at runtime during the app's operation,
 * such as whether the function is enabled.
 *
 * This is distinct from [androidx.appfunctions.metadata.AppFunctionMetadata], which represents the
 * metadata that remains constant until the providing package is updated. While
 * [androidx.appfunctions.metadata.AppFunctionMetadata] defines what a function is, the
 * [AppFunctionState] defines its current operational status.
 */
public class AppFunctionState
internal constructor(
    /** The [AppFunctionName] associated with this state. */
    public val functionName: AppFunctionName,

    // TODO(b/524139557): Link to Jetpack registerAppFunction API once ready.
    /**
     * Whether this app function can be executed.
     *
     * This can be false if:
     * - The app disabled the function with [AppFunctionManager.setAppFunctionEnabled].
     * - The function is disabled by default using
     *   [android.app.appfunctions.AppFunctionMetadata.PROPERTY_ENABLED_BY_DEFAULT] and was never
     *   enabled.
     * - A function without an associated [AppFunctionService] has not been registered using
     *   [android.app.appfunctions.AppFunctionManager.registerAppFunction] or has been unregistered.
     * - The process registering the function using
     *   [android.app.appfunctions.AppFunctionManager.registerAppFunction] is frozen, or the
     *   [android.content.Context] used to register it has been destroyed.
     */
    public val isEnabled: Boolean,

    /**
     * The [android.app.appfunctions.AppFunctionActivityId]s this app function is associated with.
     *
     * This will only be non-null when [android.app.appfunctions.AppFunctionMetadata.scope] equals
     * to [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_ACTIVITY]. See
     * [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_ACTIVITY] for more details.
     */
    @get:Suppress(
        // Performance optimization to avoid creating empty collections for an
        // unbound list of
        // AppFunctionStates (using null instead), and to allow indexed for-loop (using ArraySet).
        "NullableCollection",
        "ConcreteCollection",
    )
    @get:RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public val activityIds: ArraySet<android.app.appfunctions.AppFunctionActivityId>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionState

        if (functionName != other.functionName) return false
        if (isEnabled != other.isEnabled) return false
        if (activityIds != other.activityIds) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(functionName, isEnabled, activityIds)
    }

    override fun toString(): String {
        return "AppFunctionState(functionName=$functionName, isEnabled=$isEnabled, activityIds=$activityIds)"
    }
}
