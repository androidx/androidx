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

/**
 * An internal utility class that allows tests to specify whether to enable basic extender or
 * advanced extender of this testlib. If OEM implementation exists on the device, the
 * implementation type is always {@link ImplementationType#OEM_IMPL} and can't be changed to
 * other types.
 */
public class ExtensionsTestlibControl {
    public enum ImplementationType {
        OEM_IMPL,
        TESTLIB_ADVANCED,
        TESTLIB_BASIC
    }

    private static ExtensionsTestlibControl sInstance;
    private static Object sLock = new Object();
    private volatile ImplementationType mImplementationType = ImplementationType.TESTLIB_BASIC;

    private ExtensionsTestlibControl() {
        mImplementationType = doesOEMImplementationExist()
                ? ImplementationType.OEM_IMPL : ImplementationType.TESTLIB_BASIC;
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

    /**
     * Set the implementation type.
     *
     * <p>When OEM implementation exists on the device, the only possible type is
     * {@link ImplementationType#OEM_IMPL}. Setting the implementation type to
     * {@link ImplementationType#TESTLIB_BASIC} or {@link ImplementationType#TESTLIB_ADVANCED}
     *  when OEM implementation exist will throw an {@link IllegalArgumentException}.
     *
     * <p>When OEM implementation doesn't exist on the device, it is allowed to set it to
     * {@link ImplementationType#TESTLIB_BASIC} or {@link ImplementationType#TESTLIB_ADVANCED}.
     * Setting it to {@link ImplementationType#OEM_IMPL} in this case will throw an
     * {@link IllegalArgumentException}.
     */
    public void setImplementationType(@NonNull ImplementationType type) {
        if (mImplementationType != ImplementationType.OEM_IMPL) { // OEM impl doesn't exist
            if (type == ImplementationType.OEM_IMPL) {
                throw new IllegalArgumentException("OEM_IMPL is not supported on this device.");
            }
        } else { // OEM impl exists
            if (type != ImplementationType.OEM_IMPL) {
                throw new IllegalArgumentException("Can't change the implementation type because "
                        + "OEM implementation exists on the device");
            }
        }

        mImplementationType = type;
    }

    private boolean doesOEMImplementationExist() {
        try {
            new ExtensionVersionImpl().checkTestlibRunning();
            return false;
        } catch (NoSuchMethodError e) { // checkTestlibRunning doesn't exist in OEM implementation.
            return true;
        }
    }

    /**
     * Gets the implementation type;
     */
    @NonNull
    public ImplementationType getImplementationType() {
        return mImplementationType;
    }
}
