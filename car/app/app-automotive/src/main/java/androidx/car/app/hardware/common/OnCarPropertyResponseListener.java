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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Registers this listener to get property updates by {@link CarPropertyResponse}.
 *
 */
@RestrictTo(LIBRARY)
public interface OnCarPropertyResponseListener {
    /**
     * Called when the associated properties are updated in the car.
     *
     * @param carPropertyResponses a list of {@link CarPropertyResponse}
     */
    void onCarPropertyResponses(@NonNull List<CarPropertyResponse<?>> carPropertyResponses);
}
