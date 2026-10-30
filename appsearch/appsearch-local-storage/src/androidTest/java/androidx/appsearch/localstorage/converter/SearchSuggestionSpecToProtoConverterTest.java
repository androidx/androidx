/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.localstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.localstorage.AppSearchConfigImpl;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.AppSearchUserPlugins;
import androidx.appsearch.localstorage.LocalStorageIcingOptionsConfig;
import androidx.appsearch.localstorage.NamespaceCache;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.SchemaCache;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.testutil.AppSearchTestUtils;

import com.google.android.icing.proto.NamespaceDocumentUriGroup;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SuggestionSpecProto;
import com.google.android.icing.proto.TypePropertyMask;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SearchSuggestionSpecToProtoConverterTest {
    /** An optimize strategy that always triggers optimize. */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private AppSearchImpl mAppSearchImpl;

    @Before
    public void setUp() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig()),
                AppSearchUserPlugins.EMPTY,
                ALWAYS_OPTIMIZE);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testToProto() throws Exception {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterDocumentIds("namespace1", ImmutableList.of("doc1", "doc2"))
                        .addFilterSchemas("typeA", "typeB")
                        .build();

        String prefix1 = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSuggestionSpecToProtoConverter converter = new SearchSuggestionSpecToProtoConverter(
                /*queryExpression=*/"prefix",
                searchSuggestionSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                new NamespaceCache(ImmutableMap.of(
                prefix1, ImmutableSet.of(
                        prefix1 + "namespace1",
                        prefix1 + "namespace2"))),
                new SchemaCache(/*schemaMap=*/ImmutableMap.of(
                        prefix1, ImmutableMap.of(
                                prefix1 + "typeA", configProto,
                                prefix1 + "typeB", configProto))));

        SuggestionSpecProto proto = converter.toSearchSuggestionSpecProto();

        assertThat(proto.getPrefix()).isEqualTo("prefix");
        assertThat(proto.getNumToReturn()).isEqualTo(123);
        assertThat(proto.getSchemaTypeFiltersList()).containsExactly(
                "package$database/typeA", "package$database/typeB");
        assertThat(proto.getNamespaceFiltersList()).containsExactly(
                "package$database/namespace1", "package$database/namespace2");
        assertThat(proto.getDocumentUriFiltersList()).containsExactly(
                NamespaceDocumentUriGroup.newBuilder()
                        .setNamespace("package$database/namespace1")
                        .addDocumentUris("doc1").addDocumentUris("doc2")
                        .build());
    }

    @Test
    public void testToProto_propertyFilters() throws Exception {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterProperties("typeA", ImmutableList.of("property1", "property2"))
                        .build();

        String prefix1 = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSuggestionSpecToProtoConverter converter = new SearchSuggestionSpecToProtoConverter(
                /*queryExpression=*/"prefix",
                searchSuggestionSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                new NamespaceCache(ImmutableMap.of()),
                new SchemaCache(/*schemaMap=*/ImmutableMap.of(
                        prefix1, ImmutableMap.of(
                                prefix1 + "typeA", configProto,
                                prefix1 + "typeB", configProto))));

        SuggestionSpecProto proto = converter.toSearchSuggestionSpecProto();
        assertThat(proto.getTypePropertyFiltersList()).containsExactly(
                TypePropertyMask.newBuilder()
                        .setSchemaType("package$database/typeA")
                        .addPaths("property1").addPaths("property2")
                        .build());
    }

    @Test
    public void testRemoveInaccessibleSchemaFilters() throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(mAppSearchImpl,
                        /*callStatsBuilder=*/ null);
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterProperties("typeA", ImmutableList.of("property1"))
                        .addFilterProperties("typeB", ImmutableList.of("property2"))
                        .build();

        String prefix1 = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSuggestionSpecToProtoConverter converter = new SearchSuggestionSpecToProtoConverter(
                /*queryExpression=*/"prefix",
                searchSuggestionSpec,
                /*prefixes=*/ImmutableSet.of(prefix1),
                new NamespaceCache(ImmutableMap.of(
                        prefix1, ImmutableSet.of(prefix1 + "namespace1"))),
                new SchemaCache(/*schemaMap=*/ImmutableMap.of(
                        prefix1, ImmutableMap.of(
                                prefix1 + "typeA", configProto,
                                prefix1 + "typeB", configProto,
                                prefix1 + "typeC", configProto))));

        converter.removeInaccessibleSchemaFilters(
                new CallerAccess(/*callingPackageName=*/"otherPackageName"),
                visibilityStore,
                AppSearchTestUtils.createMockVisibilityChecker(
                        /*visiblePrefixedSchemas=*/ImmutableSet.of(
                                prefix1 + "typeA", prefix1 + "typeC")));

        assertThat(converter.hasNothingToSearch()).isFalse();
        SuggestionSpecProto proto = converter.toSearchSuggestionSpecProto();
        // typeB is filtered out since it is not searchable for the caller, and so are its property
        // filters.
        assertThat(proto.getSchemaTypeFiltersList()).containsExactly(
                "package$database/typeA", "package$database/typeC");
        assertThat(proto.getTypePropertyFiltersList()).containsExactly(
                TypePropertyMask.newBuilder()
                        .setSchemaType("package$database/typeA")
                        .addPaths("property1")
                        .build());
    }

    @Test
    public void testRemoveInaccessibleSchemaFilters_noAccessibleSchemaType_hasNothingToSearch()
            throws Exception {
        String prefix1 = PrefixUtil.createPrefix("package", "database");
        SchemaTypeConfigProto configProto = SchemaTypeConfigProto.getDefaultInstance();
        SearchSuggestionSpecToProtoConverter converter = new SearchSuggestionSpecToProtoConverter(
                /*queryExpression=*/"prefix",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123).build(),
                /*prefixes=*/ImmutableSet.of(prefix1),
                new NamespaceCache(ImmutableMap.of(
                        prefix1, ImmutableSet.of(prefix1 + "namespace1"))),
                new SchemaCache(/*schemaMap=*/ImmutableMap.of(
                        prefix1, ImmutableMap.of(
                                prefix1 + "typeA", configProto,
                                prefix1 + "typeB", configProto))));
        assertThat(converter.hasNothingToSearch()).isFalse();

        // Without a visibility checker, a caller from another package cannot access any of the
        // schema types, so there is nothing to search.
        converter.removeInaccessibleSchemaFilters(
                new CallerAccess(/*callingPackageName=*/"otherPackageName"),
                /*visibilityStore=*/null,
                /*visibilityChecker=*/null);
        assertThat(converter.hasNothingToSearch()).isTrue();
    }
}
