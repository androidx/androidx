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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

public class LocalStorageTest {
    @Test
    public void testSameInstance() throws Exception {
        LocalStorage b1 =
                LocalStorage.getOrCreateInstance(ApplicationProvider.getApplicationContext());
        LocalStorage b2 =
                LocalStorage.getOrCreateInstance(ApplicationProvider.getApplicationContext());
        assertThat(b1).isSameInstanceAs(b2);
    }

    @Test
    public void testDatabaseName() {
        // Test special character can present in database name. When a special character is banned
        // in database name, add checker in SearchContext.Builder and reflect it in java doc.
        LocalStorage.SearchContext.Builder contextBuilder =
                new LocalStorage.SearchContext.Builder(
                        ApplicationProvider.getApplicationContext(),
                        /*databaseName=*/ "");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new LocalStorage.SearchContext.Builder(
                        ApplicationProvider.getApplicationContext(),
                        "testDatabaseNameEndWith/").build());
        assertThat(e).hasMessageThat().isEqualTo("Database name cannot contain '/'");
        e = assertThrows(IllegalArgumentException.class,
                () -> new LocalStorage.SearchContext.Builder(
                        ApplicationProvider.getApplicationContext(),
                        "/testDatabaseNameStartWith").build());
        assertThat(e).hasMessageThat().isEqualTo("Database name cannot contain '/'");
    }
}
