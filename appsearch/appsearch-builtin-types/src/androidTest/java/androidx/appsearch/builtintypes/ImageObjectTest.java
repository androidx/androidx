/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.appsearch.builtintypes.properties.Keyword;

import org.junit.Test;

public class ImageObjectTest {
    @Test
    public void testBuilder() {
        ImageObject imageObject = new ImageObject.Builder("namespace", "id")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my image")
                .addAlternateName("my photo")
                .addAlternateName("my picture")
                .setDescription("this is my image")
                .setUrl("content://images/1")
                .addKeyword("pretty")
                .addKeyword("wow")
                .setSha256("6ed48")
                .setThumbnailSha256("8df68")
                .build();

        assertThat(imageObject.getNamespace()).isEqualTo("namespace");
        assertThat(imageObject.getId()).isEqualTo("id");
        assertThat(imageObject.getDocumentScore()).isEqualTo(1);
        assertThat(imageObject.getDocumentTtlMillis()).isEqualTo(20000);
        assertThat(imageObject.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(imageObject.getName()).isEqualTo("my image");
        assertThat(imageObject.getAlternateNames()).containsExactly("my photo", "my picture");
        assertThat(imageObject.getDescription()).isEqualTo("this is my image");
        assertThat(imageObject.getUrl()).isEqualTo("content://images/1");
        assertThat(imageObject.getKeywords())
                .containsExactly(new Keyword("pretty"), new Keyword("wow"));
        assertThat(imageObject.getSha256()).isEqualTo("6ed48");
        assertThat(imageObject.getThumbnailSha256()).isEqualTo("8df68");
    }

    @Test
    public void testBuilder_CopyConstructor() {
        ImageObject imageObject1 = new ImageObject.Builder("namespace", "id")
                .setDocumentScore(1)
                .setDocumentTtlMillis(20000)
                .setCreationTimestampMillis(100)
                .setName("my image")
                .addAlternateName("my photo")
                .addAlternateName("my picture")
                .setDescription("this is my image")
                .setUrl("content://images/1")
                .addKeyword("pretty")
                .addKeyword("wow")
                .setSha256("6ed48")
                .setThumbnailSha256("8df68")
                .build();
        ImageObject imageObject2 = new ImageObject.Builder(imageObject1).build();

        assertThat(imageObject2.getNamespace()).isEqualTo("namespace");
        assertThat(imageObject2.getId()).isEqualTo("id");
        assertThat(imageObject2.getDocumentScore()).isEqualTo(1);
        assertThat(imageObject2.getDocumentTtlMillis()).isEqualTo(20000);
        assertThat(imageObject2.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(imageObject2.getName()).isEqualTo("my image");
        assertThat(imageObject2.getAlternateNames()).containsExactly("my photo", "my picture");
        assertThat(imageObject2.getDescription()).isEqualTo("this is my image");
        assertThat(imageObject2.getUrl()).isEqualTo("content://images/1");
        assertThat(imageObject2.getKeywords())
                .containsExactly(new Keyword("pretty"), new Keyword("wow"));
        assertThat(imageObject2.getSha256()).isEqualTo("6ed48");
        assertThat(imageObject2.getThumbnailSha256()).isEqualTo("8df68");
    }
}
