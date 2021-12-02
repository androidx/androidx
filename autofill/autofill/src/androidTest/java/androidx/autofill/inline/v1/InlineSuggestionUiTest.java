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

package androidx.autofill.inline.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.PendingIntent;
import android.app.slice.Slice;
import android.app.slice.SliceSpec;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.autofill.R;
import androidx.autofill.inline.InlineUiActivity;
import androidx.autofill.inline.common.ImageViewStyle;
import androidx.autofill.inline.common.TestUtils;
import androidx.autofill.inline.common.TextViewStyle;
import androidx.autofill.inline.common.ViewStyle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 30) // Needed only on 30 and above
public class InlineSuggestionUiTest {

    private static final String TITLE = "Hello world!";
    private static final String SUB_TITLE = "From God";

    @SuppressWarnings("deprecation")
    @Rule
    @NonNull
    public final androidx.test.rule.ActivityTestRule<InlineUiActivity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(InlineUiActivity.class);

    private Instrumentation mInstrumentation;
    private Context mContext;
    private LinearLayout mLinearLayout;
    private PendingIntent mAttributionIntent;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getContext();
        mLinearLayout = mActivityTestRule.getActivity().findViewById(
                androidx.autofill.test.R.id.linear_layout);
        mAttributionIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
    }

    /** Below are tests for the end to end style/content building and rendering */

    @Test
    public void testRender_titleOnly_defaultStyle() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder().build();
        InlineSuggestionUi.Content content =
                InlineSuggestionUi.newContentBuilder(
                        mAttributionIntent).setTitle(TITLE).setContentDescription(
                        "content blabla").build();
        View view = InlineSuggestionUi.render(mContext, content, style);
        addView(view);

        verifyVisibility(true, false, false, false);
        TestUtils.verifyPaddingForDp(mContext, view, 13, 0, 13, 0);

        TextView titleView = mLinearLayout.findViewById(R.id.autofill_inline_suggestion_title);
        assertEquals(TITLE, titleView.getText());
        assertEquals(Color.parseColor("#FF202124"), titleView.getCurrentTextColor());
        assertEquals("content blabla", view.getContentDescription());
    }

    @Test
    public void testRender_titleOnly_customStyle() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder().setTitleStyle(
                new TextViewStyle.Builder().setTextColor(Color.GREEN)
                        .setTextSize(30).build()).build();
        InlineSuggestionUi.Content content = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent).setTitle(TITLE).build();
        View view = InlineSuggestionUi.render(mContext, content, style);
        addView(view);

        verifyVisibility(true, false, false, false);
        TestUtils.verifyPaddingForDp(mContext, view, 13, 0, 13, 0);

        TextView titleView = mLinearLayout.findViewById(R.id.autofill_inline_suggestion_title);
        assertEquals(TITLE, titleView.getText());
        assertEquals(Color.GREEN, titleView.getCurrentTextColor());
        TestUtils.verifyTextSize(mContext, titleView, 30);
    }

    @Test
    public void testRender_singleIcon_defaultStyle() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder().build();
        InlineSuggestionUi.Content content = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent).setStartIcon(
                Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings)).build();
        View view = InlineSuggestionUi.render(mContext, content, style);
        addView(view);

        verifyVisibility(false, false, true, false);
        TestUtils.verifyPaddingForDp(mContext, view, 13, 0, 13, 0);
    }

    @Test
    public void testRender_singleIcon_customStyle() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder().setChipStyle(
                new ViewStyle.Builder().setBackgroundColor(
                        Color.GREEN).setPadding(2, 3, 4, 5).build())
                .setSingleIconChipStyle(new ViewStyle.Builder().setBackground(
                        Icon.createWithResource(mContext, Color.TRANSPARENT)).setPadding(12,
                        13, 14, 15).build())
                .setSingleIconChipIconStyle(
                        new ImageViewStyle.Builder().setTintList(
                                ColorStateList.valueOf(Color.BLUE)).build())
                .build();
        InlineSuggestionUi.Content content = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent).setStartIcon(
                Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings)).build();
        View view = InlineSuggestionUi.render(mContext, content, style);
        addView(view);

        verifyVisibility(false, false, true, false);
        TestUtils.verifyPadding(view, 12, 13, 14, 15);

        ImageView iconView =
                mLinearLayout.findViewById(R.id.autofill_inline_suggestion_start_icon);
        Assert.assertEquals(ColorStateList.valueOf(Color.BLUE), iconView.getImageTintList());
    }

    @Test
    public void testRender_singleIcon_customStyleFallback() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder().setChipStyle(
                new ViewStyle.Builder().setBackgroundColor(
                        Color.GREEN).setPadding(2, 3, 4, 5).build())
                .setStartIconStyle(new ImageViewStyle.Builder().setTintList(
                        ColorStateList.valueOf(Color.YELLOW)).build())
                .build();
        InlineSuggestionUi.Content content = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent).setStartIcon(
                Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings)).build();
        View view = InlineSuggestionUi.render(mContext, content, style);
        addView(view);

        verifyVisibility(false, false, true, false);
        TestUtils.verifyPadding(view, 2, 3, 4, 5);

        ImageView iconView =
                mLinearLayout.findViewById(R.id.autofill_inline_suggestion_start_icon);
        Assert.assertEquals(ColorStateList.valueOf(Color.YELLOW), iconView.getImageTintList());
    }

    @Test
    public void testRender_allWidgets() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder()
                .setChipStyle(new ViewStyle.Builder().setPadding(31, 32, 33, 34).build())
                .setTitleStyle(new TextViewStyle.Builder().setTextColor(Color.BLUE).build())
                .setSubtitleStyle(
                        new TextViewStyle.Builder().setTypeface("serif", Typeface.ITALIC).build())
                .setStartIconStyle(new ImageViewStyle.Builder().setScaleType(
                        ImageView.ScaleType.FIT_START).build())
                .setEndIconStyle(new ImageViewStyle.Builder().setPadding(21, 22, 23, 24).build())
                .build();
        InlineSuggestionUi.Content content = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setContentDescription("Content blabla")
                .setTitle(TITLE)
                .setSubtitle(SUB_TITLE)
                .setStartIcon(Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings))
                .setEndIcon(Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings)).build();
        View view = InlineSuggestionUi.render(mContext, content, style);
        addView(view);

        verifyVisibility(true, true, true, true);
        TestUtils.verifyPadding(view, 31, 32, 33, 34);
        assertEquals("Content blabla", view.getContentDescription());

        TextView titleView = mLinearLayout.findViewById(R.id.autofill_inline_suggestion_title);
        assertEquals(TITLE, titleView.getText());
        assertEquals(Color.BLUE, titleView.getCurrentTextColor());

        TextView subtitleView =
                mLinearLayout.findViewById(R.id.autofill_inline_suggestion_subtitle);
        assertEquals(SUB_TITLE, subtitleView.getText());
        assertEquals(Typeface.ITALIC, subtitleView.getTypeface().getStyle());

        ImageView startIcon =
                mLinearLayout.findViewById(R.id.autofill_inline_suggestion_start_icon);
        assertEquals(ImageView.ScaleType.FIT_START, startIcon.getScaleType());

        ImageView endIcon = mLinearLayout.findViewById(R.id.autofill_inline_suggestion_end_icon);
        TestUtils.verifyPadding(endIcon, 21, 22, 23, 24);
    }

    @Test
    public void testRender_allWidgets_rtl() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder()
                .setLayoutDirection(View.LAYOUT_DIRECTION_RTL)
                .setChipStyle(new ViewStyle.Builder().setPadding(11, 12, 13, 14).build())
                .setStartIconStyle(new ImageViewStyle.Builder()
                        .setPadding(21, 22, 23, 24)
                        .setLayoutMargin(31, 32, 33, 34).build())
                .setSingleIconChipStyle(new ViewStyle.Builder().setPadding(41, 42, 43, 44).build())
                .setSingleIconChipIconStyle(new ImageViewStyle.Builder()
                        .setPadding(51, 52, 53, 54)
                        .setLayoutMargin(61, 62, 63, 64).build())
                .build();
        InlineSuggestionUi.Content contentAllWidgets = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setContentDescription("Content blabla")
                .setTitle(TITLE)
                .setSubtitle(SUB_TITLE)
                .setStartIcon(Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings))
                .setEndIcon(Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings)).build();
        View view = InlineSuggestionUi.render(mContext, contentAllWidgets, style);
        addView(view);

        verifyVisibility(true, true, true, true);
        TestUtils.verifyPadding(view, 13, 12, 11, 14);
        assertEquals("Content blabla", view.getContentDescription());

        ImageView startIcon =
                mLinearLayout.findViewById(R.id.autofill_inline_suggestion_start_icon);
        TestUtils.verifyPadding(startIcon, 23, 22, 21, 24);
        TestUtils.verifyLayoutMargin(startIcon, 33, 32, 31, 34);
    }

    @Test
    public void testRender_singleIcon_rtl() {
        InlineSuggestionUi.Style style = new InlineSuggestionUi.Style.Builder()
                .setLayoutDirection(View.LAYOUT_DIRECTION_RTL)
                .setChipStyle(new ViewStyle.Builder().setPadding(11, 12, 13, 14).build())
                .setStartIconStyle(new ImageViewStyle.Builder()
                        .setPadding(21, 22, 23, 24)
                        .setLayoutMargin(31, 32, 33, 34).build())
                .setSingleIconChipStyle(new ViewStyle.Builder().setPadding(41, 42, 43, 44).build())
                .setSingleIconChipIconStyle(new ImageViewStyle.Builder()
                        .setPadding(51, 52, 53, 54)
                        .setLayoutMargin(61, 62, 63, 64).build())
                .build();
        InlineSuggestionUi.Content contentSingleIcon = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setContentDescription("Content blabla")
                .setStartIcon(Icon.createWithResource(mContext,
                        androidx.autofill.test.R.drawable.ic_settings)).build();
        View view = InlineSuggestionUi.render(mContext, contentSingleIcon, style);
        addView(view);

        verifyVisibility(false, false, true, false);
        TestUtils.verifyPadding(view, 43, 42, 41, 44);
        assertEquals("Content blabla", view.getContentDescription());

        ImageView startIcon =
                mLinearLayout.findViewById(R.id.autofill_inline_suggestion_start_icon);
        TestUtils.verifyPadding(startIcon, 53, 52, 51, 54);
        TestUtils.verifyLayoutMargin(startIcon, 63, 62, 61, 64);
    }

    /** Below are tests for the Content class */

    @Test
    public void testContent_titleOnly() {
        InlineSuggestionUi.Content originalContent = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setTitle("title")
                .setContentDescription("content blabla")
                .build();
        InlineSuggestionUi.Content transportedContent = new InlineSuggestionUi.Content(
                originalContent.getSlice());
        assertContent(transportedContent, "title", null, null, null);
        assertEquals("content blabla", transportedContent.getContentDescription());
    }

    @Test
    public void testContent_titleAndSubtitle() {
        InlineSuggestionUi.Content originalContent = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setTitle("title")
                .setSubtitle("subtitle")
                .build();

        InlineSuggestionUi.Content transportedContent = new InlineSuggestionUi.Content(
                originalContent.getSlice());
        assertContent(transportedContent, "title", "subtitle", null, null);
    }

    @Test
    public void testContent_startIcon() {
        Icon icon = Icon.createWithResource(mContext,
                androidx.autofill.test.R.drawable.ic_settings);
        InlineSuggestionUi.Content originalContent = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setStartIcon(icon)
                .build();

        InlineSuggestionUi.Content transportedContent = new InlineSuggestionUi.Content(
                originalContent.getSlice());
        assertContent(transportedContent, null, null, icon, null);
    }

    @Test
    public void testContent_startIconAndTitle() {
        Icon icon = Icon.createWithResource(mContext,
                androidx.autofill.test.R.drawable.ic_settings);
        InlineSuggestionUi.Content originalContent = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setStartIcon(icon)
                .setTitle("title")
                .build();

        InlineSuggestionUi.Content transportedContent = new InlineSuggestionUi.Content(
                originalContent.getSlice());
        assertContent(transportedContent, "title", null, icon, null);
    }

    @Test
    public void testContent_titleAndEndIcon() {
        Icon icon = Icon.createWithResource(mContext,
                androidx.autofill.test.R.drawable.ic_settings);
        InlineSuggestionUi.Content originalContent = InlineSuggestionUi.newContentBuilder(
                mAttributionIntent)
                .setTitle("title")
                .setEndIcon(icon)
                .build();

        InlineSuggestionUi.Content transportedContent = new InlineSuggestionUi.Content(
                originalContent.getSlice());
        assertContent(transportedContent, "title", null, null, icon);
    }

    @Test
    public void testContent_subtitleWithoutTitle_exception() {
        try {
            InlineSuggestionUi.newContentBuilder(mAttributionIntent)
                    .setSubtitle("subtitle")
                    .build();
            fail(); // this line should not be executed
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void testContent_invalidSlice() {
        Slice slice = new Slice.Builder(Uri.parse("inline.slice"),
                new SliceSpec("random_version", 1)).build();
        InlineSuggestionUi.Content content = new InlineSuggestionUi.Content(slice);
        assertFalse(content.isValid());
    }

    @Test
    public void testContent_invalidSlice_noException() {
        Slice slice = new Slice.Builder(Uri.parse("inline.slice"),
                new SliceSpec("random_version", 1)).addText("title", null,
                Collections.singletonList("RANDOM_HINT")).build();
        InlineSuggestionUi.Content content = new InlineSuggestionUi.Content(slice);
        assertFalse(content.isValid());
    }

    /** Below are tests for the Style class */

    @Test
    public void testStyleWithNothing() {
        InlineSuggestionUi.Style.Builder builder = new InlineSuggestionUi.Style.Builder();
        InlineSuggestionUi.Style style = builder.build();
        style.applyStyle(new FrameLayout(mContext), new ImageView(mContext));
        style.applyStyle(new FrameLayout(mContext), new ImageView(mContext),
                new TextView(mContext), new TextView(mContext), new ImageView(mContext));
    }

    @Test
    public void testStyleWithEverything() {
        InlineSuggestionUi.Style.Builder builder = new InlineSuggestionUi.Style.Builder();
        ViewStyle chipStyle = new ViewStyle.Builder()
                .setBackgroundColor(Color.BLUE).build();
        ImageViewStyle startIconStyle = new ImageViewStyle.Builder().setScaleType(
                ImageView.ScaleType.CENTER).setBackgroundColor(Color.GREEN).build();
        TextViewStyle titleStyle =
                new TextViewStyle.Builder().setTextColor(Color.GRAY).setBackgroundColor(
                        Color.LTGRAY).build();
        TextViewStyle subtitleStyle =
                new TextViewStyle.Builder().setTextColor(Color.BLACK).setBackgroundColor(
                        Color.WHITE).build();
        ImageViewStyle endIconStyle = new ImageViewStyle.Builder().setScaleType(
                ImageView.ScaleType.CENTER).setBackgroundColor(Color.RED).build();
        InlineSuggestionUi.Style style = builder
                .setChipStyle(chipStyle)
                .setStartIconStyle(startIconStyle)
                .setEndIconStyle(endIconStyle)
                .setTitleStyle(titleStyle)
                .setSubtitleStyle(subtitleStyle)
                .build();

        View suggestionView = LayoutInflater.from(mContext).inflate(
                R.layout.autofill_inline_suggestion, null);
        ImageView startIconView = suggestionView.findViewById(
                R.id.autofill_inline_suggestion_start_icon);
        startIconView.setVisibility(View.VISIBLE);
        TextView titleView = suggestionView.findViewById(R.id.autofill_inline_suggestion_title);
        titleView.setVisibility(View.VISIBLE);
        TextView subtitleView = suggestionView.findViewById(
                R.id.autofill_inline_suggestion_subtitle);
        subtitleView.setVisibility(View.VISIBLE);
        ImageView endIconView = suggestionView.findViewById(
                R.id.autofill_inline_suggestion_end_icon);
        endIconView.setVisibility(View.VISIBLE);

        style.applyStyle(suggestionView, startIconView, titleView, subtitleView, endIconView);

        TestUtils.verifyBackgroundColor(suggestionView, Color.BLUE);
        TestUtils.verifyBackgroundColor(startIconView, Color.GREEN);
        TestUtils.verifyBackgroundColor(titleView, Color.LTGRAY);
        TestUtils.verifyBackgroundColor(subtitleView, Color.WHITE);
        TestUtils.verifyBackgroundColor(endIconView, Color.RED);
    }

    @Test
    public void testStyleWithRtl() {
        InlineSuggestionUi.Style.Builder builder = new InlineSuggestionUi.Style.Builder();
        InlineSuggestionUi.Style style = builder.setLayoutDirection(
                View.LAYOUT_DIRECTION_RTL).build();

        View suggestionView = LayoutInflater.from(mContext).inflate(
                R.layout.autofill_inline_suggestion, null);
        ImageView startIconView = suggestionView.findViewById(
                R.id.autofill_inline_suggestion_start_icon);
        startIconView.setVisibility(View.VISIBLE);
        TextView titleView = suggestionView.findViewById(R.id.autofill_inline_suggestion_title);
        titleView.setVisibility(View.VISIBLE);
        TextView subtitleView = suggestionView.findViewById(
                R.id.autofill_inline_suggestion_subtitle);
        subtitleView.setVisibility(View.VISIBLE);
        ImageView endIconView = suggestionView.findViewById(
                R.id.autofill_inline_suggestion_end_icon);
        endIconView.setVisibility(View.VISIBLE);

        style.applyStyle(suggestionView, startIconView, titleView, subtitleView, endIconView);

        assertEquals(View.LAYOUT_DIRECTION_RTL, suggestionView.getLayoutDirection());
        assertEquals(View.LAYOUT_DIRECTION_RTL, startIconView.getLayoutDirection());
        assertEquals(View.LAYOUT_DIRECTION_RTL, titleView.getLayoutDirection());
        assertEquals(View.LAYOUT_DIRECTION_RTL, subtitleView.getLayoutDirection());
        assertEquals(View.LAYOUT_DIRECTION_RTL, endIconView.getLayoutDirection());
    }

    /** Below are private helper methods */

    private void addView(View view) {
        final CountDownLatch viewAddedLatch = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mLinearLayout.addView(view);
            viewAddedLatch.countDown();
        });
        try {
            viewAddedLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }
    }

    private void verifyVisibility(boolean title, boolean subtitle, boolean startIcon,
            boolean endIcon) {
        verifyVisibility(R.id.autofill_inline_suggestion_title, title);
        verifyVisibility(R.id.autofill_inline_suggestion_subtitle, subtitle);
        verifyVisibility(R.id.autofill_inline_suggestion_start_icon, startIcon);
        verifyVisibility(R.id.autofill_inline_suggestion_end_icon, endIcon);
    }

    private void verifyVisibility(int viewId, boolean visible) {
        View view = mLinearLayout.findViewById(viewId);
        assertNotNull(view);
        assertEquals(visible ? View.VISIBLE : View.GONE, view.getVisibility());
    }

    private void assertContent(InlineSuggestionUi.Content content, String title, String subtitle,
            Icon startIcon,
            Icon endIcon) {
        assertTrue(content.isValid());
        assertEquals(title, content.getTitle());
        assertEquals(subtitle, content.getSubtitle());
        assertEquals(startIcon, content.getStartIcon());
        assertEquals(endIcon, content.getEndIcon());
    }
}
