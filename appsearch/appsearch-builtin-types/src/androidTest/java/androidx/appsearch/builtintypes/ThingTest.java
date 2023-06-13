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

package androidx.appsearch.builtintypes;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ThingTest {
    @Test
    public void testBuilder() {
        long now = System.currentTimeMillis();
        Thing thing = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .addPotentialAction(new PotentialAction.Builder()
                        .setName("Start Action")
                        .setDescription("Starts the thing")
                        .setUri("package://start")
                        .build())
                .addPotentialAction(new PotentialAction.Builder()
                        .setName("Stop Action")
                        .setDescription("Stops the thing")
                        .setUri("package://stop")
                        .build())
                .build();

        assertThat(thing.getNamespace()).isEqualTo("namespace");
        assertThat(thing.getId()).isEqualTo("thing1");
        assertThat(thing.getDocumentScore()).isEqualTo(1);
        assertThat(thing.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing.getName()).isEqualTo("my first thing");
        assertThat(thing.getAlternateNames()).isNotNull();
        assertThat(thing.getAlternateNames())
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(thing.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing.getImage()).isEqualTo("content://images/thing1");
        assertThat(thing.getUrl()).isEqualTo("content://things/1");
        assertThat(thing.getPotentialActions()).hasSize(2);

        PotentialAction startAction = thing.getPotentialActions().get(0);
        assertThat(startAction.getName()).isEqualTo("Start Action");
        assertThat(startAction.getDescription()).isEqualTo("Starts the thing");
        assertThat(startAction.getUri()).isEqualTo("package://start");

        PotentialAction stopAction = thing.getPotentialActions().get(1);
        assertThat(stopAction.getName()).isEqualTo("Stop Action");
        assertThat(stopAction.getDescription()).isEqualTo("Stops the thing");
        assertThat(stopAction.getUri()).isEqualTo("package://stop");
    }

    @Test
    public void testBuilderCopy_allFieldsAreCopied() {
        long now = System.currentTimeMillis();
        Thing thing1 = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .addPotentialAction(new PotentialAction.Builder()
                        .setName("Stop Action")
                        .setDescription("Stops the thing")
                        .setUri("package://stop")
                        .build())
                .build();
        Thing thing2 = new Thing.Builder(thing1).build();

        assertThat(thing2.getNamespace()).isEqualTo("namespace");
        assertThat(thing2.getId()).isEqualTo("thing1");
        assertThat(thing2.getDocumentScore()).isEqualTo(1);
        assertThat(thing2.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing2.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing2.getName()).isEqualTo("my first thing");
        assertThat(thing2.getAlternateNames()).isNotNull();
        assertThat(thing2.getAlternateNames())
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(thing2.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing2.getImage()).isEqualTo("content://images/thing1");
        assertThat(thing2.getUrl()).isEqualTo("content://things/1");
        assertThat(thing2.getPotentialActions()).isNotNull();
        assertThat(thing2.getPotentialActions()).hasSize(1);
        assertThat(thing2.getPotentialActions().get(0).getName()).isEqualTo("Stop Action");
        assertThat(thing2.getPotentialActions().get(0).getDescription())
                .isEqualTo("Stops the thing");
        assertThat(thing2.getPotentialActions().get(0).getUri()).isEqualTo("package://stop");
    }

    @Test
    public void testBuilderCopy_copiedFieldsCanBeUpdated() {
        long now = System.currentTimeMillis();
        Thing thing1 = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .addPotentialAction(new PotentialAction.Builder()
                        .setDescription("View this thing")
                        .setUri("package://view")
                        .build())
                .addPotentialAction(new PotentialAction.Builder()
                        .setDescription("Edit this thing")
                        .setUri("package://edit")
                        .build())
                .build();
        Thing thing2 = new Thing.Builder(thing1)
                .clearAlternateNames()
                .setImage("content://images/thing2")
                .setUrl("content://things/2")
                .clearPotentialActions()
                .addPotentialAction(new PotentialAction.Builder()
                        .setName("DeleteAction")
                        .setDescription("Delete this thing")
                        .setUri("package://delete")
                        .build())
                .build();

        assertThat(thing2.getNamespace()).isEqualTo("namespace");
        assertThat(thing2.getId()).isEqualTo("thing1");
        assertThat(thing2.getDocumentScore()).isEqualTo(1);
        assertThat(thing2.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing2.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing2.getName()).isEqualTo("my first thing");
        assertThat(thing2.getAlternateNames()).isEmpty();
        assertThat(thing2.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing2.getImage()).isEqualTo("content://images/thing2");
        assertThat(thing2.getUrl()).isEqualTo("content://things/2");

        List<PotentialAction> potentialActions = thing2.getPotentialActions();
        assertThat(potentialActions).hasSize(1);
        assertThat(potentialActions.get(0).getName()).isEqualTo("DeleteAction");
        assertThat(potentialActions.get(0).getDescription()).isEqualTo("Delete this thing");
        assertThat(potentialActions.get(0).getUri()).isEqualTo("package://delete");
    }

    @Test
    public void testBuilderCopy_builderReuse() {
        long now = System.currentTimeMillis();
        Thing.Builder builder = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .addPotentialAction(new PotentialAction.Builder()
                        .setDescription("View this thing")
                        .setUri("package://view")
                        .build())
                .addPotentialAction(new PotentialAction.Builder()
                        .setDescription("Edit this thing")
                        .setUri("package://edit")
                        .build());

        Thing thing1 = builder.build();

        builder.clearAlternateNames()
                .setImage("content://images/thing2")
                .setUrl("content://things/2")
                .clearPotentialActions()
                .addPotentialAction(new PotentialAction.Builder()
                        .setName("DeleteAction")
                        .setDescription("Delete this thing")
                        .setUri("package://delete")
                        .build());

        Thing thing2 = builder.build();

        // Check that thing1 wasn't altered
        assertThat(thing1.getNamespace()).isEqualTo("namespace");
        assertThat(thing1.getId()).isEqualTo("thing1");
        assertThat(thing1.getDocumentScore()).isEqualTo(1);
        assertThat(thing1.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing1.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing1.getName()).isEqualTo("my first thing");
        assertThat(thing1.getAlternateNames())
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(thing1.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing1.getImage()).isEqualTo("content://images/thing1");
        assertThat(thing1.getUrl()).isEqualTo("content://things/1");

        List<PotentialAction> actions1 = thing1.getPotentialActions();
        assertThat(actions1).hasSize(2);
        assertThat(actions1.get(0).getDescription()).isEqualTo("View this thing");
        assertThat(actions1.get(0).getUri()).isEqualTo("package://view");
        assertThat(actions1.get(1).getDescription()).isEqualTo("Edit this thing");
        assertThat(actions1.get(1).getUri()).isEqualTo("package://edit");

        // Check that thing2 has the new values
        assertThat(thing2.getNamespace()).isEqualTo("namespace");
        assertThat(thing2.getId()).isEqualTo("thing1");
        assertThat(thing2.getDocumentScore()).isEqualTo(1);
        assertThat(thing2.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(thing2.getDocumentTtlMillis()).isEqualTo(30000);
        assertThat(thing2.getName()).isEqualTo("my first thing");
        assertThat(thing2.getAlternateNames()).isEmpty();
        assertThat(thing2.getDescription()).isEqualTo("this is my first schema.org object");
        assertThat(thing2.getImage()).isEqualTo("content://images/thing2");
        assertThat(thing2.getUrl()).isEqualTo("content://things/2");

        List<PotentialAction> actions2 = thing2.getPotentialActions();
        assertThat(actions2).hasSize(1);
        assertThat(actions2.get(0).getName()).isEqualTo("DeleteAction");
        assertThat(actions2.get(0).getDescription()).isEqualTo("Delete this thing");
        assertThat(actions2.get(0).getUri()).isEqualTo("package://delete");
    }


    @Test
    public void testToGenericDocument() throws Exception {
        long now = System.currentTimeMillis();
        PotentialAction potentialAction = new PotentialAction.Builder()
                .setDescription("Make a phone call")
                .setName("actions.intent.CALL")
                .setUri("package://call")
                .build();

        Thing thing = new Thing.Builder("namespace", "thing1")
                .setDocumentScore(1)
                .setCreationTimestampMillis(now)
                .setDocumentTtlMillis(30000)
                .setName("my first thing")
                .addAlternateName("my first object")
                .addAlternateName("माझी पहिली गोष्ट")
                .setDescription("this is my first schema.org object")
                .setImage("content://images/thing1")
                .setUrl("content://things/1")
                .addPotentialAction(potentialAction)
                .build();

        GenericDocument document = GenericDocument.fromDocumentClass(thing);
        assertThat(document.getSchemaType()).isEqualTo("builtin:Thing");
        assertThat(document.getNamespace()).isEqualTo("namespace");
        assertThat(document.getId()).isEqualTo("thing1");
        assertThat(document.getScore()).isEqualTo(1);
        assertThat(document.getCreationTimestampMillis()).isEqualTo(now);
        assertThat(document.getTtlMillis()).isEqualTo(30000);
        assertThat(document.getPropertyString("name")).isEqualTo("my first thing");
        assertThat(document.getPropertyStringArray("alternateNames")).isNotNull();
        assertThat(Arrays.asList(document.getPropertyStringArray("alternateNames")))
                .containsExactly("my first object", "माझी पहिली गोष्ट");
        assertThat(document.getPropertyString("description"))
                .isEqualTo("this is my first schema.org object");
        assertThat(document.getPropertyString("image")).isEqualTo("content://images/thing1");
        assertThat(document.getPropertyString("url")).isEqualTo("content://things/1");

        assertThat(document.getPropertyString("potentialActions[0].name"))
                .isEqualTo("actions.intent.CALL");
        assertThat(document.getPropertyString("potentialActions[0].description"))
                .isEqualTo("Make a phone call");
        assertThat(document.getPropertyString("potentialActions[0].uri"))
                .isEqualTo("package://call");
    }
}
