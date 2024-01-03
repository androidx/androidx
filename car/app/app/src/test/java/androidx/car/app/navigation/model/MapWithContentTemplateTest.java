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

package androidx.car.app.navigation.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.signin.PinSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link MapWithContentTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class MapWithContentTemplateTest {
    private final ActionStrip mMapActionStrip = new ActionStrip.Builder()
            .addAction(TestUtils.createAction(null,
                    TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                            "ic_test_1")))
            .build();

    private static MessageTemplate createMessageTemplate() {
        return new MessageTemplate.Builder("foo")
                .setTitle("bar")
                .build();
    }

    private static GridTemplate createGridTemplate() {
        ItemList list = TestUtils.getGridItemList(2);
        return new GridTemplate.Builder()
                .setTitle("Title")
                .setSingleList(list)
                .build();
    }

    private static ListTemplate createListTemplate() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        return new ListTemplate.Builder()
                .setTitle("Title")
                .setSingleList(new ItemList.Builder().addItem(row1).build())
                .build();
    }

    private static PaneTemplate createPaneTemplate() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        return new PaneTemplate.Builder(new Pane.Builder()
                .addRow(row1)
                .build())
                .setTitle("Title")
                .setHeaderAction(Action.BACK)
                .build();
    }

    private static SignInTemplate createSignInTemplate() {
        PinSignInMethod pinSignInMethod = new PinSignInMethod("123456789ABC");
        return new SignInTemplate.Builder(pinSignInMethod)
                .setTitle("Title")
                .setHeaderAction(Action.BACK)
                .build();
    }

    @Test
    public void createInstance_noContentTemplate_notLoading_throws() {
        assertThrows(IllegalStateException.class, () -> new MapWithContentTemplate.Builder()
                .build());
    }

    @Test
    public void createInstance_isLoading_hasContentTemplate_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new MapWithContentTemplate.Builder()
                        .setLoading(true)
                        .setContentTemplate(createListTemplate())
                        .build());
    }

    @Test
    public void createInstance_noContentTemplate_loading_doesNotThrow() {
        new MapWithContentTemplate.Builder()
                .setLoading(true)
                .build();
    }

    @Test
    public void createInstance_unsupportedTemplate_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new MapWithContentTemplate.Builder()
                .setContentTemplate(createSignInTemplate())
                .build());
    }

    @Test
    public void createInstance_listTemplate_doesNotThrow() {
        new MapWithContentTemplate.Builder()
                .setContentTemplate(createListTemplate())
                .build();
    }

    @Test
    public void createInstance_paneTemplate_doesNotThrow() {
        new MapWithContentTemplate.Builder()
                .setContentTemplate(createPaneTemplate())
                .build();
    }

    @Test
    public void createInstance_gridTemplate_doesNotThrow() {
        new MapWithContentTemplate.Builder()
            .setContentTemplate(createGridTemplate())
            .build();
    }

    @Test
    public void createInstance_messageTemplate_doesNotThrow() {
        new MapWithContentTemplate.Builder()
            .setContentTemplate(createMessageTemplate())
            .build();
    }

    @Test
    public void equals_withMessage() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapWithContentTemplate template = new MapWithContentTemplate.Builder()
                .setMapController(mapController)
                .setActionStrip(actionStrip)
                .setContentTemplate(createMessageTemplate())
                .build();

        assertThat(template.getContentTemplate()).isEqualTo(createMessageTemplate());
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
        assertThat(template.getMapController().getMapActionStrip()).isEqualTo(mMapActionStrip);
    }

    @Test
    public void equals_withGridItems() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapWithContentTemplate template = new MapWithContentTemplate.Builder()
                .setMapController(mapController)
                .setActionStrip(actionStrip)
                .setContentTemplate(createGridTemplate())
                .build();

        assertThat(template.getContentTemplate()).isEqualTo(createGridTemplate());
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
        assertThat(template.getMapController().getMapActionStrip()).isEqualTo(mMapActionStrip);
    }

    @Test
    public void notEquals_differentActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapWithContentTemplate template = new MapWithContentTemplate.Builder()
                .setMapController(mapController)
                .setActionStrip(actionStrip)
                .setContentTemplate(createGridTemplate())
                .build();

        assertThat(template).isNotEqualTo(new MapWithContentTemplate.Builder()
                .setMapController(mapController)
                .setActionStrip(new ActionStrip.Builder().addAction(Action.APP_ICON).build())
                .setContentTemplate(createGridTemplate())
                .build());
    }

    @Test
    public void notEquals_differentTemplates() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapWithContentTemplate template = new MapWithContentTemplate.Builder()
                .setMapController(mapController)
                .setActionStrip(actionStrip)
                .setContentTemplate(createGridTemplate())
                .build();

        assertThat(template).isNotEqualTo(new MapWithContentTemplate.Builder()
                .setMapController(mapController)
                .setActionStrip(actionStrip)
                .setContentTemplate(createMessageTemplate())
                .build());
    }
}
