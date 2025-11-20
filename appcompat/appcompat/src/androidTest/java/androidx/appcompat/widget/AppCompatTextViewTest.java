/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.appcompat.widget;

import static androidx.appcompat.testutils.TestUtilsActions.setEnabled;
import static androidx.appcompat.testutils.TestUtilsActions.setTextAppearance;
import static androidx.appcompat.testutils.TestUtilsMatchers.isBackground;
import static androidx.core.view.ViewKt.drawToBitmap;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.LocaleList;
import android.text.Layout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatTextView} class.
 */
@LargeTest
public class AppCompatTextViewTest
        extends AppCompatBaseViewTest<AppCompatTextViewActivity, AppCompatTextView> {
    private static final String SAMPLE_TEXT_1 = "Hello, World!";
    private static final String SAMPLE_TEXT_2 = "Hello, Android!";
    private static final int UNLIMITED_MEASURE_SPEC = View.MeasureSpec.makeMeasureSpec(0,
            View.MeasureSpec.UNSPECIFIED);

    public AppCompatTextViewTest() {
        super(AppCompatTextViewActivity.class);
    }

    /**
     * This method tests that background tinting is applied when the call to
     * {@link androidx.core.view.ViewCompat#setBackgroundTintList(View, ColorStateList)}
     * is done as a deferred event.
     */
    @Test
    @MediumTest
    public void testDeferredBackgroundTinting() throws Throwable {
        onView(withId(R.id.view_untinted_deferred))
                .check(matches(isBackground(0xff000000, true)));

        final @ColorInt int oceanDefault = ResourcesCompat.getColor(
                mResources, R.color.ocean_default, null);

        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_ocean, null);

        // Emulate delay in kicking off the call to ViewCompat.setBackgroundTintList
        Thread.sleep(200);
        final CountDownLatch latch = new CountDownLatch(1);
        mActivityTestRule.getScenario().onActivity(activity -> {
            TextView view = activity.findViewById(R.id.view_untinted_deferred);
            ViewCompat.setBackgroundTintList(view, oceanColor);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Check that the background has switched to the matching entry in the newly set
        // color state list.
        onView(withId(R.id.view_untinted_deferred))
                .check(matches(isBackground(oceanDefault, true)));
    }

    @Test
    public void testAllCaps() {
        final String text1 = mResources.getString(R.string.sample_text1);
        final String text2 = mResources.getString(R.string.sample_text2);

        final AppCompatTextView textView1 = mContainer.findViewById(R.id.text_view_caps1);
        final AppCompatTextView textView2 = mContainer.findViewById(R.id.text_view_caps2);

        // Note that TextView.getText() returns the original text. We are interested in
        // the transformed text that is set on the Layout object used to draw the final
        // (transformed) content.
        assertEquals("Text view starts in all caps on",
                text1.toUpperCase(), textView1.getLayout().getText().toString());
        assertEquals("Text view starts in all caps off",
                text2, textView2.getLayout().getText().toString());

        // Toggle all-caps mode on the two text views
        onView(withId(R.id.text_view_caps1)).perform(
                setTextAppearance(R.style.TextStyleAllCapsOff));
        assertEquals("Text view is now in all caps off",
                text1, textView1.getLayout().getText().toString());

        onView(withId(R.id.text_view_caps2)).perform(
                setTextAppearance(R.style.TextStyleAllCapsOn));
        assertEquals("Text view is now in all caps on",
                text2.toUpperCase(), textView2.getLayout().getText().toString());
    }

    @Test
    public void testAppCompatAllCapsFalseOnButton() {
        final String text = mResources.getString(R.string.sample_text2);
        final AppCompatTextView textView =
                mContainer.findViewById(R.id.text_view_app_allcaps_false);

        assertEquals("Text view is not in all caps", text, textView.getLayout().getText());
    }

    @Test
    public void testTextColorSetHex() {
        final TextView textView = mContainer.findViewById(R.id.view_text_color_hex);
        assertEquals(Color.RED, textView.getCurrentTextColor());
    }

    @Test
    public void testTextColorSetColorStateList() {
        final TextView textView = mContainer.findViewById(R.id.view_text_color_csl);

        onView(withId(R.id.view_text_color_csl)).perform(setEnabled(true));
        assertEquals(ContextCompat.getColor(textView.getContext(), R.color.ocean_default),
                textView.getCurrentTextColor());

        onView(withId(R.id.view_text_color_csl)).perform(setEnabled(false));
        assertEquals(ContextCompat.getColor(textView.getContext(), R.color.ocean_disabled),
                textView.getCurrentTextColor());
    }

    @Test
    public void testTextColorSetThemeAttrHex() {
        final TextView textView = mContainer.findViewById(R.id.view_text_color_primary);
        assertEquals(Color.BLUE, textView.getCurrentTextColor());
    }

    @Test
    public void testTextColorSetThemeAttrColorStateList() {
        final TextView textView = mContainer.findViewById(R.id.view_text_color_secondary);

        onView(withId(R.id.view_text_color_secondary)).perform(setEnabled(true));
        assertEquals(ContextCompat.getColor(textView.getContext(), R.color.sand_default),
                textView.getCurrentTextColor());

        onView(withId(R.id.view_text_color_secondary)).perform(setEnabled(false));
        assertEquals(ContextCompat.getColor(textView.getContext(), R.color.sand_disabled),
                textView.getCurrentTextColor());
    }

    private void verifyTextLinkColor(TextView textView,
            @ColorRes int expectedEnabledColor, @ColorRes int expectedDisabledColor) {
        ColorStateList linkColorStateList = textView.getLinkTextColors();
        assertEquals(ContextCompat.getColor(textView.getContext(), expectedEnabledColor),
                linkColorStateList.getColorForState(new int[]{android.R.attr.state_enabled}, 0));
        assertEquals(ContextCompat.getColor(textView.getContext(), expectedDisabledColor),
                linkColorStateList.getColorForState(new int[]{-android.R.attr.state_enabled}, 0));
    }

    @Test
    @UiThreadTest
    public void testTextLinkColor() {
        TextView textLinkEnabledView = mContainer.findViewById(R.id.view_text_link_enabled);
        TextView textLinkDisabledView = mContainer.findViewById(R.id.view_text_link_disabled);

        // Verify initial enabled and disabled text link colors set from the activity theme
        verifyTextLinkColor(textLinkEnabledView, R.color.lilac_default, R.color.lilac_disabled);
        verifyTextLinkColor(textLinkDisabledView, R.color.lilac_default, R.color.lilac_disabled);

        // Set new text appearance on the two views - the appearance has new text link color
        // state list that references theme-level attributes. And verify that the new text
        // link colors are correctly resolved.
        TextViewCompat.setTextAppearance(textLinkEnabledView, R.style.TextStyleNew);
        verifyTextLinkColor(textLinkEnabledView, R.color.ocean_default, R.color.ocean_disabled);
        TextViewCompat.setTextAppearance(textLinkDisabledView, R.style.TextStyleNew);
        verifyTextLinkColor(textLinkDisabledView, R.color.ocean_default, R.color.ocean_disabled);
    }

    @Test
    @UiThreadTest
    public void testTextSize_canBeZero() {
        final TextView textView = mContainer.findViewById(R.id.textview_zero_text_size);
        // text size should be 0 as set in xml, rather than the text view default (15.0)
        assertEquals(0.0f, textView.getTextSize(), 0.01f);
        // text size can be set programmatically to non-negative values
        textView.setTextSize(20f);
        assertTrue(textView.getTextSize() > 0.0f);
        // text size should become 0 when we set a text appearance with 0 text size
        TextViewCompat.setTextAppearance(textView, R.style.TextView_ZeroTextSize);
        assertEquals(0f, textView.getTextSize(), 0.01f);
    }

    @Test
    public void testFontResources_setInStringFamilyName() {
        TextView textView =
                mContainer.findViewById(R.id.textview_fontresource_fontfamily_string_resource);
        assertNotNull(textView.getTypeface());
        assertEquals(Typeface.SANS_SERIF, textView.getTypeface());
        textView = mContainer.findViewById(R.id.textview_fontresource_fontfamily_string_direct);
        assertNotNull(textView.getTypeface());
        assertEquals(Typeface.SANS_SERIF, textView.getTypeface());
    }

    @Test
    public void testFontResources_setInXmlFamilyName() {
        TextView textView = mContainer.findViewById(R.id.textview_fontresource_fontfamily);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testFontResourcesXml_setInXmlFamilyName() {
        TextView textView = mContainer.findViewById(R.id.textview_fontxmlresource_fontfamily);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplexmlfont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testFontResourcesXml_setInXmlFamilyNameWithTextStyle() {
        TextView textView =
                mContainer.findViewById(R.id.textview_fontxmlresource_fontfamily_textstyle);

        assertNotEquals(Typeface.DEFAULT, textView.getTypeface());
    }

    @Test
    public void testFontResourcesXml_setInXmlFamilyNameWithTextStyle2() {
        TextView textView =
                mContainer.findViewById(R.id.textview_fontxmlresource_fontfamily_textstyle2);

        assertNotEquals(Typeface.DEFAULT, textView.getTypeface());
    }

    @Test
    public void testFontResources_setInXmlStyle() {
        TextView textView = mContainer.findViewById(R.id.textview_fontresource_style);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testFontResourcesXml_setInXmlStyle() {
        TextView textView = mContainer.findViewById(R.id.textview_fontxmlresource_style);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplexmlfont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testFontResources_setInXmlTextAppearance() {
        TextView textView = mContainer.findViewById(R.id.textview_fontresource_textAppearance);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testFontResourcesXml_setInXmlTextAppearance() {
        TextView textView = mContainer.findViewById(R.id.textview_fontxmlresource_textAppearance);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplexmlfont);

        assertEquals(expected, textView.getTypeface());
    }

    @Test
    public void testTextStyle_setTextStyleInStyle() {
        // TextView has a TextAppearance by default, but the textStyle can be overriden in style.
        TextView textView = mContainer.findViewById(R.id.textview_textStyleOverride);

        assertEquals(Typeface.ITALIC, textView.getTypeface().getStyle());
    }

    @Test
    public void testTextStyle_setTextStyleDirectly() {
        TextView textView = mContainer.findViewById(R.id.textview_textStyleDirect);

        assertEquals(Typeface.ITALIC, textView.getTypeface().getStyle());
    }

    @Test
    @UiThreadTest
    public void testFontResources_setTextAppearance() {
        TextView textView = mContainer.findViewById(R.id.textview_simple);

        TextViewCompat.setTextAppearance(textView, R.style.TextView_FontResourceWithStyle);

        assertNotEquals(Typeface.DEFAULT, textView.getTypeface());
    }

    @Test
    @UiThreadTest
    public void testSetTextAppearance_resetTypeface() throws PackageManager.NameNotFoundException {
        TextView textView = mContainer.findViewById(R.id.textview_simple);

        TextViewCompat.setTextAppearance(textView, R.style.TextView_SansSerif);
        Typeface firstTypeface = textView.getTypeface();

        TextViewCompat.setTextAppearance(textView, R.style.TextView_Serif);
        Typeface secondTypeface = textView.getTypeface();
        assertNotNull(firstTypeface);
        assertNotNull(secondTypeface);
        assertNotEquals(firstTypeface, secondTypeface);
    }

    @Test
    @UiThreadTest
    public void testTypefaceAttribute_serif() {
        TextView textView = mContainer.findViewById(R.id.textview_simple);

        TextViewCompat.setTextAppearance(textView, R.style.TextView_Typeface_Serif);

        assertEquals(Typeface.SERIF, textView.getTypeface());
    }

    @Test
    public void testTextLocale_setInXml() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_textLocale_textView);
        if (Build.VERSION.SDK_INT >= 24) {
            assertEquals(LocaleList.forLanguageTags("ja-JP,zh-CN"), textView.getTextLocales());
        } else {
            assertEquals(Locale.forLanguageTag("ja-JP"), textView.getTextLocale());
        }
    }

    @Test
    public void testTextLocale_setInXml_singleLocale() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_textLocale_textView_singleLocale);
        if (Build.VERSION.SDK_INT >= 24) {
            assertEquals(LocaleList.forLanguageTags("zh-CN"), textView.getTextLocales());
        } else {
            assertEquals(Locale.forLanguageTag("zh-CN"), textView.getTextLocale());
        }
    }

    @Test
    public void testTextLocale_setInXmlByTextAppearance() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_textLocale_textAppearance);
        if (Build.VERSION.SDK_INT >= 24) {
            assertEquals(LocaleList.forLanguageTags("zh-CN,ja-JP"), textView.getTextLocales());
        } else {
            assertEquals(Locale.forLanguageTag("zh-CN"), textView.getTextLocale());
        }
    }

    @Test
    public void testTextLocalePriority_setInXml() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_textLocale_textView_and_textAppearance);
        if (Build.VERSION.SDK_INT >= 24) {
            assertEquals(LocaleList.forLanguageTags("ja-JP,zh-CN"), textView.getTextLocales());
        } else {
            assertEquals(Locale.forLanguageTag("ja-JP"), textView.getTextLocale());
        }
    }

    @Test
    @UiThreadTest
    public void testTypefaceAttribute_monospace() {
        TextView textView = mContainer.findViewById(R.id.textview_simple);

        TextViewCompat.setTextAppearance(textView, R.style.TextView_Typeface_Monospace);

        assertEquals(Typeface.MONOSPACE, textView.getTypeface());
    }

    @Test
    @UiThreadTest
    public void testTypefaceAttribute_serifFromXml() {
        TextView textView = mContainer.findViewById(R.id.textview_typeface_serif);

        assertEquals(Typeface.SERIF, textView.getTypeface());
    }

    @Test
    @UiThreadTest
    public void testTypefaceAttribute_monospaceFromXml() {
        TextView textView = mContainer.findViewById(R.id.textview_typeface_monospace);

        assertEquals(Typeface.MONOSPACE, textView.getTypeface());
    }

    @Test
    @UiThreadTest
    public void testTypefaceAttribute_fontFamilyHierarchy() {
        // This view has typeface=serif set on the view directly and a fontFamily on the appearance
        TextView textView = mContainer.findViewById(R.id.textview_typeface_and_fontfamily);

        assertEquals(Typeface.SERIF, textView.getTypeface());
    }

    @Test
    @UiThreadTest
    public void testfontFamilyNamespaceHierarchy() {
        // This view has fontFamily set in both the app and android namespace. App should be used.
        TextView textView = mContainer.findViewById(R.id.textview_app_and_android_fontfamily);

        assertEquals(Typeface.MONOSPACE, textView.getTypeface());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFontVariation_setInXml() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_fontVariation_textView);
        assertEquals("'wght' 200", textView.getFontVariationSettings());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        runCommandAndAssertBitmapChangedOrSame(textView, /* expectSame*/ true, () ->
                instrumentation.runOnMainSync(() -> {
                    textView.setFontVariationSettings("'wght' 100"); /* reset */
                    textView.setFontVariationSettings("'wght' 200");
                })
        );
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFontVariation_setInXmlByTextAppearance() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_fontVariation_textAppearance);
        assertEquals("'wght' 300", textView.getFontVariationSettings());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        runCommandAndAssertBitmapChangedOrSame(textView, /* expectSame*/ true, () ->
                instrumentation.runOnMainSync(() -> {
                    textView.setFontVariationSettings("'wght' 100"); /* reset */
                    textView.setFontVariationSettings("'wght' 300");
                })
        );
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFontVariation_setByBothXmlAndTextAppearance() throws InterruptedException {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_fontVariation_textAppearance);

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        runCommandAndAssertBitmapChangedOrSame(textView, /* expectSame*/ true, () ->
                instrumentation.runOnMainSync(() ->
                        textView.setTextAppearance(R.style.TextView_FontVariation)
                )
        );
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFontVariationPriority_setInXml() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_fontVariation_textView_and_textAppearance);
        //FontVariation is set in both AppCompatTextView and textAppearance,
        //we should use the one in AppCompatTextView.
        assertEquals("'wght' 700", textView.getFontVariationSettings());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @UiThreadTest
    public void testFontVariation_setTextAppearance() throws Throwable {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_fontVariation_textView
        );
        textView.setTextAppearance(textView.getContext(), R.style.TextView_FontVariation);
        assertEquals("'wght' 300", textView.getFontVariationSettings());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testFontVariation_setTextAppearance_changesPixels() throws Throwable {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.textview_fontVariation_textView
        );

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() ->
                textView.setFontVariationSettings("'wght' 900")
        );
        runCommandAndAssertBitmapChangedOrSame(textView, /* expectSame*/ false, () ->
                instrumentation.runOnMainSync(() ->
                        textView.setTextAppearance(textView.getContext(),
                                R.style.TextView_FontVariation)
                )
        );
    }

    /**
     * Run a command and confirm that the result of the command was a visible change to at least
     * one pixel or exact similarity
     *
     * @param subject view to change
     * @param expectSame when true, expect pixels to be identical, otherwise expect differences
     * @param command comamnd to be called. it is not dispatched
     */
    public void runCommandAndAssertBitmapChangedOrSame(TextView subject, Boolean expectSame,
            Runnable command) {
        Bitmap before = drawToBitmap(subject, Bitmap.Config.ARGB_8888);
        try {
            command.run();
            Bitmap after = drawToBitmap(subject, Bitmap.Config.ARGB_8888);
            try {
                if (expectSame) {
                    assertTrue(before.sameAs(after));
                } else {
                    assertFalse(before.sameAs(after));
                }
            } finally {
                after.recycle();
            }
        } finally {
            before.recycle();
        }
    }

    @Test
    @UiThreadTest
    public void testBaselineAttributes() {
        TextView textView = mContainer.findViewById(R.id.textview_baseline);

        final int firstBaselineToTopHeight = textView.getResources()
                .getDimensionPixelSize(R.dimen.textview_firstBaselineToTopHeight);
        final int lastBaselineToBottomHeight = textView.getResources()
                .getDimensionPixelSize(R.dimen.textview_lastBaselineToBottomHeight);
        final int lineHeight = textView.getResources()
                .getDimensionPixelSize(R.dimen.textview_lineHeight);

        assertEquals(firstBaselineToTopHeight,
                TextViewCompat.getFirstBaselineToTopHeight(textView));
        assertEquals(lastBaselineToBottomHeight,
                TextViewCompat.getLastBaselineToBottomHeight(textView));
        assertEquals(lineHeight, textView.getLineHeight());
    }

    private static class ManualExecutor implements Executor {
        private static final int TIMEOUT_MS = 5000;
        private final Lock mLock = new ReentrantLock();
        private final Condition mCond = mLock.newCondition();

        @GuardedBy("mLock")
        ArrayList<Runnable> mRunnables = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            mLock.lock();
            try {
                mRunnables.add(command);
                mCond.signal();
            } finally {
                mLock.unlock();
            }
        }

        /**
         * Synchronously execute the i-th runnable
         */
        public void doExecution(int i) {
            getRunnableAt(i).run();
        }

        private Runnable getRunnableAt(int i) {
            mLock.lock();
            try {
                long remainingNanos = TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
                while (mRunnables.size() <= i) {
                    try {
                        remainingNanos = mCond.awaitNanos(remainingNanos);
                        if (remainingNanos <= 0) {
                            throw new RuntimeException("Timeout during waiting runnables.");
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                return mRunnables.get(i);
            } finally {
                mLock.unlock();
            }
        }
    }

    @Test
    public void testSetTextFuture() {
        final ManualExecutor executor = new ManualExecutor();

        mActivityTestRule.getScenario().onActivity(activity -> {
            AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.setText(""); // Make the measured width to be zero.
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), executor));
        });

        executor.doExecution(0);

        mActivityTestRule.getScenario().onActivity(activity -> {
            AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.measure(UNLIMITED_MEASURE_SPEC, UNLIMITED_MEASURE_SPEC);

            // 0 is a initial value of the text view width of this test. setTextFuture should
            // block and set text inside measure method. So, the result of measurment should not
            // be zero
            assertNotEquals(0.0f, tv.getMeasuredWidth());
            // setText may wrap the given text with SpannedString. Check the contents by casting
            // to String.
            assertEquals(SAMPLE_TEXT_1, tv.getText().toString());
        });
    }

    @Ignore // Test blocks forever.
    @Test
    public void testSetTextAsync_getTextBlockingTest() {
        final ManualExecutor executor = new ManualExecutor();
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.setText(""); // Make the measured width to be zero.
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), executor));
            tv.measure(UNLIMITED_MEASURE_SPEC, UNLIMITED_MEASURE_SPEC);
            assertNotEquals(0.0f, tv.getMeasuredWidth());
            assertEquals(SAMPLE_TEXT_1, tv.getText().toString());
        });
        executor.doExecution(0);
    }

    @Test
    public void testSetTextAsync_executionOrder() {
        final ManualExecutor executor = new ManualExecutor();
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.setText(""); // Make the measured width to be zero.
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), executor));
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_2, tv.getTextMetricsParamsCompat(), executor));
        });
        executor.doExecution(1);  // Do execution of 2nd runnable.
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.measure(UNLIMITED_MEASURE_SPEC, UNLIMITED_MEASURE_SPEC);
            assertNotEquals(0.0f, tv.getMeasuredWidth());
            // setText may wrap the given text with SpannedString. Check the contents by casting
            // to String.
            assertEquals(SAMPLE_TEXT_2, tv.getText().toString());
        });
        executor.doExecution(0);  // Do execution of 1st runnable.
        // Even the previous setTextAsync finishes after the next setTextAsync, the last one should
        // be displayed.
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            // setText may wrap the given text with SpannedString. Check the contents by casting
            // to String.
            assertEquals(SAMPLE_TEXT_2, tv.getText().toString());
        });
    }

    @Test
    public void testSetTextAsync_directionDifference() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            activity.setContentView(R.layout.appcompat_textview_rtl);
            final ViewGroup container = activity.findViewById(R.id.container);
            final AppCompatTextView tv = activity.findViewById(R.id.text_view_rtl);
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), null));
            container.measure(UNLIMITED_MEASURE_SPEC, UNLIMITED_MEASURE_SPEC);
        });
    }

    @Test
    public void testSetTextAsync_createAndAttach() {
        mActivityTestRule.getScenario().onActivity(activity -> {
            activity.setContentView(R.layout.appcompat_textview_rtl);
            final ViewGroup container = activity.findViewById(R.id.container);

            final AppCompatTextView tv = new AppCompatTextView(activity);
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), null));
            container.addView(tv);
            container.measure(UNLIMITED_MEASURE_SPEC, UNLIMITED_MEASURE_SPEC);
        });
    }

    @Test
    public void testSetTextAsync_executionOrder_withNull() {
        final ManualExecutor executor = new ManualExecutor();
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.setText(""); // Make the measured width to be zero.
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), executor));
            tv.setTextFuture(null);
        });
        executor.doExecution(0);  // Do execution of 1st runnable.
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.measure(UNLIMITED_MEASURE_SPEC, UNLIMITED_MEASURE_SPEC);
            // The setTextFuture was reset by passing null.
            assertEquals(0.0f, tv.getMeasuredWidth(), 0.0f);
        });
    }

    @Test
    public void testSetTextAsync_throwExceptionAfterSetTextFuture() {
        final ManualExecutor executor = new ManualExecutor();
        mActivityTestRule.getScenario().onActivity(activity -> {
            final AppCompatTextView tv = activity.findViewById(R.id.textview_set_text_async);
            tv.setText(""); // Make the measured width to be zero.
            tv.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    SAMPLE_TEXT_1, tv.getTextMetricsParamsCompat(), executor));
            tv.setTextSize(tv.getTextSize() * 2.0f + 1.0f);
            executor.doExecution(0);

            // setText may wrap the given text with SpannedString. Check the contents by casting
            // to String.
            try {
                tv.getText();
                fail();
            } catch (IllegalArgumentException e) {
                // pass
            }
        });
    }

    @Test
    public void testHyphenationFrequencyDefaultValue_withDefaultConstructor() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        assertEquals(Layout.HYPHENATION_FREQUENCY_NONE, textView.getHyphenationFrequency());
    }

    @Test
    public void testHyphenationFrequencyDefaultValue_withInflator() {
        final AppCompatTextView textView = mActivity.findViewById(R.id.text_view_default_values);
        assertEquals(Layout.HYPHENATION_FREQUENCY_NONE, textView.getHyphenationFrequency());
    }

    @Test
    public void testHyphenationFrequencyOverride_withInflator() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_hyphen_break_override);
        assertEquals(Layout.HYPHENATION_FREQUENCY_FULL, textView.getHyphenationFrequency());
    }

    @Test
    public void testBreakStrategyDefaultValue_withDefaultConstructor() {
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        assertEquals(Layout.BREAK_STRATEGY_HIGH_QUALITY, textView.getBreakStrategy());
    }

    @Test
    public void testBreakStrategyDefaultValue_withInflator() {
        final AppCompatTextView textView = mActivity.findViewById(R.id.text_view_default_values);
        assertEquals(Layout.BREAK_STRATEGY_HIGH_QUALITY, textView.getBreakStrategy());
    }

    @Test
    public void testBreakStrategyOverride_withInflator() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_hyphen_break_override);
        assertEquals(Layout.BREAK_STRATEGY_BALANCED, textView.getBreakStrategy());
    }

    @Test
    public void testCompoundDrawablesCompat() {
        // Given an ACTV with drawable[Left,Top,Right,Bottom]Compat set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat);
        // Then all 4 drawables should be present
        final Drawable[] compoundDrawables = textView.getCompoundDrawables();
        assertNotNull(compoundDrawables[0]);
        assertNotNull(compoundDrawables[1]);
        assertNotNull(compoundDrawables[2]);
        assertNotNull(compoundDrawables[3]);
    }

    @Test
    public void testCompoundDrawablesCompat_relative() {
        // Given an ACTV with both drawableStartCompat and drawableEndCompat set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_relative);
        // Then both should be present
        final Drawable[] compoundDrawablesRelative = textView.getCompoundDrawablesRelative();
        assertNotNull(compoundDrawablesRelative[0]);
        assertNotNull(compoundDrawablesRelative[2]);
    }

    @Test
    public void testCompoundDrawablesCompat_relativeAndAbsolute() {
        // Given an ACTV with both drawableStartCompat and drawableRightCompat set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_relative_and_absolute);
        // Then the start drawable should be present
        assertNotNull(textView.getCompoundDrawablesRelative()[0]);
        // Then the absolute right drawable should be ignored
        assertNull(textView.getCompoundDrawables()[2]);
    }

    @Test
    public void testCompoundDrawablesCompat_overridesPlatform() {
        // Given an ACTV with both a raster android:drawableLeft & a vector app:drawableLeftCompat
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_and_platform_same);
        boolean isVector = false;
        // Then the left drawable should be present & should be a vector i.e. from the compat attr
        final Drawable drawableLeft = textView.getCompoundDrawables()[0];
        assertNotNull(drawableLeft);
        isVector = drawableLeft instanceof VectorDrawableCompat
                || drawableLeft instanceof VectorDrawable;
        assertTrue(isVector);
    }

    @Test
    public void testCompoundDrawablesCompat_coexistPlatform() {
        // Given an ACTV with app:drawableTopCompat & android:drawableBottom set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_and_platform_mix);
        // Then both should be present
        final Drawable[] compoundDrawables = textView.getCompoundDrawables();
        assertNotNull(compoundDrawables[1]);
        assertNotNull(compoundDrawables[3]);
    }

    @Test
    public void testCompoundDrawablesRelative_platformCompatCoexist() {
        // Given an ACTV with app:drawableStartCompat & android:drawableEnd set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_and_platform_relative_mix);
        // Then both should be present
        final Drawable[] compoundDrawables = textView.getCompoundDrawablesRelative();
        assertNotNull(compoundDrawables[0]);
        assertNotNull(compoundDrawables[2]);
    }

    @Test
    public void testCompoundDrawables_relativePlatform_ignoresCompatAbsolute() {
        // Given an ACTV with app:drawableLeftCompat & android:drawableEnd set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_abs_platform_relative);
        // Then the relative drawable is present
        assertNotNull(textView.getCompoundDrawablesRelative()[2]);
        // Then the absolute drawable is ignored
        assertNull(textView.getCompoundDrawablesRelative()[0]);
    }

    @Test
    public void testCompoundDrawables_relativeCompat_ignoresPlatformAbsolute() {
        // Given an ACTV with app:drawableStartCompat & android:drawableRight set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawables_compat_relative_platform_abs);
        // Then the relative drawable is present
        assertNotNull(textView.getCompoundDrawablesRelative()[0]);
        // Then the absolute drawable is ignored
        assertNull(textView.getCompoundDrawablesRelative()[2]);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testGetTextClassifier() {
        AppCompatTextView textView = mContainer.findViewById(R.id.textview_simple);
        textView.getTextClassifier();
        NoOpTextClassifier noOpTextClassifier = new NoOpTextClassifier();

        TextClassificationManager textClassificationManager =
                mActivity.getSystemService(TextClassificationManager.class);
        textClassificationManager.setTextClassifier(noOpTextClassifier);

        assertEquals(noOpTextClassifier, textView.getTextClassifier());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testSetTextClassifier() {
        final AppCompatTextView textview = new AppCompatTextView(mActivity);
        NoOpTextClassifier noOpTextClassifier = new NoOpTextClassifier();

        textview.setTextClassifier(noOpTextClassifier);

        assertEquals(noOpTextClassifier, textview.getTextClassifier());
    }

    @RequiresApi(26)
    private static class NoOpTextClassifier implements TextClassifier {}

    class TestCase {
        public final int id;
        public final char expected3EMChar;

        TestCase(int id, char expected3EMChar) {
            this.id = id;
            this.expected3EMChar = expected3EMChar;
        }
    }

    /**
     * Find a character which has 3em width.
     *
     * The search range is from 'a' to 'r'.
     * @param p a paint
     * @return the character which has 3em width.
     */
    private static char find3EmChar(Paint p) {
        char threeEmChar = '\0';
        p.setTextSize(100.0f);  // Make 1EM = 100px
        for (char c = 'a'; c <= 'r'; c++) {
            final float charWidth = p.measureText(new char[] { c }, 0, 1);
            if (charWidth != 100.0f && charWidth != 300.0f) {
                throw new RuntimeException("Char width must be 1em or 3em. Test setup error?");
            }

            if (charWidth == 300.0f) {
                if (threeEmChar != '\0') {
                    throw new RuntimeException(
                            "two or more 3em character found. Test setup error?");
                }
                threeEmChar = c;
            }
        }
        if (threeEmChar == '\0') {
            throw new RuntimeException("No 3em character found. Test setup error?");
        }
        return threeEmChar;
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    @UiThreadTest
    public void testFontWeight() {
        // For the details of the font files, see comments in multiweight_family.xml
        TestCase[] testCases = {
                new TestCase(R.id.textview_family_selection_weight_100_upright, 'a'),
                new TestCase(R.id.textview_family_selection_weight_100_italic, 'b'),
                new TestCase(R.id.textview_family_selection_weight_200_upright, 'c'),
                new TestCase(R.id.textview_family_selection_weight_200_italic, 'd'),
                new TestCase(R.id.textview_family_selection_weight_300_upright, 'e'),
                new TestCase(R.id.textview_family_selection_weight_300_italic, 'f'),
                new TestCase(R.id.textview_family_selection_weight_400_upright, 'g'),
                new TestCase(R.id.textview_family_selection_weight_400_italic, 'h'),
                new TestCase(R.id.textview_family_selection_weight_500_upright, 'i'),
                new TestCase(R.id.textview_family_selection_weight_500_italic, 'j'),
                new TestCase(R.id.textview_family_selection_weight_600_upright, 'k'),
                new TestCase(R.id.textview_family_selection_weight_600_italic, 'l'),
                new TestCase(R.id.textview_family_selection_weight_700_upright, 'm'),
                new TestCase(R.id.textview_family_selection_weight_700_italic, 'n'),
                new TestCase(R.id.textview_family_selection_weight_800_upright, 'o'),
                new TestCase(R.id.textview_family_selection_weight_800_italic, 'p'),
                new TestCase(R.id.textview_family_selection_weight_900_upright, 'q'),
                new TestCase(R.id.textview_family_selection_weight_900_italic, 'r'),
        };

        mActivity.setContentView(R.layout.appcompat_textview_family_selection);

        for (TestCase testCase : testCases) {
            final AppCompatTextView tv = mActivity.findViewById(testCase.id);

            final Paint p = tv.getPaint();
            char actual3EMChar = find3EmChar(p);

            final String msg = "Expected 3em character is " + testCase.expected3EMChar
                    + " but actual 3em character is " + actual3EMChar;
            assertEquals(msg, testCase.expected3EMChar, actual3EMChar);
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    @UiThreadTest
    public void testFontWeight_styleConflict() {
        mActivity.setContentView(R.layout.appcompat_textview_family_selection);
        final AppCompatTextView tv = mActivity.findViewById(
                R.id.textview_family_selection_style_conflict);
        // The layout has both textFontWeight=400 and textStyle=bold. The textFontWeight is used if
        // these two attributes are conflict.
        final char actual = find3EmChar(tv.getPaint());
        final String msg = "Expected 3em character is 'g' but actual 3em character is " + actual;
        assertEquals(msg, 'g', find3EmChar(tv.getPaint()));
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    @UiThreadTest
    public void testFontWeight_styleConflict_italic_preserve() {
        mActivity.setContentView(R.layout.appcompat_textview_family_selection);
        final AppCompatTextView tv = mActivity.findViewById(
                R.id.textview_family_selection_style_conflict_italic_preserve);
        // The layout has both textFontWeight=400 and textStyle=bold|italic. The textFontWeight is
        // used if these two attributes are conflict, but italic information should be preserved.
        final char actual = find3EmChar(tv.getPaint());
        final String msg = "Expected 3em character is 'h' but actual 3em character is " + actual;
        assertEquals(msg, 'h', find3EmChar(tv.getPaint()));
    }

    @Test
    public void testCompoundDrawablesTint() {
        // Given an ACTV with a white drawableLeftCompat set and a #f0f drawableTint
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint);
        final int tint = 0xffff00ff;
        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(ColorStateList.valueOf(tint),
                TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testCompoundDrawableRelativeTint() {
        // Given an ACTV with a white drawableStartCompat set and a #f0f drawableTint
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_relative_tint);
        final int tint = 0xffff00ff;
        // Then the drawable should be tinted
        final Drawable drawable = TextViewCompat.getCompoundDrawablesRelative(textView)[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(ColorStateList.valueOf(tint),
                TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testCompoundDrawablesTintList() {
        // Given an ACTV with a white drawableLeftCompat and a ColorStateList drawableTint set
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint_list);
        final int defaultTint = ResourcesCompat.getColor(mResources, R.color.lilac_default, null);
        final int disabledTint = ResourcesCompat.getColor(mResources, R.color.lilac_disabled, null);

        // Then the initial drawable tint is applied
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                defaultTint,
                0,
                true);

        // When the view is disabled
        onView(withId(textView.getId())).perform(scrollTo(), setEnabled(false));
        // Then the appropriate drawable tint is applied
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                disabledTint,
                0,
                true);
    }

    @Test
    public void testCompoundDrawablesTintMode() {
        // Given an ACTV with a red drawableLeft, a semi-transparent blue drawableTint
        // & drawableTintMode of src_over
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint_mode);
        final int expected = ColorUtils.compositeColors(0x800000ff, 0xffff0000);
        final int tolerance = 2; // allow some tolerance for the blending
        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                expected,
                tolerance,
                true);
        // Then the TextViewCompat getter returns the mode
        assertEquals(PorterDuff.Mode.SRC_OVER,
                TextViewCompat.getCompoundDrawableTintMode(textView));
    }

    @Test
    public void testSetCompoundDrawablesTintList() {
        // Given an ACTV with a compound drawable
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.white_square), null, null, null);

        // When a tint is set programmatically
        final int tint = 0xffa4c639;
        final ColorStateList tintList = ColorStateList.valueOf(tint);
        TextViewCompat.setCompoundDrawableTintList(textView, tintList);

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(tintList, TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testSetCompoundDrawablesTintMode() {
        // Given an ACTV with a red compound drawable
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.red_square), null, null, null);

        // When a semi-transparent blue tint is set programmatically with a mode of SRC_OVER
        final int tint = 0x800000ff;
        final PorterDuff.Mode mode = PorterDuff.Mode.SRC_OVER;
        final ColorStateList tintList = ColorStateList.valueOf(tint);
        TextViewCompat.setCompoundDrawableTintList(textView, tintList);
        TextViewCompat.setCompoundDrawableTintMode(textView, mode);
        final int expected = ColorUtils.compositeColors(tint, 0xffff0000);
        final int tolerance = 2; // allow some tolerance for the blending

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                expected,
                tolerance,
                true);
        // Then the TextViewCompat getter should return the tint mode
        assertEquals(mode, TextViewCompat.getCompoundDrawableTintMode(textView));
    }


    @Test
    public void testCompoundDrawablesSetAfterTint() {
        // Given an ACTV with a magenta tint
        final AppCompatTextView textView = new AppCompatTextView(mActivity);
        final int tint = 0xffff00ff;
        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(tint));

        // When a white compound drawable is set
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.white_square), null, null, null);

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatTextView compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
    }

    @UiThreadTest
    public void testSetCustomSelectionActionModeCallback() {
        final AppCompatTextView view = new AppCompatTextView(mActivity);
        final ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        };

        // Default value is documented as null.
        assertNull(view.getCustomSelectionActionModeCallback());

        // Setter and getter should be symmetric.
        view.setCustomSelectionActionModeCallback(callback);
        assertEquals(callback, view.getCustomSelectionActionModeCallback());

        // Argument is nullable.
        view.setCustomSelectionActionModeCallback(null);
        assertNull(view.getCustomSelectionActionModeCallback());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    public void testTextFontWeight() {
        final AppCompatTextView textView = mActivity.findViewById(
                R.id.text_view_text_font_weight);

        if (textView.getTypeface() == null) return;

        final int textFontWeight = textView.getTypeface().getWeight();

        assertEquals(textFontWeight, 900);
    }

    @RequiresApi(26)
    private void assertSystemHasVariableFont() {
        AppCompatTextView textView = mActivity.findViewById(R.id.text_view_text_font_adjustment);

        textView.setFontVariationSettings("'wght' 400");
        Bitmap regular = drawToBitmap(textView, Bitmap.Config.ARGB_8888);

        textView.setFontVariationSettings("'wght' 700");
        Bitmap bold = drawToBitmap(textView, Bitmap.Config.ARGB_8888);

        assumeFalse(regular.sameAs(bold));
    }

    @UiThreadTest
    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testFontAdjustment() {
        // Skip if the system default font is not a variable font.
        assertSystemHasVariableFont();

        AppCompatTextView textView = mActivity.findViewById(R.id.text_view_text_font_adjustment);

        textView.setFontVariationSettings("'wght' 400.0");
        Bitmap before = drawToBitmap(textView, Bitmap.Config.ARGB_8888);

        textView.getFontVariationSettingsManager().setFontWeightAdjustmentForTesting(300);
        textView.setFontVariationSettings("'wght' 400.0");
        Bitmap after = drawToBitmap(textView, Bitmap.Config.ARGB_8888);

        // The font weight adjustment should change the weight of the variation settings.
        assertFalse(before.sameAs(after));
    }
}
