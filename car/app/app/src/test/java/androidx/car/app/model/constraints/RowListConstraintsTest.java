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

package androidx.car.app.model.constraints;

import static org.junit.Assert.assertThrows;

import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link RowListConstraints}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RowListConstraintsTest {
    @Test
    public void validate_itemList_noSelectable() {
        RowListConstraints disallowConstraints =
                new RowListConstraints.Builder()
                        .setAllowSelectableLists(false)
                        .build();
        RowListConstraints allowConstraints =
                new RowListConstraints.Builder()
                        .setAllowSelectableLists(true)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> disallowConstraints.validateOrThrow(TestUtils.createItemList(5, true)));

        // Positive case
        disallowConstraints.validateOrThrow(TestUtils.createItemList(5, false));
        allowConstraints.validateOrThrow(TestUtils.createItemList(5, true));
    }

    @Test
    public void validate_sectionItemList_noSelectable() {
        RowListConstraints disallowConstraints =
                new RowListConstraints.Builder()
                        .setAllowSelectableLists(false)
                        .build();
        RowListConstraints allowConstraints =
                new RowListConstraints.Builder()
                        .setAllowSelectableLists(true)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> disallowConstraints.validateOrThrow(TestUtils.createSections(2, 2, true)));

        // Positive case
        disallowConstraints.validateOrThrow(TestUtils.createSections(2, 2, false));
        allowConstraints.validateOrThrow(TestUtils.createSections(2, 2, true));
    }

    @Test
    public void validate_pane_maxActions() {
        RowListConstraints constraints =
                new RowListConstraints.Builder()
                        .setMaxActions(2)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> constraints.validateOrThrow(TestUtils.createPane(5, 3)));

        // Positive case
        constraints.validateOrThrow(TestUtils.createPane(5, 2));
    }
}
