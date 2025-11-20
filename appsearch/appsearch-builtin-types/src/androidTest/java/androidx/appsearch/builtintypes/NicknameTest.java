/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NicknameTest {
    private static final String NICKNAME_ID = "nickname-id";
    private static final String NICKNAME_NS = "nickname-ns";
    private static final String SAMPLE_QUALIFIED_ID =
            DocumentIdUtil.createQualifiedId("android", "apps-db", "apps", "com.example.app");
    private static final List<String> ALTERNATE_NAMES_1 =
            ImmutableList.of("alternate name 1", "alternate name 2");

    @Test
    public void testBuilder() {
        Nickname.Builder builder =
                new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, SAMPLE_QUALIFIED_ID);
        builder.setAlternateNames(ALTERNATE_NAMES_1);
        Nickname nickname = builder.build();
        assertThat(nickname.getId()).isEqualTo(NICKNAME_ID);
        assertThat(nickname.getNamespace()).isEqualTo(NICKNAME_NS);
        assertThat(nickname.getReferencedQualifiedId()).isEqualTo(SAMPLE_QUALIFIED_ID);
        assertThat(nickname.getAlternateNames()).containsExactlyElementsIn(ALTERNATE_NAMES_1);
    }

    @Test
    public void testBuilder_nullChecks() {
        assertThrows(NullPointerException.class, () ->
                new Nickname.Builder(null, NICKNAME_ID, SAMPLE_QUALIFIED_ID));
        assertThrows(NullPointerException.class, () ->
                new Nickname.Builder(NICKNAME_NS, null, SAMPLE_QUALIFIED_ID));
        assertThrows(NullPointerException.class, () ->
                new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, null));
        // This should not throw, it should just set alternate names to empty.
        Nickname noNicknames = new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, SAMPLE_QUALIFIED_ID)
                .setAlternateNames(null).build();
        assertThat(noNicknames.getAlternateNames()).isEmpty();
    }

    @Test
    public void testBuilder_copyConstructor() {
        Nickname original = new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, SAMPLE_QUALIFIED_ID)
                .setAlternateNames(ALTERNATE_NAMES_1)
                .build();
        Nickname copy = new Nickname.Builder(original).build();
        assertThat(copy.getId()).isEqualTo(NICKNAME_ID);
        assertThat(copy.getNamespace()).isEqualTo(NICKNAME_NS);
        assertThat(copy.getReferencedQualifiedId()).isEqualTo(SAMPLE_QUALIFIED_ID);
        assertThat(copy.getAlternateNames()).isEqualTo(ALTERNATE_NAMES_1);
    }

    @Test
    public void testBuilder_immutableAfterBuilt() {
        Nickname.Builder builder =
                new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, SAMPLE_QUALIFIED_ID);
        builder.setAlternateNames(ALTERNATE_NAMES_1);
        Nickname nickname = builder.build();
        // Further modifications to the builder should not affect the built object.
        builder.setAlternateNames(Arrays.asList("new alternate name 1", "new alternate name 2"));
        // Assert the original object hasn't changed
        assertThat(nickname.getAlternateNames()).containsExactlyElementsIn(ALTERNATE_NAMES_1);
    }

    @Test
    public void testBuilder_inputListIsCopied() {
        List<String> alternateNames = new ArrayList<>(Arrays.asList("a", "b"));
        Nickname.Builder builder =
                new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, SAMPLE_QUALIFIED_ID);

        builder.setAlternateNames(alternateNames);
        Nickname nickname1 = builder.build();

        builder.setAlternateNames(alternateNames);
        Nickname nickname2 = builder.build();

        assertThat(nickname1).isEqualTo(nickname2);

        // This should not affect the built objects because the builder made a defensive copy
        alternateNames.add("c");

        assertThat(nickname1.getAlternateNames()).containsExactly("a", "b");
        assertThat(nickname2.getAlternateNames()).containsExactly("a", "b");
        assertThat(nickname1).isEqualTo(nickname2);
    }

    @Test
    public void testGenericDocument() throws Exception {
        Nickname nickname =
                new Nickname.Builder(NICKNAME_NS, NICKNAME_ID, SAMPLE_QUALIFIED_ID)
                        .setAlternateNames(ALTERNATE_NAMES_1)
                        .build();
        GenericDocument doc = GenericDocument.fromDocumentClass(nickname);
        assertThat(doc.getSchemaType()).isEqualTo("builtin:Nickname");
        assertThat(doc.getNamespace()).isEqualTo(NICKNAME_NS);
        assertThat(doc.getId()).isEqualTo(NICKNAME_ID);
        assertThat(doc.getPropertyString("referencedQualifiedId")).isEqualTo(SAMPLE_QUALIFIED_ID);
        assertThat(
                doc.getPropertyStringArray("alternateNames"))
                .asList()
                .isEqualTo(ALTERNATE_NAMES_1);
        // Test the round-trip conversion
        Nickname converted = doc.toDocumentClass(Nickname.class);
        assertThat(converted.getId()).isEqualTo(NICKNAME_ID);
        assertThat(converted.getNamespace()).isEqualTo(NICKNAME_NS);
        assertThat(converted.getReferencedQualifiedId()).isEqualTo(SAMPLE_QUALIFIED_ID);
        assertThat(converted.getAlternateNames()).isEqualTo(ALTERNATE_NAMES_1);
    }

    @Test
    public void testCreateFromGenericDocument() throws Exception {
        GenericDocument genericDocNickname =
                new GenericDocument.Builder<>(NICKNAME_NS, NICKNAME_ID, "builtin:Nickname")
                        .setPropertyString("alternateNames",
                                "alternate name 1", "alternate name 2")
                        .setPropertyString("referencedQualifiedId", SAMPLE_QUALIFIED_ID)
                        .build();

        Nickname converted = genericDocNickname.toDocumentClass(Nickname.class);
        assertThat(converted.getAlternateNames()).containsExactlyElementsIn(ALTERNATE_NAMES_1);
        assertThat(converted.getReferencedQualifiedId()).isEqualTo(SAMPLE_QUALIFIED_ID);
    }
}
