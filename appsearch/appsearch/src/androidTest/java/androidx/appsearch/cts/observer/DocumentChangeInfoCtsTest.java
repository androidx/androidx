/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.cts.observer;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.observer.DocumentChangeInfo;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

public class DocumentChangeInfoCtsTest {
    @Test
    public void testConstructor() {
        DocumentChangeInfo DocumentChangeInfo = new DocumentChangeInfo(
                "packageName",
                "databaseName",
                "namespace",
                "SchemaName",
                ImmutableSet.of("documentId1", "documentId2"));
        assertThat(DocumentChangeInfo.getPackageName()).isEqualTo("packageName");
        assertThat(DocumentChangeInfo.getDatabaseName()).isEqualTo("databaseName");
        assertThat(DocumentChangeInfo.getNamespace()).isEqualTo("namespace");
        assertThat(DocumentChangeInfo.getSchemaName()).isEqualTo("SchemaName");
        assertThat(DocumentChangeInfo.getChangedDocumentIds())
                .containsExactly("documentId1", "documentId2");
    }

    @Test
    public void testEqualsAndHasCode() {
        DocumentChangeInfo info1Copy1 = new DocumentChangeInfo(
                "packageName",
                "databaseName",
                "namespace",
                "SchemaName",
                ImmutableSet.of("documentId1", "documentId2"));
        DocumentChangeInfo info1Copy2 = new DocumentChangeInfo(
                "packageName",
                "databaseName",
                "namespace",
                "SchemaName",
                ImmutableSet.of("documentId1", "documentId2"));
        DocumentChangeInfo info2 = new DocumentChangeInfo(
                "packageName",
                "databaseName",
                "namespace",
                "SchemaName",
                ImmutableSet.of("documentId3", "documentId2"));

        assertThat(info1Copy1).isEqualTo(info1Copy2);
        assertThat(info1Copy1.hashCode()).isEqualTo(info1Copy2.hashCode());
        assertThat(info1Copy1).isNotEqualTo(info2);
        assertThat(info1Copy1.hashCode()).isNotEqualTo(info2.hashCode());
    }
}
