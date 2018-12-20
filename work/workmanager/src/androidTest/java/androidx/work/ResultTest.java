/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.test.filters.SmallTest;
import androidx.work.ListenableWorker.Result;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ResultTest {

    private static Data sData;

    @Before
    public void setUp() {
        sData = new Data.Builder()
                .put("int", 1)
                .put("String", "some_value")
                .build();
    }

    @Test
    public void testSuccessfulResults() {
        assertThat(Result.success(), is(equalTo(Result.success())));
        assertThat(Result.success(sData), is(equalTo(Result.success(sData))));
        assertThat(Result.success(sData), is(not(equalTo(Result.success()))));
    }

    @Test
    public void testFailureResults() {
        assertThat(Result.failure(), is(equalTo(Result.failure())));
        assertThat(Result.failure(sData), is(equalTo(Result.failure(sData))));
        assertThat(Result.failure(sData), is(not(equalTo(Result.failure()))));
    }

    @Test
    public void testRetryResults() {
        assertThat(Result.retry(), is(equalTo(Result.retry())));
        assertThat(Result.retry().hashCode(), is(equalTo(Result.retry().hashCode())));
        assertThat(Result.retry(), is(not(equalTo(Result.success()))));
        assertThat(Result.retry(), is(not(equalTo(Result.failure()))));
        assertThat(Result.retry().hashCode(), is(not(equalTo(Result.success().hashCode()))));
        assertThat(Result.retry().hashCode(), is(not(equalTo(Result.failure().hashCode()))));
    }

    @Test
    public void testHashCodes() {
        assertThat(Result.success().hashCode(), is(not(equalTo(Result.failure().hashCode()))));
        assertThat(Result.success(sData).hashCode(),
                is(not(equalTo(Result.failure(sData).hashCode()))));
    }
}
