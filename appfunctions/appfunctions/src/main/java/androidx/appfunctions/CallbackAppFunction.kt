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

import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import java.util.function.Consumer

/**
 * An interface for implementing the logic of an app function registered at runtime using
 * [AppFunctionManager.registerAppFunction].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface CallbackAppFunction {
    /**
     * Called when the app function is invoked using [AppFunctionManager.executeAppFunction].
     *
     * The implementation should try to respect the provided [CancellationSignal], if possible.
     *
     * @param request The [ExecuteAppFunctionRequest] containing the parameters for this execution.
     * @param cancellationSignal A signal to cancel the execution.
     * @param callback The [Consumer] that the implementation must use to return the result of the
     *   execution. It must be called exactly once with either a successful
     *   [ExecuteAppFunctionResponse.Success] or [ExecuteAppFunctionResponse.Error] in case of an
     *   error.
     */
    public fun execute(
        request: ExecuteAppFunctionRequest,
        cancellationSignal: CancellationSignal,
        callback: Consumer<ExecuteAppFunctionResponse>,
    )
}
