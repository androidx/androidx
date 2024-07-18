/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.AppSearchConfigImpl;
import androidx.appsearch.localstorage.DefaultIcingOptionsConfig;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Map;

public class SearchResultToProtoConverterTest {
    @Test
    public void testToSearchResultProto() throws Exception {
        final String prefix =
                "com.package.foo" + PrefixUtil.PACKAGE_DELIMITER + "databaseName"
                        + PrefixUtil.DATABASE_DELIMITER;
        final String id = "id";
        final String namespace = prefix + "namespace";
        final String schemaType = prefix + "schema";
        final AppSearchConfigImpl config = new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                new DefaultIcingOptionsConfig());

        // Building the SearchResult received from query.
        DocumentProto.Builder documentProtoBuilder = DocumentProto.newBuilder()
                .setUri(id)
                .setNamespace(namespace)
                .setSchema(schemaType);

        // A joined document
        DocumentProto.Builder joinedDocProtoBuilder = DocumentProto.newBuilder()
                .setUri("id2")
                .setNamespace(namespace)
                .setSchema(schemaType);

        SearchResultProto.ResultProto joinedResultProto = SearchResultProto.ResultProto.newBuilder()
                .setDocument(joinedDocProtoBuilder).build();

        SearchResultProto.ResultProto resultProto = SearchResultProto.ResultProto.newBuilder()
                .setDocument(documentProtoBuilder)
                .addJoinedResults(joinedResultProto)
                .build();

        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(resultProto).build();

        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType(schemaType)
                        .build();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(prefix,
                ImmutableMap.of(schemaType, schemaTypeConfigProto));

        removePrefixesFromDocument(documentProtoBuilder);
        removePrefixesFromDocument(joinedDocProtoBuilder);
        SearchResultPage searchResultPage = SearchResultToProtoConverter.toSearchResultPage(
                searchResultProto, schemaMap, config);
        assertThat(searchResultPage.getResults()).hasSize(1);
        SearchResult result = searchResultPage.getResults().get(0);
        assertThat(result.getPackageName()).isEqualTo("com.package.foo");
        assertThat(result.getDatabaseName()).isEqualTo("databaseName");
        assertThat(result.getGenericDocument()).isEqualTo(
                GenericDocumentToProtoConverter.toGenericDocument(
                        documentProtoBuilder.build(), prefix, schemaMap.get(prefix),
                        config));

        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument()).isEqualTo(
                GenericDocumentToProtoConverter.toGenericDocument(
                        joinedDocProtoBuilder.build(), prefix, schemaMap.get(prefix),
                        config));
    }

    @Test
    public void testToSearchResultProtoWithDoublyNested() throws Exception {
        final String prefix =
                "com.package.foo" + PrefixUtil.PACKAGE_DELIMITER + "databaseName"
                        + PrefixUtil.DATABASE_DELIMITER;
        final String id = "id";
        final String namespace = prefix + "namespace";
        final String schemaType = prefix + "schema";

        // Building the SearchResult received from query.
        DocumentProto.Builder documentProtoBuilder = DocumentProto.newBuilder()
                .setUri(id)
                .setNamespace(namespace)
                .setSchema(schemaType);

        // A joined document
        DocumentProto.Builder joinedDocProtoBuilder = DocumentProto.newBuilder()
                .setUri("id2")
                .setNamespace(namespace)
                .setSchema(schemaType);

        SearchResultProto.ResultProto joinedResultProto = SearchResultProto.ResultProto.newBuilder()
                .setDocument(joinedDocProtoBuilder).build();

        SearchResultProto.ResultProto nestedJoinedResultProto = SearchResultProto.ResultProto
                .newBuilder()
                .setDocument(joinedDocProtoBuilder)
                .addJoinedResults(joinedResultProto)
                .build();

        SearchResultProto.ResultProto resultProto = SearchResultProto.ResultProto.newBuilder()
                .setDocument(documentProtoBuilder)
                .addJoinedResults(nestedJoinedResultProto)
                .build();

        SearchResultProto searchResultProto = SearchResultProto.newBuilder()
                .addResults(resultProto).build();

        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType(schemaType)
                        .build();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(prefix,
                ImmutableMap.of(schemaType, schemaTypeConfigProto));

        removePrefixesFromDocument(documentProtoBuilder);
        Exception e = assertThrows(AppSearchException.class,
                () -> SearchResultToProtoConverter.toSearchResultPage(searchResultProto, schemaMap,
                        new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                                new DefaultIcingOptionsConfig())));
        assertThat(e.getMessage())
                .isEqualTo("Nesting joined results within joined results not allowed.");
    }
}
