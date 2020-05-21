/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.util;

import android.compat.annotation.UnsupportedAppUsage;

import java.lang.reflect.Array;

/**
 * ArrayUtils contains some methods that you can call to find out
 * the most efficient increments by which to grow arrays.
 */
public class ArrayUtils {
    private ArrayUtils() { /* cannot be instantiated */ }

    /**
     * newUnpaddedObjectArray
     */
    public static Object[] newUnpaddedObjectArray(int minLen) {
        return new Object[minLen];
    }

    /**
     * newUnpaddedArray
     */
    @UnsupportedAppUsage
    @SuppressWarnings("unchecked")
    public static <T> T[] newUnpaddedArray(Class<T> clazz, int minLen) {
        return (T[]) Array.newInstance(clazz, minLen);
    }

    /**
     * newUnpaddedIntArray
     */
    public static int[] newUnpaddedIntArray(int minLen) {
        return new int[minLen];
    }

    /**
     * newUnpaddedLongArray
     */
    public static long[] newUnpaddedLongArray(int minLen) {
        return new long[minLen];
    }

    /**
     * newUnpaddedFloatArray
     */
    public static float[] newUnpaddedFloatArray(int minLen) {
        return new float[minLen];
    }

    /**
     * newUnpaddedBooleanArray
     */
    public static boolean[] newUnpaddedBooleanArray(int minLen) {
        return new boolean[minLen];
    }

    /**
     * newUnpaddedCharArray
     */
    public static char[] newUnpaddedCharArray(int minLen) {
        return new char[minLen];
    }

    /**
     * emptyArray
     */
    public static <T> T[] emptyArray(Class<T> kind) {
        return (T[]) Array.newInstance(kind, 0);
    }
}
