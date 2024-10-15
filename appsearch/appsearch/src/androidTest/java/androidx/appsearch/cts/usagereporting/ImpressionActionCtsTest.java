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
// @exportToFramework:skipFile()

package androidx.appsearch.cts.usagereporting;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.usagereporting.ActionConstants;
import androidx.appsearch.usagereporting.ImpressionAction;
import androidx.appsearch.usagereporting.TakenAction;

import org.junit.Test;

public class ImpressionActionCtsTest {
    @Test
    public void testBuilder() {
        ImpressionAction impressionAction =
                new ImpressionAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .build();

        assertThat(impressionAction.getNamespace()).isEqualTo("namespace");
        assertThat(impressionAction.getId()).isEqualTo("id");
        assertThat(impressionAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(impressionAction.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(impressionAction.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_IMPRESSION);
        assertThat(impressionAction.getQuery()).isEqualTo("query");
        assertThat(impressionAction.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");
        assertThat(impressionAction.getResultRankInBlock()).isEqualTo(1);
        assertThat(impressionAction.getResultRankGlobal()).isEqualTo(3);
    }

    @Test
    public void testBuilder_defaultValues() {
        ImpressionAction impressionAction =
                new ImpressionAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .build();

        assertThat(impressionAction.getNamespace()).isEqualTo("namespace");
        assertThat(impressionAction.getId()).isEqualTo("id");
        assertThat(impressionAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(impressionAction.getDocumentTtlMillis())
                .isEqualTo(TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS);
        assertThat(impressionAction.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_IMPRESSION);
        assertThat(impressionAction.getQuery()).isNull();
        assertThat(impressionAction.getReferencedQualifiedId()).isNull();
        assertThat(impressionAction.getResultRankInBlock()).isEqualTo(-1);
        assertThat(impressionAction.getResultRankGlobal()).isEqualTo(-1);
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        ImpressionAction impressionAction1 =
                new ImpressionAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .build();
        ImpressionAction impressionAction2 =
                new ImpressionAction.Builder(impressionAction1).build();

        // All fields should be copied correctly from impressionAction1 to the builder and
        // propagates to impressionAction2 after calling build().
        assertThat(impressionAction2.getNamespace()).isEqualTo("namespace");
        assertThat(impressionAction2.getId()).isEqualTo("id");
        assertThat(impressionAction2.getActionTimestampMillis()).isEqualTo(123);
        assertThat(impressionAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(impressionAction2.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_IMPRESSION);
        assertThat(impressionAction2.getQuery()).isEqualTo("query");
        assertThat(impressionAction2.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");
        assertThat(impressionAction2.getResultRankInBlock()).isEqualTo(1);
        assertThat(impressionAction2.getResultRankGlobal()).isEqualTo(3);
    }

    @Test
    public void testToGenericDocument() throws Exception {
        ImpressionAction impressionAction =
                new ImpressionAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .build();

        GenericDocument document = GenericDocument.fromDocumentClass(impressionAction);
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("id");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(document.getTtlMillis()).isEqualTo(456);
        assertThat(document.getPropertyLong("actionType"))
                .isEqualTo(ActionConstants.ACTION_TYPE_IMPRESSION);
        assertThat(document.getPropertyString("query")).isEqualTo("query");
        assertThat(document.getPropertyString("referencedQualifiedId"))
                .isEqualTo("pkg$db/ns#refId");
        assertThat(document.getPropertyLong("resultRankInBlock")).isEqualTo(1);
        assertThat(document.getPropertyLong("resultRankGlobal")).isEqualTo(3);
    }
}
