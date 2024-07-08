/*
 * Copyright 2024 The Android Open Source Project
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

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages device/camera-specific quirks (workarounds or customizations) for CameraX.
 *
 * <p>{@link Quirk}s are used to handle variations in device behavior or capabilities that may
 * affect CameraX functionality. This class allows for fine-grained control over which quirks are
 * enabled or disabled.
 *
 * <p>Key Features
 * <ul>
 *   <li>Default Behavior: Configure whether quirks should be enabled by default if the
 *       device natively exhibits them.</li>
 *   <li>Force Enable/Disable: Explicitly force specific quirks to be enabled or disabled,
 *       overriding the default behavior.</li>
 * </ul>
 */
public class QuirkSettings {

    private final boolean mEnabledWhenDeviceHasQuirk;
    private final Set<Class<? extends Quirk>> mForceEnabledQuirks;
    private final Set<Class<? extends Quirk>> mForceDisabledQuirks;

    /**
     * Private constructor for building `QuirkSettings` instances.
     *
     * @param enabledWhenDeviceHasQuirk Whether to enable quirks if the device natively exhibits
     *                                  the quirk.
     * @param forceEnabledQuirks        The set of quirks to be force-enabled.
     * @param forceDisabledQuirks       The set of quirks to be force-disabled.
     */
    private QuirkSettings(boolean enabledWhenDeviceHasQuirk,
            @Nullable Set<Class<? extends Quirk>> forceEnabledQuirks,
            @Nullable Set<Class<? extends Quirk>> forceDisabledQuirks) {
        mEnabledWhenDeviceHasQuirk = enabledWhenDeviceHasQuirk;
        mForceEnabledQuirks = forceEnabledQuirks == null ? emptySet() : new HashSet<>(
                forceEnabledQuirks);
        mForceDisabledQuirks = forceDisabledQuirks == null ? emptySet() : new HashSet<>(
                forceDisabledQuirks);
    }

    /**
     * Creates a QuirkSettings instance with default behavior, enabling all quirks if the device
     * natively exhibits the quirk.
     *
     * @return A QuirkSettings instance with default behavior, enabling all quirks if the device
     * natively exhibits the quirk.
     */
    @NonNull
    public static QuirkSettings withDefaultBehavior() {
        return new QuirkSettings.Builder().setEnabledWhenDeviceHasQuirk(true).build();
    }

    /**
     * Creates a QuirkSettings instance with all quirks disabled.
     *
     * @return A QuirkSettings instance with all quirks disabled.
     */
    @NonNull
    public static QuirkSettings withAllQuirksDisabled() {
        return new QuirkSettings.Builder().setEnabledWhenDeviceHasQuirk(false).build();
    }

    /**
     * Creates a QuirkSettings instance with specific quirks force-enabled.
     *
     * @param quirks The quirks to force-enable.
     * @return A new QuirkSettings instance with the specified quirks force-enabled.
     */
    @NonNull
    public static QuirkSettings withQuirksForceEnabled(
            @NonNull Set<Class<? extends Quirk>> quirks) {
        return new QuirkSettings.Builder().forceEnableQuirks(quirks).build();
    }

    /**
     * Creates a QuirkSettings instance with specific quirks force-disabled.
     *
     * @param quirks The quirks to force-disable.
     * @return A new QuirkSettings instance with the specified quirks force-disabled.
     */
    @NonNull
    public static QuirkSettings withQuirksForceDisabled(
            @NonNull Set<Class<? extends Quirk>> quirks) {
        return new QuirkSettings.Builder().forceDisableQuirks(quirks).build();
    }

    /**
     * Gets whether quirks should be enabled if the device natively exhibits the quirk.
     *
     * @return {@code true} if quirks should be enabled, {@code false} otherwise.
     */
    public boolean isEnabledWhenDeviceHasQuirk() {
        return mEnabledWhenDeviceHasQuirk;
    }

    /**
     * Gets the set of quirks that are force-enabled, regardless of device behavior.
     *
     * @return An unmodifiable set containing the names of force-enabled quirks.
     */
    @NonNull
    public Set<Class<? extends Quirk>> getForceEnabledQuirks() {
        return unmodifiableSet(mForceEnabledQuirks);
    }

    /**
     * Gets the set of quirks that are force-disabled, regardless of device behavior.
     *
     * @return An unmodifiable set containing the names of force-disabled quirks.
     */
    @NonNull
    public Set<Class<? extends Quirk>> getForceDisabledQuirks() {
        return unmodifiableSet(mForceDisabledQuirks);
    }

    /**
     * Determines whether a specific quirk should be enabled based on these settings and whether
     * the device natively exhibits the quirk.
     *
     * <p>If a quirk is in both the force-enabled and force-disabled sets, it will be enabled.
     *
     * @param quirk          The quirk class to check.
     * @param deviceHasQuirk Whether the device natively exhibits the quirk.
     * @return true if the quirk should be enabled, false otherwise.
     */
    public boolean shouldEnableQuirk(@NonNull Class<? extends Quirk> quirk,
            boolean deviceHasQuirk) {
        if (mForceEnabledQuirks.contains(quirk)) {
            return true;
        } else if (mForceDisabledQuirks.contains(quirk)) {
            return false;
        } else {
            return mEnabledWhenDeviceHasQuirk && deviceHasQuirk;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof QuirkSettings)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        QuirkSettings other = (QuirkSettings) obj;
        return mEnabledWhenDeviceHasQuirk == other.mEnabledWhenDeviceHasQuirk
                && Objects.equals(mForceEnabledQuirks, other.mForceEnabledQuirks)
                && Objects.equals(mForceDisabledQuirks, other.mForceDisabledQuirks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEnabledWhenDeviceHasQuirk, mForceEnabledQuirks, mForceDisabledQuirks);
    }

    @NonNull
    @Override
    public String toString() {
        return "QuirkSettings{"
                + "enabledWhenDeviceHasQuirk=" + mEnabledWhenDeviceHasQuirk
                + ", forceEnabledQuirks=" + mForceEnabledQuirks
                + ", forceDisabledQuirks=" + mForceDisabledQuirks
                + '}';
    }

    /**
     * Builder class for constructing {@link QuirkSettings} instances.
     */
    public static class Builder {
        private boolean mEnabledWhenDeviceHasQuirk = true;
        private Set<Class<? extends Quirk>> mForceEnabledQuirks;
        private Set<Class<? extends Quirk>> mForceDisabledQuirks;

        /**
         * Sets whether to enable quirks if the device natively exhibits the quirk.
         */
        @NonNull
        public Builder setEnabledWhenDeviceHasQuirk(boolean enabled) {
            mEnabledWhenDeviceHasQuirk = enabled;
            return this;
        }

        /**
         * Forces the specified quirks to be enabled, regardless of other settings.
         */
        @NonNull
        public Builder forceEnableQuirks(@NonNull Set<Class<? extends Quirk>> quirks) {
            mForceEnabledQuirks = new HashSet<>(quirks);
            return this;
        }

        /**
         * Forces the specified quirks to be disabled, regardless of other settings.
         */
        @NonNull
        public Builder forceDisableQuirks(@NonNull Set<Class<? extends Quirk>> quirks) {
            mForceDisabledQuirks = new HashSet<>(quirks);
            return this;
        }

        /**
         * Builds a new {@link QuirkSettings} instance with the configured options.
         *
         * @return A new `QuirkSettings` instance.
         */
        @NonNull
        public QuirkSettings build() {
            return new QuirkSettings(mEnabledWhenDeviceHasQuirk, mForceEnabledQuirks,
                    mForceDisabledQuirks);
        }
    }
}
