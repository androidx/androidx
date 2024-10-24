/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.serialization.Bundleable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A host-side interface for handling success and failure scenarios on calls to the client.
 */
@CarProtocol
public interface OnDoneCallback {
    /**
     * Notifies that the request has been successfully processed the request and provides a
     * response.
     *
     * @param response the {@link Bundleable} containing the success response
     */
    default void onSuccess(@Nullable Bundleable response) {
    }

    /**
     * Notifies that the request was not fulfilled successfully.
     *
     * @param response the {@link Bundleable} containing the failure response
     */
    default void onFailure(@NonNull Bundleable response) {
    }
}
