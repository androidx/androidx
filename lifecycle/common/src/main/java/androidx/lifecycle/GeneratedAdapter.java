/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.lifecycle;

import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GeneratedAdapter {

    /**
     * Called when a state transition event happens.
     *
     * @param source The source of the event
     * @param event The event
     * @param onAny approveCall onAny handlers
     * @param logger if passed, used to track called methods and prevent calling the same method
     *              twice
     */
    void callMethods(LifecycleOwner source, Lifecycle.Event event, boolean onAny,
            MethodCallsLogger logger);
}
