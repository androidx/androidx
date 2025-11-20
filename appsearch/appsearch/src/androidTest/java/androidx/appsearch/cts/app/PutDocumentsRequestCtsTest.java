/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;

import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.usagereporting.ClickAction;
import androidx.appsearch.usagereporting.DismissAction;
import androidx.appsearch.usagereporting.ImpressionAction;
import androidx.appsearch.usagereporting.SearchAction;
import androidx.appsearch.usagereporting.TakenAction;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Set;

public class PutDocumentsRequestCtsTest {

    @Test
    public void addGenericDocument_byCollection() {
        Set<AppSearchEmail> emails =
                ImmutableSet.of(new AppSearchEmail.Builder("namespace", "test1").build(),
                        new AppSearchEmail.Builder("namespace", "test2").build());
        PutDocumentsRequest request = new PutDocumentsRequest.Builder().addGenericDocuments(emails)
                .build();

        assertThat(request.getGenericDocuments().get(0).getId()).isEqualTo("test1");
        assertThat(request.getGenericDocuments().get(1).getId()).isEqualTo("test2");
    }

    @Test
    public void duplicateIdForNormalAndTakenActionGenericDocumentThrowsException()
            throws Exception {
        GenericDocument normalDocument = new GenericDocument.Builder<>(
                "namespace", "id", "builtin:Thing").build();
        GenericDocument takenActionGenericDocument = new GenericDocument.Builder<>(
                "namespace", "id", "builtin:ClickAction").build();

        PutDocumentsRequest.Builder builder = new PutDocumentsRequest.Builder()
                .addGenericDocuments(normalDocument)
                .addTakenActionGenericDocuments(takenActionGenericDocument);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.build());
        assertThat(e)
            .hasMessageThat()
            .isEqualTo(
                "Document id "
                    + takenActionGenericDocument.getId()
                    + " cannot exist in both taken action and normal document");
    }

    @Test
    public void addTakenActionGenericDocuments() throws Exception {
        GenericDocument searchActionGenericDocument1 = new GenericDocument.Builder<>(
                "namespace", "search1", "builtin:SearchAction").build();
        GenericDocument clickActionGenericDocument1 = new GenericDocument.Builder<>(
                "namespace", "click1", "builtin:ClickAction").build();
        GenericDocument clickActionGenericDocument2 = new GenericDocument.Builder<>(
                "namespace", "click2", "builtin:ClickAction").build();
        GenericDocument impressionActionGenericDocument1 = new GenericDocument.Builder<>(
                "namespace", "impression1", "builtin:ImpressionAction").build();
        GenericDocument dismissActionGenericDocument1 = new GenericDocument.Builder<>(
                "namespace", "dismiss1", "builtin:DismissAction").build();
        GenericDocument searchActionGenericDocument2 = new GenericDocument.Builder<>(
                "namespace", "search2", "builtin:SearchAction").build();
        GenericDocument clickActionGenericDocument3 = new GenericDocument.Builder<>(
                "namespace", "click3", "builtin:ClickAction").build();
        GenericDocument clickActionGenericDocument4 = new GenericDocument.Builder<>(
                "namespace", "click4", "builtin:ClickAction").build();
        GenericDocument clickActionGenericDocument5 = new GenericDocument.Builder<>(
                "namespace", "click5", "builtin:ClickAction").build();
        GenericDocument impressionActionGenericDocument2 = new GenericDocument.Builder<>(
                "namespace", "impression2", "builtin:ImpressionAction").build();
        GenericDocument dismissActionGenericDocument2 = new GenericDocument.Builder<>(
                "namespace", "dismiss2", "builtin:DismissAction").build();

        PutDocumentsRequest request = new PutDocumentsRequest.Builder()
                .addTakenActionGenericDocuments(
                        searchActionGenericDocument1, clickActionGenericDocument1,
                        clickActionGenericDocument2, impressionActionGenericDocument1,
                        dismissActionGenericDocument1, searchActionGenericDocument2,
                        clickActionGenericDocument3, clickActionGenericDocument4,
                        clickActionGenericDocument5, impressionActionGenericDocument2,
                        dismissActionGenericDocument2)
                .build();

        // Generic documents should contain nothing.
        assertThat(request.getGenericDocuments()).isEmpty();

        // Taken action generic documents should contain correct taken action generic documents.
        assertThat(request.getTakenActionGenericDocuments()).hasSize(11);
        assertThat(request.getTakenActionGenericDocuments().get(0).getId()).isEqualTo("search1");
        assertThat(request.getTakenActionGenericDocuments().get(1).getId()).isEqualTo("click1");
        assertThat(request.getTakenActionGenericDocuments().get(2).getId()).isEqualTo("click2");
        assertThat(request.getTakenActionGenericDocuments().get(3).getId())
                .isEqualTo("impression1");
        assertThat(request.getTakenActionGenericDocuments().get(4).getId()).isEqualTo("dismiss1");
        assertThat(request.getTakenActionGenericDocuments().get(5).getId()).isEqualTo("search2");
        assertThat(request.getTakenActionGenericDocuments().get(6).getId()).isEqualTo("click3");
        assertThat(request.getTakenActionGenericDocuments().get(7).getId()).isEqualTo("click4");
        assertThat(request.getTakenActionGenericDocuments().get(8).getId()).isEqualTo("click5");
        assertThat(request.getTakenActionGenericDocuments().get(9).getId())
                .isEqualTo("impression2");
        assertThat(request.getTakenActionGenericDocuments().get(10).getId()).isEqualTo("dismiss2");
    }

    @Test
    public void addTakenActionGenericDocuments_byCollection() throws Exception {
        Set<GenericDocument> takenActionGenericDocuments = ImmutableSet.of(
                new GenericDocument.Builder<>(
                        "namespace", "search1", "builtin:SearchAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "click1", "builtin:ClickAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "click2", "builtin:ClickAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "impression1", "builtin:ImpressionAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "dismiss1", "builtin:DismissAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "search2", "builtin:SearchAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "click3", "builtin:ClickAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "click4", "builtin:ClickAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "click5", "builtin:ClickAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "impression2", "builtin:ImpressionAction").build(),
                new GenericDocument.Builder<>(
                        "namespace", "dismiss2", "builtin:DismissAction").build());

        PutDocumentsRequest request = new PutDocumentsRequest.Builder()
                .addTakenActionGenericDocuments(takenActionGenericDocuments)
                .build();

        // Generic documents should contain nothing.
        assertThat(request.getGenericDocuments()).isEmpty();

        // Taken action generic documents should contain correct taken action generic documents.
        assertThat(request.getTakenActionGenericDocuments()).hasSize(11);
        assertThat(request.getTakenActionGenericDocuments().get(0).getId()).isEqualTo("search1");
        assertThat(request.getTakenActionGenericDocuments().get(1).getId()).isEqualTo("click1");
        assertThat(request.getTakenActionGenericDocuments().get(2).getId()).isEqualTo("click2");
        assertThat(request.getTakenActionGenericDocuments().get(3).getId())
                .isEqualTo("impression1");
        assertThat(request.getTakenActionGenericDocuments().get(4).getId()).isEqualTo("dismiss1");
        assertThat(request.getTakenActionGenericDocuments().get(5).getId()).isEqualTo("search2");
        assertThat(request.getTakenActionGenericDocuments().get(6).getId()).isEqualTo("click3");
        assertThat(request.getTakenActionGenericDocuments().get(7).getId()).isEqualTo("click4");
        assertThat(request.getTakenActionGenericDocuments().get(8).getId()).isEqualTo("click5");
        assertThat(request.getTakenActionGenericDocuments().get(9).getId())
                .isEqualTo("impression2");
        assertThat(request.getTakenActionGenericDocuments().get(10).getId()).isEqualTo("dismiss2");
    }

    // @exportToFramework:startStrip()
    @Document
    static class Card {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
        String mString;

        Card(String namespace, String id, String string) {
            mId = id;
            mNamespace = namespace;
            mString = string;
        }
    }

    @Test
    public void addDocumentClasses_byCollection() throws Exception {
        // A schema with Card must be set in order to be able to add a Card instance to
        // PutDocumentsRequest.
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession session = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, /*databaseName=*/ "")
                        .build()
        ).get();
        session.setSchemaAsync(
                new SetSchemaRequest.Builder().addDocumentClasses(Card.class).build()).get();

        Set<Card> cards = ImmutableSet.of(new Card("cardNamespace", "cardId", "cardProperty"));
        PutDocumentsRequest request = new PutDocumentsRequest.Builder().addDocuments(cards)
                .build();

        assertThat(request.getGenericDocuments().get(0).getId()).isEqualTo("cardId");
    }

    @Test
    public void addTakenActions() throws Exception {
        SearchAction searchAction1 =
                new SearchAction.Builder("namespace", "search1", /* actionTimestampMillis= */1000)
                        .build();
        ClickAction clickAction1 =
                new ClickAction.Builder("namespace", "click1", /* actionTimestampMillis= */2000)
                        .build();
        ClickAction clickAction2 =
                new ClickAction.Builder("namespace", "click2", /* actionTimestampMillis= */3000)
                        .build();
        ImpressionAction impressionAction1 =
                new ImpressionAction.Builder(
                        "namespace", "impression1", /* actionTimestampMillis= */4000)
                        .build();
        DismissAction dismissAction1 =
                new DismissAction.Builder(
                        "namespace", "dismiss1", /* actionTimestampMillis= */4000)
                        .build();
        SearchAction searchAction2 =
                new SearchAction.Builder("namespace", "search2", /* actionTimestampMillis= */5000)
                        .build();
        ClickAction clickAction3 =
                new ClickAction.Builder("namespace", "click3", /* actionTimestampMillis= */6000)
                        .build();
        ClickAction clickAction4 =
                new ClickAction.Builder("namespace", "click4", /* actionTimestampMillis= */7000)
                        .build();
        ClickAction clickAction5 =
                new ClickAction.Builder("namespace", "click5", /* actionTimestampMillis= */8000)
                        .build();
        ImpressionAction impressionAction2 =
                new ImpressionAction.Builder(
                        "namespace", "impression2", /* actionTimestampMillis= */9000)
                        .build();
        DismissAction dismissAction2 =
                new DismissAction.Builder(
                        "namespace", "dismiss2", /* actionTimestampMillis= */4000)
                        .build();

        PutDocumentsRequest request = new PutDocumentsRequest.Builder()
                .addTakenActions(searchAction1, clickAction1, clickAction2, impressionAction1,
                        dismissAction1, searchAction2, clickAction3, clickAction4, clickAction5,
                        impressionAction2, dismissAction2)
                .build();

        // Generic documents should contain nothing.
        assertThat(request.getGenericDocuments()).isEmpty();

        // Taken action generic documents should contain correct taken action generic documents.
        assertThat(request.getTakenActionGenericDocuments()).hasSize(11);
        assertThat(request.getTakenActionGenericDocuments().get(0).getId()).isEqualTo("search1");
        assertThat(request.getTakenActionGenericDocuments().get(1).getId()).isEqualTo("click1");
        assertThat(request.getTakenActionGenericDocuments().get(2).getId()).isEqualTo("click2");
        assertThat(request.getTakenActionGenericDocuments().get(3).getId())
                .isEqualTo("impression1");
        assertThat(request.getTakenActionGenericDocuments().get(4).getId()).isEqualTo("dismiss1");
        assertThat(request.getTakenActionGenericDocuments().get(5).getId()).isEqualTo("search2");
        assertThat(request.getTakenActionGenericDocuments().get(6).getId()).isEqualTo("click3");
        assertThat(request.getTakenActionGenericDocuments().get(7).getId()).isEqualTo("click4");
        assertThat(request.getTakenActionGenericDocuments().get(8).getId()).isEqualTo("click5");
        assertThat(request.getTakenActionGenericDocuments().get(9).getId())
                .isEqualTo("impression2");
        assertThat(request.getTakenActionGenericDocuments().get(10).getId()).isEqualTo("dismiss2");
    }

    @Test
    public void addTakenActions_byCollection() throws Exception {
        Set<TakenAction> takenActions = ImmutableSet.of(
                new SearchAction.Builder("namespace", "search1", /* actionTimestampMillis= */1000)
                        .build(),
                new ClickAction.Builder("namespace", "click1", /* actionTimestampMillis= */2000)
                        .build(),
                new ClickAction.Builder("namespace", "click2", /* actionTimestampMillis= */3000)
                        .build(),
                new ImpressionAction.Builder(
                        "namespace", "impression1", /* actionTimestampMillis= */4000)
                        .build(),
                new DismissAction.Builder(
                        "namespace", "dismiss1", /* actionTimestampMillis= */4000)
                        .build(),
                new SearchAction.Builder("namespace", "search2", /* actionTimestampMillis= */5000)
                        .build(),
                new ClickAction.Builder("namespace", "click3", /* actionTimestampMillis= */6000)
                        .build(),
                new ClickAction.Builder("namespace", "click4", /* actionTimestampMillis= */7000)
                        .build(),
                new ClickAction.Builder("namespace", "click5", /* actionTimestampMillis= */8000)
                        .build(),
                new ImpressionAction.Builder(
                        "namespace", "impression2", /* actionTimestampMillis= */9000)
                        .build(),
                new DismissAction.Builder(
                        "namespace", "dismiss2", /* actionTimestampMillis= */4000)
                        .build());

        PutDocumentsRequest request = new PutDocumentsRequest.Builder()
                .addTakenActions(takenActions)
                .build();

        // Generic documents should contain nothing.
        assertThat(request.getGenericDocuments()).isEmpty();

        // Taken action generic documents should contain correct taken action generic documents.
        assertThat(request.getTakenActionGenericDocuments()).hasSize(11);
        assertThat(request.getTakenActionGenericDocuments().get(0).getId()).isEqualTo("search1");
        assertThat(request.getTakenActionGenericDocuments().get(1).getId()).isEqualTo("click1");
        assertThat(request.getTakenActionGenericDocuments().get(2).getId()).isEqualTo("click2");
        assertThat(request.getTakenActionGenericDocuments().get(3).getId())
                .isEqualTo("impression1");
        assertThat(request.getTakenActionGenericDocuments().get(4).getId()).isEqualTo("dismiss1");
        assertThat(request.getTakenActionGenericDocuments().get(5).getId()).isEqualTo("search2");
        assertThat(request.getTakenActionGenericDocuments().get(6).getId()).isEqualTo("click3");
        assertThat(request.getTakenActionGenericDocuments().get(7).getId()).isEqualTo("click4");
        assertThat(request.getTakenActionGenericDocuments().get(8).getId()).isEqualTo("click5");
        assertThat(request.getTakenActionGenericDocuments().get(9).getId())
                .isEqualTo("impression2");
        assertThat(request.getTakenActionGenericDocuments().get(10).getId()).isEqualTo("dismiss2");
    }
// @exportToFramework:endStrip()
}
