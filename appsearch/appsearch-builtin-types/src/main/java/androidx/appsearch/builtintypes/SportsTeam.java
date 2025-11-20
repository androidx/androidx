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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * AppSearch document representing a {@link SportsTeam} entity.
 */
@Document(name = "builtin:SportsTeam")
public class SportsTeam extends SportsOrganization {

    @Document.LongProperty
    private long mWins;

    @Document.LongProperty
    private long mLosses;

    @Document.LongProperty
    private long mTies;

    @Document.LongProperty
    private long mOvertimeLosses;

    @Document.LongProperty
    private long mOvertimeWins;

    @Document.StringProperty
    private @Nullable String mFormattedRecord;

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    protected SportsTeam(
        @NonNull String namespace, @NonNull String id, int documentScore,
        long creationTimestampMillis, long documentTtlMillis,
        @Nullable String name, @Nullable List<String> alternateNames,
        @Nullable String description, @Nullable String image,
        @Nullable String url, @NonNull List<PotentialAction> potentialActions,
        @Nullable ImageObject logo, @NonNull String sport,
        @Nullable Color accentColor, long wins, long losses, long ties,
        long overtimeLosses, long overtimeWins,
        @Nullable String formattedRecord) {
        super(namespace, id, documentScore, creationTimestampMillis,
            documentTtlMillis, name, alternateNames, description, image, url,
            potentialActions, logo, sport, accentColor);
        this.mWins = wins;
        this.mLosses = losses;
        this.mTies = ties;
        this.mOvertimeLosses = overtimeLosses;
        this.mOvertimeWins = overtimeWins;
        this.mFormattedRecord = formattedRecord;
    }

    /**
     * Returns the number of wins of the sports team.
     */
    public long getWins() {
        return mWins;
    }

    /**
     * Returns the number of losses of the sports team.
     */
    public long getLosses() {
        return mLosses;
    }

    /**
     * Returns the number of ties of the sports team.
     */
    public long getTies() {
        return mTies;
    }

    /**
     * Returns the number of overtime losses of the sports team.
     */
    public long getOvertimeLosses() {
        return mOvertimeLosses;
    }

    /**
     * Returns the number of overtime wins of the sports team.
     */
    public long getOvertimeWins() {
        return mOvertimeWins;
    }

    /**
     * Returns the formatted record of the sports team.
     *
     * eg: "10 - 5", "10 - 5 (3 - 2)", "10 - 5 (3 - 2) - 1 OT" depending
     * on the sport.
     */
    public @Nullable String getFormattedRecord() {
        return mFormattedRecord;
    }

    @Document.BuilderProducer
    public static final class Builder extends BuilderImpl<Builder> {

        /**
         * Constructor for {@link SportsTeam.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         * @param sport The sport of the sports team.
         **/
        public Builder(@NonNull String namespace, @NonNull String id,
            @NonNull String sport) {
            super(namespace, id, sport);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull SportsTeam sportsTeam) {
            super(sportsTeam);
        }
    }

    /**
     * Builder for {@link SportsTeam}.
     */
    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> extends SportsOrganization.BuilderImpl<T> {
        // Initialize the default values to 0.
        protected long mWins = 0;
        protected long mLosses = 0;
        protected long mTies = 0;
        protected long mOvertimeLosses = 0;
        protected long mOvertimeWins = 0;
        protected @Nullable String mFormattedRecord;

        BuilderImpl(@NonNull String namespace, @NonNull String id,
            @NonNull String sport) {
            super(namespace, id, sport);
        }

        BuilderImpl(@NonNull SportsTeam sportsTeam) {
            super(sportsTeam);
            this.mWins = sportsTeam.getWins();
            this.mLosses = sportsTeam.getLosses();
            this.mTies = sportsTeam.getTies();
            this.mOvertimeLosses = sportsTeam.getOvertimeLosses();
            this.mOvertimeWins = sportsTeam.getOvertimeWins();
            this.mFormattedRecord = sportsTeam.getFormattedRecord();
        }

        /**
         * Sets the number of wins of the sports team.
         *
         * @param wins The number of wins of the sports team.
         */
        public @NonNull T setWins(long wins) {
            this.mWins = wins;
            return (T) this;
        }

        /**
         * Sets the number of losses of the sports team.
         *
         * @param losses The number of losses of the sports team.
         */
        public @NonNull T setLosses(long losses) {
            this.mLosses = losses;
            return (T) this;
        }

        /**
         * Sets the number of ties of the sports team.
         *
         * @param ties The number of ties of the sports team.
         */
        public @NonNull T setTies(long ties) {
            this.mTies = ties;
            return (T) this;
        }

        /**
         * Sets the number of overtime losses of the sports team.
         *
         * @param overtimeLosses The number of overtime losses of the
         * sports team.
         */
        public @NonNull T setOvertimeLosses(long overtimeLosses) {
            this.mOvertimeLosses = overtimeLosses;
            return (T) this;
        }

        /**
         * Sets the number of overtime wins of the sports team.
         *
         * @param overtimeWins The number of overtime wins of the sports team.
         */
        public @NonNull T setOvertimeWins(long overtimeWins) {
            this.mOvertimeWins = overtimeWins;
            return (T) this;
        }

        /**
         * Sets the formatted record of the sports team.
         *
         * eg: "10 - 5", "10 - 5 (3 - 2)", "10 - 5 (3 - 2) - 1 OT" depending
         * on the sport.
         *
         * @param formattedRecord The formatted record of the sports team.
         */
        public @NonNull T setFormattedRecord(@Nullable String formattedRecord) {
            this.mFormattedRecord = formattedRecord;
            return (T) this;
        }

        @Override
        public @NonNull SportsTeam build() {
            return new SportsTeam(
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
                mAccentColor,
                mWins,
                mLosses,
                mTies,
                mOvertimeLosses,
                mOvertimeWins,
                mFormattedRecord);
        }
    }
}