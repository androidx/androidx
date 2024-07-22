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

package androidx.appsearch.localstorage.usagereporting;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.usagereporting.ActionConstants;
import androidx.appsearch.usagereporting.ClickAction;

import org.junit.Test;

public class ClickActionGenericDocumentTest {
    @Test
    public void testBuild() {
        ClickActionGenericDocument clickActionGenericDocument =
                new ClickActionGenericDocument.Builder("namespace", "click", "builtin:ClickAction")
                        .setCreationTimestampMillis(1000)
                        .setQuery("body")
                        .setResultRankInBlock(12)
                        .setResultRankGlobal(34)
                        .setTimeStayOnResultMillis(2000)
                        .build();

        assertThat(clickActionGenericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(clickActionGenericDocument.getId()).isEqualTo("click");
        assertThat(clickActionGenericDocument.getSchemaType()).isEqualTo("builtin:ClickAction");
        assertThat(clickActionGenericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(clickActionGenericDocument.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(clickActionGenericDocument.getQuery()).isEqualTo("body");
        assertThat(clickActionGenericDocument.getResultRankInBlock()).isEqualTo(12);
        assertThat(clickActionGenericDocument.getResultRankGlobal()).isEqualTo(34);
        assertThat(clickActionGenericDocument.getTimeStayOnResultMillis()).isEqualTo(2000);
    }

    @Test
    public void testBuild_fromGenericDocument() {
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "click", "builtin:ClickAction")
                        .setCreationTimestampMillis(1000)
                        .setPropertyLong("actionType", ActionConstants.ACTION_TYPE_CLICK)
                        .setPropertyString("query", "body")
                        .setPropertyLong("resultRankInBlock", 12)
                        .setPropertyLong("resultRankGlobal", 34)
                        .setPropertyLong("timeStayOnResultMillis", 2000)
                        .build();
        ClickActionGenericDocument clickActionGenericDocument =
                new ClickActionGenericDocument.Builder(document).build();

        assertThat(clickActionGenericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(clickActionGenericDocument.getId()).isEqualTo("click");
        assertThat(clickActionGenericDocument.getSchemaType()).isEqualTo("builtin:ClickAction");
        assertThat(clickActionGenericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(clickActionGenericDocument.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(clickActionGenericDocument.getQuery()).isEqualTo("body");
        assertThat(clickActionGenericDocument.getResultRankInBlock()).isEqualTo(12);
        assertThat(clickActionGenericDocument.getResultRankGlobal()).isEqualTo(34);
        assertThat(clickActionGenericDocument.getTimeStayOnResultMillis()).isEqualTo(2000);
    }

// @exportToFramework:startStrip()
    @Test
    public void testBuild_fromDocumentClass() throws Exception {
        ClickAction clickAction =
                new ClickAction.Builder("namespace", "click", /* actionTimestampMillis= */1000)
                        .setQuery("body")
                        .setReferencedQualifiedId("pkg$db/ns#doc")
                        .setResultRankInBlock(12)
                        .setResultRankGlobal(34)
                        .setTimeStayOnResultMillis(2000)
                        .build();
        ClickActionGenericDocument clickActionGenericDocument =
                new ClickActionGenericDocument.Builder(
                        GenericDocument.fromDocumentClass(clickAction)).build();

        assertThat(clickActionGenericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(clickActionGenericDocument.getId()).isEqualTo("click");
        assertThat(clickActionGenericDocument.getSchemaType()).isEqualTo("builtin:ClickAction");
        assertThat(clickActionGenericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(clickActionGenericDocument.getActionType())
                .isEqualTo(ActionConstants.ACTION_TYPE_CLICK);
        assertThat(clickActionGenericDocument.getQuery()).isEqualTo("body");
        assertThat(clickActionGenericDocument.getResultRankInBlock()).isEqualTo(12);
        assertThat(clickActionGenericDocument.getResultRankGlobal()).isEqualTo(34);
        assertThat(clickActionGenericDocument.getTimeStayOnResultMillis()).isEqualTo(2000);
    }
// @exportToFramework:endStrip()

    @Test
    public void testBuild_invalidActionTypeThrowsException() {
        GenericDocument documentWithoutActionType =
                new GenericDocument.Builder<>("namespace", "search", "builtin:ClickAction")
                        .build();
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> new ClickActionGenericDocument.Builder(documentWithoutActionType));
        assertThat(e1.getMessage())
                .isEqualTo("Invalid action type for ClickActionGenericDocument");

        GenericDocument documentWithUnknownActionType =
                new GenericDocument.Builder<>("namespace", "search", "builtin:ClickAction")
                        .setPropertyLong("actionType", ActionConstants.ACTION_TYPE_UNKNOWN)
                        .build();
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> new ClickActionGenericDocument.Builder(documentWithUnknownActionType));
        assertThat(e2.getMessage())
                .isEqualTo("Invalid action type for ClickActionGenericDocument");

        GenericDocument documentWithIncorrectActionType =
                new GenericDocument.Builder<>("namespace", "search", "builtin:SearchAction")
                        .setPropertyLong("actionType", ActionConstants.ACTION_TYPE_SEARCH)
                        .build();
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                () -> new ClickActionGenericDocument.Builder(documentWithIncorrectActionType));
        assertThat(e3.getMessage())
                .isEqualTo("Invalid action type for ClickActionGenericDocument");
    }
}
