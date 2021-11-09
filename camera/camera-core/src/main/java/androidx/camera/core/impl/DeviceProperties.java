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

package androidx.camera.core.impl;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/**
 * Container of the device properties.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class DeviceProperties {
    /** Creates an instance by querying the properties from {@link android.os.Build}. */
    @NonNull
    public static DeviceProperties create() {
        return create(Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT);
    }

    /** Creates an instance from the given properties. */
    @NonNull
    public static DeviceProperties create(@NonNull String manufacturer, @NonNull String model,
            int sdkVersion) {
        return new AutoValue_DeviceProperties(manufacturer, model, sdkVersion);
    }

    /** Returns the manufacturer of the device. */
    @NonNull
    public abstract String manufacturer();

    /** Returns the model of the device. */
    @NonNull
    public abstract String model();

    /** Returns the SDK version of the OS running on the device. */
    public abstract int sdkVersion();
}
