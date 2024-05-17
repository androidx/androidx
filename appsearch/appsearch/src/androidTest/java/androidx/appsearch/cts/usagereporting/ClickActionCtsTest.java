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
// @exportToFramework:skipFile()

package androidx.appsearch.cts.usagereporting;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.usagereporting.ActionConstants;
import androidx.appsearch.usagereporting.ClickAction;
import androidx.appsearch.usagereporting.TakenAction;

import org.junit.Test;

public class ClickActionCtsTest {
    @Test
    public void testBuilder() {
        ClickAction clickAction =
                new ClickAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .setTimeStayOnResultMillis(65536)
                        .build();

        assertThat(clickAction.getNamespace()).isEqualTo("namespace");
        assertThat(clickAction.getId()).isEqualTo("id");
        assertThat(clickAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(clickAction.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(clickAction.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(clickAction.getQuery()).isEqualTo("query");
        assertThat(clickAction.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");
        assertThat(clickAction.getResultRankInBlock()).isEqualTo(1);
        assertThat(clickAction.getResultRankGlobal()).isEqualTo(3);
        assertThat(clickAction.getTimeStayOnResultMillis()).isEqualTo(65536);
    }

    @Test
    public void testBuilder_defaultValues() {
        ClickAction clickAction =
                new ClickAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .build();

        assertThat(clickAction.getNamespace()).isEqualTo("namespace");
        assertThat(clickAction.getId()).isEqualTo("id");
        assertThat(clickAction.getActionTimestampMillis()).isEqualTo(123);
        assertThat(clickAction.getDocumentTtlMillis())
                .isEqualTo(TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS);
        assertThat(clickAction.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(clickAction.getQuery()).isNull();
        assertThat(clickAction.getReferencedQualifiedId()).isNull();
        assertThat(clickAction.getResultRankInBlock()).isEqualTo(-1);
        assertThat(clickAction.getResultRankGlobal()).isEqualTo(-1);
        assertThat(clickAction.getTimeStayOnResultMillis()).isEqualTo(-1);
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        ClickAction clickAction1 =
                new ClickAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .setTimeStayOnResultMillis(65536)
                        .build();
        ClickAction clickAction2 = new ClickAction.Builder(clickAction1).build();

        // All fields should be copied correctly from clickAction1 to the builder and propagates to
        // clickAction2 after calling build().
        assertThat(clickAction2.getNamespace()).isEqualTo("namespace");
        assertThat(clickAction2.getId()).isEqualTo("id");
        assertThat(clickAction2.getActionTimestampMillis()).isEqualTo(123);
        assertThat(clickAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(clickAction2.getActionType()).isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(clickAction2.getQuery()).isEqualTo("query");
        assertThat(clickAction2.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");
        assertThat(clickAction2.getResultRankInBlock()).isEqualTo(1);
        assertThat(clickAction2.getResultRankGlobal()).isEqualTo(3);
        assertThat(clickAction2.getTimeStayOnResultMillis()).isEqualTo(65536);
    }

    @Test
    public void testToGenericDocument() throws Exception {
        ClickAction clickAction =
                new ClickAction.Builder("namespace", "id", /* actionTimestampMillis= */123)
                        .setDocumentTtlMillis(456)
                        .setQuery("query")
                        .setReferencedQualifiedId("pkg$db/ns#refId")
                        .setResultRankInBlock(1)
                        .setResultRankGlobal(3)
                        .setTimeStayOnResultMillis(65536)
                        .build();

        GenericDocument document = GenericDocument.fromDocumentClass(clickAction);
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("id");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(document.getTtlMillis()).isEqualTo(456);
        assertThat(document.getPropertyLong("actionType"))
                .isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(document.getPropertyString("query")).isEqualTo("query");
        assertThat(document.getPropertyString("referencedQualifiedId"))
                .isEqualTo("pkg$db/ns#refId");
        assertThat(document.getPropertyLong("resultRankInBlock")).isEqualTo(1);
        assertThat(document.getPropertyLong("resultRankGlobal")).isEqualTo(3);
        assertThat(document.getPropertyLong("timeStayOnResultMillis")).isEqualTo(65536);
    }
}
