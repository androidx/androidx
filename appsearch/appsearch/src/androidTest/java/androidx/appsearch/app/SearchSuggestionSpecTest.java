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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

// TODO(b/227356108): move this test to cts test once we un-hide search suggestion API.
public class SearchSuggestionSpecTest {
    @Test
    public void testBuildDefaultSearchSuggestionSpec() {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123).build();
        assertThat(searchSuggestionSpec.getMaximumResultCount()).isEqualTo(123);
        assertThat(searchSuggestionSpec.getFilterNamespaces()).isEmpty();
    }

    @Test
    public void testBuildSearchSuggestionSpec() {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterNamespaces(ImmutableList.of("namespace3"))
                        .build();

        assertThat(searchSuggestionSpec.getMaximumResultCount()).isEqualTo(123);
        assertThat(searchSuggestionSpec.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2", "namespace3");
    }

    @Test
    public void testRebuild() {
        SearchSuggestionSpec.Builder builder =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterNamespaces("namespace1", "namespace2");

        SearchSuggestionSpec original = builder.build();

        builder.addFilterNamespaces("namespace3", "namespace4");
        SearchSuggestionSpec rebuild = builder.build();

        assertThat(original.getMaximumResultCount()).isEqualTo(123);
        assertThat(original.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2");
        assertThat(rebuild.getMaximumResultCount()).isEqualTo(123);
        assertThat(rebuild.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2", "namespace3", "namespace4");
    }
}
