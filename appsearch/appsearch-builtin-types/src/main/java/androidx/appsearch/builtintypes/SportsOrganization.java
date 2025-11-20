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

import java.util.List;

/**
 * AppSearch document representing a {@link SportsOrganization} entity.
 */
@Document(name = "builtin:SportsOrganization")
public class SportsOrganization extends Organization {

    @Document.StringProperty
    private @NonNull String mSport;

    @Document.LongProperty(serializer = ColorAsLongSerializer.class)
    private @Nullable Color mAccentColor;

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    protected SportsOrganization(
        @NonNull String namespace, @NonNull String id, int documentScore,
        long creationTimestampMillis, long documentTtlMillis,
        @Nullable String name, @Nullable List<String> alternateNames,
        @Nullable String description, @Nullable String image,
        @Nullable String url, @NonNull List<PotentialAction> potentialActions,
        @Nullable ImageObject logo,
        @NonNull String sport,
        @Nullable Color accentColor) {
        super(namespace, id, documentScore, creationTimestampMillis,
            documentTtlMillis, name, alternateNames, description, image, url,
            potentialActions, logo);
        this.mSport = sport;
        this.mAccentColor = accentColor;
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
    public static final class Builder extends BuilderImpl<Builder> {

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
    static class BuilderImpl<T extends BuilderImpl<T>> extends Organization.BuilderImpl<T> {
        protected @NonNull String mSport;
        protected @Nullable Color mAccentColor;

        BuilderImpl(@NonNull String namespace, @NonNull String id,
            @NonNull String sport) {
            super(namespace, id);
            mSport = Preconditions.checkNotNull(sport);
        }

        BuilderImpl(@NonNull SportsOrganization sportsOrganization) {
            super(new Organization.Builder(sportsOrganization).build());
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
            return new SportsOrganization(
                mNamespace,
                mId,
                mDocumentScore,
                mCreationTimestampMillis,
                mDocumentTtlMillis,
                mName,
                mAlternateNames,
                mDescription,
                mImage,
                mUrl,
                mPotentialActions,
                mLogo,
                mSport,
                mAccentColor);
        }
      }
}