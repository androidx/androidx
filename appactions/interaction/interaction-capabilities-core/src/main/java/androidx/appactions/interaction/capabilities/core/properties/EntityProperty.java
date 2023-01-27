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

package androidx.appactions.interaction.capabilities.core.properties;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;

/** The property which describes a entity parameter for {@code ActionCapability}. */
@AutoValue
public abstract class EntityProperty extends ParamProperty<Entity> {
    /** A default EntityProperty instance. This property will accept any entity value. */
    public static final EntityProperty EMPTY = EntityProperty.newBuilder().build();

    /**
     * Using EntityProperty.PROHIBITED will ensure no Argument containing a value for this field
     * will
     * be received by this capability.
     */
    public static final EntityProperty PROHIBITED =
            EntityProperty.newBuilder().setIsProhibited(true).build();

    // TODO (b/260137899)

    /** Returns a Builder instance for EntityProperty. */
    @NonNull
    public static Builder newBuilder() {
        return new Builder();
    }

    /** Builder for {@link EntityProperty}. */
    public static class Builder {

        private final ArrayList<Entity> mPossibleEntityList = new ArrayList<>();
        private boolean mIsRequired;
        private boolean mEntityMatchRequired;
        private boolean mIsProhibited;

        private Builder() {
        }

        /**
         * Adds one or more possible entities for this entity parameter.
         *
         * @param entities the possible entities.
         */
        @NonNull
        public Builder addPossibleEntity(@NonNull Entity... entities) {
            Collections.addAll(mPossibleEntityList, entities);
            return this;
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        @NonNull
        public Builder setIsRequired(boolean isRequired) {
            this.mIsRequired = isRequired;
            return this;
        }

        /**
         * Sets whether or not this property requires that the value for this property must match
         * one of
         * the Entity in the defined possible entities.
         */
        @NonNull
        public Builder setValueMatchRequired(boolean valueMatchRequired) {
            this.mEntityMatchRequired = valueMatchRequired;
            return this;
        }

        /**
         * Sets whether this property is prohibited in the response.
         *
         * @param isProhibited Whether this property is prohibited in the response.
         */
        @NonNull
        public Builder setIsProhibited(boolean isProhibited) {
            this.mIsProhibited = isProhibited;
            return this;
        }

        /** Builds the property for this entity parameter. */
        @NonNull
        public EntityProperty build() {
            return new AutoValue_EntityProperty(
                    Collections.unmodifiableList(mPossibleEntityList),
                    mIsRequired,
                    mEntityMatchRequired,
                    mIsProhibited);
        }
    }
}
