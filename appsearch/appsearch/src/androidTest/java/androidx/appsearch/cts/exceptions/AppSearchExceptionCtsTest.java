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

package androidx.appsearch.cts.exceptions;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.exceptions.AppSearchException;

import org.junit.Test;

public class AppSearchExceptionCtsTest {
    @Test
    public void testNoMessageException() {
        AppSearchException e = new AppSearchException(AppSearchResult.RESULT_IO_ERROR);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_IO_ERROR);

        AppSearchResult<?> result = e.toAppSearchResult();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResultCode()).isEqualTo(AppSearchResult.RESULT_IO_ERROR);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    public void testExceptionWithMessage() {
        AppSearchException e =
                new AppSearchException(AppSearchResult.RESULT_NOT_FOUND, "ERROR!");
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        AppSearchResult<?> result = e.toAppSearchResult();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(result.getErrorMessage()).isEqualTo("ERROR!");
    }

    @Test
    public void testExceptionWithThrowable() {
        IllegalArgumentException throwable = new IllegalArgumentException("You can't do that!");
        AppSearchException e = new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                "ERROR!", throwable);
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_INVALID_ARGUMENT);
        assertThat(e.getCause()).isEqualTo(throwable);

        AppSearchResult<?> result = e.toAppSearchResult();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResultCode()).isEqualTo(AppSearchResult.RESULT_INVALID_ARGUMENT);
        assertThat(result.getErrorMessage()).isEqualTo("ERROR!");
    }
}
