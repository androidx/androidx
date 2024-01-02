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
    private static final boolean BOOLEAN_PROPERTY_DEFAULT = false;
    private static final int INT_PROPERTY_DEFAULT = 0;

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
            Boolean result = (Boolean) getMethod.invoke(
                    systemProperties,
                    name,
                    BOOLEAN_PROPERTY_DEFAULT);
            if (result != null) {
                return result.booleanValue();
            }
        } catch (Exception e) {
        }
        return BOOLEAN_PROPERTY_DEFAULT;
    }

    /**
     * Reads a system property and returns its integer value.
     *
     * @param name the name of the system property
     * @return the integer value of the property if defined, zero otherwise
     */
    public static int getInt(@NonNull String name) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod(
                    "getInt",
                    String.class,
                    int.class);
            @SuppressLint("BanUncheckedReflection")
            Integer result = (Integer) getMethod.invoke(
                    systemProperties,
                    name,
                    INT_PROPERTY_DEFAULT);
            if (result != null) {
                return result.intValue();
            }
        } catch (Exception e) {
        }
        return INT_PROPERTY_DEFAULT;
    }
}
