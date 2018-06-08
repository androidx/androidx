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
package androidx.wear.ambient;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.RestrictTo;

import com.google.android.wearable.compat.WearableActivityController;

import java.lang.reflect.Method;

/**
 * Provides a {@link WearableActivityController} for ambient mode control.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WearableControllerProvider {

    private static final String TAG = "WearableControllerProvider";

    private static volatile boolean sAmbientCallbacksVerifiedPresent;

    /**
     * Retrieves a {@link WearableActivityController} to use for ambient mode.
     *
     * @param activity The {@link Activity} to be associated with the Controller.
     * @param callback The {@link AmbientDelegate.AmbientCallback} for the Controller.
     * @return the platform-appropriate version of the {@link WearableActivityController}.
     */
    public WearableActivityController getWearableController(Activity activity,
            final AmbientDelegate.AmbientCallback callback) {
        SharedLibraryVersion.verifySharedLibraryPresent();

        // The AmbientCallback is an abstract class instead of an interface.
        WearableActivityController.AmbientCallback callbackBridge =
                new WearableActivityController.AmbientCallback() {
                    @Override
                    public void onEnterAmbient(Bundle ambientDetails) {
                        callback.onEnterAmbient(ambientDetails);
                    }

                    @Override
                    public void onUpdateAmbient() {
                        callback.onUpdateAmbient();
                    }

                    @Override
                    public void onExitAmbient() {
                        callback.onExitAmbient();
                    }
                };

        verifyAmbientCallbacksPresent();

        return new WearableActivityController(TAG, activity, callbackBridge);
    }

    private static void verifyAmbientCallbacksPresent() {
        if (sAmbientCallbacksVerifiedPresent) {
            return;
        }
        try {
            Method method =
                    WearableActivityController.AmbientCallback.class.getDeclaredMethod(
                            "onEnterAmbient", Bundle.class);
            // Proguard is sneaky -- it will actually rewrite strings it finds in addition to
            // function names. Therefore add a "." prefix to the method name check to ensure the
            // function was not renamed by proguard.
            if (!(".onEnterAmbient".equals("." + method.getName()))) {
                throw new NoSuchMethodException();
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Could not find a required method for "
                            + "ambient support, likely due to proguard optimization. Please add "
                            + "com.google.android.wearable:wearable jar to the list of library jars"
                            + " for your project");
        }
        sAmbientCallbacksVerifiedPresent = true;
    }
}
