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
 * The state of an activity from the perspective of app functions, retrieved using
 * [AppFunctionManager.getAppFunctionActivityStates].
 *
 * This holds which app functions are registered for a given activity, a property that can change at
 * runtime during the app's operation.
 *
 * @see AppFunctionState
 */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
public class AppFunctionActivityState
internal constructor(
    /** The [android.app.appfunctions.AppFunctionActivityId] associated with this state. */
    public val activityId: android.app.appfunctions.AppFunctionActivityId,
    /** The [AppFunctionName]s associated with this state. */
    @get:Suppress(
        // Performance optimization to allow indexed for-loop (using ArraySet).
        "ConcreteCollection"
    )
    public val functionNames: ArraySet<AppFunctionName>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionActivityState

        if (activityId != other.activityId) return false
        if (functionNames != other.functionNames) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(activityId, functionNames)
    }

    override fun toString(): String {
        return "AppFunctionActivityState(activityId=$activityId, functionNames=$functionNames)"
    }

    internal companion object {
        internal fun fromPlatformAppFunctionActivityState(
            platformAppFunctionActivityState: android.app.appfunctions.AppFunctionActivityState
        ): AppFunctionActivityState {
            val functionNames =
                platformAppFunctionActivityState.functionNames.map {
                    AppFunctionName.fromPlatformAppFunctionName(it)
                }
            return AppFunctionActivityState(
                activityId = platformAppFunctionActivityState.activityId,
                functionNames = ArraySet(functionNames),
            )
        }
    }
}
