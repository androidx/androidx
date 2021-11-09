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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

import java.util.Set;

/**
 * A Config is a collection of options and values.
 *
 * <p>Config object hold pairs of Options/Values and offer methods for querying whether
 * Options are contained in the configuration along with methods for retrieving the associated
 * values for options.
 *
 * <p>Config allows different values to be set with different {@link OptionPriority} on the same
 * Option. While {@link Config#retrieveOption} will return the option value of the highest priority,
 * {@link Config#retrieveOptionWithPriority} and {@link Config#getPriorities} can be used to
 * retrieve option value of specified priority.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface Config {

    /**
     * Returns whether this configuration contains the supplied option.
     *
     * @param id The {@link Option} to search for in this configuration.
     * @return <code>true</code> if this configuration contains the supplied option; <code>false
     * </code> otherwise.
     */
    boolean containsOption(@NonNull Option<?> id);

    /**
     * Retrieves the value for the specified option if it exists in the configuration.
     *
     * <p>If the option does not exist, an exception will be thrown. If there are multiple values
     * being set with multiple {@link OptionPriority}, it will return the value of highest
     * priority.
     *
     * @param id       The {@link Option} to search for in this configuration.
     * @param <ValueT> The type for the value associated with the supplied {@link Option}.
     * @return The value stored in this configuration, or <code>null</code> if it does not exist.
     * @throws IllegalArgumentException if the given option does not exist in this configuration.
     */
    @Nullable
    <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id);

    /**
     * Retrieves the value for the specified option if it exists in the configuration.
     *
     * <p>If the option does not exist, <code>valueIfMissing</code> will be returned. If there are
     * multiple values being set with multiple {@link OptionPriority}, it will return the value of
     * highest priority.
     *
     * @param id             The {@link Option} to search for in this configuration.
     * @param valueIfMissing The value to return if the specified {@link Option} does not exist in
     *                       this configuration.
     * @param <ValueT>       The type for the value associated with the supplied {@link Option}.
     * @return The value stored in this configuration, or <code>null</code> if it does not exist.
     */
    @Nullable
    <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id, @Nullable ValueT valueIfMissing);

    /**
     * Retrieves the value for the specified option and specified priority if it exists in the
     * configuration.
     *
     * <p>If the option does not exist, an exception will be thrown.
     *
     * @param id             The {@link Option} to search for in this configuration.
     * @param <ValueT>       The type for the value associated with the supplied {@link Option}.
     * @throws IllegalArgumentException if the given option with specified priority does not exist
     * in this configuration.
     */
    @Nullable
    <ValueT> ValueT retrieveOptionWithPriority(@NonNull Option<ValueT> id,
            @NonNull OptionPriority priority);

    /**
     * Returns the current priority of the value for the specified option.
     *
     * <p>If there are multiple values of various priorities for the specified options, the highest
     * priority will be returned. If the option does not exist, an
     * {@link IllegalArgumentException} will be thrown.
     */
    @NonNull
    OptionPriority getOptionPriority(@NonNull Option<?> opt);

    /**
     * Search the configuration for {@link Option}s whose id match the supplied search string.
     *
     * @param idSearchString The id string to search for. This could be a fully qualified id such as
     *                       \"<code>camerax.core.example.option</code>\" or the stem for an
     *                       option such as \"<code>
     *                       camerax.core.example</code>\".
     * @param matcher        A callback used to receive results of the search. Results will be
     *                       sent to
     *                       {@link OptionMatcher#onOptionMatched(Option)} in the order in which
     *                       they are found inside
     *                       this configuration. Subsequent results will continue to be sent as
     *                       long as {@link
     *                       OptionMatcher#onOptionMatched(Option)} returns <code>true</code>.
     */
    void findOptions(@NonNull String idSearchString, @NonNull OptionMatcher matcher);

    /**
     * Lists all options contained within this configuration.
     *
     * @return A {@link Set} of {@link Option}s contained within this configuration.
     */
    @NonNull
    Set<Option<?>> listOptions();

    /**
     *
     * Returns a {@link Set} of all priorities set for the specified option.
     *
     */
    @NonNull
    Set<OptionPriority> getPriorities(@NonNull Option<?> option);

    /**
     * A callback for retrieving results of a {@link Config.Option} search.
     */
    interface OptionMatcher {
        /**
         * Receives results from {@link Config#findOptions(String, OptionMatcher)}.
         *
         * <p>When searching for a specific option in a {@link Config}, {@link Option}s will
         * be sent to {@link #onOptionMatched(Option)} in the order in which they are found.
         *
         * @param option The matched option.
         * @return <code>false</code> if no further results are needed; <code>true</code> otherwise.
         */
        boolean onOptionMatched(@NonNull Option<?> option);
    }

    /**
     * An {@link Option} is used to set and retrieve values for settings defined in a {@link
     * Config}.
     *
     * <p>{@link Option}s can be thought of as the key in a key/value pair that makes up a setting.
     * As the name suggests, {@link Option}s are optional, and may or may not exist inside a {@link
     * Config}.
     *
     * @param <T> The type of the value for this option.
     */
    @AutoValue
    abstract class Option<T> {

        /** Prevent subclassing */
        Option() {
        }

        /**
         * Creates an {@link Option} from an id and value class.
         *
         * @param id         A unique string identifier for this option. This generally follows
         *                   the scheme
         *                   <code>&lt;owner&gt;.[optional.subCategories.]&lt;optionId&gt;</code>.
         * @param valueClass The class of the value stored by this option.
         * @param <T>        The type of the value stored by this option.
         * @return An {@link Option} object which can be used to store/retrieve values from a {@link
         * Config}.
         */
        @NonNull
        public static <T> Option<T> create(@NonNull String id, @NonNull Class<?> valueClass) {
            return Option.create(id, valueClass, /*token=*/ null);
        }

        /**
         * Creates an {@link Option} from an id, value class and token.
         *
         * @param id         A unique string identifier for this option. This generally follows
         *                   the scheme
         *                   <code>&lt;owner&gt;.[optional.subCategories.]&lt;optionId&gt;</code>.
         * @param valueClass The class of the value stored by this option.
         * @param <T>        The type of the value stored by this option.
         * @param token      An optional, type-erased object for storing more context for this
         *                   specific
         *                   option. Generally this object should have static scope and be
         *                   immutable.
         * @return An {@link Option} object which can be used to store/retrieve values from a {@link
         * Config}.
         */
        @SuppressWarnings("unchecked")
        @NonNull
        public static <T> Option<T> create(@NonNull String id, @NonNull Class<?> valueClass,
                @Nullable Object token) {
            return new AutoValue_Config_Option<>(id, (Class<T>) valueClass, token);
        }

        /**
         * Returns the unique string identifier for this option.
         *
         * <p>This generally follows the scheme * <code>
         * &lt;owner&gt;.[optional.subCategories.]&lt;optionId&gt;
         * </code>.
         *
         * @return The identifier.
         */
        @NonNull
        public abstract String getId();

        /**
         * Returns the class object associated with the value for this option.
         *
         * @return The class object for the value's type.
         */
        @NonNull
        public abstract Class<T> getValueClass();

        /**
         * Returns the optional type-erased context object for this option.
         *
         * <p>Generally this object should have static scope and be immutable.
         *
         * @return The type-erased context object.
         */
        @Nullable
        public abstract Object getToken();
    }

    /**
     * Defines the priorities for resolving conflicting options.
     *
     * <p>Priority must be declared from high priority to low priority.
     */
    enum OptionPriority {
        /**
         * Should only be used externally by apps. It takes precedence over any other option
         * values at the risk of causing unexpected behavior.
         *
         * <p>This should not used internally in CameraX. It conflicts when merging different
         * values set to ALWAY_OVERRIDE.
         */
        ALWAYS_OVERRIDE,

        /**
         * It's a required option value in order to achieve expected CameraX behavior. It takes
         * precedence over {@link #OPTIONAL} option values.
         *
         * <p>If apps set ALWAYS_OVERRIDE options, it'll override REQUIRED option values and can
         * potentially cause unexpected behaviors. It conflicts when merging different values set
         * to REQUIRED.
         */
        REQUIRED,

        /**
         * The lowest priority, it can be overridden by any other option value. When two option
         * values are set as OPTIONAL, the newer value takes precedence over the old one.
         */
        OPTIONAL
    }

    /**
     * Returns if values with these {@link OptionPriority} conflict or not.
     *
     * Currently it is not allowed to have different values with same ALWAYS_OVERRIDE
     * priority or to have different values with same REQUIRED priority.
     */
    static boolean hasConflict(@NonNull OptionPriority priority1,
            @NonNull OptionPriority priority2) {
        if (priority1 == OptionPriority.ALWAYS_OVERRIDE
                && priority2 == OptionPriority.ALWAYS_OVERRIDE) {
            return true;
        }

        if (priority1 == OptionPriority.REQUIRED
                && priority2 == OptionPriority.REQUIRED) {
            return true;
        }

        return false;
    }

    /**
     * Merges two configs
     *
     * @param extendedConfig the extended config. The options in the extendedConfig will be applied
     *                       on top of the baseConfig based on the option priorities.
     * @param baseConfig the base config
     * @return a {@link MutableOptionsBundle} of the merged config
     */
    @NonNull
    static Config mergeConfigs(@Nullable Config extendedConfig,
            @Nullable Config baseConfig) {
        if (extendedConfig == null && baseConfig == null) {
            return OptionsBundle.emptyBundle();
        }

        MutableOptionsBundle mergedConfig;

        if (baseConfig != null) {
            mergedConfig = MutableOptionsBundle.from(baseConfig);
        } else {
            mergedConfig = MutableOptionsBundle.create();
        }

        if (extendedConfig != null) {
            // If any options need special handling, this is the place to do it. For now we'll
            // just copy over all options.
            for (Config.Option<?> opt : extendedConfig.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                        Config.Option<Object> objectOpt = (Config.Option<Object>) opt;

                mergedConfig.insertOption(objectOpt,
                        extendedConfig.getOptionPriority(opt),
                        extendedConfig.retrieveOption(objectOpt));
            }
        }

        return OptionsBundle.from(mergedConfig);
    }
}
