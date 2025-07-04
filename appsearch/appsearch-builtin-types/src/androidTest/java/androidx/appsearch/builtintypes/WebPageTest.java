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

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

public class WebPageTest {
    @Test
    public void testBuilder() {
        WebPage webPage = new WebPage.Builder("namespace", "id1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my web page")
                .setFavicon(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
                .setSource("tab")
                .build();

        assertThat(webPage.getNamespace()).isEqualTo("namespace");
        assertThat(webPage.getId()).isEqualTo("id1");
        assertThat(webPage.getDocumentScore()).isEqualTo(1);
        assertThat(webPage.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(webPage.getDocumentTtlMillis()).isEqualTo(6000);
        assertThat(webPage.getFavicon().getSha256()).isEqualTo("123456");
        assertThat(webPage.getSource()).isEqualTo("tab");
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        WebPage webPage1 = new WebPage.Builder("namespace", "id1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setFavicon(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
                .setSource("tab")
                .build();
        WebPage webPage2 = new WebPage.Builder(webPage1).build();

        assertThat(webPage2.getNamespace()).isEqualTo("namespace");
        assertThat(webPage2.getId()).isEqualTo("id1");
        assertThat(webPage2.getDocumentScore()).isEqualTo(1);
        assertThat(webPage2.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(webPage2.getDocumentTtlMillis()).isEqualTo(6000);
        assertThat(webPage2.getFavicon().getSha256()).isEqualTo("123456");
        assertThat(webPage2.getSource()).isEqualTo("tab");
    }

    @Test
    public void testToGenericDocument() throws Exception {
        WebPage webPage = new WebPage.Builder("namespace", "id1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my web page")
                .setFavicon(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
                .setSource("tab")
                .build();

        GenericDocument document = GenericDocument.fromDocumentClass(webPage);
        assertThat(document.getSchemaType()).isEqualTo("builtin:WebPage");
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("id1");
        assertThat(document.getScore()).isEqualTo(1);
        assertThat(document.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(document.getTtlMillis()).isEqualTo(6000);
        assertThat(document.getPropertyString("favicon.sha256")).isEqualTo("123456");
        assertThat(document.getPropertyString("source")).isEqualTo("tab");

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                document.toDocumentClass(WebPage.class));
        assertThat(newGenericDocument).isEqualTo(document);
    }
}
