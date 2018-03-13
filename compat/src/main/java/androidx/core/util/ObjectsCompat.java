/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.core.util;

import android.os.Build;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class consists of static utility methods for operating on objects.
 */
public class ObjectsCompat {
    private ObjectsCompat() {
        // Non-instantiable.
    }

    /**
     * Returns {@code true} if the arguments are equal to each other
     * and {@code false} otherwise.
     * <p>
     * Consequently, if both arguments are {@code null}, {@code true}
     * is returned and if exactly one argument is {@code null}, {@code
     * false} is returned. Otherwise, equality is determined by using
     * the {@link Object#equals equals} method of the first
     * argument.
     *
     * @param a an object
     * @param b an object to be compared with {@code a} for equality
     * @return {@code true} if the arguments are equal to each other
     *         and {@code false} otherwise
     * @see Object#equals(Object)
     */
    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        if (Build.VERSION.SDK_INT >= 19) {
            return Objects.equals(a, b);
        } else {
            return (a == b) || (a != null && a.equals(b));
        }
    }

    /**
     * Returns the hash code of a non-{@code null} argument and 0 for a {@code null} argument.
     *
     * @param o an object
     * @return the hash code of a non-{@code null} argument and 0 for a {@code null} argument
     * @see Object#hashCode
     */
    public static int hashCode(@Nullable Object o) {
        return o != null ? o.hashCode() : 0;
    }

    /**
     * Generates a hash code for a sequence of input values. The hash code is generated as if all
     * the input values were placed into an array, and that array were hashed by calling
     * {@link Arrays#hashCode(Object[])}.
     *
     * <p>This method is useful for implementing {@link Object#hashCode()} on objects containing
     * multiple fields. For example, if an object that has three fields, {@code x}, {@code y}, and
     * {@code z}, one could write:
     *
     * <blockquote><pre>
     * &#064;Override public int hashCode() {
     *     return ObjectsCompat.hash(x, y, z);
     * }
     * </pre></blockquote>
     *
     * <b>Warning: When a single object reference is supplied, the returned value does not equal the
     * hash code of that object reference.</b> This value can be computed by calling
     * {@link #hashCode(Object)}.
     *
     * @param values the values to be hashed
     * @return a hash value of the sequence of input values
     * @see Arrays#hashCode(Object[])
     */
    public static int hash(@Nullable Object... values) {
        if (Build.VERSION.SDK_INT >= 19) {
            return Objects.hash(values);
        } else {
            return Arrays.hashCode(values);
        }
    }
}
