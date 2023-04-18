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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.material.ButtonDefaults.DEFAULT_SIZE;
import static androidx.wear.protolayout.material.ButtonDefaults.EXTRA_LARGE_SIZE;
import static androidx.wear.protolayout.material.ButtonDefaults.LARGE_SIZE;
import static androidx.wear.protolayout.material.ButtonDefaults.PRIMARY_COLORS;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
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
    private static final String RESOURCE_ID = "icon";
    private static final String TEXT = "ABC";
    private static final StringProp CONTENT_DESCRIPTION =
            new StringProp.Builder("clickable button").build();
    private static final StringProp DYNAMIC_CONTENT_DESCRIPTION =
            new StringProp.Builder("static value")
                    .setDynamicValue(DynamicString.constant("clickable button"))
                    .build();
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final LayoutElement CONTENT =
            new Text.Builder(CONTEXT, "ABC").setColor(argb(0)).build();

    @Test
    public void testButtonCustomAddedContentNoContentDesc() {
        Button button = new Button.Builder(CONTEXT, CLICKABLE).setCustomContent(CONTENT).build();

        assertButton(
                button,
                DEFAULT_SIZE,
                new ButtonColors(Colors.PRIMARY, 0),
                null,
                Button.METADATA_TAG_CUSTOM_CONTENT,
                null,
                null,
                null,
                CONTENT);
    }

    @Test
    public void testButtonCustom() {
        DpProp mSize = LARGE_SIZE;
        ButtonColors mButtonColors = new ButtonColors(0x11223344, 0);

        Button button =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setCustomContent(CONTENT)
                        .setSize(mSize)
                        .setButtonColors(mButtonColors)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                mSize,
                mButtonColors,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_CUSTOM_CONTENT,
                null,
                null,
                null,
                CONTENT);
    }

    @Test
    public void testButtonSetIcon() {

        Button button =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setIconContent(RESOURCE_ID)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                DEFAULT_SIZE,
                PRIMARY_COLORS,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_ICON,
                null,
                RESOURCE_ID,
                null,
                null);
    }

    @Test
    public void testButtonSetIconSetSize() {
        Button button =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setIconContent(RESOURCE_ID)
                        .setSize(LARGE_SIZE)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                LARGE_SIZE,
                PRIMARY_COLORS,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_ICON,
                null,
                RESOURCE_ID,
                null,
                null);
    }

    @Test
    public void testButtonSetIconCustomSize() {
        DpProp mSize = dp(36);

        Button button =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setIconContent(RESOURCE_ID, mSize)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                DEFAULT_SIZE,
                PRIMARY_COLORS,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_ICON,
                null,
                RESOURCE_ID,
                null,
                null);
    }

    @Test
    public void testButtonSetText() {
        Button button =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setTextContent(TEXT)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButton(
                button,
                DEFAULT_SIZE,
                PRIMARY_COLORS,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_TEXT,
                TEXT,
                null,
                null,
                null);
    }

    @Test
    public void testButtonSetTextSetSize() {
        Button button =
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setTextContent(TEXT)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .setSize(EXTRA_LARGE_SIZE)
                        .build();

        assertButton(
                button,
                EXTRA_LARGE_SIZE,
                PRIMARY_COLORS,
                CONTENT_DESCRIPTION,
                Button.METADATA_TAG_TEXT,
                TEXT,
                null,
                null,
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
                new Button.Builder(CONTEXT, CLICKABLE)
                        .setTextContent(TEXT)
                        .setContentDescription(DYNAMIC_CONTENT_DESCRIPTION)
                        .setSize(EXTRA_LARGE_SIZE)
                        .build();

        assertThat(button.getContentDescription().toProto())
                .isEqualTo(DYNAMIC_CONTENT_DESCRIPTION.toProto());
    }

    private void assertButton(
            @NonNull Button actualButton,
            @NonNull DpProp expectedSize,
            @NonNull ButtonColors expectedButtonColors,
            @Nullable StringProp expectedContentDescription,
            @NonNull String expectedMetadataTag,
            @Nullable String expectedTextContent,
            @Nullable String expectedIconContent,
            @Nullable String expectedImageContent,
            @Nullable LayoutElement expectedCustomContent) {
        assertButtonIsEqual(
                actualButton,
                expectedSize,
                expectedButtonColors,
                expectedContentDescription,
                expectedMetadataTag,
                expectedTextContent,
                expectedIconContent,
                expectedImageContent,
                expectedCustomContent);

        assertFromLayoutElementButtonIsEqual(
                actualButton,
                expectedSize,
                expectedButtonColors,
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
            @NonNull DpProp expectedSize,
            @NonNull ButtonColors expectedButtonColors,
            @Nullable StringProp expectedContentDescription,
            @NonNull String expectedMetadataTag,
            @Nullable String expectedTextContent,
            @Nullable String expectedIconContent,
            @Nullable String expectedImageContent,
            @Nullable LayoutElement expectedCustomContent) {
        // Mandatory
        assertThat(actualButton.getMetadataTag()).isEqualTo(expectedMetadataTag);
        assertThat(actualButton.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(actualButton.getSize().toContainerDimensionProto())
                .isEqualTo(expectedSize.toContainerDimensionProto());
        assertThat(actualButton.getButtonColors().getBackgroundColor().getArgb())
                .isEqualTo(expectedButtonColors.getBackgroundColor().getArgb());
        assertThat(actualButton.getButtonColors().getContentColor().getArgb())
                .isEqualTo(expectedButtonColors.getContentColor().getArgb());

        // Nullable
        if (expectedContentDescription == null) {
            assertThat(actualButton.getContentDescription()).isNull();
        } else {
            assertThat(actualButton.getContentDescription().toProto())
                    .isEqualTo(expectedContentDescription.toProto());
        }

        if (expectedTextContent == null) {
            assertThat(actualButton.getTextContent()).isNull();
        } else {
            assertThat(actualButton.getTextContent()).isEqualTo(expectedTextContent);
        }

        if (expectedIconContent == null) {
            assertThat(actualButton.getIconContent()).isNull();
        } else {
            assertThat(actualButton.getIconContent()).isEqualTo(expectedIconContent);
        }

        if (expectedImageContent == null) {
            assertThat(actualButton.getImageContent()).isNull();
        } else {
            assertThat(actualButton.getImageContent()).isEqualTo(expectedImageContent);
        }

        if (expectedCustomContent == null) {
            assertThat(actualButton.getCustomContent()).isNull();
        } else {
            assertThat(actualButton.getCustomContent().toLayoutElementProto())
                    .isEqualTo(expectedCustomContent.toLayoutElementProto());
        }
    }

    private void assertFromLayoutElementButtonIsEqual(
            @NonNull Button button,
            @NonNull DpProp expectedSize,
            @NonNull ButtonColors expectedButtonColors,
            @Nullable StringProp expectedContentDescription,
            @NonNull String expectedMetadataTag,
            @Nullable String expectedTextContent,
            @Nullable String expectedIconContent,
            @Nullable String expectedImageContent,
            @Nullable LayoutElement expectedCustomContent) {
        Box box = new Box.Builder().addContent(button).build();

        Button newButton = Button.fromLayoutElement(box.getContents().get(0));

        assertThat(newButton).isNotNull();
        assertButtonIsEqual(
                newButton,
                expectedSize,
                expectedButtonColors,
                expectedContentDescription,
                expectedMetadataTag,
                expectedTextContent,
                expectedIconContent,
                expectedImageContent,
                expectedCustomContent);
    }
}
