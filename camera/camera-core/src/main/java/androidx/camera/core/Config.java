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

import com.google.auto.value.AutoValue;

import java.util.Set;

/**
 * A Configuration is a collection of options and values.
 *
 * <p>Configuration object hold pairs of Options/Values and offer methods for querying whether
 * Options are contained in the configuration along with methods for retrieving the associated
 * values for options.
 */
public interface Config {

    /**
     * Returns whether this configuration contains the supplied option.
     *
     * @param id The {@link Option} to search for in this configuration.
     * @return <code>true</code> if this configuration contains the supplied option; <code>false
     * </code> otherwise.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    boolean containsOption(Option<?> id);

    /**
     * Retrieves the value for the specified option if it exists in the configuration.
     *
     * <p>If the option does not exist, an exception will be thrown.
     *
     * @param id       The {@link Option} to search for in this configuration.
     * @param <ValueT> The type for the value associated with the supplied {@link Option}.
     * @return The value stored in this configuration, or <code>null</code> if it does not exist.
     * @throws IllegalArgumentException if the given option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    <ValueT> ValueT retrieveOption(Option<ValueT> id);

    /**
     * Retrieves the value for the specified option if it exists in the configuration.
     *
     * <p>If the option does not exist, <code>valueIfMissing</code> will be returned.
     *
     * @param id             The {@link Option} to search for in this configuration.
     * @param valueIfMissing The value to return if the specified {@link Option} does not exist in
     *                       this configuration.
     * @param <ValueT>       The type for the value associated with the supplied {@link Option}.
     * @return The value stored in this configuration, or <code>null</code> if it does not exist.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing);

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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    void findOptions(String idSearchString, OptionMatcher matcher);

    /**
     * Lists all options contained within this configuration.
     *
     * @return A {@link Set} of {@link Option}s contained within this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Set<Option<?>> listOptions();

    /**
     * Extendable builders are used to add externally defined options to a configuration.
     */
    interface ExtendableBuilder {

        /**
         * Returns the underlying {@link MutableConfig} being modified by this builder.
         *
         * @return The underlying {@link MutableConfig}.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        MutableConfig getMutableConfig();
    }

    /**
     * A callback for retrieving results of a {@link Config.Option} search.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
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
        boolean onOptionMatched(Option<?> option);
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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
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
        public static <T> Option<T> create(String id, Class<?> valueClass) {
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
        public static <T> Option<T> create(String id, Class<?> valueClass, @Nullable Object token) {
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
        public abstract String getId();

        /**
         * Returns the class object associated with the value for this option.
         *
         * @return The class object for the value's type.
         */
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
}
