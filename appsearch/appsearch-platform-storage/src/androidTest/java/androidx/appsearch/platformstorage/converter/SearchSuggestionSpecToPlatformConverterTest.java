/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.platformstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;
import android.os.ext.SdkExtensions;

import androidx.annotation.OptIn;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.appsearch.platformstorage.UnsupportedPlatformConversionAdapter;
import androidx.appsearch.testutil.TestPlatformConversionAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@OptIn(markerClass = ExperimentalAppSearchApi.class)
public class SearchSuggestionSpecToPlatformConverterTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testSetSearchStringParameters() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS));
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU)
                >= AppSearchVersionUtil.TExtensionVersions.U_BASE);
        SearchSuggestionSpec jetpackSearchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalLimit=*/5)
                .addSearchStringParameters("param1")
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        // This if statement is completely unnecessary because of the assumeTrue.
        // I am only including it so that the linter will stop yelling at me.
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU)
                >= 7) {
            SearchSuggestionSpecToPlatformConverter.toPlatformSearchSuggestionSpec(
                    jetpackSearchSuggestionSpec, adapter);
        }

        assertThat(adapter.getSearchSuggestionSpecSearchStringParameters())
                .containsExactly("param1");
    }

    @Test
    public void testSetSearchStringParameters_throws() {
        assumeFalse(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS));
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU)
                >= AppSearchVersionUtil.TExtensionVersions.U_BASE);
        SearchSuggestionSpec jetpackSearchSuggestionSpec =
                new SearchSuggestionSpec.Builder(/*totalLimit=*/5)
                .addSearchStringParameters("param1")
                .build();
        UnsupportedPlatformConversionAdapter adapter = new UnsupportedPlatformConversionAdapter();

        // This if statement is completely unnecessary because of the assumeTrue.
        // I am only including it so that the linter will stop yelling at me.
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU)
                >= 7) {
            assertThrows(UnsupportedOperationException.class,
                    () -> SearchSuggestionSpecToPlatformConverter.toPlatformSearchSuggestionSpec(
                            jetpackSearchSuggestionSpec, adapter));
        }
    }
}
