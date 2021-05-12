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

package androidx.appsearch.app;

import static androidx.appsearch.app.AppSearchSchema.PropertyConfig.INDEXING_TYPE_PREFIXES;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.annotation.AppSearchDocument;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Set;

public class PutDocumentsRequestTest {

    @Test
    public void addGenericDocument_byCollection() {
        Set<AppSearchEmail> emails = ImmutableSet.of(new AppSearchEmail.Builder("test1").build(),
                new AppSearchEmail.Builder("test2").build());
        PutDocumentsRequest request = new PutDocumentsRequest.Builder().addGenericDocument(emails)
                .build();

        assertThat(request.getDocuments().get(0).getUri()).isEqualTo("test1");
        assertThat(request.getDocuments().get(1).getUri()).isEqualTo("test2");
    }

// @exportToFramework:startStrip()
    @AppSearchDocument
    static class Card {
        @AppSearchDocument.Uri
        String mUri;

        @AppSearchDocument.Property(indexingType = INDEXING_TYPE_PREFIXES)
        String mString;

        Card(String mUri, String mString) {
            this.mUri = mUri;
            this.mString = mString;
        }
    }

    @Test
    public void addDataClass_byCollection() throws Exception {
        // A schema with Card must be set in order to be able to add a Card instance to
        // PutDocumentsRequest.
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession session = LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context)
                        .setDatabaseName(LocalStorage.DEFAULT_DATABASE_NAME)
                        .build()
        ).get();
        session.setSchema(new SetSchemaRequest.Builder().addDataClass(Card.class).build()).get();

        Set<Card> cards = ImmutableSet.of(new Card("cardUri", "cardProperty"));
        PutDocumentsRequest request = new PutDocumentsRequest.Builder().addDataClass(cards)
                .build();

        assertThat(request.getDocuments().get(0).getUri()).isEqualTo("cardUri");
    }
// @exportToFramework:endStrip()
}
