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

package androidx.wear.tiles.material;

import static androidx.wear.tiles.ColorBuilders.argb;
import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS;
import static androidx.wear.tiles.material.ButtonDefaults.DEFAULT_BUTTON_SIZE;
import static androidx.wear.tiles.material.ButtonDefaults.EXTRA_LARGE_BUTTON_SIZE;
import static androidx.wear.tiles.material.ButtonDefaults.LARGE_BUTTON_SIZE;
import static androidx.wear.tiles.material.ButtonDefaults.PRIMARY_BUTTON_COLORS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.ActionBuilders.LaunchAction;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders.ColorFilter;
import androidx.wear.tiles.LayoutElementBuilders.Image;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Clickable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class ButtonTest {
    private static final String RESOURCE_ID = "icon";
    private static final String TEXT = "ABC";
    private static final String CONTENT_DESCRIPTION = "clickable button";
    private static final Clickable CLICKABLE =
            new Clickable.Builder()
                    .setOnClick(new LaunchAction.Builder().build())
                    .setId("action_id")
                    .build();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final LayoutElement mContent =
            new Text.Builder(mContext, "ABC").setColor(argb(0)).build();

    @Test
    public void testButtonEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Button.Builder(mContext, CLICKABLE).build());
    }

    @Test
    public void testButtonCustomAddedContentNoContentDesc() {
        Button button = new Button.Builder(mContext, CLICKABLE).setContent(mContent).build();

        assertButtonIsEqual(
                button, mContent, DEFAULT_BUTTON_SIZE, new ButtonColors(Colors.PRIMARY, 0), null);
    }

    @Test
    public void testButtonCustom() {
        DpProp mSize = LARGE_BUTTON_SIZE;
        ButtonColors mButtonColors = new ButtonColors(0x11223344, 0);

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setContent(mContent)
                        .setSize(mSize)
                        .setButtonColors(mButtonColors)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButtonIsEqual(button, mContent, mSize, mButtonColors, CONTENT_DESCRIPTION);
    }

    @Test
    public void testButtonSetIcon() {
        LayoutElement content =
                new Image.Builder()
                        .setResourceId(RESOURCE_ID)
                        .setColorFilter(
                                new ColorFilter.Builder().setTint(argb(Colors.ON_PRIMARY)).build())
                        .setWidth(dp(DEFAULT_BUTTON_SIZE.getValue() / 2))
                        .setHeight(dp(DEFAULT_BUTTON_SIZE.getValue() / 2))
                        .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS)
                        .build();

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setIconContent(RESOURCE_ID)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButtonIsEqual(
                button, content, DEFAULT_BUTTON_SIZE, PRIMARY_BUTTON_COLORS, CONTENT_DESCRIPTION);
    }

    @Test
    public void testButtonSetIconSetSize() {
        LayoutElement content =
                new Image.Builder()
                        .setResourceId(RESOURCE_ID)
                        .setColorFilter(
                                new ColorFilter.Builder().setTint(argb(Colors.ON_PRIMARY)).build())
                        .setWidth(dp(LARGE_BUTTON_SIZE.getValue() / 2))
                        .setHeight(dp(LARGE_BUTTON_SIZE.getValue() / 2))
                        .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS)
                        .build();

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setIconContent(RESOURCE_ID)
                        .setSize(LARGE_BUTTON_SIZE)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButtonIsEqual(
                button, content, LARGE_BUTTON_SIZE, PRIMARY_BUTTON_COLORS, CONTENT_DESCRIPTION);
    }

    @Test
    public void testButtonSetIconCustomSize() {
        DpProp mSize = dp(36);
        LayoutElement content =
                new Image.Builder()
                        .setResourceId(RESOURCE_ID)
                        .setColorFilter(
                                new ColorFilter.Builder().setTint(argb(Colors.ON_PRIMARY)).build())
                        .setWidth(mSize)
                        .setHeight(mSize)
                        .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS)
                        .build();

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setIconContent(RESOURCE_ID, mSize)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButtonIsEqual(
                button, content, DEFAULT_BUTTON_SIZE, PRIMARY_BUTTON_COLORS, CONTENT_DESCRIPTION);
    }

    @Test
    public void testButtonSetText() {
        LayoutElement content =
                new Text.Builder(mContext, TEXT)
                        .setTypography(Typography.TYPOGRAPHY_TITLE2)
                        .setColor(argb(Colors.ON_PRIMARY))
                        .setMaxLines(1)
                        .build();

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setTextContent(TEXT)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButtonIsEqual(
                button, content, DEFAULT_BUTTON_SIZE, PRIMARY_BUTTON_COLORS, CONTENT_DESCRIPTION);
    }

    @Test
    public void testButtonSetTextCustomTypographyCode() {
        LayoutElement content =
                new Text.Builder(mContext, TEXT)
                        .setTypography(Typography.TYPOGRAPHY_BODY2)
                        .setColor(argb(Colors.ON_PRIMARY))
                        .setMaxLines(1)
                        .build();

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setTextContent(TEXT, Typography.TYPOGRAPHY_BODY2)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .build();

        assertButtonIsEqual(
                button, content, DEFAULT_BUTTON_SIZE, PRIMARY_BUTTON_COLORS, CONTENT_DESCRIPTION);
    }

    @Test
    public void testButtonSetTextSetSize() {
        LayoutElement content =
                new Text.Builder(mContext, TEXT)
                        .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                        .setColor(argb(Colors.ON_PRIMARY))
                        .setMaxLines(1)
                        .build();

        Button button =
                new Button.Builder(mContext, CLICKABLE)
                        .setTextContent(TEXT)
                        .setContentDescription(CONTENT_DESCRIPTION)
                        .setSize(EXTRA_LARGE_BUTTON_SIZE)
                        .build();

        assertButtonIsEqual(
                button,
                content,
                EXTRA_LARGE_BUTTON_SIZE,
                PRIMARY_BUTTON_COLORS,
                CONTENT_DESCRIPTION);
    }

    private void assertButtonIsEqual(
            @NonNull Button actualButton,
            @NonNull  LayoutElement expectedContent,
            @NonNull DpProp expectedSize,
            @NonNull  ButtonColors expectedButtonColors,
            @Nullable String expectedContentDescription) {
        // Mandatory
        assertThat(actualButton.getClickable().toProto()).isEqualTo(CLICKABLE.toProto());
        assertThat(actualButton.getSize().toContainerDimensionProto())
                .isEqualTo(expectedSize.toContainerDimensionProto());
        assertThat(actualButton.getButtonColors().getBackgroundColor().getArgb())
                .isEqualTo(expectedButtonColors.getBackgroundColor().getArgb());
        assertThat(actualButton.getButtonColors().getContentColor().getArgb())
                .isEqualTo(expectedButtonColors.getContentColor().getArgb());
        assertThat(actualButton.getContent().toLayoutElementProto())
                .isEqualTo(expectedContent.toLayoutElementProto());

        // Nullable
        if (expectedContentDescription == null) {
            assertThat(actualButton.getContentDescription()).isNull();
        } else {
            assertThat(actualButton.getContentDescription().toString())
                    .isEqualTo(expectedContentDescription);
        }
    }
}
