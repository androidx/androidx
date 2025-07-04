/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

public class GlobalSearchApplicationInfoTest {
    @Test
    public void testBuilder_defaultValues() {
        GlobalSearchApplicationInfo appInfo = new GlobalSearchApplicationInfo.Builder(
                "namespace", "id", GlobalSearchApplicationInfo.APPLICATION_TYPE_CONSUMER)
                .build();
        assertThat(appInfo.getNamespace()).isEqualTo("namespace");
        assertThat(appInfo.getId()).isEqualTo("id");
        assertThat(appInfo.getApplicationType())
                .isEqualTo(GlobalSearchApplicationInfo.APPLICATION_TYPE_CONSUMER);
        assertThat(appInfo.getSchemaTypes()).isEmpty();
    }

    @Test
    public void testBuilder_setAllValues() {
        GlobalSearchApplicationInfo appInfo = new GlobalSearchApplicationInfo.Builder(
                "namespace", "id", GlobalSearchApplicationInfo.APPLICATION_TYPE_PRODUCER)
                .setSchemaTypes(ImmutableList.of("Type1", "Type2"))
                .build();
        assertThat(appInfo.getNamespace()).isEqualTo("namespace");
        assertThat(appInfo.getId()).isEqualTo("id");
        assertThat(appInfo.getApplicationType())
                .isEqualTo(GlobalSearchApplicationInfo.APPLICATION_TYPE_PRODUCER);
        assertThat(appInfo.getSchemaTypes()).containsExactly("Type1", "Type2").inOrder();
    }

    @Test
    public void testBuilder_setDocumentClasses() throws Exception {
        GlobalSearchApplicationInfo appInfo = new GlobalSearchApplicationInfo.Builder(
                "namespace", "id", GlobalSearchApplicationInfo.APPLICATION_TYPE_PRODUCER)
                .setSchemaTypes(ImmutableList.of("Type1", "Type2"))
                .setSchemaTypes(ImmutableList.of("Type3", "Type4"))
                .setDocumentClasses(ImmutableList.of(Person.class, MobileApplication.class))
                .build();
        assertThat(appInfo.getNamespace()).isEqualTo("namespace");
        assertThat(appInfo.getId()).isEqualTo("id");
        assertThat(appInfo.getApplicationType())
                .isEqualTo(GlobalSearchApplicationInfo.APPLICATION_TYPE_PRODUCER);
        assertThat(appInfo.getSchemaTypes())
                .containsExactly("builtin:Person", "builtin:MobileApplication").inOrder();
    }

    @Test
    public void testBuilder_setNull() {
        GlobalSearchApplicationInfo.Builder builder = new GlobalSearchApplicationInfo.Builder(
                "namespace", "id", GlobalSearchApplicationInfo.APPLICATION_TYPE_PRODUCER);
        assertThrows(NullPointerException.class, () -> builder.setSchemaTypes(null));
        assertThrows(NullPointerException.class, () -> builder.setDocumentClasses(null));
        assertThrows(
                NullPointerException.class,
                () -> builder.setSchemaTypes(ImmutableList.of("Type1", null, "Type2")));
        assertThrows(
                NullPointerException.class,
                () -> builder.setDocumentClasses(
                        ImmutableList.of(Person.class, null, MobileApplication.class)));
    }

    @Test
    public void testBuilder_modifiedAfterBuild_doesNotAffectBuiltObjects() {
        GlobalSearchApplicationInfo.Builder builder = new GlobalSearchApplicationInfo.Builder(
                "namespace", "id", GlobalSearchApplicationInfo.APPLICATION_TYPE_CONSUMER);

        // First build with default values
        GlobalSearchApplicationInfo appInfo1 = builder.build();
        assertThat(appInfo1.getNamespace()).isEqualTo("namespace");
        assertThat(appInfo1.getId()).isEqualTo("id");
        assertThat(appInfo1.getApplicationType())
                .isEqualTo(GlobalSearchApplicationInfo.APPLICATION_TYPE_CONSUMER);
        assertThat(appInfo1.getSchemaTypes()).isEmpty();

        // Modify the builder and build again
        GlobalSearchApplicationInfo appInfo2 = builder
                .setSchemaTypes(ImmutableList.of("Type1", "Type2"))
                .build();

        // Check the second object has the new values
        assertThat(appInfo2.getNamespace()).isEqualTo("namespace");
        assertThat(appInfo2.getId()).isEqualTo("id");
        assertThat(appInfo2.getApplicationType())
                .isEqualTo(GlobalSearchApplicationInfo.APPLICATION_TYPE_CONSUMER);
        assertThat(appInfo2.getSchemaTypes()).containsExactly("Type1", "Type2").inOrder();

        // Check the first object is not affected
        assertThat(appInfo1.getNamespace()).isEqualTo("namespace");
        assertThat(appInfo1.getId()).isEqualTo("id");
        assertThat(appInfo1.getApplicationType())
                .isEqualTo(GlobalSearchApplicationInfo.APPLICATION_TYPE_CONSUMER);
        assertThat(appInfo1.getSchemaTypes()).isEmpty();
    }
}
