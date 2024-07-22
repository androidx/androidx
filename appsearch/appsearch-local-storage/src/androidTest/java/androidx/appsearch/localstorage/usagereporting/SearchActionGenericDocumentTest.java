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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.usagereporting.ActionConstants;
import androidx.appsearch.usagereporting.SearchAction;

import org.junit.Test;

public class SearchActionGenericDocumentTest {
    @Test
    public void testBuild() {
        SearchActionGenericDocument searchActionGenericDocument =
                new SearchActionGenericDocument.Builder(
                        "namespace", "search", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("body")
                        .setFetchedResultCount(123)
                        .build();

        assertThat(searchActionGenericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(searchActionGenericDocument.getId()).isEqualTo("search");
        assertThat(searchActionGenericDocument.getSchemaType()).isEqualTo("builtin:SearchAction");
        assertThat(searchActionGenericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(searchActionGenericDocument.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(searchActionGenericDocument.getQuery()).isEqualTo("body");
        assertThat(searchActionGenericDocument.getFetchedResultCount()).isEqualTo(123);
    }

    @Test
    public void testBuild_fromGenericDocument() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "search", "builtin:SearchAction")
                        .setCreationTimestampMillis(1000)
                        .setPropertyLong("actionType", ActionConstants.ACTION_TYPE_SEARCH)
                        .setPropertyString("query", "body")
                        .setPropertyLong("fetchedResultCount", 123)
                        .build();
        SearchActionGenericDocument searchActionGenericDocument =
                new SearchActionGenericDocument(document);

        assertThat(searchActionGenericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(searchActionGenericDocument.getId()).isEqualTo("search");
        assertThat(searchActionGenericDocument.getSchemaType()).isEqualTo("builtin:SearchAction");
        assertThat(searchActionGenericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(searchActionGenericDocument.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(searchActionGenericDocument.getQuery()).isEqualTo("body");
        assertThat(searchActionGenericDocument.getFetchedResultCount()).isEqualTo(123);
    }

// @exportToFramework:startStrip()
    @Test
    public void testBuild_fromDocumentClass() throws Exception {
        SearchAction searchAction =
                new SearchAction.Builder("namespace", "search", /* actionTimestampMillis= */1000)
                        .setQuery("body")
                        .setFetchedResultCount(123)
                        .build();
        SearchActionGenericDocument searchActionGenericDocument =
                new SearchActionGenericDocument(GenericDocument.fromDocumentClass(searchAction));

        assertThat(searchActionGenericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(searchActionGenericDocument.getId()).isEqualTo("search");
        assertThat(searchActionGenericDocument.getSchemaType()).isEqualTo("builtin:SearchAction");
        assertThat(searchActionGenericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(searchActionGenericDocument.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(searchActionGenericDocument.getQuery()).isEqualTo("body");
        assertThat(searchActionGenericDocument.getFetchedResultCount()).isEqualTo(123);
    }
// @exportToFramework:endStrip()

    @Test
    public void testBuild_invalidActionTypeThrowsException() {
        GenericDocument documentWithoutActionType =
                new GenericDocument.Builder<>("namespace", "search", "builtin:SearchAction")
                        .build();
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> new SearchActionGenericDocument.Builder(documentWithoutActionType));
        assertThat(e1.getMessage())
                .isEqualTo("Invalid action type for SearchActionGenericDocument");

        GenericDocument documentWithUnknownActionType =
                new GenericDocument.Builder<>("namespace", "search", "builtin:SearchAction")
                        .setPropertyLong("actionType", ActionConstants.ACTION_TYPE_UNKNOWN)
                        .build();
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> new SearchActionGenericDocument.Builder(documentWithUnknownActionType));
        assertThat(e2.getMessage())
                .isEqualTo("Invalid action type for SearchActionGenericDocument");

        GenericDocument documentWithIncorrectActionType =
                new GenericDocument.Builder<>("namespace", "search", "builtin:SearchAction")
                        .setPropertyLong("actionType", ActionConstants.ACTION_TYPE_CLICK)
                        .build();
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                () -> new SearchActionGenericDocument.Builder(documentWithIncorrectActionType));
        assertThat(e3.getMessage())
                .isEqualTo("Invalid action type for SearchActionGenericDocument");
    }
}
