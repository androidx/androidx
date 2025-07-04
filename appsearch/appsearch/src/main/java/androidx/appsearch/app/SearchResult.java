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

import static androidx.appsearch.app.SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE;
import static androidx.appsearch.app.SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.GenericDocumentParcel;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.EmbeddingMatchInfoCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.MatchInfoCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.MatchRangeCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.SearchResultCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.TextMatchInfoCreator;
import androidx.appsearch.util.BundleUtil;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@SafeParcelable.Class(creator = "SearchResultCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SearchResult extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<SearchResult> CREATOR =
            new SearchResultCreator();

    @Field(id = 1)
    final GenericDocumentParcel mDocument;
    @Field(id = 2)
    final List<MatchInfo> mMatchInfos;
    @Field(id = 3, getter = "getPackageName")
    private final String mPackageName;
    @Field(id = 4, getter = "getDatabaseName")
    private final String mDatabaseName;
    @Field(id = 5, getter = "getRankingSignal")
    private final double mRankingSignal;
    @Field(id = 6, getter = "getJoinedResults")
    private final List<SearchResult> mJoinedResults;
    @Field(id = 7, getter = "getInformationalRankingSignals")
    private final @NonNull List<Double> mInformationalRankingSignals;
    /**
     * Holds the map from schema type names to the list of their parent types.
     *
     * <p>The map includes entries for the {@link GenericDocument}'s own type and all of the
     * nested documents' types. Child types are guaranteed to appear before parent types in each
     * list.
     *
     * <p>Parent types include transitive parents.
     *
     * <p>All schema names in this map are un-prefixed, for both keys and values.
     */
    @Field(id = 8)
    final @NonNull Bundle mParentTypeMap;


    /** Cache of the {@link GenericDocument}. Comes from mDocument at first use. */
    private @Nullable GenericDocument mDocumentCached;

    /** Cache of the inflated {@link MatchInfo}. Comes from inflating mMatchInfos at first use. */
    private @Nullable List<MatchInfo> mMatchInfosCached;

    /** @exportToFramework:hide */
    @Constructor
    SearchResult(
            @Param(id = 1) @NonNull GenericDocumentParcel document,
            @Param(id = 2) @NonNull List<MatchInfo> matchInfos,
            @Param(id = 3) @NonNull String packageName,
            @Param(id = 4) @NonNull String databaseName,
            @Param(id = 5) double rankingSignal,
            @Param(id = 6) @NonNull List<SearchResult> joinedResults,
            @Param(id = 7) @Nullable List<Double> informationalRankingSignals,
            @Param(id = 8) @Nullable Bundle parentTypeMap) {
        mDocument = Preconditions.checkNotNull(document);
        mMatchInfos = Preconditions.checkNotNull(matchInfos);
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        mRankingSignal = rankingSignal;
        mJoinedResults = Collections.unmodifiableList(Preconditions.checkNotNull(joinedResults));
        if (informationalRankingSignals != null) {
            mInformationalRankingSignals = Collections.unmodifiableList(
                    informationalRankingSignals);
        } else {
            mInformationalRankingSignals = Collections.emptyList();
        }
        if (parentTypeMap != null) {
            mParentTypeMap = parentTypeMap;
        } else {
            mParentTypeMap = Bundle.EMPTY;
        }
    }

// @exportToFramework:startStrip()
    /**
     * Contains the matching document, converted to the given document class.
     *
     * <p>This is equivalent to calling {@code getGenericDocument().toDocumentClass(T.class)}.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the object returned
     * by this function, rather than calling it multiple times.
     *
     * @param documentClass the document class to be passed to
     *                      {@link GenericDocument#toDocumentClass(java.lang.Class)}.
     * @return Document object which matched the query.
     * @throws AppSearchException if no factory for this document class could be found on the
     *       classpath.
     * @see GenericDocument#toDocumentClass(java.lang.Class)
     */
    public <T> @NonNull T getDocument(@NonNull java.lang.Class<T> documentClass)
            throws AppSearchException {
        return getDocument(documentClass, /* documentClassMap= */null);
    }

    /**
     * Contains the matching document, converted to the given document class.
     *
     * <p>This is equivalent to calling {@code getGenericDocument().toDocumentClass(T.class,
     * new DocumentClassMappingContext(documentClassMap, getParentTypeMap()))}.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the object returned
     * by this function, rather than calling it multiple times.
     *
     * @param documentClass the document class to be passed to
     *        {@link GenericDocument#toDocumentClass(java.lang.Class, DocumentClassMappingContext)}.
     * @param documentClassMap A map from AppSearch's type name specified by
     *                         {@link androidx.appsearch.annotation.Document#name()}
     *                         to the list of the fully qualified names of the corresponding
     *                         document classes. In most cases, passing the value returned by
     *                         {@link AppSearchDocumentClassMap#getGlobalMap()} will be sufficient.
     * @return Document object which matched the query.
     * @throws AppSearchException if no factory for this document class could be found on the
     *                            classpath.
     * @see GenericDocument#toDocumentClass(java.lang.Class, DocumentClassMappingContext)
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public <T> @NonNull T getDocument(@NonNull java.lang.Class<T> documentClass,
            @Nullable Map<String, List<String>> documentClassMap) throws AppSearchException {
        Preconditions.checkNotNull(documentClass);
        return getGenericDocument().toDocumentClass(documentClass,
                new DocumentClassMappingContext(documentClassMap,
                        getParentTypeMap()));
    }
// @exportToFramework:endStrip()

    /**
     * Contains the matching {@link GenericDocument}.
     *
     * @return Document object which matched the query.
     */
    public @NonNull GenericDocument getGenericDocument() {
        if (mDocumentCached == null) {
            mDocumentCached = new GenericDocument(mDocument);
        }
        return mDocumentCached;
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
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public @NonNull List<MatchInfo> getMatchInfos() {
        if (mMatchInfosCached == null) {
            mMatchInfosCached = new ArrayList<>(mMatchInfos.size());
            for (int i = 0; i < mMatchInfos.size(); i++) {
                MatchInfo matchInfo = mMatchInfos.get(i);
                matchInfo.setDocument(getGenericDocument());
                if (matchInfo.getTextMatch() != null) {
                    // This is necessary in order to use the TextMatchInfo after IPC, since
                    // TextMatch.mPropertyPath is private and is not retained by SafeParcelable
                    // across IPC.
                    matchInfo.mTextMatch.setPropertyPath(matchInfo.getPropertyPath());
                }
                if (mMatchInfosCached != null) {
                    // This additional check is added for NullnessChecker.
                    mMatchInfosCached.add(matchInfo);
                }
            }
            mMatchInfosCached = Collections.unmodifiableList(mMatchInfosCached);
        }
        // This check is added for NullnessChecker, mMatchInfos will always be NonNull.
        return Preconditions.checkNotNull(mMatchInfosCached);
    }

    /**
     * Contains the package name of the app that stored the {@link GenericDocument}.
     *
     * @return Package name that stored the document
     */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /**
     * Contains the database name that stored the {@link GenericDocument}.
     *
     * @return Name of the database within which the document is stored
     */
    public @NonNull String getDatabaseName() {
        return mDatabaseName;
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
        return mRankingSignal;
    }

    /**
     * Returns the informational ranking signals of the {@link GenericDocument}, according to the
     * expressions added in {@link SearchSpec.Builder#addInformationalRankingExpressions}.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
    public @NonNull List<Double> getInformationalRankingSignals() {
        return mInformationalRankingSignals;
    }

    /**
     * Returns the map from schema type names to the list of their parent types.
     *
     * <p>The map includes entries for the {@link GenericDocument}'s own type and all of the
     * nested documents' types. Child types are guaranteed to appear before parent types in each
     * list.
     *
     * <p>Parent types include transitive parents.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned
     * by this function, rather than calling it multiple times.
     */
    @ExperimentalAppSearchApi
    @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES)
    public @NonNull Map<String, List<String>> getParentTypeMap() {
        Set<String> schemaTypes = mParentTypeMap.keySet();
        Map<String, List<String>> parentTypeMap = new ArrayMap<>(schemaTypes.size());
        for (String schemaType : schemaTypes) {
            ArrayList<String> parentTypes = mParentTypeMap.getStringArrayList(schemaType);
            if (parentTypes != null) {
                parentTypeMap.put(schemaType, parentTypes);
            }
        }
        return parentTypeMap;
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
    public @NonNull List<SearchResult> getJoinedResults() {
        return mJoinedResults;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchResultCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link SearchResult} objects. */
    public static final class Builder {
        private final String mPackageName;
        private final String mDatabaseName;
        private List<MatchInfo> mMatchInfos = new ArrayList<>();
        private GenericDocument mGenericDocument;
        private double mRankingSignal;
        private List<Double> mInformationalRankingSignals = new ArrayList<>();
        private Bundle mParentTypeMap = new Bundle();
        private List<SearchResult> mJoinedResults = new ArrayList<>();
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

        /** @exportToFramework:hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        public Builder(@NonNull SearchResult searchResult) {
            Preconditions.checkNotNull(searchResult);
            mPackageName = searchResult.getPackageName();
            mDatabaseName = searchResult.getDatabaseName();
            mGenericDocument = searchResult.getGenericDocument();
            mRankingSignal = searchResult.getRankingSignal();
            mInformationalRankingSignals = new ArrayList<>(
                    searchResult.getInformationalRankingSignals());
            setParentTypeMap(searchResult.getParentTypeMap());
            List<MatchInfo> matchInfos = searchResult.getMatchInfos();
            for (int i = 0; i < matchInfos.size(); i++) {
                addMatchInfo(new MatchInfo.Builder(matchInfos.get(i)).build());
            }
            List<SearchResult> joinedResults = searchResult.getJoinedResults();
            for (int i = 0; i < joinedResults.size(); i++) {
                addJoinedResult(joinedResults.get(i));
            }
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
        public @NonNull Builder setDocument(@NonNull Object document) throws AppSearchException {
            Preconditions.checkNotNull(document);
            resetIfBuilt();
            return setGenericDocument(GenericDocument.fromDocumentClass(document));
        }
// @exportToFramework:endStrip()

        /** Sets the document which matched. */
        @CanIgnoreReturnValue
        public @NonNull Builder setGenericDocument(@NonNull GenericDocument document) {
            Preconditions.checkNotNull(document);
            resetIfBuilt();
            mGenericDocument = document;
            return this;
        }

        /** Adds another match to this SearchResult. */
        @CanIgnoreReturnValue
        public @NonNull Builder addMatchInfo(@NonNull MatchInfo matchInfo) {
            Preconditions.checkState(
                    matchInfo.mDocument == null,
                    "This MatchInfo is already associated with a SearchResult and can't be "
                            + "reassigned");
            resetIfBuilt();
            mMatchInfos.add(matchInfo);
            return this;
        }

        /** Sets the ranking signal of the matched document in this SearchResult. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRankingSignal(double rankingSignal) {
            resetIfBuilt();
            mRankingSignal = rankingSignal;
            return this;
        }

        /** Adds the informational ranking signal of the matched document in this SearchResult. */
        @CanIgnoreReturnValue
        @FlaggedApi(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
        public @NonNull Builder addInformationalRankingSignal(double rankingSignal) {
            resetIfBuilt();
            mInformationalRankingSignals.add(rankingSignal);
            return this;
        }

        /**
         * Sets the map from schema type names to the list of their parent types.
         *
         * <p>The map should include entries for the {@link GenericDocument}'s own type and all
         * of the nested documents' types.
         *
         *  <!--@exportToFramework:ifJetpack()-->
         * <p>Child types must appear before parent types in each list. Otherwise, the
         * {@link GenericDocument#toDocumentClass(java.lang.Class, DocumentClassMappingContext)}
         * method may not correctly identify the most concrete type. This could lead to unintended
         * deserialization into a more general type instead of a more specific type.
         *  <!--@exportToFramework:else()
         * <p>Child types must appear before parent types in each list. Otherwise, the
         * GenericDocument's toDocumentClass method (an AndroidX-only API) may not correctly
         * identify the most concrete type. This could lead to unintended deserialization into a
         * more general type instead of a
         * more specific type.
         * -->
         *
         * <p>Parent types should include transitive parents.
         */
        @CanIgnoreReturnValue
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES)
        public @NonNull Builder setParentTypeMap(@NonNull Map<String, List<String>> parentTypeMap) {
            Preconditions.checkNotNull(parentTypeMap);
            resetIfBuilt();
            mParentTypeMap.clear();

            for (Map.Entry<String, List<String>> entry : parentTypeMap.entrySet()) {
                Preconditions.checkNotNull(entry.getKey());
                Preconditions.checkNotNull(entry.getValue());

                ArrayList<String> parentTypes = new ArrayList<>(entry.getValue().size());
                for (int i = 0; i < entry.getValue().size(); i++) {
                    String parentType = entry.getValue().get(i);
                    parentTypes.add(Preconditions.checkNotNull(parentType));
                }
                mParentTypeMap.putStringArrayList(entry.getKey(), parentTypes);
            }
            return this;
        }


        /**
         * Adds a {@link SearchResult} that was joined by the {@link JoinSpec}.
         * @param joinedResult The joined SearchResult to add.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addJoinedResult(@NonNull SearchResult joinedResult) {
            resetIfBuilt();
            mJoinedResults.add(joinedResult);
            return this;
        }

        /**
         * Clears the {@link MatchInfo}s.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CanIgnoreReturnValue
        public @NonNull Builder clearMatchInfos() {
            resetIfBuilt();
            mMatchInfos.clear();
            return this;
        }


        /**
         * Clears the {@link SearchResult}s that were joined.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CanIgnoreReturnValue
        public @NonNull Builder clearJoinedResults() {
            resetIfBuilt();
            mJoinedResults.clear();
            return this;
        }

        /** Constructs a new {@link SearchResult}. */
        public @NonNull SearchResult build() {
            mBuilt = true;
            return new SearchResult(
                    mGenericDocument.getDocumentParcel(),
                    mMatchInfos,
                    mPackageName,
                    mDatabaseName,
                    mRankingSignal,
                    mJoinedResults,
                    mInformationalRankingSignals,
                    mParentTypeMap);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mMatchInfos = new ArrayList<>(mMatchInfos);
                mJoinedResults = new ArrayList<>(mJoinedResults);
                mInformationalRankingSignals = new ArrayList<>(mInformationalRankingSignals);
                mParentTypeMap = BundleUtil.deepCopy(mParentTypeMap);
                mBuilt = false;
            }
        }
    }

    /**
     * This class represents match objects for any snippets that might be present in
     * {@link SearchResults} from a query.
     *
     * <p> A {@link MatchInfo} contains either a {@link TextMatchInfo} representing a text match
     * snippet, or an {@link EmbeddingMatchInfo} representing an embedding match snippet.
     */
    @SafeParcelable.Class(creator = "MatchInfoCreator")
    @SuppressWarnings("HiddenSuperclass")
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class MatchInfo extends AbstractSafeParcelable {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        public static final @NonNull Parcelable.Creator<MatchInfo> CREATOR =
                new MatchInfoCreator();

        /** The path of the matching snippet property. */
        @Field(id = 1, getter = "getPropertyPath")
        private final String mPropertyPath;
        @Field(id = 2)
        final int mExactMatchRangeStart;
        @Field(id = 3)
        final int mExactMatchRangeEnd;
        @Field(id = 4)
        final int mSubmatchRangeStart;
        @Field(id = 5)
        final int mSubmatchRangeEnd;
        @Field(id = 6)
        final int mSnippetRangeStart;
        @Field(id = 7)
        final int mSnippetRangeEnd;

        /** Represents text-based match information. */
        @Field(id = 8, getter = "getTextMatch")
        private @Nullable final TextMatchInfo mTextMatch;

        /** Represents embedding-based match information. */
        @Field(id = 9, getter = "getEmbeddingMatch")
        private @Nullable final EmbeddingMatchInfo mEmbeddingMatch;

        private @Nullable PropertyPath mPropertyPathObject = null;

        /**
         * Document which the match comes from.
         *
         * <p>If this is {@code null}, methods which require access to the document, like
         * {@link #getExactMatch}, will throw {@link NullPointerException}.
         */
        private @Nullable GenericDocument mDocument = null;

        @Constructor
        MatchInfo(
                @Param(id = 1) @NonNull String propertyPath,
                @Param(id = 2) int exactMatchRangeStart,
                @Param(id = 3) int exactMatchRangeEnd,
                @Param(id = 4) int submatchRangeStart,
                @Param(id = 5) int submatchRangeEnd,
                @Param(id = 6) int snippetRangeStart,
                @Param(id = 7) int snippetRangeEnd,
                @Param(id = 8) @Nullable TextMatchInfo textMatchInfo,
                @Param(id = 9) @Nullable EmbeddingMatchInfo embeddingMatchInfo) {
            mPropertyPath = Preconditions.checkNotNull(propertyPath);
            mExactMatchRangeStart = exactMatchRangeStart;
            mExactMatchRangeEnd = exactMatchRangeEnd;
            mSubmatchRangeStart = submatchRangeStart;
            mSubmatchRangeEnd = submatchRangeEnd;
            mSnippetRangeStart = snippetRangeStart;
            mSnippetRangeEnd = snippetRangeEnd;
            mEmbeddingMatch = embeddingMatchInfo;
            TextMatchInfo tempTextMatch = textMatchInfo;
            if (tempTextMatch == null && mEmbeddingMatch == null) {
                tempTextMatch = new TextMatchInfo(
                        new MatchRange(exactMatchRangeStart, exactMatchRangeEnd),
                        new MatchRange(submatchRangeStart, submatchRangeEnd),
                        new MatchRange(snippetRangeStart, snippetRangeEnd));
                tempTextMatch.setPropertyPath(mPropertyPath);
            }

            mTextMatch = tempTextMatch;
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
        public @NonNull String getPropertyPath() {
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
        public @NonNull PropertyPath getPropertyPathObject() {
            if (mPropertyPathObject == null) {
                mPropertyPathObject = new PropertyPath(mPropertyPath);
            }
            return mPropertyPathObject;
        }

        /**
         * Retrieves the text-based match information.
         *
         * @return A {@link TextMatchInfo} instance, or null if the match is not text-based.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @Nullable TextMatchInfo getTextMatch() {
            return mTextMatch;
        }

        /**
         * Retrieves the embedding-based match information. Only populated when
         * {@link SearchSpec#shouldRetrieveEmbeddingMatchInfos()} is true.
         *
         * @return A {@link EmbeddingMatchInfo} instance, or null if the match is not an
         * embedding match.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @Nullable EmbeddingMatchInfo getEmbeddingMatch() {
            return mEmbeddingMatch;
        }

        /**
         * <p>Gets the full text corresponding to the given entry. Returns an empty string if the
         * match is not text-based.
         */
        public @NonNull String getFullText() {
            if (mTextMatch == null) {
                return "";
            }
            return mTextMatch.getFullText();
        }

        /**
         * Gets the {@link MatchRange} of the exact term of the given entry that matched the query.
         * Returns [0, 0] if the match is not text-based.
         */
        public @NonNull MatchRange getExactMatchRange() {
            if (mTextMatch == null) {
                return new MatchRange(0, 0);
            }
            return mTextMatch.getExactMatchRange();
        }

        /**
         * Gets the exact term of the given entry that matched the query. Returns an empty
         * CharSequence if the match is not text-based.
         */
        public @NonNull CharSequence getExactMatch() {
            if (mTextMatch == null) {
                return "";
            }
            return mTextMatch.getExactMatch();
        }

        /**
         * Gets the {@link MatchRange} of the submatch term subsequence of the given entry that
         * matched the query. Returns [0, 0] if the match is not text-based.
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         *
         * @throws UnsupportedOperationException if {@link Features#isFeatureSupported} is
         * false.
         * <!--@exportToFramework:else()-->
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)
        public @NonNull MatchRange getSubmatchRange() {
            if (mTextMatch == null) {
                return new MatchRange(0, 0);
            }
            return mTextMatch.getSubmatchRange();
        }

        /**
         * <p> Gets the exact term subsequence of the given entry that matched the query. Returns an
         * empty CharSequence if the match is not text-based.
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         *
         * @throws UnsupportedOperationException if {@link Features#isFeatureSupported} is
         * false.
         * <!--@exportToFramework:else()-->
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)
        public @NonNull CharSequence getSubmatch() {
            if (mTextMatch == null) {
                return "";
            }
            return mTextMatch.getSubmatch();
        }

        /**
         * <p>Gets the snippet {@link MatchRange} corresponding to the given entry. Returns [0,0]
         * if the match is not text-based.
         * <p>Only populated when set maxSnippetSize > 0 in
         * {@link SearchSpec.Builder#setMaxSnippetSize}.
         */
        public @NonNull MatchRange getSnippetRange() {
            if (mTextMatch == null) {
                return new MatchRange(0, 0);
            }
            return mTextMatch.getSnippetRange();
        }

        /**
         * <p>Gets the snippet corresponding to the given entry. Returns an empty CharSequence if
         * the match is not text-based.
         * <p>Snippet - Provides a subset of the content to display. Only populated when requested
         * maxSnippetSize > 0. The size of this content can be changed by
         * {@link SearchSpec.Builder#setMaxSnippetSize}. Windowing is centered around the middle of
         * the matched token with content on either side clipped to token boundaries.
         */
        public @NonNull CharSequence getSnippet() {
            if (mTextMatch == null) {
                return "";
            }
            return mTextMatch.getSnippet();
        }

        /**
         * Sets the {@link GenericDocument} for {@link MatchInfo}.
         *
         * <p>{@link MatchInfo} lacks a constructor that populates {@link MatchInfo#mDocument}
         * This provides the ability to set {@link MatchInfo#mDocument}
         */
        void setDocument(@NonNull GenericDocument document) {
            mDocument = Preconditions.checkNotNull(document);
            if (mTextMatch != null) {
                mTextMatch.setDocument(document);
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            MatchInfoCreator.writeToParcel(this, dest, flags);
        }

        /** Builder for {@link MatchInfo} objects. */
        public static final class Builder {
            private final String mPropertyPath;
            private EmbeddingMatchInfo mEmbeddingMatch = null;
            private MatchRange mExactMatchRange = new MatchRange(0, 0);
            private MatchRange mSubmatchRange = new MatchRange(-1, -1);
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

            /** @exportToFramework:hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public Builder(@NonNull MatchInfo matchInfo) {
                Preconditions.checkNotNull(matchInfo);
                mPropertyPath = matchInfo.mPropertyPath;
                mEmbeddingMatch = matchInfo.getEmbeddingMatch();
                mExactMatchRange = matchInfo.getExactMatchRange();
                // Using the fields directly instead of getSubmatchRange() to bypass the
                // checkSubmatchSupported check.
                mSubmatchRange = new MatchRange(matchInfo.mSubmatchRangeStart,
                        matchInfo.mSubmatchRangeEnd);
                mSnippetRange = matchInfo.getSnippetRange();
            }

            /**
             * Sets the {@link EmbeddingMatchInfo} corresponding to the given entry.
             */
            @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
            @ExperimentalAppSearchApi
            @CanIgnoreReturnValue
            public @NonNull Builder setEmbeddingMatch(@Nullable EmbeddingMatchInfo embeddingMatch) {
                mEmbeddingMatch = embeddingMatch;
                return this;
            }

            /**
             * Sets the exact {@link MatchRange} corresponding to the given entry.
             */
            @CanIgnoreReturnValue
            public @NonNull Builder setExactMatchRange(@NonNull MatchRange matchRange) {
                mExactMatchRange = Preconditions.checkNotNull(matchRange);
                return this;
            }


            /**
             * Sets the submatch {@link MatchRange} corresponding to the given entry.
             */
            @CanIgnoreReturnValue
            public @NonNull Builder setSubmatchRange(@NonNull MatchRange matchRange) {
                mSubmatchRange = Preconditions.checkNotNull(matchRange);
                return this;
            }

            /**
             * Sets the snippet {@link MatchRange} corresponding to the given entry.
             */
            @CanIgnoreReturnValue
            public @NonNull Builder setSnippetRange(@NonNull MatchRange matchRange) {
                mSnippetRange = Preconditions.checkNotNull(matchRange);
                return this;
            }

            /** Constructs a new {@link MatchInfo}. */
            public @NonNull MatchInfo build() {
                TextMatchInfo textMatch = null;
                if (mEmbeddingMatch == null) {
                    textMatch = new TextMatchInfo(mExactMatchRange, mSubmatchRange, mSnippetRange);
                    textMatch.setPropertyPath(mPropertyPath);
                }
                return new MatchInfo(
                        mPropertyPath,
                        mExactMatchRange.getStart(),
                        mExactMatchRange.getEnd(),
                        mSubmatchRange.getStart(),
                        mSubmatchRange.getEnd(),
                        mSnippetRange.getStart(),
                        mSnippetRange.getEnd(),
                        textMatch,
                        mEmbeddingMatch);
            }
        }
    }

    /**
     * This class represents match objects for any text match snippets that might be present in
     * {@link SearchResults} from a string query. Using this class, you can get:
     * <ul>
     *     <li>the full text - all of the text in that String property</li>
     *     <li>the exact term match - the 'term' (full word) that matched the query</li>
     *     <li>the subterm match - the portion of the matched term that appears in the query</li>
     *     <li>a suggested text snippet - a portion of the full text surrounding the exact term
     *     match, set to term boundaries. The size of the snippet is specified in
     *     {@link SearchSpec.Builder#setMaxSnippetSize}</li>
     * </ul>
     * for each text match in the document.
     *
     * <p>Class Example 1:
     * <p>A document contains the following text in property "subject":
     * <p>"A commonly used fake word is foo. Another nonsense word that’s used a lot is bar."
     *
     * <p>If the queryExpression is "foo" and {@link SearchSpec#getMaxSnippetSize}  is 10,
     * <ul>
     *      <li>{@link TextMatchInfo#getFullText()} returns "A commonly used fake word is foo.
     *      Another nonsense word that’s used a lot is bar."</li>
     *      <li>{@link TextMatchInfo#getExactMatchRange()} returns [29, 32]</li>
     *      <li>{@link TextMatchInfo#getExactMatch()} returns "foo"</li>
     *      <li>{@link TextMatchInfo#getSubmatchRange()} returns [29, 32]</li>
     *      <li>{@link TextMatchInfo#getSubmatch()} returns "foo"</li>
     *      <li>{@link TextMatchInfo#getSnippetRange()} returns [26, 33]</li>
     *      <li>{@link TextMatchInfo#getSnippet()} returns "is foo."</li>
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
     *      <li>{@link TextMatchInfo#getFullText()} returns "Test Name Jr."</li>
     *      <li>{@link TextMatchInfo#getExactMatchRange()} returns [0, 4]</li>
     *      <li>{@link TextMatchInfo#getExactMatch()} returns "Test"</li>
     *      <li>{@link TextMatchInfo#getSubmatchRange()} returns [0, 4]</li>
     *      <li>{@link TextMatchInfo#getSubmatch()} returns "Test"</li>
     *      <li>{@link TextMatchInfo#getSnippetRange()} returns [0, 9]</li>
     *      <li>{@link TextMatchInfo#getSnippet()} returns "Test Name"</li>
     * </ul>
     * <p> Match-2
     * <ul>
     *      <li>{@link TextMatchInfo#getFullText()} returns "Testing 1 2 3"</li>
     *      <li>{@link TextMatchInfo#getExactMatchRange()} returns [0, 7]</li>
     *      <li>{@link TextMatchInfo#getExactMatch()} returns "Testing"</li>
     *      <li>{@link TextMatchInfo#getSubmatchRange()} returns [0, 4]</li>
     *      <li>{@link TextMatchInfo#getSubmatch()} returns "Test"</li>
     *      <li>{@link TextMatchInfo#getSnippetRange()} returns [0, 9]</li>
     *      <li>{@link TextMatchInfo#getSnippet()} returns "Testing 1"</li>
     * </ul>
     */
    @SafeParcelable.Class(creator = "TextMatchInfoCreator")
    @SuppressWarnings("HiddenSuperclass")
    @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
    @ExperimentalAppSearchApi
    public static final class TextMatchInfo extends AbstractSafeParcelable {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        public static final @NonNull Parcelable.Creator<TextMatchInfo> CREATOR =
                new TextMatchInfoCreator();

        @Field(id = 1, getter = "getExactMatchRange")
        private final MatchRange mExactMatchRange;
        @Field(id = 2, getter = "getSubmatchRange")
        private final MatchRange mSubmatchRange;
        @Field(id = 3, getter = "getSnippetRange")
        private final MatchRange mSnippetRange;

        /**
         * The path of the matching snippet property.
         *
         * <p>If this is {@code null}, methods which require access to the property, like
         * {@link #getExactMatch}, will throw {@link NullPointerException}.
         */
        private @Nullable String mPropertyPath = null;

        /**
         * Document which the match comes from.
         *
         * <p>If this is {@code null}, methods which require access to the document, like
         * {@link #getExactMatch}, will throw {@link NullPointerException}.
         */
        private @Nullable GenericDocument mDocument = null;

        /** Full text of the matched property. Populated on first use. */
        private @Nullable String mFullText;

        /**
         * Creates a new immutable TextMatchInfo.
         *
         * @param exactMatchRange the exact {@link MatchRange} for the entry.
         * @param submatchRange the sub-match {@link MatchRange} for the entry.
         * @param snippetRange the snippet {@link MatchRange} for the entry.
         */
        @ExperimentalAppSearchApi
        @Constructor
        public TextMatchInfo(
                @Param(id = 1) @NonNull MatchRange exactMatchRange,
                @Param(id = 2) @NonNull MatchRange submatchRange,
                @Param(id = 3) @NonNull MatchRange snippetRange) {
            mExactMatchRange = exactMatchRange;
            mSubmatchRange = submatchRange;
            mSnippetRange = snippetRange;
        }

        /**
         * Gets the full text corresponding to the given entry.
         * <p>Class example 1: this returns "A commonly used fake word is foo. Another nonsense
         * word that's used a lot is bar."
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns "Test Name Jr."
         * and, for the second {@link TextMatchInfo}, this returns "Testing 1 2 3".
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull String getFullText() {
            if (mFullText == null) {
                if (mDocument == null || mPropertyPath == null) {
                    throw new IllegalStateException(
                            "Document or property path has not been populated; this TextMatchInfo"
                                    + " cannot be used yet");
                }
                mFullText = getPropertyValues(mDocument, mPropertyPath);
            }
            return mFullText;
        }

        /**
         * Gets the {@link MatchRange} of the exact term of the given entry that matched the query.
         * <p>Class example 1: this returns [29, 32].
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns [0, 4] and, for the
         * second {@link TextMatchInfo}, this returns [0, 7].
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull MatchRange getExactMatchRange() {
            return mExactMatchRange;
        }

        /**
         * Gets the exact term of the given entry that matched the query.
         * <p>Class example 1: this returns "foo".
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns "Test" and, for the
         * second {@link TextMatchInfo}, this returns "Testing".
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull CharSequence getExactMatch() {
            return getSubstring(getExactMatchRange());
        }

        /**
         * Gets the {@link MatchRange} of the exact term subsequence of the given entry that matched
         * the query.
         * <p>Class example 1: this returns [29, 32].
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns [0, 4] and, for the
         * second {@link TextMatchInfo}, this returns [0, 4].
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         *
         * @throws UnsupportedOperationException if {@link Features#isFeatureSupported} is
         *                                       false.
         *                                       <!--@exportToFramework:else()-->
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull MatchRange getSubmatchRange() {
            checkSubmatchSupported();
            return mSubmatchRange;
        }

        /**
         * Gets the exact term subsequence of the given entry that matched the query.
         * <p>Class example 1: this returns "foo".
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns "Test" and, for the
         * second {@link TextMatchInfo}, this returns "Test".
         *
         * <!--@exportToFramework:ifJetpack()-->
         * <p>This information may not be available depending on the backend and Android API
         * level. To ensure it is available, call {@link Features#isFeatureSupported}.
         *
         * @throws UnsupportedOperationException if {@link Features#isFeatureSupported} is
         *                                       false.
         *                                       <!--@exportToFramework:else()-->
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull CharSequence getSubmatch() {
            checkSubmatchSupported();
            return getSubstring(getSubmatchRange());
        }

        /**
         * Gets the snippet {@link TextMatchInfo} corresponding to the given entry.
         * <p>Only populated when set maxSnippetSize > 0 in
         * {@link SearchSpec.Builder#setMaxSnippetSize}.
         * <p>Class example 1: this returns [29, 41].
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns [0, 9] and, for the
         * second {@link TextMatchInfo}, this returns [0, 13].
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull MatchRange getSnippetRange() {
            return mSnippetRange;
        }

        /**
         * Gets the snippet corresponding to the given entry.
         * <p>Snippet - Provides a subset of the content to display. Only populated when requested
         * maxSnippetSize > 0. The size of this content can be changed by
         * {@link SearchSpec.Builder#setMaxSnippetSize}. Windowing is centered around the middle of
         * the matched token with content on either side clipped to token boundaries.
         * <p>Class example 1: this returns "foo. Another".
         * <p>Class example 2: for the first {@link TextMatchInfo}, this returns "Test Name" and,
         * for
         * the second {@link TextMatchInfo}, this returns "Testing 1 2 3".
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public @NonNull CharSequence getSnippet() {
            return getSubstring(getSnippetRange());
        }

        private CharSequence getSubstring(MatchRange range) {
            return getFullText().substring(range.getStart(), range.getEnd());
        }

        private void checkSubmatchSupported() {
            if (mSubmatchRange.getStart() == -1) {
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

        /**
         * Sets the {@link GenericDocument} for this {@link TextMatchInfo}.
         *
         * {@link TextMatchInfo} lacks a constructor that populates {@link TextMatchInfo#mDocument}
         * This provides the ability to set {@link TextMatchInfo#mDocument}
         */
        void setDocument(@NonNull GenericDocument document) {
            mDocument = Preconditions.checkNotNull(document);
        }

        /**
         * Sets the property path for this {@link TextMatchInfo}.
         *
         * {@link TextMatchInfo} lacks a constructor that populates
         * {@link TextMatchInfo#mPropertyPath}
         * This provides the ability to set it.
         */
        void setPropertyPath(@NonNull String propertyPath) {
            mPropertyPath = Preconditions.checkNotNull(propertyPath);
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            TextMatchInfoCreator.writeToParcel(this, dest, flags);
        }
    }

    /**
     * This class represents match objects for any snippets that might be present in
     * {@link SearchResults} from an embedding query. Using this class, you can get:
     * <ul>
     *     <li>the semantic score of the matching vector with the embedding query</li>
     *     <li>the query embedding vector index - the index of the query {@link EmbeddingVector}
     *          in the list returned by {@link SearchSpec#getEmbeddingParameters()}</li>
     *     <li>the embedding search metric type for the corresponding query</li>
     * </ul>
     * for each vector match in the document.
     */
    @SafeParcelable.Class(creator = "EmbeddingMatchInfoCreator")
    @SuppressWarnings("HiddenSuperclass")
    @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
    @ExperimentalAppSearchApi
    public static final class EmbeddingMatchInfo extends AbstractSafeParcelable {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        public static final @NonNull Parcelable.Creator<EmbeddingMatchInfo> CREATOR =
                new EmbeddingMatchInfoCreator();

        @Field(id = 1, getter = "getSemanticScore")
        private final double mSemanticScore;

        @Field(id = 2, getter = "getQueryEmbeddingVectorIndex")
        private final int mQueryEmbeddingVectorIndex;

        @Field(id = 3, getter = "getEmbeddingSearchMetricType")
        private final int mEmbeddingSearchMetricType;

        /**
         * Creates a new immutable EmbeddingMatchInfo.
         *
         * @param semanticScore the semantic score of the embedding match against the query vector.
         * @param queryEmbeddingVectorIndex the index of the matched query embedding vector in
         *                    {@link SearchSpec#getEmbeddingParameters()}
         * @param embeddingSearchMetricType the search metric type used to calculate the score
         *                                  for the match and the query vector
         */
        @ExperimentalAppSearchApi
        @Constructor
        public EmbeddingMatchInfo(
                @Param(id = 1) double semanticScore,
                @Param(id = 2) int queryEmbeddingVectorIndex,
                @Param(id = 3)
                @SearchSpec.EmbeddingSearchMetricType int embeddingSearchMetricType) {
            Preconditions.checkArgumentInRange(embeddingSearchMetricType,
                    EMBEDDING_SEARCH_METRIC_TYPE_COSINE,
                    EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN, "Embedding search metric type");
            mSemanticScore = semanticScore;
            mQueryEmbeddingVectorIndex = queryEmbeddingVectorIndex;
            mEmbeddingSearchMetricType = embeddingSearchMetricType;
        }

        /**
         * Gets the semantic score corresponding to the embedding match.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public double getSemanticScore() {
            return mSemanticScore;
        }

        /**
         * Gets the index of the query vector that this embedding match corresponds to. This is
         * the index of the query {@link EmbeddingVector} in the list returned by
         * {@link SearchSpec#getEmbeddingParameters()}
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        public int getQueryEmbeddingVectorIndex() {
            return mQueryEmbeddingVectorIndex;
        }

        /**
         * Gets the embedding search metric type that this embedding match corresponds to.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_EMBEDDING_MATCH_INFO)
        @ExperimentalAppSearchApi
        @SearchSpec.EmbeddingSearchMetricType
        public int getEmbeddingSearchMetricType() {
            return mEmbeddingSearchMetricType;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            EmbeddingMatchInfoCreator.writeToParcel(this, dest, flags);
        }
    }

    /**
     * Class providing the position range of a text match information.
     *
     * <p> All ranges are finite, and the left side of the range is always {@code <=} the right
     * side of the range.
     *
     * <p> Example: MatchRange(0, 100) represents hundred ints from 0 to 99."
     */
    @SafeParcelable.Class(creator = "MatchRangeCreator")
    @SuppressWarnings("HiddenSuperclass")
    public static final class MatchRange extends AbstractSafeParcelable {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        public static final @NonNull Parcelable.Creator<MatchRange> CREATOR =
                new MatchRangeCreator();

        @Field(id = 1, getter = "getStart")
        private final int mStart;
        @Field(id = 2, getter = "getEnd")
        private final int mEnd;

        /**
         * Creates a new immutable range.
         * <p> The endpoints are {@code [start, end)}; that is the range is bounded. {@code start}
         * must be lesser or equal to {@code end}.
         *
         * @param start The start point (inclusive)
         * @param end   The end point (exclusive)
         */
        @Constructor
        public MatchRange(
                @Param(id = 1) int start,
                @Param(id = 2) int end) {
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
        public @NonNull String toString() {
            return "MatchRange { start: " + mStart + " , end: " + mEnd + "}";
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mStart, mEnd);
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            MatchRangeCreator.writeToParcel(this, dest, flags);
        }
    }
}
