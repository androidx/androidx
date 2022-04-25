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

package androidx.car.app.model;

import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ActionStrip}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ActionStripTest {
    @Test
    public void createEmpty_throws() {
        assertThrows(IllegalStateException.class, () -> new ActionStrip.Builder().build());
    }

    @Test
    public void defaultBackgroundColor_doesNotThrow() {
        Action action = new Action.Builder().setTitle("Test").setBackgroundColor(
                CarColor.DEFAULT).build();
    }

    @Test
    public void backgroundColor_throws() {
        Action action1 = new Action.Builder().setTitle("Test").setBackgroundColor(
                CarColor.BLUE).build();
        assertThrows(IllegalArgumentException.class,
                () -> new ActionStrip.Builder().addAction(action1));
    }

    @Test
    public void addDuplicatedTypes_throws() {
        Action action1 = Action.BACK;
        Action action2 = new Action.Builder().setTitle("Test").setOnClickListener(() -> {
        }).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionStrip.Builder().addAction(action1).addAction(action1));

        // Duplicated custom types will not throw.
        new ActionStrip.Builder().addAction(action1).addAction(action2).addAction(action2).build();
    }

    @Test
    public void unsupportedPrimaryActions_throws() {
        Action primaryAction = new Action.Builder().setTitle("primaryAction")
                .setOnClickListener(() -> {})
                .setFlags(FLAG_PRIMARY).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionStrip.Builder()
                              .addAction(primaryAction)
        );

    }

    @Test
    public void unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        Action action1 = new Action.Builder().setTitle(title).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionStrip.Builder().addAction(action1));

        CarText title2 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        Action action2 = new Action.Builder().setTitle(title2).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ActionStrip.Builder().addAction(action2));
    }

    @Test
    public void createActions() {
        Action action1 = Action.BACK;
        Action action2 = new Action.Builder().setTitle("Test").setOnClickListener(() -> {
        }).build();
        ActionStrip list = new ActionStrip.Builder().addAction(action1).addAction(action2).build();

        assertThat(list.getActions()).hasSize(2);
        assertThat(action1).isEqualTo(list.getActions().get(0));
        assertThat(action2).isEqualTo(list.getActions().get(1));
    }

    @Test
    public void getFirstActionOfType() {
        Action action1 = Action.BACK;
        Action action2 = new Action.Builder().setTitle("Test").setOnClickListener(() -> {
        }).build();
        ActionStrip list = new ActionStrip.Builder().addAction(action1).addAction(action2).build();

        assertThat(list.getFirstActionOfType(Action.TYPE_BACK)).isEqualTo(action1);
        assertThat(list.getFirstActionOfType(Action.TYPE_CUSTOM)).isEqualTo(action2);
    }

    @Test
    public void equals() {
        Action action1 = Action.BACK;
        Action action2 = Action.APP_ICON;
        ActionStrip list = new ActionStrip.Builder().addAction(action1).addAction(action2).build();
        ActionStrip list2 = new ActionStrip.Builder().addAction(action1).addAction(action2).build();

        assertThat(list2).isEqualTo(list);
    }

    @Test
    public void notEquals() {
        Action action1 = Action.BACK;
        Action action2 = Action.APP_ICON;
        ActionStrip list = new ActionStrip.Builder().addAction(action1).addAction(action2).build();
        ActionStrip list2 = new ActionStrip.Builder().addAction(action2).build();

        assertThat(list).isNotEqualTo(list2);
    }
}
