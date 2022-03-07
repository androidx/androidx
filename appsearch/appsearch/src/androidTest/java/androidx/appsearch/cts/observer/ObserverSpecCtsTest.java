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

import androidx.appsearch.annotation.Document;
import androidx.appsearch.observer.ObserverSpec;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

public class ObserverSpecCtsTest {
    @Test
    public void testFilterSchemas() {
        ObserverSpec observerSpec = new ObserverSpec.Builder()
                .addFilterSchemas("Schema1", "Schema2")
                .addFilterSchemas(ImmutableSet.of("Schema3", "Schema4"))
                .build();
        assertThat(observerSpec.getFilterSchemas()).containsExactly(
                "Schema1", "Schema2", "Schema3", "Schema4");
    }

// @exportToFramework:startStrip()

    @Document
    public static class King {
        @Document.Namespace String mNamespace;
        @Document.Id String mId;
    }

    @Document
    public static class Queen {
        @Document.Namespace String mNamespace;
        @Document.Id String mId;
    }

    @Document
    public static class Jack {
        @Document.Namespace String mNamespace;
        @Document.Id String mId;
    }

    @Document
    public static class Ace {
        @Document.Namespace String mNamespace;
        @Document.Id String mId;
    }

    @Test
    public void testFilterSchemas_documentClass() throws Exception {
        ObserverSpec observerSpec = new ObserverSpec.Builder()
                .addFilterSchemas("Schema1", "Schema2")
                .addFilterDocumentClasses(King.class, Queen.class)
                .addFilterSchemas(ImmutableSet.of("Schema3", "Schema4"))
                .addFilterDocumentClasses(ImmutableSet.of(Jack.class, Ace.class))
                .build();
        assertThat(observerSpec.getFilterSchemas()).containsExactly(
                "Schema1", "Schema2", "King", "Queen", "Schema3", "Schema4", "Jack", "Ace");
    }

// @exportToFramework:endStrip()
}
