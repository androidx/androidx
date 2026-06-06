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
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;

import androidx.annotation.OptIn;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.platformstorage.UnsupportedPlatformConversionAdapter;
import androidx.appsearch.testutil.TestPlatformConversionAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@OptIn(markerClass = ExperimentalAppSearchApi.class)
public class SearchSpecToPlatformConverterTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testSetSearchStringParameters() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS));
        SearchSpec jetpackSearchSpec = new SearchSpec.Builder()
                .addSearchStringParameters("param1")
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        SearchSpecToPlatformConverter.toPlatformSearchSpec(mContext, jetpackSearchSpec, adapter);

        assertThat(adapter.getSearchSpecSearchStringParameters())
                .containsExactly("param1");
    }

    @Test
    public void testSetSearchStringParameters_throws() {
        assumeFalse(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS));
        SearchSpec jetpackSearchSpec = new SearchSpec.Builder()
                .addSearchStringParameters("param1")
                .build();
        UnsupportedPlatformConversionAdapter adapter = new UnsupportedPlatformConversionAdapter();

        assertThrows(UnsupportedOperationException.class,
                () -> SearchSpecToPlatformConverter.toPlatformSearchSpec(
                        mContext, jetpackSearchSpec, adapter));
    }
}
