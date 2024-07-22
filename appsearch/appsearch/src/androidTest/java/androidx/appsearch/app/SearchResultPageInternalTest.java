/*
 * Copyright 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SearchResultPageInternalTest {
    @Test
    public void testSearchResultPage() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "schemaType").build();
        List<SearchResult> results = Arrays.asList(
                new SearchResult.Builder("package1", "database1").setGenericDocument(
                        document).build(),
                new SearchResult.Builder("package2", "database2").setGenericDocument(
                        document).build()
        );
        SearchResultPage searchResultPage = new SearchResultPage(/*nextPageToken=*/ 123, results);
        assertThat(searchResultPage.getNextPageToken()).isEqualTo(123);
        List<SearchResult> searchResults = searchResultPage.getResults();
        assertThat(searchResults).hasSize(2);
        assertThat(searchResults.get(0).getPackageName()).isEqualTo("package1");
        assertThat(searchResults.get(0).getDatabaseName()).isEqualTo("database1");
        assertThat(searchResults.get(0).getGenericDocument()).isEqualTo(document);
        assertThat(searchResults.get(1).getPackageName()).isEqualTo("package2");
        assertThat(searchResults.get(1).getDatabaseName()).isEqualTo("database2");
        assertThat(searchResults.get(1).getGenericDocument()).isEqualTo(document);
    }
}
