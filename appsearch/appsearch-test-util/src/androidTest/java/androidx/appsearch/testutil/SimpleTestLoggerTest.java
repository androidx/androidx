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

package androidx.appsearch.testutil;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;

import org.junit.Test;

public class SimpleTestLoggerTest {
    @Test
    public void testLogger_fieldsAreNullByDefault() {
        SimpleTestLogger logger = new SimpleTestLogger();

        assertThat(logger.mCallStats).isNull();
        assertThat(logger.mPutDocumentStats).isNull();
        assertThat(logger.mInitializeStats).isNull();
        assertThat(logger.mSearchStats).isNull();
        assertThat(logger.mRemoveStats).isNull();
        assertThat(logger.mOptimizeStats).isNull();
        assertThat(logger.mSetSchemaStats).isNull();
    }

    @Test
    public void testLogger_fieldsAreSetAfterLogging() {
        SimpleTestLogger logger = new SimpleTestLogger();

        logger.logStats(new CallStats.Builder().build());
        logger.logStats(new PutDocumentStats.Builder("package", "db").build());
        logger.logStats(new InitializeStats.Builder().build());
        logger.logStats(
                new SearchStats.Builder(SearchStats.VISIBILITY_SCOPE_UNKNOWN, "package").build());
        logger.logStats(new RemoveStats.Builder("package", "db").build());
        logger.logStats(new OptimizeStats.Builder().build());
        logger.logStats(new SetSchemaStats.Builder("package", "db").build());


        assertThat(logger.mCallStats).isNotNull();
        assertThat(logger.mPutDocumentStats).isNotNull();
        assertThat(logger.mInitializeStats).isNotNull();
        assertThat(logger.mSearchStats).isNotNull();
        assertThat(logger.mRemoveStats).isNotNull();
        assertThat(logger.mOptimizeStats).isNotNull();
        assertThat(logger.mSetSchemaStats).isNotNull();
    }
}
