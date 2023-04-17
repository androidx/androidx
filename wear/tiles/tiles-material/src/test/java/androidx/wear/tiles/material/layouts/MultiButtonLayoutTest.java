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

package androidx.wear.tiles.material.layouts;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.material.Button;
import androidx.wear.tiles.material.ButtonDefaults;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
@SuppressWarnings("deprecation")
public class MultiButtonLayoutTest {
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final androidx.wear.tiles.ModifiersBuilders.Clickable CLICKABLE =
            new androidx.wear.tiles.ModifiersBuilders.Clickable.Builder()
                    .setOnClick(
                            new androidx.wear.tiles.ActionBuilders.LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();

    @Test
    public void test_1button() {
        Button button1 =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setTextContent("1")
                        .setSize(ButtonDefaults.EXTRA_LARGE_SIZE)
                        .build();

        MultiButtonLayout layout =
                new MultiButtonLayout.Builder().addButtonContent(button1).build();

        assertLayout(layout, Arrays.asList(button1));
    }

    @Test
    public void test_2buttons() {
        Button button1 = new Button.Builder(CONTEXT, CLICKABLE).setTextContent("1").build();
        Button button2 = new Button.Builder(CONTEXT, CLICKABLE).setTextContent("2").build();

        MultiButtonLayout layout =
                new MultiButtonLayout.Builder()
                        .addButtonContent(button1)
                        .addButtonContent(button2)
                        .build();

        assertLayout(layout, Arrays.asList(button1, button2));
    }

    @Test
    public void test_5buttons() {
        List<Button> buttons = new ArrayList<>();
        int size = 5;
        for (int i = 0; i < size; i++) {
            buttons.add(new Button.Builder(CONTEXT, CLICKABLE).setTextContent("" + i).build());
        }

        MultiButtonLayout.Builder layoutBuilder = new MultiButtonLayout.Builder();
        for (Button b : buttons) {
            layoutBuilder.addButtonContent(b);
        }
        layoutBuilder.setFiveButtonDistribution(
                MultiButtonLayout.FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY);
        MultiButtonLayout layout = layoutBuilder.build();

        assertLayout(layout, buttons);
        assertThat(layout.getFiveButtonDistribution())
                .isEqualTo(MultiButtonLayout.FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY);
    }

    @Test
    public void test_too_many_button() {
        Button button = new Button.Builder(CONTEXT, CLICKABLE).setTextContent("1").build();
        MultiButtonLayout.Builder layoutBuilder = new MultiButtonLayout.Builder();
        for (int i = 0; i < LayoutDefaults.MULTI_BUTTON_MAX_NUMBER + 1; i++) {
            layoutBuilder.addButtonContent(button);
        }

        assertThrows(IllegalArgumentException.class, layoutBuilder::build);
    }

    @Test
    public void testWrongElement() {
        androidx.wear.tiles.LayoutElementBuilders.Column box =
                new androidx.wear.tiles.LayoutElementBuilders.Column.Builder().build();

        assertThat(MultiButtonLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build();

        assertThat(MultiButtonLayout.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongTag() {
        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .setModifiers(
                                new androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                        .setMetadata(
                                                new androidx.wear.tiles.ModifiersBuilders
                                                                .ElementMetadata.Builder()
                                                        .setTagData("test".getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(MultiButtonLayout.fromLayoutElement(box)).isNull();
    }

    private void assertLayout(MultiButtonLayout actualLayout, List<Button> expectedButtons) {
        assertLayoutIsEqual(actualLayout, expectedButtons);

        androidx.wear.tiles.LayoutElementBuilders.Box box =
                new androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(actualLayout)
                        .build();

        MultiButtonLayout newLayout = MultiButtonLayout.fromLayoutElement(box.getContents().get(0));

        assertThat(newLayout).isNotNull();
        assertLayoutIsEqual(newLayout, expectedButtons);

        assertThat(MultiButtonLayout.fromLayoutElement(actualLayout)).isEqualTo(actualLayout);
    }

    private void assertLayoutIsEqual(MultiButtonLayout actualLayout, List<Button> expectedButtons) {
        int size = expectedButtons.size();
        assertThat(actualLayout.getMetadataTag()).isEqualTo(MultiButtonLayout.METADATA_TAG);
        assertThat(actualLayout.getButtonContents()).hasSize(size);
        for (int i = 0; i < size; i++) {
            assertThat(actualLayout.getButtonContents().get(i).toLayoutElementProto())
                    .isEqualTo(expectedButtons.get(i).toLayoutElementProto());
        }
    }
}
