/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * An internal utility class that allows tests to specify whether to enable basic extender or
 * advanced extender of this testlib.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionsTestlibControl {
    public enum ImplementationType {
        ADVANCED_EXTENDER,
        BASIC_EXTENDER
    }

    private static ExtensionsTestlibControl sInstance;
    private static Object sLock = new Object();

    private ExtensionsTestlibControl() {
    }

    /**
     * Gets the singleton instance.
     */
    @NonNull
    public static ExtensionsTestlibControl getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ExtensionsTestlibControl();
            }
            return sInstance;
        }
    }

    private ImplementationType mImplementationType = ImplementationType.BASIC_EXTENDER;

    /**
     * Set the implementation type.
     */
    public void setImplementationType(@NonNull ImplementationType type) {
        mImplementationType = type;
    }

    /**
     * Gets the implementation type;
     */
    @NonNull
    public ImplementationType getImplementationType() {
        return mImplementationType;
    }
}
