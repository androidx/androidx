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

import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;

/**
 * AppSearch document representing a {@link SportsEvent} entity.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
@Document(name = "builtin:SportsEvent")
public class SportsEvent extends Event {

     /** The status for this {@link SportsEvent}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            STATUS_UNSPECIFIED,
            STATUS_UPCOMING,
            STATUS_LIVE,
            STATUS_COMPLETE,
            STATUS_DELAYED,
            STATUS_INTERRUPTED,
            STATUS_POSTPONED,
            STATUS_CANCELLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SportsEventStatus {}
    /** The status is unspecified. */
    public static final int STATUS_UNSPECIFIED = 0;
    /** The sports event is upcoming. */
    public static final int STATUS_UPCOMING = 1;
    /** The sports event is live. */
    public static final int STATUS_LIVE = 2;
    /** The sports event is complete. */
    public static final int STATUS_COMPLETE = 3;
    /** The sports event is delayed. */
    public static final int STATUS_DELAYED = 4;
    /** The sports event is interrupted. */
    public static final int STATUS_INTERRUPTED = 5;
    /** The sports event is postponed. */
    public static final int STATUS_POSTPONED = 6;
    /** The sports event is cancelled. */
    public static final int STATUS_CANCELLED = 7;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            RESULT_UNSPECIFIED,
            RESULT_HOME_TEAM_WON,
            RESULT_AWAY_TEAM_WON,
            RESULT_DRAW})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SportsEventResult {}
    /** The sports event result is unspecified. */
    public static final int RESULT_UNSPECIFIED = 0;
    /** The home team won the sports event. */
    public static final int RESULT_HOME_TEAM_WON = 1;
    /** The away team won the sports event. */
    public static final int RESULT_AWAY_TEAM_WON = 2;
    /** The sports event resolved in a draw. */
    public static final int RESULT_DRAW = 3;

    @Document.StringProperty
    private @NonNull String mSport;

    @Document.DocumentProperty
    private @Nullable Organization mOrganizer;

    @Document.LongProperty
    private @SportsEventStatus long mSportsEventStatus;

    @Document.StringProperty
    private @Nullable String mSportsEventStatusLabel;

    @Document.StringProperty
    private @Nullable String mGameTemporalState;

    @Document.StringProperty
    private @Nullable String mNotableDetail;

    @Document.DocumentProperty
    private @NonNull SportsTeam mHomeTeam;

    @Document.StringProperty
    private @Nullable String mHomeTeamScore;

    @Document.StringProperty
    private @Nullable String mHomeTeamAccessoryScore;

    @Document.DoubleProperty
    private double mHomeTeamWinProbability;

    @Document.DocumentProperty
    private @NonNull SportsTeam mAwayTeam;

    @Document.StringProperty
    private @Nullable String mAwayTeamScore;

    @Document.StringProperty
    private @Nullable String mAwayTeamAccessoryScore;

    @Document.DoubleProperty
    private double mAwayTeamWinProbability;

    @Document.BooleanProperty
    private boolean mPlaceHomeTeamAtStart;

    @Document.LongProperty
    private @SportsEventResult long mResult;

    /**
     * Constructor for {@link SportsEvent}.
     *
     * @param builder The builder to construct the {@link SportsEvent} from.
     */
    @ExperimentalAppSearchApi
    public SportsEvent(@NonNull BuilderBase<?> builder) {
        super(builder);
        this.mSport = builder.mSport;
        this.mOrganizer = builder.mOrganizer;
        this.mSportsEventStatus = builder.mSportsEventStatus;
        this.mSportsEventStatusLabel = builder.mSportsEventStatusLabel;
        this.mGameTemporalState = builder.mGameTemporalState;
        this.mNotableDetail = builder.mNotableDetail;
        this.mHomeTeam = builder.mHomeTeam;
        this.mHomeTeamScore = builder.mHomeTeamScore;
        this.mHomeTeamAccessoryScore = builder.mHomeTeamAccessoryScore;
        this.mHomeTeamWinProbability = builder.mHomeTeamWinProbability;
        this.mAwayTeam = builder.mAwayTeam;
        this.mAwayTeamScore = builder.mAwayTeamScore;
        this.mAwayTeamAccessoryScore = builder.mAwayTeamAccessoryScore;
        this.mAwayTeamWinProbability = builder.mAwayTeamWinProbability;
        this.mPlaceHomeTeamAtStart = builder.mPlaceHomeTeamAtStart;
        this.mResult = builder.mResult;
    }

    /**
     * Returns the sport of the sports event.
     */
    public @NonNull String getSport() {
        return mSport;
    }

    /**
     * Returns the {@link Organization} of the sports event, if set.
     */
    public @Nullable Organization getOrganizer() {
        return mOrganizer;
    }

    /**
     * Returns the {@link SportsEventStatus} of the sports event.
     */
    public @SportsEventStatus long getSportsEventStatus() {
        return mSportsEventStatus;
    }

    /**
     * Returns the status label of the sports event.
     *
     * Which is a localized string representation of the
     * {@link SportsEventStatus}.
     */
    public @Nullable String getSportsEventStatusLabel() {
        return mSportsEventStatusLabel;
    }

    /**
     * Returns the game temporal state of the sports event, if set.
     *
     * Temporal state of the game, e.g. "Quarter 1 - 10:00" for Basketball,
     * "Halftime" for Soccer, "9th inning" for American Baseball, etc.
     */
    public @Nullable String getGameTemporalState() {
        return mGameTemporalState;
    }

    /**
     * Returns the most recent notable detail of the sports event, if set.
     *
     * For example, "2 outs, 2 balls, 1 strike" in baseball.
     */
    public @Nullable String getNotableDetail() {
        return mNotableDetail;
    }

    /**
     * Returns the home team of the sports event.
     */
    public @NonNull SportsTeam getHomeTeam() {
        return mHomeTeam;
    }

    /**
     * Returns the home team score of the sports event, if set.
     *
     * e.g., "100" for Basketball, "3" for American Baseball,
     * "Love" or "Deuce" for Tennis, etc.
     */
    public @Nullable String getHomeTeamScore() {
        return mHomeTeamScore;
    }

    /**
     * Returns the home team accessory score of the sports event, if set.
     *
     * The accessory score is an additional contextual score that is used in
     * some sports, e.g., like penalties in Soccer, overs in Cricket, etc.
     */
    public @Nullable String getHomeTeamAccessoryScore() {
        return mHomeTeamAccessoryScore;
    }

    /**
     * Returns the home team win probability of the sports event.
     *
     * The win probability is a range from 0 to 1.
     */
    public double getHomeTeamWinProbability() {
        return mHomeTeamWinProbability;
    }

    /**
     * Returns the away team of the sports event.
     */
    public @NonNull SportsTeam getAwayTeam() {
        return mAwayTeam;
    }

    /**
     * Returns the home team score of the sports event, if set.
     *
     * e.g., "100" for Basketball, "3" for American Baseball,
     * "Love" or "Deuce" for Tennis, etc.
     */
    public @Nullable String getAwayTeamScore() {
        return mAwayTeamScore;
    }

    /**
     * Returns the away team accessory score of the sports event, if set.
     *
     * The accessory score is an additional contextual score that is used in
     * some sports, e.g., like penalties in Soccer, overs in Cricket, etc.
     */
    public @Nullable String getAwayTeamAccessoryScore() {
        return mAwayTeamAccessoryScore;
    }

    /**
     * Returns the away team win probability of the sports event, if set.
     *
     * The win probability is a range from 0 to 1.
     */
    public double getAwayTeamWinProbability() {
        return mAwayTeamWinProbability;
    }

    /**
     * Returns whether the home team should be placed at the start for
     * a visual representation of the sports event.
     */
    public boolean isPlaceHomeTeamAtStart() {
        return mPlaceHomeTeamAtStart;
    }

    /**
     * Returns the {@link SportsEventResult} for the sports event.
     *
     * During the course of the game, the value of this field should not be
     * considered. The value of this field should only be set after the sports
     * event has ended @{link SportsEventStatus.STATUS_COMPLETE} or has been
     * cancelled @{link SportsEventStatus.STATUS_CANCELLED}, or any other
     * terminal state.
     */
    public @SportsEventResult long getResult() {
        return mResult;
    }

    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {

        /**
         * Constructor for {@link SportsEvent.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         * @param startDate The start date of the sports event.
         * @param sport The sport of the sports event.
         * @param homeTeam The home team of the sports event.
         * @param awayTeam The away team of the sports event.
         **/
        public Builder(@NonNull String namespace, @NonNull String id,
            @NonNull Instant startDate,
            @NonNull String sport, @NonNull SportsTeam homeTeam,
            @NonNull SportsTeam awayTeam) {
            super(namespace, id, startDate, sport, homeTeam, awayTeam);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull SportsEvent sportsEvent) {
            super(sportsEvent);
        }
    }

    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Event.BuilderBase<T> {
        private String mSport;
        private @Nullable Organization mOrganizer;
        // Initialized to STATUS_UNSPECIFIED.
        private @SportsEventStatus long mSportsEventStatus = SportsEvent.STATUS_UNSPECIFIED;
        private @Nullable String mSportsEventStatusLabel;
        private @Nullable String mGameTemporalState;
        private @Nullable String mNotableDetail;
        private final SportsTeam mHomeTeam;
        private @Nullable String mHomeTeamScore;
        private @Nullable String mHomeTeamAccessoryScore;
        // Initialized to 0.0.
        private double mHomeTeamWinProbability = 0.0;
        private final SportsTeam mAwayTeam;
        private @Nullable String mAwayTeamScore;
        private @Nullable String mAwayTeamAccessoryScore;
        // Initialized to 0.0.
        private double mAwayTeamWinProbability = 0.0;
        // Initialized to true.
        private boolean mPlaceHomeTeamAtStart = true;
        // Initialized to RESULT_UNSPECIFIED.
        private @SportsEventResult long mResult = SportsEvent.RESULT_UNSPECIFIED;

        /**
         * Constructor for {@link SportsEvent.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         * @param startDate The start date of the sports event.
         * @param sport The sport of the sports event.
         * @param homeTeam The home team of the sports event.
         * @param awayTeam The away team of the sports event.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id,
            @NonNull Instant startDate,
            @NonNull String sport, @NonNull SportsTeam homeTeam,
            @NonNull SportsTeam awayTeam) {
            super(namespace, id, startDate);
            mSport = Preconditions.checkNotNull(sport);
            mHomeTeam = Preconditions.checkNotNull(homeTeam);
            mAwayTeam = Preconditions.checkNotNull(awayTeam);
        }

        /**
         * Constructor for {@link SportsEvent.BuilderBase} with all the existing values.
         *
         * @param sportsEvent The existing {@link SportsEvent} to copy values from.
         */
        public BuilderBase(@NonNull SportsEvent sportsEvent) {
            super(sportsEvent);
            mSport = sportsEvent.getSport();
            mHomeTeam = sportsEvent.getHomeTeam();
            mAwayTeam = sportsEvent.getAwayTeam();
            mOrganizer = sportsEvent.getOrganizer();
            mSportsEventStatus = sportsEvent.getSportsEventStatus();
            mSportsEventStatusLabel = sportsEvent.getSportsEventStatusLabel();
            mGameTemporalState = sportsEvent.getGameTemporalState();
            mNotableDetail = sportsEvent.getNotableDetail();
            mHomeTeamScore = sportsEvent.getHomeTeamScore();
            mHomeTeamAccessoryScore = sportsEvent.getHomeTeamAccessoryScore();
            mHomeTeamWinProbability = sportsEvent.getHomeTeamWinProbability();
            mAwayTeamScore = sportsEvent.getAwayTeamScore();
            mAwayTeamAccessoryScore = sportsEvent.getAwayTeamAccessoryScore();
            mAwayTeamWinProbability = sportsEvent.getAwayTeamWinProbability();
            mPlaceHomeTeamAtStart = sportsEvent.isPlaceHomeTeamAtStart();
            mResult = sportsEvent.getResult();
        }

        /**
         * Sets the {@link Organization} of the sports event.
         */
        public @NonNull T setOrganizer(@Nullable Organization organizer) {
            mOrganizer = organizer;
            return (T) this;
        }

        /**
         * Sets the status of the sports event.
         *
         * @param sportsEventStatus the {@link SportsEventStatus} of the sports
         * event.
         */
        public @NonNull T setSportsEventStatus(
            @SportsEventStatus long sportsEventStatus) {
            mSportsEventStatus = sportsEventStatus;
            return (T) this;
        }

        /**
         * Sets the status label of the sports event.
         *
         * @param sportsEventStatusLabel the status label of the sports event.
         */
        public @NonNull T setSportsEventStatusLabel(
            @Nullable String sportsEventStatusLabel) {
            mSportsEventStatusLabel = sportsEventStatusLabel;
            return (T) this;
        }

        /**
         * Sets the game temporal state of the sports event.
         *
         * Format should be a temporal state of the game, e.g.
         * "Quarter 1 - 10:00" for Basketball, "Halftime" for Soccer,
         * "9th inning" for American Baseball, etc.
         */
        public @NonNull T setGameTemporalState(
            @Nullable String gameTemporalState) {
            mGameTemporalState = gameTemporalState;
            return (T) this;
        }

        /**
         * Sets the most recent notable detail of the sports event.
         *
         * Format should be a most recent notable detail of the sports event,
         * e.g. "2 outs, 2 balls, 1 strike" in American Baseball.
         */
        public @NonNull T setNotableDetail(@Nullable String notableDetail) {
            mNotableDetail = notableDetail;
            return (T) this;
        }

        /**
         * Sets the home team score of the sports event.
         *
         * Format should be a score of the home team, e.g., "100"
         * for basketball, "3" for American Baseball, etc.
         */
        public @NonNull T setHomeTeamScore(@Nullable String homeTeamScore) {
            mHomeTeamScore = homeTeamScore;
            return (T) this;
        }

        /**
         * Sets the home team accessory score of the sports event.
         *
         * Format should be an additional contextual score that is used in some
         * sports, e.g., like penalties in Soccer, overs in Cricket, etc.
         */
        public @NonNull T setHomeTeamAccessoryScore(
            @Nullable String homeTeamAccessoryScore) {
            mHomeTeamAccessoryScore = homeTeamAccessoryScore;
            return (T) this;
        }

        /**
         * Sets the home team win probability of the sports event.
         *
         * Must be a range from 0 to 1.
         */
        public @NonNull T setHomeTeamWinProbability(double homeTeamWinProbability) {
            Preconditions.checkArgumentInRange(homeTeamWinProbability, 0, 1,
                "homeTeamWinProbability");
            mHomeTeamWinProbability = homeTeamWinProbability;
            return (T) this;
        }

        /**
         * Sets the away team score of the sports event.
         *
         * Format should be a score of the away team, e.g., "100" for
         * Basketball, "3" for American Baseball, etc.
         */
        public @NonNull T setAwayTeamScore(@Nullable String awayTeamScore) {
            mAwayTeamScore = awayTeamScore;
            return (T) this;
        }

        /**
         * Sets the away team accessory score of the sports event.
         *
         * Format should be an additional contextual score that is used in some
         * sports, e.g., like penalties in Soccer, overs in Cricket, etc.
         */
        public @NonNull T setAwayTeamAccessoryScore(
            @Nullable String awayTeamAccessoryScore) {
            mAwayTeamAccessoryScore = awayTeamAccessoryScore;
            return (T) this;
        }

        /**
         * Sets the away team win probability of the sports event.
         *
         * Must be a range from 0 to 1.
         */
        public @NonNull T setAwayTeamWinProbability(
            double awayTeamWinProbability) {
            Preconditions.checkArgumentInRange(awayTeamWinProbability, 0, 1,
                "awayTeamWinProbability");
            mAwayTeamWinProbability = awayTeamWinProbability;
            return (T) this;
        }

        /**
         * Sets whether the home team should be placed at the start for
         * a visual representation of the sports event.
         */
        public @NonNull T setPlaceHomeTeamAtStart(
            boolean placeHomeTeamAtStart) {
            mPlaceHomeTeamAtStart = placeHomeTeamAtStart;
            return (T) this;
        }

        /**
         * Sets whether the home team won the sports event.
         *
         * During the course of the game, the value of this field should not be
         * considered. The value of this field should only be set after the
         * sports event has ended @{link SportsEventStatus.STATUS_COMPLETE} or
         * has been cancelled @{link SportsEventStatus.STATUS_CANCELLED}, or
         * any other terminal state.
         */
        public @NonNull T setResult(@SportsEventResult long result) {
            mResult = result;
            return (T) this;
        }

        @Override
        public @NonNull SportsEvent build() {
            return new SportsEvent(this);
        }
    }
}