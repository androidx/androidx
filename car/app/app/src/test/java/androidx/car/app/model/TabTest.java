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

import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Tab}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TabTest {

    private static final Tab TEST_TAB = new Tab.Builder()
            .setTitle("title")
            .setIcon(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                    "ic_test_1"))
            .setContentId("id")
            .build();

    @Test
    public void createInstance_emptyTab_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Tab.Builder().build());
    }

    @Test
    public void createInstance_missingTitle_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Tab.Builder()
                        .setIcon(TestUtils.getTestCarIcon(
                                ApplicationProvider.getApplicationContext(),
                                "ic_test_1"))
                        .setContentId("id")
                        .build());
    }

    @Test
    public void createInstance_missingIcon_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Tab.Builder()
                        .setTitle("title")
                        .setContentId("id")
                        .build());
    }

    @Test
    public void createInstance_missingContentId_Throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Tab.Builder()
                        .setTitle("title")
                        .setIcon(TestUtils.getTestCarIcon(
                                ApplicationProvider.getApplicationContext(),
                                "ic_test_1"))
                        .build());
    }

    @Test
    public void createInstance_valid() {
        Tab tab = new Tab.Builder()
                .setTitle("title")
                .setIcon(TestUtils.getTestCarIcon(
                ApplicationProvider.getApplicationContext(),
                "ic_test_1"))
                .setContentId("id")
                .build();

        assertEquals(tab.getContentId(), "id");
    }

    @Test
    public void equals() {
        Tab tab = new Tab.Builder()
                .setTitle("title")
                .setIcon(TestUtils.getTestCarIcon(
                        ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .setContentId("id")
                .build();

        assertEquals(tab, TEST_TAB);
    }

    @Test
    public void equals_Builder() {
        Tab tab = TEST_TAB.toBuilder().build();

        assertEquals(tab, TEST_TAB);
    }

    @Test
    public void notEquals_differentTitle() {
        Tab tab = TEST_TAB.toBuilder().setTitle("New Tab").build();

        assertNotEquals(tab, TEST_TAB);
    }

    @Test
    public void notEquals_differentIcon() {
        Tab tab = TEST_TAB.toBuilder()
                .setIcon(TestUtils.getTestCarIcon(
                        ApplicationProvider.getApplicationContext(),
                        "ic_test_2"))
                        .build();

        assertNotEquals(tab, TEST_TAB);
    }

    @Test
    public void notEquals_differentContentId() {
        Tab tab = TEST_TAB.toBuilder().setContentId("new id").build();

        assertNotEquals(tab, TEST_TAB);
    }
}
