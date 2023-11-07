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

package androidx.input.motionprediction.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Method;

/**
 */
@RestrictTo(LIBRARY)
public class SystemProperty {
    private static final boolean PROPERTY_DEFAULT = false;

    private SystemProperty() {
        // This class is non-instantiable.
    }

    /**
     * Reads a system property and returns its boolean value.
     *
     * @param name the name of the system property
     * @return true if the property is set and true, false otherwise
     */
    public static boolean getBoolean(@NonNull String name) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod(
                    "getBoolean",
                    String.class,
                    boolean.class);
            @SuppressLint("BanUncheckedReflection")
            Boolean result = (Boolean) getMethod.invoke(systemProperties, name, PROPERTY_DEFAULT);
            if (result != null) {
                return result.booleanValue();
            }
        } catch (Exception e) {
        }
        return false;
    }
}
