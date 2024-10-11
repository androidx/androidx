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
import androidx.appsearch.usagereporting.DismissAction;
import androidx.appsearch.usagereporting.TakenAction;

import org.junit.Test;

public class DismissActionCtsTest {
    @Test
    public void testBuilder() {
        DismissAction dismissAction =
                new DismissAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .build();

        assertThat(dismissAction.getNamespace()).isEqualTo("namespace");
        assertThat(dismissAction.getId()).isEqualTo("id");
        assertThat(dismissAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(dismissAction.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(dismissAction.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_DISMISS);
        assertThat(dismissAction.getQuery()).isEqualTo("query");
        assertThat(dismissAction.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");
        assertThat(dismissAction.getResultRankInBlock()).isEqualTo(1);
        assertThat(dismissAction.getResultRankGlobal()).isEqualTo(3);
    }

    @Test
    public void testBuilder_defaultValues() {
        DismissAction dismissAction =
                new DismissAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .build();

        assertThat(dismissAction.getNamespace()).isEqualTo("namespace");
        assertThat(dismissAction.getId()).isEqualTo("id");
        assertThat(dismissAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(dismissAction.getDocumentTtlMillis())
                .isEqualTo(TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS);
        assertThat(dismissAction.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_DISMISS);
        assertThat(dismissAction.getQuery()).isNull();
        assertThat(dismissAction.getReferencedQualifiedId()).isNull();
        assertThat(dismissAction.getResultRankInBlock()).isEqualTo(-1);
        assertThat(dismissAction.getResultRankGlobal()).isEqualTo(-1);
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        DismissAction dismissAction1 =
                new DismissAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .build();
        DismissAction dismissAction2 = new DismissAction.Builder(dismissAction1).build();

        // All fields should be copied correctly from dismissAction1 to the builder and propagates
        // to dismissAction2 after calling build().
        assertThat(dismissAction2.getNamespace()).isEqualTo("namespace");
        assertThat(dismissAction2.getId()).isEqualTo("id");
        assertThat(dismissAction2.getActionTimestampMillis()).isEqualTo(123);
        assertThat(dismissAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(dismissAction2.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_DISMISS);
        assertThat(dismissAction2.getQuery()).isEqualTo("query");
        assertThat(dismissAction2.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");
        assertThat(dismissAction2.getResultRankInBlock()).isEqualTo(1);
        assertThat(dismissAction2.getResultRankGlobal()).isEqualTo(3);
    }

    @Test
    public void testToGenericDocument() throws Exception {
        DismissAction dismissAction =
                new DismissAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .build();

        GenericDocument document = GenericDocument.fromDocumentClass(dismissAction);
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("id");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(document.getTtlMillis()).isEqualTo(456);
        assertThat(document.getPropertyLong("actionType"))
                .isEqualTo(ActionConstants.ACTION_TYPE_DISMISS);
        assertThat(document.getPropertyString("query")).isEqualTo("query");
        assertThat(document.getPropertyString("referencedQualifiedId"))
                .isEqualTo("pkg$db/ns#refId");
        assertThat(document.getPropertyLong("resultRankInBlock")).isEqualTo(1);
        assertThat(document.getPropertyLong("resultRankGlobal")).isEqualTo(3);
    }
}
