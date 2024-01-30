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

package androidx.appsearch.builtintypes;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

public class PotentialActionTest {
    @Test
    public void testBuilder() {
        PotentialAction potentialAction = new PotentialAction.Builder()
                .setName("actions.intent.CREATE_CALL")
                .setDescription("Call John")
                .build();

        assertThat(potentialAction.getName()).isEqualTo("actions.intent.CREATE_CALL");
        assertThat(potentialAction.getDescription()).isEqualTo("Call John");
    }

    @Test
    public void testBuilderCopy_returnsActionWithAllFieldsCopied() {
        PotentialAction potentialAction1 = new PotentialAction.Builder()
                .setName("actions.intent.CREATE_CALL")
                .setDescription("Call John")
                .build();

        PotentialAction potentialAction2 = new PotentialAction.Builder(potentialAction1).build();
        assertThat(potentialAction1.getName()).isEqualTo(potentialAction2.getName());
        assertThat(potentialAction1.getDescription()).isEqualTo(potentialAction2.getDescription());
        assertThat(potentialAction1.getUri()).isEqualTo(potentialAction2.getUri());
    }

    @Test
    public void testActionToGenericDocument() throws Exception {
        PotentialAction potentialAction = new PotentialAction.Builder()
                .setName("actions.intent.CREATE_CALL")
                .setDescription("Call John")
                .setUri("tel:555-123-4567")
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(potentialAction);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:PotentialAction");
        assertThat(genericDocument.getPropertyString("name"))
                .isEqualTo("actions.intent.CREATE_CALL");
        assertThat(genericDocument.getPropertyString("description"))
                .isEqualTo("Call John");
        assertThat(genericDocument.getPropertyString("uri"))
                .isEqualTo("tel:555-123-4567");
    }
}
