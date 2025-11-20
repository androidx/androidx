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

package androidx.appsearch.localstorage.usagereporting;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.localstorage.stats.ClickStats;
import androidx.appsearch.localstorage.stats.SearchIntentStats;
import androidx.appsearch.localstorage.stats.SearchSessionStats;
import androidx.appsearch.usagereporting.ActionConstants;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Extractor class for analyzing a list of taken action {@link GenericDocument} and creating a list
 * of {@link SearchSessionStats}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSessionStatsExtractor {
    // TODO(b/319285816): make thresholds configurable.
    /**
     * Threshold for noise search intent detection, in millisecond. A search action will be
     * considered as a noise (and skipped) if all of the following conditions are satisfied:
     * <ul>
     *     <li>The action timestamp (action document creation timestamp) difference between it and
     *     its previous search action is below this threshold.
     *     <li>There is no click action associated with it.
     *     <li>Its raw query string is a prefix of the previous search action's raw query string (or
     *     the other way around).
     * </ul>
     */
    private static final long NOISE_SEARCH_INTENT_TIMESTAMP_DIFF_THRESHOLD_MILLIS = 2000L;

    /**
     * Threshold for independent search intent detection, in millisecond. If the action timestamp
     * (action document creation timestamp) difference between the previous and the current search
     * action exceeds this threshold, then the current search action will be considered as a
     * completely independent search intent (i.e. belonging to a new search session), and there will
     * be no correlation analysis between the previous and the current search action.
     */
    private static final long INDEPENDENT_SEARCH_INTENT_TIMESTAMP_DIFF_THRESHOLD_MILLIS =
            10L * 60 * 1000;

    /**
     * Threshold for marking good click (compared with {@code timeStayOnResultMillis}), in
     * millisecond. A good click means the user spent decent amount of time on the clicked result
     * document.
     */
    private static final long GOOD_CLICK_TIME_STAY_ON_RESULT_THRESHOLD_MILLIS = 2000L;

    /**
     * Threshold for backspace count to become query abandonment. If the user hits backspace for at
     * least QUERY_ABANDONMENT_BACKSPACE_COUNT times, then the query correction type will be
     * determined as abandonment.
     */
    private static final int QUERY_ABANDONMENT_BACKSPACE_COUNT = 2;

    /**
     * Returns the query correction type between the previous and current search actions.
     *
     * @param currSearchAction the current search action {@link SearchActionGenericDocument}.
     * @param prevSearchAction the previous search action {@link SearchActionGenericDocument}.
     */
    @SearchIntentStats.QueryCorrectionType
    public static int getQueryCorrectionType(
            @NonNull SearchActionGenericDocument currSearchAction,
            @Nullable SearchActionGenericDocument prevSearchAction) {
        Objects.requireNonNull(currSearchAction);

        if (currSearchAction.getQuery() == null) {
            // Query correction type cannot be determined if the client didn't provide the raw query
            // string.
            return SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN;
        }
        if (prevSearchAction == null) {
            // If the previous search action is missing, then it is the first query.
            return SearchIntentStats.QUERY_CORRECTION_TYPE_FIRST_QUERY;
        } else if (prevSearchAction.getQuery() == null) {
            // Query correction type cannot be determined if the client didn't provide the raw query
            // string.
            return SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN;
        }

        // Determine the query correction type by comparing the current and previous raw query
        // strings.
        String prevQuery = prevSearchAction.getQuery();
        String currQuery = currSearchAction.getQuery();
        if (prevQuery == null || currQuery == null) {
            return SearchIntentStats.QUERY_CORRECTION_TYPE_UNKNOWN;
        }
        int commonPrefixLength = getCommonPrefixLength(prevQuery, currQuery);
        // If the user hits backspace >= QUERY_ABANDONMENT_BACKSPACE_COUNT times, then it is query
        // abandonment. Otherwise, it is query refinement.
        if (commonPrefixLength <= prevQuery.length() - QUERY_ABANDONMENT_BACKSPACE_COUNT) {
            return SearchIntentStats.QUERY_CORRECTION_TYPE_ABANDONMENT;
        } else {
            return SearchIntentStats.QUERY_CORRECTION_TYPE_REFINEMENT;
        }
    }

    /**
     * Returns a list of {@link SearchSessionStats} extracted from the given list of taken action
     * {@link GenericDocument}.
     *
     * <p>A search session consists of several related search intents.
     *
     * <p>A search intent consists of a valid search action with 0 or more click actions. To extract
     * search intent metrics, this function will try to group the given taken actions into several
     * search intents, and yield a {@link SearchIntentStats} for each search intent. Finally related
     * {@link SearchIntentStats} will be wrapped into {@link SearchSessionStats}.
     *
     * @param packageName The package name of the caller.
     * @param database The database name of the caller.
     * @param genericDocuments a list of taken actions in generic document form.
     * @param isVMEnabled whether the pVM is enabled in AppSearch.
     */
    public @NonNull List<SearchSessionStats> extract(
            @NonNull String packageName,
            @Nullable String database,
            @NonNull List<GenericDocument> genericDocuments,
            boolean isVMEnabled) {
        Objects.requireNonNull(genericDocuments);

        // Convert GenericDocument list to TakenActionGenericDocument list and sort them by document
        // creation timestamp.
        List<TakenActionGenericDocument> takenActionGenericDocuments =
                new ArrayList<>(genericDocuments.size());
        for (int i = 0; i < genericDocuments.size(); ++i) {
            try {
                takenActionGenericDocuments.add(
                        TakenActionGenericDocument.create(genericDocuments.get(i)));
            } catch (IllegalArgumentException e) {
                // Skip generic documents with unknown action type.
            }
        }
        Collections.sort(takenActionGenericDocuments,
                (TakenActionGenericDocument doc1, TakenActionGenericDocument doc2) ->
                        Long.compare(doc1.getCreationTimestampMillis(),
                                doc2.getCreationTimestampMillis()));

        List<SearchSessionStats> result = new ArrayList<>();
        SearchSessionStats.Builder searchSessionStatsBuilder = null;
        SearchActionGenericDocument prevSearchAction = null;
        // Clients are expected to report search action followed by its associated click actions.
        // For example, [searchAction1, clickAction1, searchAction2, searchAction3, clickAction2,
        // clickAction3]:
        // - There are 3 search actions and 3 click actions.
        // - clickAction1 is associated with searchAction1.
        // - There is no click action associated with searchAction2.
        // - clickAction2 and clickAction3 are associated with searchAction3.
        // Here we're going to break down the list into segments. Each segment starts with a search
        // action followed by 0 or more associated click actions, and they form a single search
        // intent. We will analyze and extract metrics from the taken actions for the search intent.
        //
        // If a search intent is considered independent from the previous one, then we will start a
        // new search session analysis.
        for (int i = 0; i < takenActionGenericDocuments.size(); ++i) {
            if (takenActionGenericDocuments.get(i).getActionType()
                    != ActionConstants.ACTION_TYPE_SEARCH) {
                continue;
            }

            SearchActionGenericDocument currSearchAction =
                    (SearchActionGenericDocument) takenActionGenericDocuments.get(i);
            List<ClickActionGenericDocument> clickActions = new ArrayList<>();
            // Get all click actions associated with the current search action by advancing until
            // the next search action.
            while (i + 1 < takenActionGenericDocuments.size()
                    && takenActionGenericDocuments.get(i + 1).getActionType()
                        != ActionConstants.ACTION_TYPE_SEARCH) {
                if (takenActionGenericDocuments.get(i + 1).getActionType()
                        == ActionConstants.ACTION_TYPE_CLICK) {
                    clickActions.add(
                            (ClickActionGenericDocument) takenActionGenericDocuments.get(i + 1));
                }
                ++i;
            }

            // Get the reference of the next search action if it exists.
            SearchActionGenericDocument nextSearchAction = null;
            if (i + 1 < takenActionGenericDocuments.size()
                    && takenActionGenericDocuments.get(i + 1).getActionType()
                        == ActionConstants.ACTION_TYPE_SEARCH) {
                nextSearchAction =
                        (SearchActionGenericDocument) takenActionGenericDocuments.get(i + 1);
            }

            if (prevSearchAction != null
                    && isIndependentSearchAction(currSearchAction, prevSearchAction)) {
                // If the current search action is independent from the previous one, then:
                // - Build and append the previous search session stats.
                // - Start a new search session analysis.
                // - Ignore the previous search action when extracting stats.
                if (searchSessionStatsBuilder != null) {
                    result.add(searchSessionStatsBuilder.build());
                    searchSessionStatsBuilder = null;
                }
                prevSearchAction = null;
            } else if (clickActions.isEmpty()
                    && isIntermediateSearchAction(
                    currSearchAction, prevSearchAction, nextSearchAction)) {
                // If the current search action is an intermediate search action with no click
                // actions, then we consider it as a noise and skip it.
                continue;
            }

            // Now we get a valid search intent (the current search action + a list of click actions
            // associated with it). Extract metrics and add SearchIntentStats into this search
            // session.
            if (searchSessionStatsBuilder == null) {
                searchSessionStatsBuilder =
                        new SearchSessionStats.Builder(packageName).setDatabase(database)
                                .setLaunchVMEnabled(isVMEnabled);
            }
            searchSessionStatsBuilder.addSearchIntentsStats(
                    createSearchIntentStats(
                            packageName,
                            database,
                            currSearchAction,
                            clickActions,
                            prevSearchAction,
                            isVMEnabled));
            prevSearchAction = currSearchAction;
        }
        if (searchSessionStatsBuilder != null) {
            result.add(searchSessionStatsBuilder.build());
        }
        return result;
    }

    /**
     * Creates a {@link SearchIntentStats} object from the current search action + its associated
     * click actions, and the previous search action (in generic document form).
     */
    private SearchIntentStats createSearchIntentStats(
            @NonNull String packageName,
            @Nullable String database,
            @NonNull SearchActionGenericDocument currSearchAction,
            @NonNull List<ClickActionGenericDocument> clickActions,
            @Nullable SearchActionGenericDocument prevSearchAction,
            boolean isVMEnabled) {
        SearchIntentStats.Builder builder = new SearchIntentStats.Builder(packageName)
                .setDatabase(database)
                .setTimestampMillis(currSearchAction.getCreationTimestampMillis())
                .setCurrQuery(currSearchAction.getQuery())
                .setNumResultsFetched(currSearchAction.getFetchedResultCount())
                .setQueryCorrectionType(getQueryCorrectionType(currSearchAction, prevSearchAction))
                .setLaunchVMEnabled(isVMEnabled);
        if (prevSearchAction != null) {
            builder.setPrevQuery(prevSearchAction.getQuery());
        }
        for (int i = 0; i < clickActions.size(); ++i) {
            builder.addClicksStats(createClickStats(clickActions.get(i), isVMEnabled));
        }
        return builder.build();
    }

    /**
     * Creates a {@link ClickStats} object from the given click action (in generic document form).
     */
    private ClickStats createClickStats(
            ClickActionGenericDocument clickAction, boolean isVMEnabled) {
        // A click is considered good if:
        // - The user spent decent amount of time on the clicked document.
        // - OR the client didn't provide timeStayOnResultMillis. In this case, the value will be 0.
        boolean isGoodClick =
                clickAction.getTimeStayOnResultMillis() <= 0
                        || clickAction.getTimeStayOnResultMillis()
                        >= GOOD_CLICK_TIME_STAY_ON_RESULT_THRESHOLD_MILLIS;
        return new ClickStats.Builder()
                .setTimestampMillis(clickAction.getCreationTimestampMillis())
                .setResultRankInBlock(clickAction.getResultRankInBlock())
                .setResultRankGlobal(clickAction.getResultRankGlobal())
                .setTimeStayOnResultMillis(clickAction.getTimeStayOnResultMillis())
                .setIsGoodClick(isGoodClick)
                .setLaunchVMEnabled(isVMEnabled)
                .build();
    }

    /**
     * Returns if the current search action is an intermediate search action.
     *
     * <p>An intermediate search action is used for detecting the situation when the user adds or
     * deletes characters from the query (e.g. "a" -> "app" -> "apple" or "apple" -> "app" -> "a")
     * within a short period of time. More precisely, it has to satisfy all of the following
     * conditions:
     * <ul>
     *     <li>There are related (non-independent) search actions before and after it.
     *     <li>It occurs within the threshold after its previous search action.
     *     <li>Its raw query string is a prefix of its previous search action's raw query string, or
     *     the opposite direction.
     * </ul>
     */
    private static boolean isIntermediateSearchAction(
            @NonNull SearchActionGenericDocument currSearchAction,
            @Nullable SearchActionGenericDocument prevSearchAction,
            @Nullable SearchActionGenericDocument nextSearchAction) {
        Objects.requireNonNull(currSearchAction);

        if (prevSearchAction == null || nextSearchAction == null) {
            return false;
        }

        // Whether the next search action is independent from the current search action. If true,
        // then the current search action will not be considered as an intermediate search action
        // since it is the last search action of the search session.
        boolean isNextSearchActionIndependent =
                isIndependentSearchAction(nextSearchAction, currSearchAction);

        // Whether the current search action occurs within the threshold after the previous search
        // action.
        boolean occursWithinTimeThreshold =
                currSearchAction.getCreationTimestampMillis()
                        - prevSearchAction.getCreationTimestampMillis()
                        <= NOISE_SEARCH_INTENT_TIMESTAMP_DIFF_THRESHOLD_MILLIS;

        // Whether the previous search action's raw query string is a prefix of the current search
        // action's, or the opposite direction (e.g. "app" -> "apple" and "apple" -> "app").
        String prevQuery = prevSearchAction.getQuery();
        String currQuery = currSearchAction.getQuery();
        boolean isPrefix = prevQuery != null && currQuery != null
                && (currQuery.startsWith(prevQuery) || prevQuery.startsWith(currQuery));

        return !isNextSearchActionIndependent && occursWithinTimeThreshold && isPrefix;
    }

    /**
     * Returns if the current search action is independent from the previous search action.
     *
     * <p>If the current search action occurs later than the threshold after the previous search
     * action, then they are considered independent.
     */
    private static boolean isIndependentSearchAction(
            @NonNull SearchActionGenericDocument currSearchAction,
            @NonNull SearchActionGenericDocument prevSearchAction) {
        Objects.requireNonNull(currSearchAction);
        Objects.requireNonNull(prevSearchAction);

        long searchTimeDiffMillis = currSearchAction.getCreationTimestampMillis()
                - prevSearchAction.getCreationTimestampMillis();
        return searchTimeDiffMillis > INDEPENDENT_SEARCH_INTENT_TIMESTAMP_DIFF_THRESHOLD_MILLIS;
    }

    /** Returns the common prefix length of the given 2 strings. */
    private static int getCommonPrefixLength(@NonNull String s1, @NonNull String s2) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);

        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; ++i) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return minLength;
    }
}
