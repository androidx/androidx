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

package androidx.core.google.shortcuts.utils;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EntityUriUtilsTest {
    @Test
    @SmallTest
    public void testGetEntityId_validUris_returnsId() {
        String[] entityUriStrings = new String[] {
                // normal uri
                "appsearch://__shortcut_adapter_db__/__shortcut_adapter_ns__/id%201",
                // uri with parameters
                "appsearch://__shortcut_adapter_db__/__shortcut_adapter_ns__/id%201?schemaType"
                        + "=Timer",
        };

        for (String entityUri : entityUriStrings) {
            String id = EntityUriUtils.getEntityId(entityUri);
            assertThat(id).isEqualTo("id 1");
        }
    }

    @Test
    @SmallTest
    public void testGetEntityId_invalidUris_returnsNull() {
        String[] entityUriStrings = new String[] {
                // not a uri
                "not_a_uri",
                // wrong scheme
                "some_scheme://__shortcut_adapter_db__/__shortcut_adapter_ns__/id%201",
                // wrong authority
                "appsearch://some_authority/__shortcut_adapter_ns__/id%201",
                // wrong namespace
                "appsearch://__shortcut_adapter_db__/some_namespace/id%201",
                // extra path
                "appsearch://__shortcut_adapter_db__/__shortcut_adapter_ns__/id%201/more_path",
                // missing id
                "appsearch://__shortcut_adapter_db__/__shortcut_adapter_ns__",
                // missing namespace
                "appsearch://__shortcut_adapter_db__/id%201",
        };

        for (String entityUri : entityUriStrings) {
            String id = EntityUriUtils.getEntityId(entityUri);
            assertThat(id).isNull();
        }
    }
}
