/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.collection.ArraySet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Interface for providing text classification related features.
 *
 * TextClassifier acts as a proxy to either the system provided TextClassifier, or an equivalent
 * implementation provided by an app. Each instance of the class therefore represents one connection
 * to the classifier implementation.
 *
 * <p>Unless otherwise stated, methods of this interface are blocking operations.
 * Avoid calling them on the UI thread.
 */
// TODO: Remove this once we finish porting the rest of the TextClassifier class.
@SuppressWarnings("PrivateConstructorForUtilityClass")
public abstract class TextClassifier {

    // TODO: describe in the class documentation how a TC implementation in chosen/located.

    /** Signifies that the TextClassifier did not identify an entity. */
    public static final String TYPE_UNKNOWN = "";
    /** Signifies that the classifier ran, but didn't recognize a know entity. */
    public static final String TYPE_OTHER = "other";
    /** Identifies an e-mail address. */
    public static final String TYPE_EMAIL = "email";
    /** Identifies a phone number. */
    public static final String TYPE_PHONE = "phone";
    /** Identifies a physical address. */
    public static final String TYPE_ADDRESS = "address";
    /** Identifies a URL. */
    public static final String TYPE_URL = "url";
    /**
     * Time reference that is no more specific than a date. May be absolute such as "01/01/2000" or
     * relative like "tomorrow".
     **/
    public static final String TYPE_DATE = "date";
    /**
     * Time reference that includes a specific time. May be absolute such as "01/01/2000 5:30pm" or
     * relative like "tomorrow at 5:30pm".
     **/
    public static final String TYPE_DATE_TIME = "datetime";
    /** Flight number in IATA format. */
    public static final String TYPE_FLIGHT_NUMBER = "flight";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @StringDef(value = {
            TYPE_UNKNOWN,
            TYPE_OTHER,
            TYPE_EMAIL,
            TYPE_PHONE,
            TYPE_ADDRESS,
            TYPE_URL,
            TYPE_DATE,
            TYPE_DATE_TIME,
            TYPE_FLIGHT_NUMBER,
    })
    @interface EntityType {}

    /** Designates that the text in question is editable. **/
    public static final String HINT_TEXT_IS_EDITABLE = "android.text_is_editable";
    /** Designates that the text in question is not editable. **/
    public static final String HINT_TEXT_IS_NOT_EDITABLE = "android.text_is_not_editable";

    // TODO: add remaining interface

    /**
     * Configuration object for specifying what entities to identify.
     *
     * Configs are initially based on a predefined preset, and can be modified from there.
     */
    public static final class EntityConfig {

        private static final String EXTRA_HINTS = "hints";
        private static final String EXTRA_EXCLUDED_ENTITY_TYPES = "excluded";
        private static final String EXTRA_INCLUDED_ENTITY_TYPES = "included";
        private static final String EXTRA_INCLUDE_ENTITY_TYPES_FROM_TC =
                "include_entity_types_from_tc";

        private final Collection<String> mHints;
        private final Collection<String> mExcludedEntityTypes;
        private final Collection<String> mIncludedEntityTypes;
        private final boolean mIncludeDefaultEntityTypes;

        private EntityConfig(
                Collection<String> includedEntityTypes,
                Collection<String> excludedEntityTypes,
                Collection<String> hints,
                boolean includeDefaultEntityTypes) {
            mIncludedEntityTypes = includedEntityTypes == null
                    ? Collections.<String>emptyList() : new ArraySet<>(includedEntityTypes);
            mExcludedEntityTypes = excludedEntityTypes == null
                    ? Collections.<String>emptyList() : new ArraySet<>(excludedEntityTypes);
            mHints = hints == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableCollection(new ArraySet<>(hints));
            mIncludeDefaultEntityTypes = includeDefaultEntityTypes;
        }

        /**
         * Returns a final list of entity types that the text classifier should look for.
         * <p>NOTE: This method is intended for use by text classifier.
         *
         * @param defaultEntityTypes entity types the text classifier thinks should be included
         *                           before factoring in the included/excluded entity types given
         *                           by the client.
         */
        public Collection<String> resolveEntityTypes(
                @Nullable Collection<String> defaultEntityTypes) {
            Set<String> entityTypes = new ArraySet<>();
            if (mIncludeDefaultEntityTypes && defaultEntityTypes != null) {
                entityTypes.addAll(defaultEntityTypes);
            }
            entityTypes.addAll(mIncludedEntityTypes);
            entityTypes.removeAll(mExcludedEntityTypes);
            return Collections.unmodifiableCollection(entityTypes);
        }

        /**
         * Retrieves the list of hints.
         *
         * @return An unmodifiable collection of the hints.
         */
        @NonNull
        public Collection<String> getHints() {
            return mHints;
        }

        /**
         * Return whether the client allows the text classifier to include its own list of default
         * entity types. If this functions returns {@code true}, text classifier can consider
         * to specify its own list in {@link #resolveEntityTypes(Collection)}.
         *
         * <p>NOTE: This method is intended for use by text classifier.
         *
         * @see #resolveEntityTypes(Collection)
         */
        public boolean shouldIncludeDefaultEntityTypes() {
            return mIncludeDefaultEntityTypes;
        }

        /**
         * Adds this EntityConfig to a Bundle that can be read back with the same parameters
         * to {@link #createFromBundle(Bundle)}.
         */
        @NonNull
        public Bundle toBundle() {
            final Bundle bundle = new Bundle();
            bundle.putStringArrayList(EXTRA_HINTS, new ArrayList<>(mHints));
            bundle.putStringArrayList(EXTRA_INCLUDED_ENTITY_TYPES,
                    new ArrayList<>(mIncludedEntityTypes));
            bundle.putStringArrayList(EXTRA_EXCLUDED_ENTITY_TYPES,
                    new ArrayList<>(mExcludedEntityTypes));
            bundle.putBoolean(EXTRA_INCLUDE_ENTITY_TYPES_FROM_TC,
                    mIncludeDefaultEntityTypes);
            return bundle;
        }

        /**
         * Extracts an EntityConfig from a bundle that was added using {@link #toBundle()}.
         */
        @NonNull
        public static EntityConfig createFromBundle(@NonNull Bundle bundle) {
            return new EntityConfig(
                    bundle.getStringArrayList(EXTRA_INCLUDED_ENTITY_TYPES),
                    bundle.getStringArrayList(EXTRA_EXCLUDED_ENTITY_TYPES),
                    bundle.getStringArrayList(EXTRA_HINTS),
                    bundle.getBoolean(EXTRA_INCLUDE_ENTITY_TYPES_FROM_TC));
        }

        /**
         * Builder class to construct the {@link EntityConfig} object.
         */
        public static final class Builder {
            @Nullable
            private Collection<String> mHints;
            @Nullable
            private Collection<String> mExcludedEntityTypes;
            @Nullable
            private Collection<String> mIncludedEntityTypes;
            private boolean mIncludeDefaultEntityTypes = true;

            /**
             * Sets a collection of entity types that are explicitly included.
             */
            public Builder setIncludedEntityTypes(
                    @Nullable Collection<String> includedEntityTypes) {
                mIncludedEntityTypes = includedEntityTypes;
                return this;
            }

            /**
             * Sets a collection of entity types that are explicitly excluded.
             */
            public Builder setExcludedEntityTypes(
                    @Nullable Collection<String> excludedEntityTypes) {
                mExcludedEntityTypes = excludedEntityTypes;
                return this;
            }

            /**
             * Sets a collection of hints for the text classifier to determine what types of
             * entities to find.
             *
             * @see #HINT_TEXT_IS_EDITABLE
             * @see #HINT_TEXT_IS_NOT_EDITABLE
             */
            public Builder setHints(@Nullable Collection<String> hints) {
                mHints = hints;
                return this;
            }

            /**
             * Specifies to include the default entity types suggested by the text classifier. By
             * default, it is included.
             */
            public Builder setIncludeDefaultEntityTypes(boolean includeDefaultEntityTypes) {
                mIncludeDefaultEntityTypes = includeDefaultEntityTypes;
                return this;
            }

            /**
             * Combines all of the options that have been set and returns a new
             * {@link EntityConfig} object.
             */
            @NonNull
            public EntityConfig build() {
                return new EntityConfig(
                        mIncludedEntityTypes,
                        mExcludedEntityTypes,
                        mHints,
                        mIncludeDefaultEntityTypes);
            }
        }
    }
}
