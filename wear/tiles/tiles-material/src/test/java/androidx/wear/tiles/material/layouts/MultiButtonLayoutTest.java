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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.ActionBuilders.LaunchAction;
import androidx.wear.tiles.ModifiersBuilders.Clickable;
import androidx.wear.tiles.material.Button;
import androidx.wear.tiles.material.ButtonDefaults;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class MultiButtonLayoutTest {
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();

    @Test
    public void test_1button() {
        Button button1 =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setTextContent("1")
                        .setSize(ButtonDefaults.EXTRA_LARGE_BUTTON_SIZE)
                        .build();

        MultiButtonLayout layout =
                new MultiButtonLayout.Builder().addButtonContent(button1).build();

        assertThat(layout.getButtonContents()).hasSize(1);
        assertThat(layout.getButtonContents().get(0).toLayoutElementProto())
                .isEqualTo(button1.toLayoutElementProto());
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

        assertThat(layout.getButtonContents()).hasSize(2);
        assertThat(layout.getButtonContents().get(0).toLayoutElementProto())
                .isEqualTo(button1.toLayoutElementProto());
        assertThat(layout.getButtonContents().get(1).toLayoutElementProto())
                .isEqualTo(button2.toLayoutElementProto());
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

        assertThat(layout.getButtonContents()).hasSize(size);
        assertThat(layout.getFiveButtonDistribution())
                .isEqualTo(MultiButtonLayout.FIVE_BUTTON_DISTRIBUTION_TOP_HEAVY);
        for (int i = 0; i < size; i++) {
            assertThat(layout.getButtonContents().get(i).toLayoutElementProto())
                    .isEqualTo(buttons.get(i).toLayoutElementProto());
        }
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
}
