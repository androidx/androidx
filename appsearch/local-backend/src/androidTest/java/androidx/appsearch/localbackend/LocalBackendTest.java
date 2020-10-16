/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.localbackend;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

public class LocalBackendTest {
    @Test
    public void testSameInstance() throws Exception {
        LocalBackend b1 = LocalBackend.getInstance(ApplicationProvider.getApplicationContext())
                .get().getResultValue();
        LocalBackend b2 = LocalBackend.getInstance(ApplicationProvider.getApplicationContext())
                .get().getResultValue();
        assertThat(b1).isSameInstanceAs(b2);
    }

    @Test
    public void testDatabaseName() throws Exception {
        // Test special character can present in database name. When a special character is banned
        // in database name, add checker in SearchContext.Builder and reflect it in java doc.
        LocalBackend.SearchContext.Builder contextBuilder =
                new LocalBackend.SearchContext.Builder(
                        ApplicationProvider.getApplicationContext());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> contextBuilder.setDatabaseName("testDatabaseNameEndWith/"));
        assertThat(e).hasMessageThat().isEqualTo("Database name cannot contain '/'");
        e = assertThrows(IllegalArgumentException.class,
                () -> contextBuilder.setDatabaseName("/testDatabaseNameStartWith"));
        assertThat(e).hasMessageThat().isEqualTo("Database name cannot contain '/'");
    }
}
