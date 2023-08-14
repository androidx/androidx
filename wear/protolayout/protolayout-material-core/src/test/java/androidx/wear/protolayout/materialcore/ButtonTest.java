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

package androidx.wear.protolayout.materialcore;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.materialcore.Button.Builder.CUSTOM_CONTENT;
import static androidx.wear.protolayout.materialcore.Button.Builder.ICON;
import static androidx.wear.protolayout.materialcore.Button.Builder.IMAGE;
import static androidx.wear.protolayout.materialcore.Button.Builder.TEXT;
import static androidx.wear.protolayout.materialcore.Button.METADATA_TAG_CUSTOM_CONTENT;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ActionBuilders.LaunchAction;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.Clickable;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class ButtonTest {
    private static final StringProp CONTENT_DESCRIPTION =
            new StringProp.Builder("clickable button").build();
    private static final StringProp DYNAMIC_CONTENT_DESCRIPTION =
            new StringProp.Builder("static")
                    .setDynamicValue(DynamicString.constant("clickable button"))
                    .build();
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();

    // Material core Button doesn't care what content is inside, so we can re-use this object in all
    // tests.
    private static final LayoutElement CONTENT = new Box.Builder().build();
    private static final DpProp SIZE = dp(50f);
    private static final int COLOR = Color.YELLOW;

    @Test
    public void testButtonCustomContentNoContentDescNoColor() {
        Button button =
                new Button.Builder(CLICKABLE)
                        .setSize(SIZE)
                        .setContent(CONTENT, CUSTOM_CONTENT)
                        .build();

        assertButton(
                button, Color.BLACK, null, METADATA_TAG_CUSTOM_CONTENT, null, null, null, CONTENT);
    }

    @Test
    public void testButtonSetIcon() {
        Button button =
                new Button.Builder(CLICKABLE)
                        .setSize(SIZE)
                        .setContent(CONTENT, ICON)
                        .setBackgroundColor(argb(COLOR))
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                COLOR,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_ICON,
                null,
                CONTENT,
                null,
                null);
    }

    @Test
    public void testButtonSetText() {
        Button button =
                new Button.Builder(CLICKABLE)
                        .setSize(SIZE)
                        .setContent(CONTENT, TEXT)
                        .setBackgroundColor(argb(COLOR))
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                COLOR,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_TEXT,
                CONTENT,
                null,
                null,
                null);
    }

    @Test
    public void testButtonSetImage() {
        Button button =
                new Button.Builder(CLICKABLE)
                        .setSize(SIZE)
                        .setContent(CONTENT, IMAGE)
                        .setBackgroundColor(argb(COLOR))
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                COLOR,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_IMAGE,
                null,
                null,
                CONTENT,
                null);
    }

    @Test
    public void testWrongElementForButton() {
        Column box = new Column.Builder().build();

        assertThat(Button.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBoxForButton() {
        Box box = new Box.Builder().build();

        assertThat(Button.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongTagForButton() {
        Box box =
                new Box.Builder()
                        .setModifiers(
                                new Modifiers.Builder()
                                        .setMetadata(
                                                new ElementMetadata.Builder()
                                                        .setTagData("test".getBytes(UTF_8))
                                                        .build())
                                        .build())
                        .build();

        assertThat(Button.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testDynamicContentDescription() {
        Button button =
                new Button.Builder(CLICKABLE)
                        .setSize(SIZE)
                        .setContent(CONTENT, TEXT)
                        .setContentDescription(DYNAMIC_CONTENT_DESCRIPTION)
                        .build();

        assertThat(button.getContentDescription().toProto())
                .isEqualTo(DYNAMIC_CONTENT_DESCRIPTION.toProto());
    }

    private void assertButton(
            @NonNull Button actualButton,
            @ColorInt int expectedBackgroundColor,
            @Nullable StringProp expectedContentDescription,
            @NonNull String expectedMetadataTag,
            @Nullable LayoutElement expectedTextContent,
            @Nullable LayoutElement expectedIconContent,
            @Nullable LayoutElement expectedImageContent,
            @Nullable LayoutElement expectedCustomContent) {
        assertButtonIsEqual(
                actualButton,
                expectedBackgroundColor,
                expectedContentDescription,
                expectedMetadataTag,
                expectedTextContent,
                expectedIconContent,
                expectedImageContent,
                expectedCustomContent);

        assertFromLayoutElementButtonIsEqual(
                actualButton,
                expectedBackgroundColor,
                expectedContentDescription,
                expectedMetadataTag,
                expectedTextContent,
                expectedIconContent,
                expectedImageContent,
                expectedCustomContent);

        assertThat(Button.fromLayoutElement(actualButton)).isEqualTo(actualButton);
    }

    private void assertButtonIsEqual(
            @NonNull Button actualButton,
            @ColorInt int expectedBackgroundColor,
            @Nullable StringProp expectedContentDescription,
            @NonNull String expectedMetadataTag,
            @Nullable LayoutElement expectedTextContent,
            @Nullable LayoutElement expectedIconContent,
            @Nullable LayoutElement expectedImageContent,
            @Nullable LayoutElement expectedCustomContent) {
        // Mandatory
        assertThat(actualButton.getMetadataTag()).isEqualTo(expectedMetadataTag);
        assertThat(actualButton.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(
                        actualButton
                                .getSize()
                                .toContainerDimensionProto()
                                .getLinearDimension()
                                .getValue())
                .isEqualTo(SIZE.getValue());
        assertThat(actualButton.getBackgroundColor().getArgb()).isEqualTo(expectedBackgroundColor);

        // Nullable
        if (expectedContentDescription == null) {
            assertThat(actualButton.getContentDescription()).isNull();
        } else {
            assertThat(actualButton.getContentDescription().toProto())
                    .isEqualTo(expectedContentDescription.toProto());
        }

        LayoutElement expectedContent = null;

        if (expectedTextContent != null) {
            expectedContent = expectedTextContent;
        }

        if (expectedIconContent != null) {
            expectedContent = expectedIconContent;
        }

        if (expectedImageContent != null) {
            expectedContent = expectedImageContent;
        }

        if (expectedCustomContent != null) {
            expectedContent = expectedCustomContent;
        }

        assertThat(actualButton.getContent().toLayoutElementProto())
                .isEqualTo(expectedContent.toLayoutElementProto());
    }

    private void assertFromLayoutElementButtonIsEqual(
            @NonNull Button button,
            @ColorInt int expectedBackgroundColor,
            @Nullable StringProp expectedContentDescription,
            @NonNull String expectedMetadataTag,
            @Nullable LayoutElement expectedTextContent,
            @Nullable LayoutElement expectedIconContent,
            @Nullable LayoutElement expectedImageContent,
            @Nullable LayoutElement expectedCustomContent) {
        Box box = new Box.Builder().addContent(button).build();

        Button newButton = Button.fromLayoutElement(box.getContents().get(0));

        assertThat(newButton).isNotNull();
        assertButtonIsEqual(
                newButton,
                expectedBackgroundColor,
                expectedContentDescription,
                expectedMetadataTag,
                expectedTextContent,
                expectedIconContent,
                expectedImageContent,
                expectedCustomContent);
    }
}
