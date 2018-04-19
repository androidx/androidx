/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.List;

/**
 * An abstract class that allows the user to define how to merge a list of inputs to a Worker.
 */

public abstract class InputMerger {

    private static final String TAG = "InputMerger";

    /**
     * Merges a list of {@link Data} and outputs a single Data object.
     *
     * @param inputs A list of {@link Data} from previous Workers or the WorkRequest.Builder
     * @return The merged output
     */
    public abstract @NonNull Data merge(@NonNull List<Data> inputs);

    /**
     * Instantiates an {@link InputMerger} from its class name.
     *
     * @param className The name of the {@link InputMerger} class
     * @return The instantiated {@link InputMerger}, or {@code null} if it could not be instantiated
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("ClassNewInstance")
    public static InputMerger fromClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (InputMerger) clazz.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "Trouble instantiating + " + className, e);
        }
        return null;
    }
}
