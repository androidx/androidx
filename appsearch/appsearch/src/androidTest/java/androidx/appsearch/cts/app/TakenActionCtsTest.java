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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.TakenAction;

import org.junit.Test;

public class TakenActionCtsTest {
    @Test
    public void testBuilder() {
        TakenAction takenAction = new TakenAction.Builder("namespace", "id")
                .setCreationTimestampMillis(123)
                .setDocumentTtlMillis(456)
                .setName("name")
                .setReferencedQualifiedId("pkg$db/ns#refId")
                .addPreviousQuery("prevQuery1")
                .addPreviousQuery("prevQuery2")
                .setFinalQuery("query")
                .setResultRankInBlock(1)
                .setResultRankGlobal(3)
                .setTimeStayOnResultMillis(65536)
                .build();

        assertThat(takenAction.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction.getId()).isEqualTo("id");
        assertThat(takenAction.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(takenAction.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(takenAction.getName()).isEqualTo("name");
        assertThat(takenAction.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");

        assertThat(takenAction.getPreviousQueries()).hasSize(2);
        assertThat(takenAction.getPreviousQueries().get(0)).isEqualTo("prevQuery1");
        assertThat(takenAction.getPreviousQueries().get(1)).isEqualTo("prevQuery2");

        assertThat(takenAction.getFinalQuery()).isEqualTo("query");
        assertThat(takenAction.getResultRankInBlock()).isEqualTo(1);
        assertThat(takenAction.getResultRankGlobal()).isEqualTo(3);
        assertThat(takenAction.getTimeStayOnResultMillis()).isEqualTo(65536);
    }

    @Test
    public void testBuilder_defaultValues() {
        TakenAction takenAction = new TakenAction.Builder("namespace", "id")
                .build();

        assertThat(takenAction.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction.getId()).isEqualTo("id");
        assertThat(takenAction.getCreationTimestampMillis()).isEqualTo(-1);
        assertThat(takenAction.getDocumentTtlMillis())
                .isEqualTo(TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS);
        assertThat(takenAction.getName()).isNull();
        assertThat(takenAction.getReferencedQualifiedId()).isNull();
        assertThat(takenAction.getPreviousQueries()).isEmpty();
        assertThat(takenAction.getFinalQuery()).isNull();
        assertThat(takenAction.getResultRankInBlock()).isEqualTo(-1);
        assertThat(takenAction.getResultRankGlobal()).isEqualTo(-1);
        assertThat(takenAction.getTimeStayOnResultMillis()).isEqualTo(-1);
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        TakenAction takenAction1 = new TakenAction.Builder("namespace", "id")
                .setCreationTimestampMillis(123)
                .setDocumentTtlMillis(456)
                .setName("name")
                .setReferencedQualifiedId("pkg$db/ns#refId")
                .addPreviousQuery("prevQuery1")
                .addPreviousQuery("prevQuery2")
                .setFinalQuery("query")
                .setResultRankInBlock(1)
                .setResultRankGlobal(3)
                .setTimeStayOnResultMillis(65536)
                .build();
        TakenAction takenAction2 = new TakenAction.Builder(takenAction1).build();

        // All fields should be copied correctly from takenAction1 to the builder and propagates to
        // takenAction2 after calling build().
        assertThat(takenAction2.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction2.getId()).isEqualTo("id");
        assertThat(takenAction2.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(takenAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(takenAction2.getName()).isEqualTo("name");
        assertThat(takenAction2.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");

        assertThat(takenAction2.getPreviousQueries()).hasSize(2);
        assertThat(takenAction2.getPreviousQueries().get(0)).isEqualTo("prevQuery1");
        assertThat(takenAction2.getPreviousQueries().get(1)).isEqualTo("prevQuery2");

        assertThat(takenAction2.getFinalQuery()).isEqualTo("query");
        assertThat(takenAction2.getResultRankInBlock()).isEqualTo(1);
        assertThat(takenAction2.getResultRankGlobal()).isEqualTo(3);
        assertThat(takenAction2.getTimeStayOnResultMillis()).isEqualTo(65536);
    }


    @Test
    public void testBuilder_copiedFieldsCanBeUpdated() {
        TakenAction takenAction1 = new TakenAction.Builder("namespace", "id")
                .setCreationTimestampMillis(123)
                .setDocumentTtlMillis(456)
                .setName("name1")
                .setReferencedQualifiedId("pkg$db/ns#refId1")
                .addPreviousQuery("prevQuery1")
                .addPreviousQuery("prevQuery2")
                .setFinalQuery("query")
                .setResultRankInBlock(1)
                .setResultRankGlobal(3)
                .setTimeStayOnResultMillis(65536)
                .build();
        TakenAction takenAction2 = new TakenAction.Builder(takenAction1)
                .setName("name2")
                .setReferencedQualifiedId("pkg$db/ns#refId2")
                .setPreviousQueries(null)
                .addPreviousQuery("prevQuery3")
                .addPreviousQuery("prevQuery4")
                .addPreviousQuery("prevQuery5")
                .setFinalQuery("queryTwo")
                .setResultRankInBlock(5)
                .setResultRankGlobal(22)
                .build();

        // Check that takenAction1 wasn't altered after copying.
        assertThat(takenAction1.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction1.getId()).isEqualTo("id");
        assertThat(takenAction1.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(takenAction1.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(takenAction1.getName()).isEqualTo("name1");
        assertThat(takenAction1.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId1");

        assertThat(takenAction1.getPreviousQueries()).hasSize(2);
        assertThat(takenAction1.getPreviousQueries().get(0)).isEqualTo("prevQuery1");
        assertThat(takenAction1.getPreviousQueries().get(1)).isEqualTo("prevQuery2");

        assertThat(takenAction1.getFinalQuery()).isEqualTo("query");
        assertThat(takenAction1.getResultRankInBlock()).isEqualTo(1);
        assertThat(takenAction1.getResultRankGlobal()).isEqualTo(3);
        assertThat(takenAction1.getTimeStayOnResultMillis()).isEqualTo(65536);

        // Check that takenAction2 has the new values.
        assertThat(takenAction2.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction2.getId()).isEqualTo("id");
        assertThat(takenAction2.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(takenAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(takenAction2.getName()).isEqualTo("name2");
        assertThat(takenAction2.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId2");

        assertThat(takenAction2.getPreviousQueries()).hasSize(3);
        assertThat(takenAction2.getPreviousQueries().get(0)).isEqualTo("prevQuery3");
        assertThat(takenAction2.getPreviousQueries().get(1)).isEqualTo("prevQuery4");
        assertThat(takenAction2.getPreviousQueries().get(2)).isEqualTo("prevQuery5");

        assertThat(takenAction2.getFinalQuery()).isEqualTo("queryTwo");
        assertThat(takenAction2.getResultRankInBlock()).isEqualTo(5);
        assertThat(takenAction2.getResultRankGlobal()).isEqualTo(22);
        assertThat(takenAction2.getTimeStayOnResultMillis()).isEqualTo(65536);
    }

    @Test
    public void testBuilderCopy_builderReuse() {
        TakenAction.Builder builder = new TakenAction.Builder("namespace", "id")
                .setCreationTimestampMillis(123)
                .setDocumentTtlMillis(456)
                .setName("name")
                .setReferencedQualifiedId("pkg$db/ns#refId")
                .addPreviousQuery("prevQuery1")
                .addPreviousQuery("prevQuery2")
                .setFinalQuery("query")
                .setResultRankInBlock(1)
                .setResultRankGlobal(3)
                .setTimeStayOnResultMillis(65536);

        TakenAction takenAction1 = builder.build();

        builder.setName("newName")
                .setPreviousQueries(null)
                .addPreviousQuery("prevQuery3")
                .setFinalQuery("queryTwo")
                .setResultRankInBlock(5)
                .setResultRankGlobal(22)
                .build();

        TakenAction takenAction2 = builder.build();

        // Check that takenAction1 wasn't altered.
        assertThat(takenAction1.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction1.getId()).isEqualTo("id");
        assertThat(takenAction1.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(takenAction1.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(takenAction1.getName()).isEqualTo("name");
        assertThat(takenAction1.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");

        assertThat(takenAction1.getPreviousQueries()).hasSize(2);
        assertThat(takenAction1.getPreviousQueries().get(0)).isEqualTo("prevQuery1");
        assertThat(takenAction1.getPreviousQueries().get(1)).isEqualTo("prevQuery2");

        assertThat(takenAction1.getFinalQuery()).isEqualTo("query");
        assertThat(takenAction1.getResultRankInBlock()).isEqualTo(1);
        assertThat(takenAction1.getResultRankGlobal()).isEqualTo(3);
        assertThat(takenAction1.getTimeStayOnResultMillis()).isEqualTo(65536);

        // Check that takenAction2 has the new values.
        assertThat(takenAction2.getNamespace()).isEqualTo("namespace");
        assertThat(takenAction2.getId()).isEqualTo("id");
        assertThat(takenAction2.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(takenAction2.getDocumentTtlMillis()).isEqualTo(456);
        assertThat(takenAction2.getName()).isEqualTo("newName");
        assertThat(takenAction2.getReferencedQualifiedId()).isEqualTo("pkg$db/ns#refId");

        assertThat(takenAction2.getPreviousQueries()).hasSize(1);
        assertThat(takenAction2.getPreviousQueries().get(0)).isEqualTo("prevQuery3");

        assertThat(takenAction2.getFinalQuery()).isEqualTo("queryTwo");
        assertThat(takenAction2.getResultRankInBlock()).isEqualTo(5);
        assertThat(takenAction2.getResultRankGlobal()).isEqualTo(22);
        assertThat(takenAction2.getTimeStayOnResultMillis()).isEqualTo(65536);
    }

    @Test
    public void testToGenericDocument() throws Exception {
        TakenAction takenAction = new TakenAction.Builder("namespace", "id")
                .setCreationTimestampMillis(123)
                .setDocumentTtlMillis(456)
                .setName("name")
                .setReferencedQualifiedId("pkg$db/ns#refId")
                .addPreviousQuery("prevQuery1")
                .addPreviousQuery("prevQuery2")
                .setFinalQuery("query")
                .setResultRankInBlock(1)
                .setResultRankGlobal(3)
                .setTimeStayOnResultMillis(65536)
                .build();

        GenericDocument document = GenericDocument.fromDocumentClass(takenAction);
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("id");
        assertThat(document.getCreationTimestampMillis()).isEqualTo(123);
        assertThat(document.getTtlMillis()).isEqualTo(456);
        assertThat(document.getPropertyString("name")).isEqualTo("name");
        assertThat(document.getPropertyString("referencedQualifiedId"))
                .isEqualTo("pkg$db/ns#refId");
        assertThat(document.getPropertyString("previousQueries[0]")).isEqualTo("prevQuery1");
        assertThat(document.getPropertyString("previousQueries[1]")).isEqualTo("prevQuery2");
        assertThat(document.getPropertyString("finalQuery")).isEqualTo("query");
        assertThat(document.getPropertyLong("resultRankInBlock")).isEqualTo(1);
        assertThat(document.getPropertyLong("resultRankGlobal")).isEqualTo(3);
        assertThat(document.getPropertyLong("timeStayOnResultMillis")).isEqualTo(65536);
    }
}
