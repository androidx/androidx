/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.navigation;


import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

/**
 * An interface that describes a navigation operation: destination's id, arguments and
 * {@link NavOptions}
 *
 * @hide Not ready for public
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface NavDirections {

    /**
     * Returns a destination id to navigate to.
     *
     * @return id of a destination
     */
    @IdRes
    int getDestinationId();

    /**
     * Returns arguments to pass to the destination
     */
    @Nullable
    Bundle getArguments();

    /**
     * Returns special options for this navigation operation
     */
    @Nullable
    NavOptions getOptions();
}
