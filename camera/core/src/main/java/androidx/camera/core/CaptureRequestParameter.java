/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.auto.value.AutoValue;

/**
 * A {@link CaptureRequest.Key}-value pair.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@AutoValue
public abstract class CaptureRequestParameter<T> {
    /** Prevent subclassing. */
    CaptureRequestParameter() {
    }

    public static <T> CaptureRequestParameter<T> create(CaptureRequest.Key<T> key, T value) {
        return new AutoValue_CaptureRequestParameter<>(key, value);
    }

    /**
     * Apply the parameter to the {@link CaptureRequest.Builder}
     *
     * <p>This provides a type safe way of setting the key-value pair since the type of the key gets
     * erased.
     */
    public final void apply(CaptureRequest.Builder builder) {
        builder.set(getKey(), getValue());
    }

    public abstract CaptureRequest.Key<T> getKey();

    public abstract T getValue();
}
