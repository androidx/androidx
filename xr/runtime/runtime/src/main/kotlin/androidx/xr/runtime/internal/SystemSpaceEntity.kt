/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/** Interface for a system-controlled SceneCore Entity that defines its own coordinate space. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SystemSpaceEntity : Entity {
    /**
     * Registers a listener to be called when the underlying space has moved or changed.
     *
     * @param listener The listener to register if non-null, else stops listening if null.
     * @param executor The executor to run the listener on. Defaults to SceneCore executor if null.
     */
    public fun setOnSpaceUpdatedListener(listener: Runnable?, executor: Executor?)
}
