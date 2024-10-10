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
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarAppMetadataHolderService;
import androidx.car.app.CarContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages 'for result' app workflow.
 *
 * <p>This is used internally by the library and its functionality is exposed through
 * {@link androidx.car.app.CarContext}
 *
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
    @Nullable ComponentName getCallingComponent();

    /**
     * Returns a platform-dependant instance of {@link ResultManager}.
     *
     * @throws IllegalStateException if none of the supported classes are found or if a supported
     *                               class was found but the constructor was mismatched
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static @NonNull ResultManager create(@NonNull CarContext context) throws IllegalStateException {
        try {
            ServiceInfo serviceInfo = CarAppMetadataHolderService.getServiceInfo(context);
            String managerClassName = null;
            if (serviceInfo.metaData != null) {
                managerClassName = serviceInfo.metaData.getString(
                        "androidx.car.app.CarAppMetadataHolderService.RESULT_MANAGER");
            }
            if (managerClassName == null) {
                throw new ClassNotFoundException("ResultManager metadata could not be found");
            }

            Class<?> managerClass = Class.forName(managerClassName);
            return (ResultManager) managerClass.getConstructor().newInstance();
        } catch (PackageManager.NameNotFoundException | ReflectiveOperationException e) {
            throw new IllegalStateException("ResultManager not configured. Did you forget "
                    + "to add a dependency on the app-automotive artifact?");
        }
    }
}
