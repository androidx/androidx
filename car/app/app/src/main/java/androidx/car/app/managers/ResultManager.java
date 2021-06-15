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

package androidx.car.app.managers;

import android.content.ComponentName;
import android.content.Intent;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;

/**
 * Manages 'for result' app workflow.
 *
 * <p>This is used internally by the library and its functionality is exposed through
 * {@link androidx.car.app.CarContext}
 *
 * @hide
 */
@MainThread
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ResultManager extends Manager {
    /**
     * Sets the result of this car app.
     *
     * @see CarContext#setCarAppResult(int, Intent) for more details
     */
    void setCarAppResult(int resultCode, @Nullable Intent data);

    /**
     * Return the component (service or activity) that invoked this car app.
     *
     * <p>This is who the data in {@link #setCarAppResult(int, Intent)} will be sent to.
     *
     * @see CarContext#getCallingComponent() for more details
     */
    @Nullable
    ComponentName getCallingComponent();

    /**
     * Returns a platform-dependant instance of {@link ResultManager}.
     *
     * @throws IllegalStateException if none of the supported classes are found or if a supported
     *                               class was found but the constructor was mismatched
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    static ResultManager create() throws IllegalStateException {
        ResultManager manager = Manager.create(ResultManager.class,
                "androidx.car.app.activity.ResultManagerAutomotive");

        if (manager == null) {
            throw new IllegalStateException("Unable to instantiate " + ResultManager.class
                    + ". Did you forget to add a dependency on app-automotive artifact?");
        }

        return manager;
    }
}
