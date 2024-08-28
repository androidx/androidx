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

package androidx.car.app.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.navigation.model.NavigationTemplate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TabContents}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TabContentsTest {

    @Test
    public void createInstance_nullTemplate_Throws() {
        assertThrows(
                NullPointerException.class,
                () -> new TabContents.Builder(null).build());
    }

    @Test
    public void createInstance_invalidTemplate_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new TabContents.Builder(new MapWithContentTemplate.Builder()
                        .setContentTemplate(new ListTemplate.Builder().build())
                        .build()).build());
    }

    @Test
    public void createInstance_listTemplate() {
        ListTemplate listTemplate = new ListTemplate.Builder()
                .setSingleList(
                        new ItemList.Builder().addItem(
                                        new Row.Builder()
                                                .setTitle("Row").addText("text1").build())
                                .build())
                .build();
        TabContents tabContents = new TabContents.Builder(listTemplate).build();

        assertEquals(listTemplate, tabContents.getTemplate());
    }

    @Test
    public void createInstance_messageTemplate() {
        MessageTemplate template = new MessageTemplate.Builder("title")
                .addAction(
                        new Action.Builder()
                                .setTitle("Click")
                                .build())
                .build();
        TabContents tabContents = new TabContents.Builder(template).build();

        assertEquals(template, tabContents.getTemplate());
    }

    @Test
    public void createInstance_navigationTemplate() {
        NavigationTemplate template =
                new NavigationTemplate.Builder().setActionStrip(new ActionStrip.Builder().addAction(
                        new Action.Builder().setTitle("test").build()).build()).build();

        TabContents tabContents = new TabContents.Builder(template).build();

        assertEquals(template, tabContents.getTemplate());
    }

    @Test
    public void createInstance_sectionedItemTemplate_Throws() {
        SectionedItemTemplate template =
                new SectionedItemTemplate.Builder().setHeader(
                        new Header.Builder().setTitle("title").build()
                ).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new TabContents.Builder(template).build());
    }

    @Test
    public void createInstance_api8_sectionedItemTemplate() {
        SectionedItemTemplate template =
                new SectionedItemTemplate.Builder().setHeader(
                        new Header.Builder().setTitle("title").build()
                ).build();

        TabContents tabContents = new TabContents.Builder(template, /* enableApi8= */ true).build();

        assertEquals(template, tabContents.getTemplate());
    }

    @Test
    public void equals() {
        MessageTemplate template = new MessageTemplate.Builder("title")
                .addAction(
                        new Action.Builder()
                                .setTitle("Click")
                                .build())
                .build();
        TabContents contents1 = new TabContents.Builder(template).build();
        TabContents contents2 = new TabContents.Builder(template).build();

        assertEquals(contents1, contents2);
    }

    @Test
    public void notEquals_differentTemplate() {
        MessageTemplate template1 = new MessageTemplate.Builder("title1")
                .addAction(
                        new Action.Builder()
                                .setTitle("Click1")
                                .build())
                .build();
        MessageTemplate template2 = new MessageTemplate.Builder("title2")
                .addAction(
                        new Action.Builder()
                                .setTitle("Click2")
                                .build())
                .build();
        TabContents contents1 = new TabContents.Builder(template1).build();
        TabContents contents2 = new TabContents.Builder(template2).build();

        assertNotEquals(contents1, contents2);
    }

    @Test
    public void notEquals_differentTemplateType() {
        MessageTemplate template1 = new MessageTemplate.Builder("title")
                .addAction(
                        new Action.Builder()
                                .setTitle("Click")
                                .build())
                .build();
        PaneTemplate template2 = new PaneTemplate.Builder(
                new Pane.Builder()
                        .addRow(new Row.Builder().setTitle("title").build())
                        .build())
                .build();
        TabContents contents1 = new TabContents.Builder(template1).build();
        TabContents contents2 = new TabContents.Builder(template2).build();

        assertNotEquals(contents1, contents2);
    }
}
