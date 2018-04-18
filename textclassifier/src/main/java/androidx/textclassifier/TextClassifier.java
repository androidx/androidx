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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.collection.ArraySet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
public class TextClassifier {

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
    })
    @interface EntityType {}

    /** Designates that the TextClassifier should identify all entity types it can. **/
    static final int ENTITY_PRESET_ALL = 0;
    /** Designates that the TextClassifier should identify no entities. **/
    static final int ENTITY_PRESET_NONE = 1;
    /** Designates that the TextClassifier should identify a base set of entities determined by the
     * TextClassifier. **/
    static final int ENTITY_PRESET_BASE = 2;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {ENTITY_PRESET_ALL, ENTITY_PRESET_NONE, ENTITY_PRESET_BASE})
    @interface EntityPreset {}

    // TODO: add constructor, suggestSelection, classifyText, generateLinks, logEvent

    /**
     * Returns a {@link Collection} of the entity types in the specified preset.
     *
     * @see #ENTITY_PRESET_ALL
     * @see #ENTITY_PRESET_NONE
     */
    /* package */ Collection<String> getEntitiesForPreset(@EntityPreset int entityPreset) {
        // TODO: forward call to the classifier implementation.
        return Collections.EMPTY_LIST;
    }

    /**
     * Configuration object for specifying what entities to identify.
     *
     * Configs are initially based on a predefined preset, and can be modified from there.
     */
    static final class EntityConfig implements Parcelable {
        private final @EntityPreset int mEntityPreset;
        private final Collection<String> mExcludedEntityTypes;
        private final Collection<String> mIncludedEntityTypes;

        EntityConfig(@EntityPreset int mEntityPreset) {
            this.mEntityPreset = mEntityPreset;
            mExcludedEntityTypes = new ArraySet<>();
            mIncludedEntityTypes = new ArraySet<>();
        }

        /**
         * Specifies an entity to include in addition to any specified by the enity preset.
         *
         * Note that if an entity has been excluded, the exclusion will take precedence.
         */
        public EntityConfig includeEntities(String... entities) {
            mIncludedEntityTypes.addAll(Arrays.asList(entities));
            return this;
        }

        /**
         * Specifies an entity to be excluded.
         */
        public EntityConfig excludeEntities(String... entities) {
            mExcludedEntityTypes.addAll(Arrays.asList(entities));
            return this;
        }

        /**
         * Returns an unmodifiable list of the final set of entities to find.
         */
        public List<String> getEntities(TextClassifier textClassifier) {
            ArrayList<String> entities = new ArrayList<>();
            for (String entity : textClassifier.getEntitiesForPreset(mEntityPreset)) {
                if (!mExcludedEntityTypes.contains(entity)) {
                    entities.add(entity);
                }
            }
            for (String entity : mIncludedEntityTypes) {
                if (!mExcludedEntityTypes.contains(entity) && !entities.contains(entity)) {
                    entities.add(entity);
                }
            }
            return Collections.unmodifiableList(entities);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mEntityPreset);
            dest.writeStringList(new ArrayList<>(mExcludedEntityTypes));
            dest.writeStringList(new ArrayList<>(mIncludedEntityTypes));
        }

        public static final Parcelable.Creator<EntityConfig> CREATOR =
                new Parcelable.Creator<EntityConfig>() {
                    @Override
                    public EntityConfig createFromParcel(Parcel in) {
                        return new EntityConfig(in);
                    }

                    @Override
                    public EntityConfig[] newArray(int size) {
                        return new EntityConfig[size];
                    }
                };

        private EntityConfig(Parcel in) {
            mEntityPreset = in.readInt();
            mExcludedEntityTypes = new ArraySet<>(in.createStringArrayList());
            mIncludedEntityTypes = new ArraySet<>(in.createStringArrayList());
        }
    }
}
