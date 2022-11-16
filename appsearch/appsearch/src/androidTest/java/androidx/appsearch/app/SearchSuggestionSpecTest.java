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

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.exceptions.AppSearchException;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

// TODO(b/227356108): move this test to cts test once we un-hide search suggestion API.
public class SearchSuggestionSpecTest {
    @Test
    public void testBuildDefaultSearchSuggestionSpec() throws Exception {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123).build();
        assertThat(searchSuggestionSpec.getMaximumResultCount()).isEqualTo(123);
        assertThat(searchSuggestionSpec.getFilterNamespaces()).isEmpty();
    }

    @Test
    public void testBuildSearchSuggestionSpec() throws Exception {
        SearchSuggestionSpec searchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterNamespaces(ImmutableList.of("namespace3"))
                        .addFilterSchemas("Person", "Email")
                        .addFilterSchemas(ImmutableList.of("Foo"))
                        .addFilterProperties("Email", ImmutableList.of("Subject", "body"))
                        .addFilterPropertyPaths("Foo",
                                ImmutableList.of(new PropertyPath("Bar")))
                        .build();

        assertThat(searchSuggestionSpec.getMaximumResultCount()).isEqualTo(123);
        assertThat(searchSuggestionSpec.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2", "namespace3");
        assertThat(searchSuggestionSpec.getFilterSchemas())
                .containsExactly("Person", "Email", "Foo");
        assertThat(searchSuggestionSpec.getFilterProperties())
                .containsExactly("Email",  ImmutableList.of("Subject", "body"),
                        "Foo",  ImmutableList.of("Bar"));
    }

    @Test
    public void testPropertyFilterMustMatchSchemaFilter() throws Exception {
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterSchemas("Person")
                        .addFilterProperties("Email", ImmutableList.of("Subject", "body"))
                        .build());
        assertThat(e.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(e).hasMessageThat().contains("The schema: Email exists in the "
                + "property filter but doesn't exist in the schema filter.");
    }

    @Test
    public void testRebuild() throws Exception {
        SearchSuggestionSpec.Builder builder =
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/123)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterSchemas("Person", "Email")
                        .addFilterProperties("Email", ImmutableList.of("Subject", "body"));

        SearchSuggestionSpec original = builder.build();

        builder.addFilterNamespaces("namespace3", "namespace4")
                .addFilterSchemas("Message", "Foo")
                .addFilterProperties("Foo", ImmutableList.of("Bar"));
        SearchSuggestionSpec rebuild = builder.build();

        assertThat(original.getMaximumResultCount()).isEqualTo(123);
        assertThat(original.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2");
        assertThat(original.getFilterSchemas())
                .containsExactly("Person", "Email");
        assertThat(original.getFilterProperties())
                .containsExactly("Email",  ImmutableList.of("Subject", "body"));

        assertThat(rebuild.getMaximumResultCount()).isEqualTo(123);
        assertThat(rebuild.getFilterNamespaces())
                .containsExactly("namespace1", "namespace2", "namespace3", "namespace4");
        assertThat(rebuild.getFilterSchemas())
                .containsExactly("Person", "Email", "Message", "Foo");
        assertThat(rebuild.getFilterProperties())
                .containsExactly("Email",  ImmutableList.of("Subject", "body"),
                        "Foo",  ImmutableList.of("Bar"));
    }
}
