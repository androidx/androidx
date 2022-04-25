/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.MutableConfig;

/**
 * Extendable builders are used to add externally defined options that can be passed to the
 * implementation being built.
 *
 * @param <T> the type being built by this builder.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ExtendableBuilder<T> {

    /**
     * Returns the underlying {@link MutableConfig} being modified by this builder.
     *
     * @return The underlying {@link MutableConfig}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    MutableConfig getMutableConfig();

    /** Creates an instance of the object that this builder produces. */
    @NonNull
    T build();
}
