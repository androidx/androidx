/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class represents one of the results obtained from an AppSearch query.
 *
 * <p>This allows clients to obtain:
 * <ul>
 *   <li>The document which matched, using {@link #getGenericDocument}
 *   <li>Information about which properties in the document matched, and "snippet" information
 *       containing textual summaries of the document's matches, using {@link #getMatchInfos}
 *  </ul>
 *
 * <p>"Snippet" refers to a substring of text from the content of document that is returned as a
 * part of search result.
 *
 * @see SearchResults
 */
public final class SearchResult {
    static final String DOCUMENT_FIELD = "document";
    static final String MATCH_INFOS_FIELD = "matchInfos";
    static final String PACKAGE_NAME_FIELD = "packageName";
    static final String DATABASE_NAME_FIELD = "databaseName";
    static final String RANKING_SIGNAL_FIELD = "rankingSignal";
    static final String JOINED_RESULTS = "joinedResults";

    @NonNull
    private final Bundle mBundle;

    /** Cache of the inflated document. Comes from inflating mDocumentBundle at first use. */
    @Nullable
    private GenericDocument mDocument;

    /** Cache of the inflated matches. Comes from inflating mMatchBundles at first use. */
    @Nullable
    private List<MatchInfo> mMatchInfos;

    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SearchResult(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

// @exportToFramework:startStrip()
    /**
     * Contains the matching document, converted to the given document class.
     *
     * <p>This is equivalent to calling {@code getGenericDocument().toDocumentClass(T.class)}.
     *
     * @param documentClass the document class to be passed to
     *                      {@link GenericDocument#toDocumentClass(Class)}.
     * @return Document object which matched the query.
     * @throws AppSearchException if no factory for this document class could be found on the
     *       classpath.
     * @see GenericDocument#toDocumentClass(Class)
     */
    @NonNull
    public <T> T getDocument(@NonNull Class<T> documentClass) throws AppSearchException {
        return getDocument(documentClass, /* documentClassMap= */null);
    }

    /**
     * Contains the matching document, converted to the given document class.
     *
     * <p>This is equivalent to calling {@code getGenericDocument().toDocumentClass(T.class,
     * documentClassMap)}.
     *
     * @param documentClass the document class to be passed to
     *                      {@link GenericDocument#toDocumentClass(Class, Map)}.
     * @param documentClassMap the document class map to be passed to
     *                         {@link GenericDocument#toDocumentClass(Class, Map)}.
     * @return Document object which matched the query.
     * @throws AppSearchException if no factory for this document class could be found on the
     *                            classpath.
     * @see GenericDocument#toDocumentClass(Class, Map)
     */
    @NonNull
    public <T> T getDocument(@NonNull Class<T> documentClass,
            @Nullable Map<String, List<String>> documentClassMap) throws AppSearchException {
        Preconditions.checkNotNull(documentClass);
        return getGenericDocument().toDocumentClass(documentClass, documentClassMap);
    }
// @exportToFramework:endStrip()

    /**
     * Contains the matching {@link GenericDocument}.
     *
     * @return Document object which matched the query.
     */
    @NonNull
    public GenericDocument getGenericDocument() {
        if (mDocument == null) {
            mDocument = new GenericDocument(
                    Preconditions.checkNotNull(mBundle.getBundle(DOCUMENT_FIELD)));
        }
        return mDocument;
    }

    /**
     * Returns a list of {@link MatchInfo}s providing information about how the document in
     * {@link #getGenericDocument} matched the query.
     *
     * @return List of matches based on {@link SearchSpec}. If snippeting is disabled using
     * {@link SearchSpec.Builder#setSnippetCount} or
     * {@link SearchSpec.Builder#setSnippetCountPerProperty}, for all results after that
     * value, this method returns an empty list.
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public List<MatchInfo> getMatchInfos() {
        if (mMatchInfos == null) {
            List<Bundle> matchBundles =
                    Preconditions.checkNotNull(mBundle.getParcelableArrayList(MATCH_INFOS_FIELD));
            mMatchInfos = new ArrayList<>(matchBundles.size());
            for (int i = 0; i < matchBundles.size(); i++) {
                MatchInfo matchInfo = new MatchInfo(matchBundles.get(i), getGenericDocument());
                if (mMatchInfos != null) {
                    // This additional check is added for NullnessChecker.
                    mMatchInfos.add(matchInfo);
                }
            }
        }
        // This check is added for NullnessChecker, mMatchInfos will always be NonNull.
        return Preconditions.checkNotNull(mMatchInfos);
    }

    /**
     * Contains the package name of the app that stored the {@link GenericDocument}.
     *
     * @return Package name that stored the document
     */
    @NonNull
    public String getPackageName() {
        return Preconditions.checkNotNull(mBundle.getString(PACKAGE_NAME_FIELD));
    }

    /**
     * Contains the database name that stored the {@link GenericDocument}.
     *
     * @return Name of the database within which the document is stored
     */
    @NonNull
    public String getDatabaseName() {
        return Preconditions.checkNotNull(mBundle.getString(DATABASE_NAME_FIELD));
    }

    /**
     * Returns the ranking signal of the {@link GenericDocument}, according to the
     * ranking strategy set in {@link SearchSpec.Builder#setRankingStrategy(int)}.
     *
     * The meaning of the ranking signal and its value is determined by the selected ranking
     * strategy:
     * <ul>
     * <li>{@link SearchSpec#RANKING_STRATEGY_NONE} - this value will be 0</li>
     * <li>{@link SearchSpec#RANKING_STRATEGY_DOCUMENT_SCORE} - the value returned by calling
     * {@link GenericDocument#getScore()} on the document returned by
     * {@link #getGenericDocument()}</li>
     * <li>{@link SearchSpec#RANKING_STRATEGY_CREATION_TIMESTAMP} - the value returned by calling
     * {@link GenericDocument#getCreationTimestampMillis()} on the document returned by
     * {@link #getGenericDocument()}</li>
     * <li>{@link SearchSpec#RANKING_STRATEGY_RELEVANCE_SCORE} - an arbitrary double value where
     * a higher value means more relevant</li>
     * <li>{@link SearchSpec#RANKING_STRATEGY_USAGE_COUNT} - the number of times usage has been
     * reported for the document returned by {@link #getGenericDocument()}</li>
     * <li>{@link SearchSpec#RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP} - the timestamp of the
     * most recent usage that has been reported for the document returned by
     * {@link #getGenericDocument()}</li>
     * </ul>
     *
     * @return Ranking signal of the document
     */
    public double getRankingSignal() {
        return mBundle.getDouble(RANKING_SIGNAL_FIELD);
    }

    /**
     * Gets a list of {@link SearchResult} joined from the join operation.
     *
     * <p> These joined documents match the outer document as specified in the {@link JoinSpec}
     * with parentPropertyExpression and childPropertyExpression. They are ordered according to the
     * {@link JoinSpec#getNestedSearchSpec}, and as many SearchResults as specified by
     * {@link JoinSpec#getMaxJoinedResultCount} will be returned. If no {@link JoinSpec} was
     * specified, this returns an empty list.
     *
     * <p> This method is inefficient to call repeatedly, as new {@link SearchResult} objects are
     * created each time.
     *
     * @return a List of SearchResults containing joined documents.
     */
    @NonNull
    @SuppressWarnings("deprecation") // Bundle#getParcelableArrayList(String) is deprecated.
    public List<SearchResult> getJoinedResults() {
        ArrayList<Bundle> bundles = mBundle.getParcelableArrayList(JOINED_RESULTS);
        if (bundles == null) {
            return new ArrayList<>();
        }
        List<SearchResult> res = new ArrayList<>(bundles.size());
        for (int i = 0; i < bundles.size(); i++) {
            res.add(new SearchResult(bundles.get(i)));
        }

        return res;
    }

    /** Builder for {@link SearchResult} objects. */
    public static final class Builder {
        private final String mPackageName;
        private final String mDatabaseName;
        private ArrayList<Bundle> mMatchInfoBundles = new ArrayList<>();
        private GenericDocument mGenericDocument;
        private double mRankingSignal;
        private ArrayList<Bundle> mJoinedResults = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Constructs a new builder for {@link SearchResult} objects.
         *
         * @param packageName the package name the matched document belongs to
         * @param databaseName the database name the matched document belongs to.
         */
        public Builder(@NonNull String packageName, @NonNull String databaseName) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mDatabaseName = Preconditions.checkNotNull(databaseName);
        }

// @exportToFramework:startStrip()
        /**
         * Sets the document which matched.
         *
         * @param document An instance of a class annotated with
         * {@link androidx.appsearch.annotation.Document}.
         *
         * @throws AppSearchException if an error occurs converting a document class into a
         *                            {@link GenericDocument}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setDocument(@NonNull Object document) throws AppSearchException {
            Preconditions.checkNotNull(document);
            resetIfBuilt();
            return setGenericDocument(GenericDocument.fromDocumentClass(document));
        }
// @exportToFramework:endStrip()

        /** Sets the document which matched. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setGenericDocument(@NonNull GenericDocument document) {
            Preconditions.checkNotNull(document);
            resetIfBuilt();
            mGenericDocument = document;
            return this;
        }

        /** Adds another match to this SearchResult. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addMatchInfo(@NonNull MatchInfo matchInfo) {
            Preconditions.checkState(
                    matchInfo.mDocument == null,
                    "This MatchInfo is already associated with a SearchResult and can't be "
                            + "reassigned");
            resetIfBuilt();
            mMatchInfoBundles.add(matchInfo.mBundle);
            return this;
        }

        /** Sets the ranking signal of the matched document in this SearchResult. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRankingSignal(double rankingSignal) {
            resetIfBuilt();
            mRankingSignal = rankingSignal;
            return this;
        }

        /**
         * Adds a {@link SearchResult} that was joined by the {@link JoinSpec}.
         * @param joinedResult The joined SearchResult to add.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addJoinedResult(@NonNull SearchResult joinedResult) {
            resetIfBuilt();
            mJoinedResults.add(joinedResult.getBundle());
            return this;
        }

        /** Constructs a new {@link SearchResult}. */
        @NonNull
        public SearchResult build() {
            Bundle bundle = new Bundle();
            bundle.putString(PACKAGE_NAME_FIELD, mPackageName);
            bundle.putString(DATABASE_NAME_FIELD, mDatabaseName);
            bundle.putBundle(DOCUMENT_FIELD, mGenericDocument.getBundle());
            bundle.putDouble(RANKING_SIGNAL_FIELD, mRankingSignal);
            bundle.putParcelableArrayList(MATCH_INFOS_FIELD, mMatchInfoBundles);
            bundle.putParcelableArrayList(JOINED_RESULTS, mJoinedResults);
            mBuilt = true;
            return new SearchResult(bundle);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mMatchInfoBundles = new ArrayList<>(mMatchInfoBundles);
                mJoinedResults = new ArrayList<>(mJoinedResults);
                mBuilt = false;
            }
        }
    }

    /**
     * This class represents match objects for any snippets that might be present in
     * {@link SearchResults} from a query. Using this class, you can get:
     * <ul>
     *     <li>the full text - all of the text in that String property</li>
     *     <li>the exact term match - the 'term' (full word) that matched the query</li>
     *     <li>the subterm match - the portion of the matched term that appears in the query</li>
     *     <li>a suggested text snippet - a portion of the full text surrounding the exact term
     *     match, set to term boundaries. The size of the snippet is specified in
     *     {@link SearchSpec.Builder#setMaxSnippetSize}</li>
     * </ul>
     * for each match in the document.
     *
     * <p>Class Example 1:
     * <p>A document contains the following text in property "subject":
     * <p>"A commonly used fake word is foo. Another nonsense word that’s used a lot is bar."
     *
     * <p>If the queryExpression is "foo" and {@link SearchSpec#getMaxSnippetSize}  is 10,
     * <ul>
     *      <li>{@link MatchInfo#getPropertyPath()} returns "subject"</li>
     *      <li>{@link MatchInfo#getFullText()} returns "A commonly used fake word is foo. Another
     * nonsense word that’s used a lot is bar."</li>
     *      <li>{@link MatchInfo#getExactMatchRange()} returns [29, 32]</li>
     *      <li>{@link MatchInfo#getExactMatch()} returns "foo"</li>
     *      <li>{@link MatchInfo#getSubmatchRange()} returns [29, 32]</li>
     *      <li>{@link MatchInfo#getSubmatch()} returns "foo"</li>
     *      <li>{@link MatchInfo#getSnippetRange()} returns [26, 33]</li>
     *      <li>{@link MatchInfo#getSnippet()} returns "is foo."</li>
     * </ul>
     * <p>
     * <p>Class Example 2:
     * <p>A document contains one property named "subject" and one property named "sender" which
     * contains a "name" property.
     *
     * In this case, we will have 2 property paths: {@code sender.name} and {@code subject}.
     * <p>Let {@code sender.name = "Test Name Jr."} and
     * {@code subject = "Testing 1 2 3"}
     *
     * <p>If the queryExpression is "Test" with {@link SearchSpec#TERM_MATCH_PREFIX} and
     * {@link SearchSpec#getMaxSnippetSize} is 10. We will have 2 matches:
     *
     * <p> Match-1
     * <ul>
     *      <li>{@link MatchInfo#getPropertyPath()} returns "sender.name"</li>
     *      <li>{@link MatchInfo#getFullText()} returns "Test Name Jr."</li>
     *      <li>{@link MatchInfo#getExactMatchRange()} returns [0, 4]</li>
     *      <li>{@link MatchInfo#getExactMatch()} returns "Test"</li>
     *      <li>{@link MatchInfo#getSubmatchRange()} returns [0, 4]</li>
     *      <li>{@link MatchInfo#getSubmatch()} returns "Test"</li>
     *      <li>{@link MatchInfo#getSnippetRange()} returns [0, 9]</li>
     *      <li>{@link MatchInfo#getSnippet()} returns "Test Name"</li>
     * </ul>
     * <p> Match-2
     * <ul>
     *      <li>{@link MatchInfo#getPropertyPath()} returns "subject"</li>
     *      <li>{@link MatchInfo#getFullText()} returns "Testing 1 2 3"</li>
     *      <li>{@link MatchInfo#getExactMatchRange()} returns [0, 7]</li>
     *      <li>{@link MatchInfo#getExactMatch()} returns "Testing"</li>
     *      <li>{@link MatchInfo#getSubmatchRange()} returns [0, 4]</li>
     *      <li>{@link MatchInfo#getSubmatch()} returns "Test"</li>
     *      <li>{@link MatchInfo#getSnippetRange()} returns [0, 9]</li>
     *      <li>{@link MatchInfo#getSnippet()} returns "Testing 1"</li>
     * </ul>
     */
    public static final class MatchInfo {
        /** The path of the matching snippet property. */
        private static final String PROPERTY_PATH_FIELD = "propertyPath";
        private static final String EXACT_MATCH_RANGE_LOWER_FIELD = "exactMatchRangeLower";
        private static final String EXACT_MATCH_RANGE_UPPER_FIELD = "exactMatchRangeUpper";
        private static final String SUBMATCH_RANGE_LOWER_FIELD = "submatchRangeLower";
        private static final String SUBMATCH_RANGE_UPPER_FIELD = "submatchRangeUpper";
        private static final String SNIPPET_RANGE_LOWER_FIELD = "snippetRangeLower";
        private static final String SNIPPET_RANGE_UPPER_FIELD = "snippetRangeUpper";

        private final String mPropertyPath;
        @Nullable
        private PropertyPath mPropertyPathObject = null;
        final Bundle mBundle;

        /**
         * Document which the match comes from.
         *
         * <p>If this is {@code null}, methods which require access to the document, like
         * {@link #getExactMatch}, will throw {@link NullPointerException}.
         */
        @Nullable
        final GenericDocument mDocument;

        /** Full text of the matched property. Populated on first use. */
        @Nullable
        private String mFullText;

        /** Range of property that exactly matched the query. Populated on first use. */
        @Nullable
        private MatchRange mExactMatchRange;

        /**
         * Range of property that corresponds to the subsequence of the exact match that directly
         * matches a query term. Populated on first use.
         */
        @Nullable
        private MatchRange mSubmatchRange;

        /** Range of some reasonable amount of context around the query. Populated on first use. */
        @Nullable
        private MatchRange mWindowRange;

        MatchInfo(@NonNull Bundle bundle, @Nullable GenericDocument document) {
            mBundle = Preconditions.checkNotNull(bundle);
            mDocument = document;
            mPropertyPath = Preconditions.checkNotNull(bundle.getString(PROPERTY_PATH_FIELD));
        }

        /**
         * Gets the property path corresponding to the given entry.
         *
         * <p>A property path is a '.' - delimited sequence of property names indicating which
         * property in the document these snippets correspond to.
         *
         * <p>Example properties: 'body', 'sender.name', 'sender.emailaddress', etc.
         * For class example 1 this returns "subject"
         */
        @NonNull
        public String getPropertyPath() {
            return mPropertyPath;
        }

        /**
         * Gets a {@link PropertyPath} object representing the property path corresponding to the
         * given entry.
         *
         * <p> Methods such as {@link GenericDocument#getPropertyDocument} accept a path as a
         * string rather than a {@link PropertyPath} object. However, you may want to manipulate
         * the path before getting a property document. This method returns a {@link PropertyPath}
         * rather than a String for easier path manipulation, which can then be converted to a
         * String.
         *
         * @see #getPropertyPath
         * @see PropertyPath
         */
        @NonNull
        public PropertyPath getPropertyPathObject() {
            if (mPropertyPathObject == null) {
                mPropertyPathObject = new PropertyPath(mPropertyPath);
            }
            return mPropertyPathObject;
        }

        /**
         * Gets the full text corresponding to the given entry.
         * <p>Class example 1: this returns "A commonly used fake word is foo. Another nonsense
         * word that's used a lot is bar."
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test Name Jr." and,
         * for the second {@link MatchInfo}, this returns "Testing 1 2 3".
         */
        @NonNull
        public String getFullText() {
            if (mFullText == null) {
                if (mDocument == null) {
                    throw new IllegalStateException(
                            "Document has not been populated; this MatchInfo cannot be used yet");
                }
                mFullText = getPropertyValues(mDocument, mPropertyPath);
            }
            return mFullText;
        }

        /**
         * Gets the {@link MatchRange} of the exact term of the given entry that matched the query.
         * <p>Class example 1: this returns [29, 32].
         * <p>Class example 2: for the first {@link MatchInfo}, this returns [0, 4] and, for the
         * second {@link MatchInfo}, this returns [0, 7].
         */
        @NonNull
        public MatchRange getExactMatchRange() {
            if (mExactMatchRange == null) {
                mExactMatchRange = new MatchRange(
                        mBundle.getInt(EXACT_MATCH_RANGE_LOWER_FIELD),
                        mBundle.getInt(EXACT_MATCH_RANGE_UPPER_FIELD));
            }
            return mExactMatchRange;
        }

        /**
         * Gets the exact term of the given entry that matched the query.
         * <p>Class example 1: this returns "foo".
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test" and, for the
         * second {@link MatchInfo}, this returns "Testing".
         */
        @NonNull
        public CharSequence getExactMatch() {
            return getSubstring(getExactMatchRange());
        }

        /**
         * Gets the {@link MatchRange} of the exact term subsequence of the given entry that matched
         * the query.
         * <p>Class example 1: this returns [29, 32].
         * <p>Class example 2: for the first {@link MatchInfo}, this returns [0, 4] and, for the
         * second {@link MatchInfo}, this returns [0, 4].
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         *
         * @throws UnsupportedOperationException if {@link Features#isFeatureSupported} is
         * false.
         * <!--@exportToFramework:else()-->
         */
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)
        // @exportToFramework:endStrip()
        @NonNull
        public MatchRange getSubmatchRange() {
            checkSubmatchSupported();
            if (mSubmatchRange == null) {
                mSubmatchRange = new MatchRange(
                        mBundle.getInt(SUBMATCH_RANGE_LOWER_FIELD),
                        mBundle.getInt(SUBMATCH_RANGE_UPPER_FIELD));
            }
            return mSubmatchRange;
        }

        /**
         * Gets the exact term subsequence of the given entry that matched the query.
         * <p>Class example 1: this returns "foo".
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test" and, for the
         * second {@link MatchInfo}, this returns "Test".
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         *
         * @throws UnsupportedOperationException if {@link Features#isFeatureSupported} is
         * false.
         * <!--@exportToFramework:else()-->
         */
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)
        // @exportToFramework:endStrip()
        @NonNull
        public CharSequence getSubmatch() {
            checkSubmatchSupported();
            return getSubstring(getSubmatchRange());
        }

        /**
         * Gets the snippet {@link MatchRange} corresponding to the given entry.
         * <p>Only populated when set maxSnippetSize > 0 in
         * {@link SearchSpec.Builder#setMaxSnippetSize}.
         * <p>Class example 1: this returns [29, 41].
         * <p>Class example 2: for the first {@link MatchInfo}, this returns [0, 9] and, for the
         * second {@link MatchInfo}, this returns [0, 13].
         */
        @NonNull
        public MatchRange getSnippetRange() {
            if (mWindowRange == null) {
                mWindowRange = new MatchRange(
                        mBundle.getInt(SNIPPET_RANGE_LOWER_FIELD),
                        mBundle.getInt(SNIPPET_RANGE_UPPER_FIELD));
            }
            return mWindowRange;
        }

        /**
         * Gets the snippet corresponding to the given entry.
         * <p>Snippet - Provides a subset of the content to display. Only populated when requested
         * maxSnippetSize > 0. The size of this content can be changed by
         * {@link SearchSpec.Builder#setMaxSnippetSize}. Windowing is centered around the middle of
         * the matched token with content on either side clipped to token boundaries.
         * <p>Class example 1: this returns "foo. Another".
         * <p>Class example 2: for the first {@link MatchInfo}, this returns "Test Name" and, for
         * the second {@link MatchInfo}, this returns "Testing 1 2 3".
         */
        @NonNull
        public CharSequence getSnippet() {
            return getSubstring(getSnippetRange());
        }

        private CharSequence getSubstring(MatchRange range) {
            return getFullText().substring(range.getStart(), range.getEnd());
        }

        private void checkSubmatchSupported() {
            if (!mBundle.containsKey(SUBMATCH_RANGE_LOWER_FIELD)) {
                throw new UnsupportedOperationException(
                        "Submatch is not supported with this backend/Android API level "
                                + "combination");
            }
        }

        /** Extracts the matching string from the document. */
        private static String getPropertyValues(GenericDocument document, String propertyName) {
            String result = document.getPropertyString(propertyName);
            if (result == null) {
                throw new IllegalStateException(
                        "No content found for requested property path: " + propertyName);
            }
            return result;
        }

        /** Builder for {@link MatchInfo} objects. */
        public static final class Builder {
            private final String mPropertyPath;
            private MatchRange mExactMatchRange = new MatchRange(0, 0);
            @Nullable private MatchRange mSubmatchRange;
            private MatchRange mSnippetRange = new MatchRange(0, 0);

            /**
             * Creates a new {@link MatchInfo.Builder} reporting a match with the given property
             * path.
             *
             * <p>A property path is a dot-delimited sequence of property names indicating which
             * property in the document these snippets correspond to.
             *
             * <p>Example properties: 'body', 'sender.name', 'sender.emailaddress', etc.
             * For class example 1, this returns "subject".
             *
             * @param propertyPath A dot-delimited sequence of property names indicating which
             *                     property in the document these snippets correspond to.
             */
            public Builder(@NonNull String propertyPath) {
                mPropertyPath = Preconditions.checkNotNull(propertyPath);
            }

            /** Sets the exact {@link MatchRange} corresponding to the given entry. */
            @CanIgnoreReturnValue
            @NonNull
            public Builder setExactMatchRange(@NonNull MatchRange matchRange) {
                mExactMatchRange = Preconditions.checkNotNull(matchRange);
                return this;
            }


            /** Sets the submatch {@link MatchRange} corresponding to the given entry. */
            @CanIgnoreReturnValue
            @NonNull
            public Builder setSubmatchRange(@NonNull MatchRange matchRange) {
                mSubmatchRange = Preconditions.checkNotNull(matchRange);
                return this;
            }

            /** Sets the snippet {@link MatchRange} corresponding to the given entry. */
            @CanIgnoreReturnValue
            @NonNull
            public Builder setSnippetRange(@NonNull MatchRange matchRange) {
                mSnippetRange = Preconditions.checkNotNull(matchRange);
                return this;
            }

            /** Constructs a new {@link MatchInfo}. */
            @NonNull
            public MatchInfo build() {
                Bundle bundle = new Bundle();
                bundle.putString(SearchResult.MatchInfo.PROPERTY_PATH_FIELD, mPropertyPath);
                bundle.putInt(MatchInfo.EXACT_MATCH_RANGE_LOWER_FIELD, mExactMatchRange.getStart());
                bundle.putInt(MatchInfo.EXACT_MATCH_RANGE_UPPER_FIELD, mExactMatchRange.getEnd());
                if (mSubmatchRange != null) {
                    // Only populate the submatch fields if it was actually set.
                    bundle.putInt(MatchInfo.SUBMATCH_RANGE_LOWER_FIELD, mSubmatchRange.getStart());
                }

                if (mSubmatchRange != null) {
                    // Only populate the submatch fields if it was actually set.
                    // Moved to separate block for Nullness Checker.
                    bundle.putInt(MatchInfo.SUBMATCH_RANGE_UPPER_FIELD, mSubmatchRange.getEnd());
                }

                bundle.putInt(MatchInfo.SNIPPET_RANGE_LOWER_FIELD, mSnippetRange.getStart());
                bundle.putInt(MatchInfo.SNIPPET_RANGE_UPPER_FIELD, mSnippetRange.getEnd());
                return new MatchInfo(bundle, /*document=*/ null);
            }
        }
    }

    /**
     * Class providing the position range of matching information.
     *
     * <p> All ranges are finite, and the left side of the range is always {@code <=} the right
     * side of the range.
     *
     * <p> Example: MatchRange(0, 100) represents hundred ints from 0 to 99."
     */
    public static final class MatchRange {
        private final int mEnd;
        private final int mStart;

        /**
         * Creates a new immutable range.
         * <p> The endpoints are {@code [start, end)}; that is the range is bounded. {@code start}
         * must be lesser or equal to {@code end}.
         *
         * @param start The start point (inclusive)
         * @param end   The end point (exclusive)
         */
        public MatchRange(int start, int end) {
            if (start > end) {
                throw new IllegalArgumentException("Start point must be less than or equal to "
                        + "end point");
            }
            mStart = start;
            mEnd = end;
        }

        /** Gets the start point (inclusive). */
        public int getStart() {
            return mStart;
        }

        /** Gets the end point (exclusive). */
        public int getEnd() {
            return mEnd;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MatchRange)) {
                return false;
            }
            MatchRange otherMatchRange = (MatchRange) other;
            return this.getStart() == otherMatchRange.getStart()
                    && this.getEnd() == otherMatchRange.getEnd();
        }

        @Override
        @NonNull
        public String toString() {
            return "MatchRange { start: " + mStart + " , end: " + mEnd + "}";
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mStart, mEnd);
        }
    }
}
