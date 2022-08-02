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

package androidx.appsearch.platformstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Build;

import androidx.appsearch.app.AppSearchResult;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class AppSearchResultToPlatformConverterTest {
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    public void testPlatformAppSearchResultToJetpack_catchException() {
        android.app.appsearch.AppSearchResult<String> platformResult =
                android.app.appsearch.AppSearchResult.newSuccessfulResult("42");
        AppSearchResult<Integer> jetpackResult =
                AppSearchResultToPlatformConverter.platformAppSearchResultToJetpack(
                        platformResult,
                        platformValue -> {
                            throw new IllegalArgumentException("Test exception");
                        }
                );
        assertThat(jetpackResult.getResultCode())
                .isEqualTo(AppSearchResult.RESULT_INVALID_ARGUMENT);
        assertThat(jetpackResult.getErrorMessage()).contains("Test exception");
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    public void testPlatformAppSearchResultToFuture_catchException() {
        android.app.appsearch.AppSearchResult<String> platformResult =
                android.app.appsearch.AppSearchResult.newSuccessfulResult("42");
        ResolvableFuture<Integer> future = ResolvableFuture.create();
        AppSearchResultToPlatformConverter.platformAppSearchResultToFuture(
                platformResult,
                future,
                platformValue -> {
                    throw new IllegalArgumentException("Test exception");
                }
        );
        ExecutionException e = assertThrows(ExecutionException.class, future::get);
        assertThat(e).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
        assertThat(e).hasCauseThat().hasMessageThat().contains("Test exception");
    }
}
