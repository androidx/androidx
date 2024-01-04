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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.SearchSuggestionResult;

import org.junit.Test;

/*@exportToFramework:SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)*/
public class SearchSuggestionResultCtsTest {
    @Test
    public void testBuildDefaultSearchSuggestionResult() {
        SearchSuggestionResult searchSuggestionResult =
                new SearchSuggestionResult.Builder().build();
        assertThat(searchSuggestionResult.getSuggestedResult()).isEmpty();
    }

    @Test
    public void testBuildSearchSuggestionResult() {
        SearchSuggestionResult searchSuggestionResult =
                new SearchSuggestionResult.Builder()
                        .setSuggestedResult("AppSearch")
                        .build();
        assertThat(searchSuggestionResult.getSuggestedResult()).isEqualTo("AppSearch");
    }

    @Test
    public void testReBuildSearchSuggestionResult() {
        SearchSuggestionResult.Builder builder =
                new SearchSuggestionResult.Builder()
                        .setSuggestedResult("AppSearch");
        SearchSuggestionResult original = builder.build();
        SearchSuggestionResult rebuild = builder.setSuggestedResult("IcingLib").build();
        assertThat(original.getSuggestedResult()).isEqualTo("AppSearch");
        assertThat(rebuild.getSuggestedResult()).isEqualTo("IcingLib");
    }
}
