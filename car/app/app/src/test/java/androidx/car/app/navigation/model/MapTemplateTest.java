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

package androidx.car.app.navigation.model;


import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Header;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;
import androidx.car.app.model.Toggle;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link MapTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class MapTemplateTest {
    private static final Header DEFAULT_HEADER = new Header.Builder()
            .setTitle("Title")
            .build();

    private final ActionStrip mMapActionStrip = new ActionStrip.Builder()
            .addAction(TestUtils.createAction(null,
                    TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                            "ic_test_1")))
            .build();

    private static Pane.Builder getPane() {
        Row row1 = new Row.Builder().setTitle("Bananas").build();
        Row row2 = new Row.Builder().setTitle("Oranges").build();
        return new Pane.Builder().addRow(row1).addRow(row2);
    }

    @Test
    public void createInstance_noPane_throws() {
        assertThrows(IllegalStateException.class, () -> new MapTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .build());
    }

    @Test
    public void createInstance_noHeader_throws() {
        Pane pane = TestUtils.createPane(2, 2);

        assertThrows(IllegalStateException.class, () -> new MapTemplate.Builder()
                .setPane(pane)
                .build());
    }

    @Test
    public void setPane_moreThanMaxActions_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setPane(TestUtils.createPane(2, 3))
                        .setHeader(DEFAULT_HEADER)
                        .build());

        // Positive case.
        new MapTemplate.Builder()
                .setPane(TestUtils.createPane(2, 2))
                .setHeader(DEFAULT_HEADER)
                .build();
    }

    @Test
    public void setPane_action_unsupportedSpans_throws() {
        CharSequence title1 = TestUtils.getCharSequenceWithClickableSpan("Title");
        Action action1 = new Action.Builder().setTitle(title1).build();
        Pane pane1 = getPane().addAction(action1).build();
        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setPane(pane1)
                        .setHeader(DEFAULT_HEADER)
                        .build());

        CarText title2 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        Action action2 = new Action.Builder().setTitle(title2).build();
        Pane pane2 = getPane().addAction(action2).build();
        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setPane(pane2)
                        .setHeader(DEFAULT_HEADER)
                        .build());


        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithColorSpan("Title");
        Action action3 = new Action.Builder().setTitle(title3).build();
        Pane pane3 = getPane().addAction(action3).build();
        new MapTemplate.Builder()
                .setPane(pane3)
                .setHeader(DEFAULT_HEADER)
                .build();

        CarText title4 = TestUtils.getCarTextVariantsWithColorSpan("Title");
        Action action4 = new Action.Builder().setTitle(title4).build();
        Pane pane4 = getPane().addAction(action4).build();
        new MapTemplate.Builder()
                .setPane(pane4)
                .setHeader(DEFAULT_HEADER)
                .build();
    }

    @Test
    public void setPane_moreThanMaxTexts_throws() {
        Row rowExceedsMaxTexts = new Row.Builder()
                .setTitle("Title")
                .addText("text1")
                .addText("text2")
                .addText("text3")
                .build();
        Row rowMeetingMaxTexts = new Row.Builder()
                .setTitle("Title")
                .addText("text1")
                .addText("text2")
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setPane(new Pane.Builder().addRow(rowExceedsMaxTexts).build())
                        .build());

        // Positive cases.
        new MapTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setPane(new Pane.Builder().addRow(rowMeetingMaxTexts).build())
                .build();
    }

    @Test
    public void setPane_moreThanMaxPrimaryButtons_throws() {
        Action primaryAction = new Action.Builder()
                .setTitle("primaryAction")
                .setOnClickListener(() -> {
                })
                .setFlags(FLAG_PRIMARY)
                .build();
        Row rowMeetingMaxTexts = new Row.Builder()
                .setTitle("Title")
                .addText("text1")
                .addText("text2")
                .build();

        Pane paneExceedsMaxPrimaryAction = new Pane.Builder()
                .addAction(primaryAction)
                .addAction(primaryAction)
                .addRow(rowMeetingMaxTexts)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setPane(paneExceedsMaxPrimaryAction)
                        .build());
    }

    @Test
    public void setPane_toggleOrClickListener_throws() {
        Row rowWithToggle = new Row.Builder()
                .setTitle("Title")
                .setToggle(new Toggle.Builder(isChecked -> {
                }).build())
                .build();
        Row rowWithClickListener = new Row.Builder()
                .setTitle("Title")
                .setOnClickListener(() -> {
                })
                .build();
        Row rowMeetingRestrictions = new Row.Builder()
                .setTitle("Title")
                .addText("text1")
                .addText("text2")
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setPane(new Pane.Builder().addRow(rowWithToggle).build())
                        .build());
        assertThrows(IllegalArgumentException.class,
                () -> new MapTemplate.Builder()
                        .setHeader(DEFAULT_HEADER)
                        .setPane(new Pane.Builder().addRow(rowWithClickListener).build())
                        .build());

        // Positive cases.
        new MapTemplate.Builder()
                .setHeader(DEFAULT_HEADER)
                .setPane(new Pane.Builder().addRow(rowMeetingRestrictions).build())
                .build();
    }

    @Test
    public void createEmpty() {
        String title = "title";
        Pane pane = TestUtils.createPane(2, 2);
        Header header = new Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .build();

        MapTemplate template = new MapTemplate.Builder()
                .setPane(pane)
                .setHeader(header)
                .build();
        assertThat(template.getMapController()).isNull();
        assertThat(template.getActionStrip()).isNull();
        assertThat(template.getPane()).isEqualTo(pane);
        assertThat(template.getHeader()).isEqualTo(header);
    }

    @Test
    public void createInstance() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        Pane pane = TestUtils.createPane(2, 2);
        Header header = new Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapTemplate template = new MapTemplate.Builder()
                .setMapController(mapController)
                .setPane(pane)
                .setHeader(header)
                .setActionStrip(actionStrip)
                .build();

        assertThat(template.getMapController()).isEqualTo(mapController);
        assertThat(template.getPane()).isEqualTo(pane);
        assertThat(template.getHeader()).isEqualTo(header);
        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void equals() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        Pane pane = TestUtils.createPane(2, 2);
        Header header = new Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapTemplate template = new MapTemplate.Builder()
                .setMapController(mapController)
                .setPane(pane)
                .setHeader(header)
                .setActionStrip(actionStrip)
                .build();

        assertThat(template).isEqualTo(new MapTemplate.Builder()
                .setMapController(mapController)
                .setPane(pane)
                .setHeader(header)
                .setActionStrip(actionStrip)
                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        String title = "title";
        Pane pane = TestUtils.createPane(2, 2);
        Header header = new Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .build();
        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mMapActionStrip)
                .build();
        MapTemplate template = new MapTemplate.Builder()
                .setMapController(mapController)
                .setPane(pane)
                .setHeader(header)
                .setActionStrip(actionStrip)
                .build();

        assertThat(template).isNotEqualTo(new MapTemplate.Builder()
                .setMapController(mapController)
                .setPane(pane)
                .setHeader(header)
                .setActionStrip(new ActionStrip.Builder().addAction(Action.APP_ICON).build())
                .build());
    }

    @Test
    public void notEquals_differentPane() {
        MapTemplate component = new MapTemplate.Builder()
                .setPane(TestUtils.createPane(2, 2))
                .setHeader(DEFAULT_HEADER)
                .build();

        assertThat(component).isNotEqualTo(new MapTemplate.Builder()
                .setPane(getPane().build())
                .setHeader(DEFAULT_HEADER)
                .build());
    }

    @Test
    public void notEquals_differentHeader() {
        MapTemplate component = new MapTemplate.Builder()
                .setPane(getPane().build())
                .setHeader(DEFAULT_HEADER)
                .build();

        assertThat(component).isNotEqualTo(new MapTemplate.Builder()
                .setPane(getPane().build())
                .setHeader(new Header.Builder().setStartHeaderAction(Action.BACK).build())
                .build());
    }
}
