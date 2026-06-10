/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import android.graphics.Color;

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.serializers.ColorAsLongSerializer;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AppSearch document representing a {@link SportsOrganization} entity.
 */
@Document(name = "builtin:SportsOrganization")
public class SportsOrganization extends Organization {

    @Document.StringProperty
    private @NonNull String mSport;

    @Document.LongProperty(serializer = ColorAsLongSerializer.class)
    private @Nullable Color mAccentColor;

    /**
     * Constructor for {@link SportsOrganization}.
     *
     * @param builder The builder to construct the {@link SportsOrganization} from.
     */
    @ExperimentalAppSearchApi
    public SportsOrganization(@NonNull BuilderBase<?> builder) {
        super(builder);
        this.mSport = builder.mSport;
        this.mAccentColor = builder.mAccentColor;
    }

    /**
     * Returns the sport of the sports organization, if set.
     */
    public @NonNull String getSport() {
        return mSport;
    }

    /**
     * Returns the accent colors of the sports organization as
     * a {@link Color}, if set.
     */
    public @Nullable Color getAccentColor() {
        return mAccentColor;
    }

    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {

        /**
         * Constructor for {@link SportsOrganization.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         * @param sport The sport of the sports organization.
         **/
        public Builder(@NonNull String namespace, @NonNull String id, @NonNull String sport) {
            super(namespace, id, sport);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull SportsOrganization sportsOrganization) {
            super(sportsOrganization);
        }
    }

    /**
     * Builder for {@link SportsOrganization}.
     */
    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends
            Organization.BuilderBase<T> {
        private final @NonNull String mSport;
        private @Nullable Color mAccentColor;

        /**
         * Constructor for {@link SportsOrganization.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         * @param sport The sport of the sports organization.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id,
            @NonNull String sport) {
            super(namespace, id);
            mSport = Preconditions.checkNotNull(sport);
        }

        /**
         * Constructor for {@link SportsOrganization.BuilderBase} with all the existing values.
         *
         * @param sportsOrganization The existing {@link SportsOrganization} to copy values from.
         */
        public BuilderBase(@NonNull SportsOrganization sportsOrganization) {
            super(sportsOrganization);
            mSport = sportsOrganization.getSport();
            mAccentColor = sportsOrganization.getAccentColor();
        }

        /**
          * Sets the accent color of the sports organization as
          * a {@link Color}.
          *
          * @param accentColor The accent color of the sports organization.
          */
        public @NonNull T setAccentColor(@Nullable Color accentColor) {
            this.mAccentColor = accentColor;
            return (T) this;
        }

        @Override
        public @NonNull SportsOrganization build() {
            return new SportsOrganization(this);
        }
      }
}