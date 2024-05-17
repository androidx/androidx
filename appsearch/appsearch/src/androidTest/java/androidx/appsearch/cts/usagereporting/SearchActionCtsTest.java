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
// @exportToFramework:skipFile()

package androidx.appsearch.cts.usagereporting;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.usagereporting.ActionConstants;
import androidx.appsearch.usagereporting.SearchAction;
import androidx.appsearch.usagereporting.TakenAction;

import org.junit.Test;

public class SearchActionCtsTest {
    @Test
    public void testBuilder() {
        SearchAction searchAction =
                new SearchAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                    .setDocumentTtlMillis(456)
                    .setQuery("query")
                    .setFetchedResultCount(1)
                    .build();

        assertThat(searchAction.getNamespace()).isEqualTo("namespace");
        assertThat(searchAction.getId()).isEqualTo("id");
        assertThat(searchAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(searchAction.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(searchAction.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(searchAction.getQuery()).isEqualTo("query");
        assertThat(searchAction.getFetchedResultCount()).isEqualTo(1);
    }

    @Test
    public void testBuilder_defaultValues() {
        SearchAction searchAction =
                new SearchAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .build();

        assertThat(searchAction.getNamespace()).isEqualTo("namespace");
        assertThat(searchAction.getId()).isEqualTo("id");
        assertThat(searchAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(searchAction.getDocumentTtlMillis())
                .isEqualTo(TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS);
        assertThat(searchAction.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(searchAction.getQuery()).isNull();
        assertThat(searchAction.getFetchedResultCount()).isEqualTo(-1);
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        SearchAction searchAction1 =
                new SearchAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setFetchedResultCount(1)
                        .build();
        SearchAction searchAction2 = new SearchAction.Builder(searchAction1).build();

        assertThat(searchAction2.getNamespace()).isEqualTo("namespace");
        assertThat(searchAction2.getId()).isEqualTo("id");
        assertThat(searchAction2.getActionTimestampMillis()).isEqualTo(123);
        assertThat(searchAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(searchAction2.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(searchAction2.getQuery()).isEqualTo("query");
        assertThat(searchAction2.getFetchedResultCount()).isEqualTo(1);
    }

    @Test
    public void testToGenericDocument() throws Exception {
        SearchAction searchAction =
                new SearchAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                    .setDocumentTtlMillis(456)
                    .setQuery("query")
                    .setFetchedResultCount(1)
                    .build();

        GenericDocument document = GenericDocument.fromDocumentClass(searchAction);
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("id");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(document.getTtlMillis()).isEqualTo(456);
        assertThat(document.getPropertyLong("actionType"))
                .isEqualTo(ActionConstants.ACTION_TYPE_SEARCH);
        assertThat(document.getPropertyString("query")).isEqualTo("query");
        assertThat(document.getPropertyLong("fetchedResultCount")).isEqualTo(1);
    }
}
