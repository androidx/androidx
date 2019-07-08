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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Config.Option;

/**
 * Configuration containing options used to identify the target class and object being configured.
 *
 * @param <T> The type of the object being configured.
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface TargetConfig<T> {

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.target.name
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<String> OPTION_TARGET_NAME = Option.create("camerax.core.target.name", String.class);
    /**
     * Option: camerax.core.target.class
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Class<?>> OPTION_TARGET_CLASS =
            Option.create("camerax.core.target.class", Class.class);

    // *********************************************************************************************

    /**
     * Retrieves the class of the object being configured.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    Class<T> getTargetClass(@Nullable Class<T> valueIfMissing);

    /**
     * Retrieves the class of the object being configured.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    Class<T> getTargetClass();

    /**
     * Retrieves the name of the target object being configured.
     *
     * <p>The name should be a value that can uniquely identify an instance of the object being
     * configured.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    String getTargetName(@Nullable String valueIfMissing);

    /**
     * Retrieves the name of the target object being configured.
     *
     * <p>The name should be a value that can uniquely identify an instance of the object being
     * configured.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    String getTargetName();

    /**
     * Builder for a {@link TargetConfig}.
     *
     * <p>A {@link TargetConfig} contains options used to identify the target class and
     * object being configured.
     *
     * @param <T> The type of the object being configured.
     * @param <B> The top level builder type for which this builder is composed with.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder<T, B> {

        /**
         * Sets the class of the object being configured.
         *
         * <p>Setting the target class will automatically generate a unique target name if one does
         * not already exist in this configuration.
         *
         * @param targetClass A class object corresponding to the class of the object being
         *                    configured.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        B setTargetClass(Class<T> targetClass);

        /**
         * Sets the name of the target object being configured.
         *
         * <p>The name should be a value that can uniquely identify an instance of the object being
         * configured.
         *
         * @param targetName A unique string identifier for the instance of the class being
         *                   configured.
         * @return the current Builder.
         */
        B setTargetName(String targetName);
    }
}
