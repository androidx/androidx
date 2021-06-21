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

package androidx.wear.watchface.complications.rendering;

import static android.support.wearable.complications.ComplicationData.TYPE_ICON;
import static android.support.wearable.complications.ComplicationData.TYPE_LONG_TEXT;
import static android.support.wearable.complications.ComplicationData.TYPE_SMALL_IMAGE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.Icon.OnDrawableLoadedListener;
import android.graphics.drawable.VectorDrawable;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.test.core.app.ApplicationProvider;
import androidx.wear.watchface.complications.rendering.ComplicationRenderer.OnInvalidateListener;
import androidx.wear.watchface.complications.rendering.ComplicationRenderer.PaintSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ComplicationRenderer}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationRendererTest {

    private static final long REFERENCE_TIME = 1234567890123L; // Fri, 13 Feb 2009 23:31:30.123 GMT
    private static final int BOUNDS_WIDTH = 100;
    private static final int BOUNDS_HEIGHT = 100;

    private ComplicationRenderer mComplicationRenderer;
    private Rect mComplicationBounds;

    @Mock
    private Icon mMockIcon;
    @Mock
    private Icon mMockBurnInProtectionIcon;
    @Mock
    private Icon mMockSmallImage;
    @Mock
    private Icon mMockBurnInProtectionSmallImage;
    @Mock
    private Canvas mMockCanvas;
    @Mock
    private OnInvalidateListener mMockInvalidateListener;
    private final Resources mResurces = ApplicationProvider.getApplicationContext().getResources();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mComplicationBounds = new Rect(0, 0, BOUNDS_WIDTH, BOUNDS_HEIGHT);

        mComplicationRenderer = createRendererWithBounds(mComplicationBounds);
        mComplicationRenderer.setOnInvalidateListener(mMockInvalidateListener);
    }

    @Test
    public void debugModeIsOff() {
        assertThat(ComplicationRenderer.DEBUG_MODE).isFalse();
    }

    @Test
    public void typeNoDataIsDisplayedAsSetNoDataText() {
        String noDataText = "No data";
        mComplicationRenderer.setNoDataText(noDataText);
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build(), true);
        assertThat(mComplicationRenderer.getComplicationData().getType())
                .isEqualTo(ComplicationData.TYPE_SHORT_TEXT);
        assertThat(mComplicationRenderer
                .getComplicationData()
                .getShortText()
                .getTextAt(mResurces, REFERENCE_TIME))
                .isEqualTo(noDataText);
    }

    @Test
    public void setNoDataTextWithStringUpdatesComplicationData() {
        String firstText = "First";
        String secondText = "Second";

        mComplicationRenderer.setNoDataText(firstText);
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build(), true);
        CharSequence firstResult =
                mComplicationRenderer
                        .getComplicationData()
                        .getShortText()
                        .getTextAt(mResurces, REFERENCE_TIME);

        mComplicationRenderer.setNoDataText(secondText);
        CharSequence secondResult =
                mComplicationRenderer
                        .getComplicationData()
                        .getShortText()
                        .getTextAt(mResurces, REFERENCE_TIME);

        assertThat(firstResult).isEqualTo(firstText);
        assertThat(secondResult).isEqualTo(secondText);
    }

    @Test
    public void setNoDataTextWithSpannableStringUpdatesComplicationData() {
        SpannableString text = new SpannableString("Text");
        ForegroundColorSpan redSpan = new ForegroundColorSpan(Color.RED);
        ForegroundColorSpan blueSpan = new ForegroundColorSpan(Color.BLUE);
        text.setSpan(redSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mComplicationRenderer.setNoDataText(text);
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build(), true);
        CharSequence firstResult =
                mComplicationRenderer
                        .getComplicationData()
                        .getShortText()
                        .getTextAt(mResurces, REFERENCE_TIME);

        text.setSpan(blueSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mComplicationRenderer.setNoDataText(text);
        CharSequence secondResult =
                mComplicationRenderer
                        .getComplicationData()
                        .getShortText()
                        .getTextAt(mResurces, REFERENCE_TIME);

        assertThat(text).isEqualTo(secondResult);
        assertThat(firstResult).isNotEqualTo(secondResult);
    }

    @Test
    public void modifyingNoDataTextOutsideDoNotUpdateStoredNoDataText() {
        SpannableString text = new SpannableString("Text");
        ForegroundColorSpan redSpan = new ForegroundColorSpan(Color.RED);
        ForegroundColorSpan blueSpan = new ForegroundColorSpan(Color.BLUE);

        text.setSpan(redSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mComplicationRenderer.setNoDataText(text);

        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build(), true);
        CharSequence firstResult =
                mComplicationRenderer
                        .getComplicationData()
                        .getShortText()
                        .getTextAt(mResurces, REFERENCE_TIME);

        text.setSpan(blueSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build(), true);
        CharSequence secondResult =
                mComplicationRenderer
                        .getComplicationData()
                        .getShortText()
                        .getTextAt(mResurces, REFERENCE_TIME);

        assertThat(firstResult).isEqualTo(secondResult);
        assertThat(text).isNotEqualTo(firstResult);
    }

    @Test
    public void componentBoundsAreNotRecalculatedWhenSizeDoesNotChange() {
        // GIVEN an icon type complication and bounds
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON).setIcon(mMockIcon).build(), true);
        // WHEN complication size changes
        mComplicationBounds.inset(10, 10);
        boolean firstCallResult = mComplicationRenderer.setBounds(mComplicationBounds);
        // AND then bounds change while keeping the size same
        mComplicationBounds.offset(10, 10);
        boolean secondCallResult = mComplicationRenderer.setBounds(mComplicationBounds);
        // THEN layout helper recalculates when size change, and does not recalculate when it
        // doesn't.
        assertThat(firstCallResult).isTrue();
        assertThat(secondCallResult).isFalse();
    }

    @Test
    public void paintSetsAreReinitializedWhenAmbientPropertiesChange() {
        // GIVEN a complication renderer
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON).setIcon(mMockIcon).build(), true);
        // WHEN complication is drawn in ambient mode
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, true, true, false);
        ComplicationRenderer.PaintSet firstPaintSet = mComplicationRenderer.mAmbientPaintSet;
        // AND ambient properties change (burn in protection is disabled)
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, true, false, false);
        ComplicationRenderer.PaintSet secondPaintSet = mComplicationRenderer.mAmbientPaintSet;
        // AND ambient properties change again (low bit ambient is disabled)
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, false, false);
        ComplicationRenderer.PaintSet thirdPaintSet = mComplicationRenderer.mAmbientPaintSet;
        // THEN different paint sets are used for every call
        assertThat(
                firstPaintSet.mIsAmbientStyle
                        && firstPaintSet.mLowBitAmbient
                        && firstPaintSet.mBurnInProtection)
                .isTrue();
        assertThat(firstPaintSet).isNotEqualTo(secondPaintSet);
        assertThat(
                secondPaintSet.mIsAmbientStyle
                        && secondPaintSet.mLowBitAmbient
                        && !secondPaintSet.mBurnInProtection)
                .isTrue();
        assertThat(secondPaintSet).isNotEqualTo(thirdPaintSet);
        assertThat(
                thirdPaintSet.mIsAmbientStyle
                        && !thirdPaintSet.mLowBitAmbient
                        && !thirdPaintSet.mBurnInProtection)
                .isTrue();
    }

    @Test
    public void lowBitAmbientPaintSetUsesAppropriateColors() {
        ComplicationStyle activeStyle = new ComplicationStyle();
        ComplicationStyle ambientStyle = new ComplicationStyle();
        ambientStyle.setTextColor(Color.RED);
        ambientStyle.setTitleColor(Color.BLUE);
        ambientStyle.setBackgroundColor(Color.GREEN);
        ambientStyle.setRangedValuePrimaryColor(Color.YELLOW);
        ambientStyle.setRangedValueSecondaryColor(Color.BLUE);
        ambientStyle.setBorderColor(Color.CYAN);
        mComplicationRenderer.updateStyle(activeStyle, ambientStyle);
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON).setIcon(mMockIcon).build(), true);

        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, true, true, false);
        ComplicationRenderer.PaintSet paintSet = mComplicationRenderer.mAmbientPaintSet;

        assertThat(paintSet.mPrimaryTextPaint.getColor()).isEqualTo(Color.WHITE);
        assertThat(paintSet.mSecondaryTextPaint.getColor()).isEqualTo(Color.WHITE);
        assertThat(paintSet.mBackgroundPaint.getColor()).isEqualTo(Color.TRANSPARENT);
        assertThat(paintSet.mInProgressPaint.getColor()).isEqualTo(Color.WHITE);
        assertThat(paintSet.mRemainingPaint.getColor()).isEqualTo(Color.TRANSPARENT);
        assertThat(paintSet.mBorderPaint.getColor()).isEqualTo(Color.WHITE);
    }

    @Test
    public void lowBitAmbientPaintSetRetainsCompatibleColors() {
        ComplicationStyle activeStyle = new ComplicationStyle();
        ComplicationStyle ambientStyle = new ComplicationStyle();
        ambientStyle.setTextColor(Color.BLACK);
        ambientStyle.setTitleColor(Color.BLACK);
        ambientStyle.setBackgroundColor(Color.BLACK);
        ambientStyle.setRangedValuePrimaryColor(Color.YELLOW);
        ambientStyle.setRangedValueSecondaryColor(Color.BLACK);
        ambientStyle.setBorderColor(Color.TRANSPARENT);
        mComplicationRenderer.updateStyle(activeStyle, ambientStyle);
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON).setIcon(mMockIcon).build(), true);

        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, true, true, false);
        ComplicationRenderer.PaintSet paintSet = mComplicationRenderer.mAmbientPaintSet;

        assertThat(paintSet.mPrimaryTextPaint.getColor()).isEqualTo(Color.WHITE);
        assertThat(paintSet.mSecondaryTextPaint.getColor()).isEqualTo(Color.WHITE);
        assertThat(paintSet.mBackgroundPaint.getColor()).isEqualTo(Color.BLACK);
        assertThat(paintSet.mInProgressPaint.getColor()).isEqualTo(Color.WHITE);
        assertThat(paintSet.mRemainingPaint.getColor()).isEqualTo(Color.BLACK);
        assertThat(paintSet.mBorderPaint.getColor()).isEqualTo(Color.TRANSPARENT);
    }

    @Test
    public void nothingIsDrawnForEmptyAndNotConfiguredTypes() {
        // GIVEN a complication renderer with not configured data
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NOT_CONFIGURED).build(), true);
        // WHEN complication is drawn onto a canvas
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);
        // AND complication data is changed to empty
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_EMPTY).build(), true);
        // AND complication is drawn again
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);
        // THEN nothing is drawn on canvas
        verifyZeroInteractions(mMockCanvas);
    }

    @Test
    public void nothingIsDrawnForInactiveData() {
        // GIVEN a complication renderer with data that has passed its end time
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("hello"))
                        .setEndDateTimeMillis(REFERENCE_TIME - 100)
                        .build(),
                true);

        // WHEN complication is drawn onto a canvas
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);

        // THEN nothing is drawn on canvas
        verifyZeroInteractions(mMockCanvas);
    }

    @Test
    public void somethingIsDrawnForActiveData() {
        // GIVEN a complication renderer with data that has passed its end time
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("hello"))
                        .setStartDateTimeMillis(REFERENCE_TIME - 5000)
                        .setEndDateTimeMillis(REFERENCE_TIME + 2000)
                        .build(),
                true);

        // WHEN complication is drawn onto a canvas
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);

        // THEN the complication is drawn on the canvas
        verify(mMockCanvas, atLeastOnce()).drawRoundRect(any(), anyFloat(), anyFloat(), any());
    }

    @Test
    public void getImageBorderRadiusForImageWithNoPadding() {
        // GIVEN a complication style with an arbitrary border radius
        int radius = 15;
        ComplicationStyle style = new ComplicationStyle();
        style.setBorderRadius(radius);
        mComplicationRenderer.updateStyle(style, style);
        // AND an image with no padding
        Rect imageBounds = new Rect(mComplicationBounds);
        imageBounds.offsetTo(0, 0);
        // THEN image has the same radius as the complication
        assertThat(mComplicationRenderer.getImageBorderRadius(style, imageBounds))
                .isEqualTo(radius);
    }

    @Test
    public void getImageBorderRadiusForImageWithPadding() {
        // GIVEN a complication style with an arbitrary border radius
        int radius = 15;
        int padding = 5;
        ComplicationStyle style = new ComplicationStyle();
        style.setBorderRadius(radius);
        mComplicationRenderer.updateStyle(style, style);
        // AND an image with padding in both directions
        Rect imageBounds = new Rect(mComplicationBounds);
        imageBounds.offsetTo(0, 0);
        imageBounds.inset(padding, padding);
        // THEN image has reduced radius
        assertThat(mComplicationRenderer.getImageBorderRadius(style, imageBounds))
                .isEqualTo(radius - padding);
    }

    @Test
    public void getImageBorderRadiusForImagesWithDifferentHorizontalAndVerticalPadding() {
        // GIVEN a complication style with an arbitrary border radius
        int radius = 15;
        int largePadding = 5;
        int smallPadding = 3;
        ComplicationStyle style = new ComplicationStyle();
        style.setBorderRadius(radius);
        // AND an image with larger horizontal padding
        Rect horizontalPadding = new Rect(mComplicationBounds);
        horizontalPadding.offsetTo(0, 0); // Element bounds should be relative to (0,0)
        horizontalPadding.inset(largePadding, smallPadding);
        // AND an image with larger vertical padding
        Rect verticalPadding = new Rect(mComplicationBounds);
        verticalPadding.offsetTo(0, 0); // Element bounds should be relative to (0,0)
        verticalPadding.inset(smallPadding, largePadding);
        // THEN both images have the reduced radius based on smaller padding
        assertThat(mComplicationRenderer.getImageBorderRadius(style, horizontalPadding))
                .isEqualTo(radius - smallPadding);
        assertThat(mComplicationRenderer.getImageBorderRadius(style, verticalPadding))
                .isEqualTo(radius - smallPadding);
    }

    @Test
    public void rangedValueIsDrawnCorrectlyInActiveMode() {
        // GIVEN a complication renderer with ranged value complication data
        int min = 0;
        int max = 100;
        int value = (max - min) / 2;
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(value)
                        .setRangedMinValue(min)
                        .setRangedMaxValue(max)
                        .build(),
                true);
        // WHEN the complication is drawn in active mode
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);
        int gap = ComplicationRenderer.STROKE_GAP_IN_DEGREES;
        int start = ComplicationRenderer.RANGED_VALUE_START_ANGLE;
        // THEN ranged value is drawn correctly as two parts and with correct paints
        verify(mMockCanvas)
                .drawArc(
                        any(),
                        eq((float) start + gap / 2.0f),
                        eq(180.0f - gap),
                        eq(false),
                        eq(mComplicationRenderer.mActivePaintSet.mInProgressPaint));
        verify(mMockCanvas)
                .drawArc(
                        any(),
                        eq(start + 180.0f + gap / 2.0f),
                        eq(180.0f - gap),
                        eq(false),
                        eq(mComplicationRenderer.mActivePaintSet.mRemainingPaint));
    }

    @Test
    public void rangedValueIsDrawnCorrectlyInAmbientMode() {
        // GIVEN a complication renderer with ranged value complication data
        int min = 0;
        int max = 100;
        int value = (max - min) / 2;
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setRangedValue(value)
                        .setRangedMinValue(min)
                        .setRangedMaxValue(max)
                        .build(),
                true);
        // WHEN the complication is drawn in ambient mode
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, false, false);
        int gap = ComplicationRenderer.STROKE_GAP_IN_DEGREES;
        int start = ComplicationRenderer.RANGED_VALUE_START_ANGLE;
        // THEN ranged value is drawn correctly as only the in progress part and with correct paint
        verify(mMockCanvas)
                .drawArc(
                        any(),
                        eq((float) start + gap / 2.0f),
                        eq(180.0f - gap),
                        eq(false),
                        eq(mComplicationRenderer.mAmbientPaintSet.mInProgressPaint));
        verify(mMockCanvas)
                .drawArc(
                        any(),
                        eq(start + 180.0f + gap / 2.0f),
                        eq(180.0f - gap),
                        eq(false),
                        eq(mComplicationRenderer.mAmbientPaintSet.mRemainingPaint));
    }

    @Test
    public void bordersBackgroundAndHighlightAreDrawn() {
        // GIVEN a complication renderer with short text data
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("Test text"))
                        .build(),
                true);
        // AND with a style that has borders and border radius
        float radius = 5;
        int borderWidth = 2;
        ComplicationStyle newStyle = new ComplicationStyle();
        newStyle.setBorderStyle(ComplicationStyle.BORDER_STYLE_SOLID);
        newStyle.setBorderRadius((int) radius);
        newStyle.setBorderWidth(borderWidth);
        mComplicationRenderer.updateStyle(
                newStyle,
                new ComplicationStyle());
        // WHEN the complication is drawn in active mode and as highlighted
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, true);
        RectF bounds = new RectF(mComplicationBounds);
        bounds.offsetTo(0, 0);
        // THEN background, borders and highlight are drawn
        verify(mMockCanvas)
                .drawRoundRect(
                        eq(bounds),
                        eq(radius),
                        eq(radius),
                        eq(mComplicationRenderer.mActivePaintSet.mHighlightPaint));
        verify(mMockCanvas)
                .drawRoundRect(
                        eq(bounds),
                        eq(radius),
                        eq(radius),
                        eq(mComplicationRenderer.mActivePaintSet.mBorderPaint));
        verify(mMockCanvas)
                .drawRoundRect(
                        eq(bounds),
                        eq(radius),
                        eq(radius),
                        eq(mComplicationRenderer.mActivePaintSet.mBackgroundPaint));
    }

    @Test
    public void highlightIsNotDrawnInAmbientMode() {
        // GIVEN a complication renderer with short text data
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("Test text"))
                        .build(),
                true);
        // WHEN the complication is drawn in ambient mode and as highlighted
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, false, true);
        RectF bounds = new RectF(mComplicationBounds);
        bounds.offsetTo(0, 0);
        // THEN highlight is not drawn
        verify(mMockCanvas, times(0))
                .drawRoundRect(
                        eq(bounds),
                        anyFloat(),
                        anyFloat(),
                        eq(mComplicationRenderer.mActivePaintSet.mHighlightPaint));
    }

    @Test
    public void antiAliasIsDisabledWhenInLowBitAmbientMode() {
        // GIVEN a complication renderer with short text data
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("Test text"))
                        .build(),
                true);
        // WHEN the complication is drawn in low bit ambient mode
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, true, false, false);
        assertThat(mComplicationRenderer.mAmbientPaintSet.mPrimaryTextPaint.isAntiAlias())
                .isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mSecondaryTextPaint.isAntiAlias())
                .isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mInProgressPaint.isAntiAlias()).isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mRemainingPaint.isAntiAlias()).isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mBorderPaint.isAntiAlias()).isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mBackgroundPaint.isAntiAlias()).isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mHighlightPaint.isAntiAlias()).isFalse();
    }

    @Test
    public void antiAliasIsEnabledWhenNotInLowBitAmbientMode() {
        // GIVEN a complication renderer with short text data
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("Test text"))
                        .build(),
                true);
        // WHEN the complication is drawn in low bit ambient mode
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, false, false);
        // THEN paints in ambient paint set has anti alias enabled
        assertThat(mComplicationRenderer.mAmbientPaintSet.mIsAmbientStyle).isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mLowBitAmbient).isFalse();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mPrimaryTextPaint.isAntiAlias()).isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mSecondaryTextPaint.isAntiAlias())
                .isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mInProgressPaint.isAntiAlias()).isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mRemainingPaint.isAntiAlias()).isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mBorderPaint.isAntiAlias()).isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mBackgroundPaint.isAntiAlias()).isTrue();
        assertThat(mComplicationRenderer.mAmbientPaintSet.mHighlightPaint.isAntiAlias()).isTrue();
    }

    @Test
    public void paintSetHasCorrectColorsAndValues() {
        // GIVEN a complication style
        Typeface textTypeface = Typeface.create("sans-serif", Typeface.NORMAL);
        Typeface titleTypeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        ComplicationStyle style = new ComplicationStyle();
        style.setBackgroundColor(0);
        style.setTextColor(1);
        style.setTextTypeface(textTypeface);
        style.setTextSize(2);
        style.setTitleColor(3);
        style.setTitleTypeface(titleTypeface);
        style.setTitleSize(4);
        style.setIconColor(5);
        style.setBorderStyle(ComplicationStyle.BORDER_STYLE_SOLID);
        style.setBorderColor(6);
        style.setBorderRadius(7);
        style.setBorderWidth(8);
        style.setBorderDashWidth(9);
        style.setBorderDashGap(10);
        style.setRangedValueRingWidth(11);
        style.setRangedValuePrimaryColor(12);
        style.setRangedValueSecondaryColor(13);
        style.setHighlightColor(14);
        // WHEN a paint set is initialized using that style
        PaintSet paintSet = new PaintSet(style, false, false, false);
        // THEN values from style are used when constructing paints
        assertThat(paintSet.mBackgroundPaint.getColor()).isEqualTo(style.getBackgroundColor());
        assertThat(paintSet.mPrimaryTextPaint.getColor()).isEqualTo(style.getTextColor());
        assertThat(paintSet.mPrimaryTextPaint.getTypeface()).isEqualTo(style.getTextTypeface());
        assertThat(paintSet.mPrimaryTextPaint.getTextSize()).isEqualTo((float) style.getTextSize());
        assertThat(paintSet.mSecondaryTextPaint.getColor()).isEqualTo(style.getTitleColor());
        assertThat(paintSet.mSecondaryTextPaint.getTypeface()).isEqualTo(style.getTitleTypeface());
        assertThat(paintSet.mSecondaryTextPaint.getTextSize())
                .isEqualTo((float) style.getTitleSize());
        assertThat(paintSet.mBorderPaint.getColor()).isEqualTo(style.getBorderColor());
        assertThat(paintSet.mBorderPaint.getStrokeWidth())
                .isEqualTo((float) style.getBorderWidth());
        assertThat(paintSet.mInProgressPaint.getStrokeWidth())
                .isEqualTo((float) style.getRangedValueRingWidth());
        assertThat(paintSet.mInProgressPaint.getColor())
                .isEqualTo(style.getRangedValuePrimaryColor());
        assertThat(paintSet.mRemainingPaint.getStrokeWidth())
                .isEqualTo((float) style.getRangedValueRingWidth());
        assertThat(paintSet.mRemainingPaint.getColor())
                .isEqualTo(style.getRangedValueSecondaryColor());
        assertThat(paintSet.mHighlightPaint.getColor()).isEqualTo(style.getHighlightColor());
    }

    @Test
    public void paintSetIsInBurnInProtectionMode() {
        // WHEN three paint sets with different properties are initialized
        ComplicationStyle style = new ComplicationStyle();
        PaintSet paintSet = new PaintSet(style, false, false, false);
        PaintSet ambientPaintSet = new PaintSet(style, true, false, false);
        PaintSet ambientBurnInProtectionPaintSet = new PaintSet(style, true, false, true);
        // THEN only the one that is for ambient mode and with burn in protection mode returns true
        assertThat(paintSet.isInBurnInProtectionMode()).isFalse();
        assertThat(ambientPaintSet.isInBurnInProtectionMode()).isFalse();
        assertThat(ambientBurnInProtectionPaintSet.isInBurnInProtectionMode()).isTrue();
    }

    @Test
    public void longTextComplicationWithIconInWideBounds() {
        int radius = 20;
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText("Foo"))
                        .setLongTitle(ComplicationText.plainText("Bar"))
                        .setIcon(mMockIcon)
                        .build(),
                true);
        ComplicationStyle newStyle = new ComplicationStyle();
        newStyle.setBorderRadius(radius);
        mComplicationRenderer.updateStyle(newStyle, new ComplicationStyle());
        mComplicationRenderer.setBounds(new Rect(0, 0, 400, 100)); // Wide bounds
        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);

        Rect iconBounds = mComplicationRenderer.getIconBounds();
        Rect mainTextBounds = mComplicationRenderer.getMainTextBounds();
        Rect subTextBounds = mComplicationRenderer.getSubTextBounds();

        Rect innerBounds = new Rect();
        mComplicationRenderer.getComplicationInnerBounds(innerBounds);

        assertThat(innerBounds.contains(iconBounds)).isTrue();
        assertThat(innerBounds.contains(mainTextBounds)).isTrue();
        assertThat(innerBounds.contains(subTextBounds)).isTrue();
    }

    @Test
    public void burnInProtectionSmallImageIsUsedInBurnInProtectionMode() {
        mComplicationRenderer.setBounds(new Rect(0, 0, 300, 100));
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText("Foo"))
                        .setSmallImage(mMockSmallImage)
                        .setBurnInProtectionSmallImage(mMockBurnInProtectionSmallImage)
                        .build(),
                true);

        Drawable smallImage = loadIconFromMock(mMockSmallImage);
        Drawable burnInProtectionSmallImage = loadIconFromMock(mMockBurnInProtectionSmallImage);

        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, true, false);
        Drawable ambientBurnInDrawable = mComplicationRenderer.getRoundedSmallImage().getDrawable();

        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, false, false);
        Drawable ambientNoBurnInDrawable =
                mComplicationRenderer.getRoundedSmallImage().getDrawable();

        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, false, false, false, false);
        Drawable activeDrawable = mComplicationRenderer.getRoundedSmallImage().getDrawable();

        assertThat(ambientBurnInDrawable).isEqualTo(burnInProtectionSmallImage);
        assertThat(ambientNoBurnInDrawable).isEqualTo(smallImage);
        assertThat(activeDrawable).isEqualTo(smallImage);
    }

    @Test
    public void noSmallImageIsDrawnInAmbientModeIfBurnInProtectionSmallImageIsNotProvided() {
        mComplicationRenderer.setBounds(new Rect(0, 0, 300, 100));
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText("Foo"))
                        .setSmallImage(mMockSmallImage)
                        .build(),
                true);

        loadIconFromMock(mMockSmallImage);

        mComplicationRenderer.draw(mMockCanvas, REFERENCE_TIME, true, false, true, false);

        assertThat(mComplicationRenderer.getRoundedSmallImage().getDrawable()).isNull();
    }

    @Test
    public void iconsAndImagesAreLoadedProperly() {
        mComplicationRenderer.setBounds(new Rect(0, 0, 300, 100));
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText("Foo"))
                        .setIcon(mMockIcon)
                        .setBurnInProtectionIcon(mMockBurnInProtectionIcon)
                        .setSmallImage(mMockSmallImage)
                        .setBurnInProtectionSmallImage(mMockBurnInProtectionSmallImage)
                        .build(),
                true);

        Drawable loadedIcon = loadIconFromMock(mMockIcon);
        Drawable loadedBurnInProtectionIcon = loadIconFromMock(mMockBurnInProtectionIcon);
        Drawable loadedSmallImage = loadIconFromMock(mMockSmallImage);
        Drawable loadedBurnInProtectionSmallImage =
                loadIconFromMock(mMockBurnInProtectionSmallImage);

        assertThat(mComplicationRenderer.getIcon()).isEqualTo(loadedIcon);
        assertThat(mComplicationRenderer.getBurnInProtectionIcon())
                .isEqualTo(loadedBurnInProtectionIcon);
        assertThat(mComplicationRenderer.getSmallImage()).isEqualTo(loadedSmallImage);
        assertThat(mComplicationRenderer.getBurnInProtectionSmallImage())
                .isEqualTo(loadedBurnInProtectionSmallImage);
    }

    @Test
    public void setRangedValueProgressHiddenForShortText() {
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setShortText(ComplicationText.plainText("foo"))
                        .setShortTitle(ComplicationText.plainText("bar"))
                        .setRangedMinValue(1)
                        .setRangedValue(5)
                        .setRangedMaxValue(10)
                        .build(),
                true);
        mComplicationRenderer.setRangedValueProgressHidden(true);

        ComplicationRenderer anotherRenderer =
                createRendererWithBounds(mComplicationRenderer.getBounds());
        anotherRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("foo"))
                        .setShortTitle(ComplicationText.plainText("bar"))
                        .build(),
                true);

        assertThat(anotherRenderer.hasSameLayout(mComplicationRenderer)).isTrue();
    }

    @Test
    public void setRangedValueProgressHiddenForIconOnly() {
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setIcon(mMockIcon)
                        .setRangedMinValue(1)
                        .setRangedValue(5)
                        .setRangedMaxValue(10)
                        .build(),
                true);
        mComplicationRenderer.setRangedValueProgressHidden(true);

        ComplicationRenderer anotherRenderer =
                createRendererWithBounds(mComplicationRenderer.getBounds());
        anotherRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON).setIcon(mMockIcon).build(), true);

        assertThat(anotherRenderer.hasSameLayout(mComplicationRenderer)).isTrue();
    }

    @Test
    public void invalidateCalledWhenIconLoaded() {
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON).setIcon(mMockIcon).build(), true);

        loadIconFromMock(mMockIcon);

        verify(mMockInvalidateListener).onInvalidate();
    }

    @Test
    public void invalidateCalledWhenSmallImageLoaded() {
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_SMALL_IMAGE)
                        .setSmallImage(mMockSmallImage)
                        .build(),
                true);

        loadIconFromMock(mMockSmallImage);

        verify(mMockInvalidateListener).onInvalidate();
    }

    @Test
    public void invalidateCalledWhenBurnInIconLoaded() {
        mComplicationRenderer.setComplicationData(
                new ComplicationData.Builder(TYPE_ICON)
                        .setIcon(mMockIcon)
                        .setBurnInProtectionIcon(mMockBurnInProtectionIcon)
                        .build(),
                true);

        loadIconFromMock(mMockBurnInProtectionIcon);

        verify(mMockInvalidateListener).onInvalidate();
    }

    @Test
    public void singleColorMatrixTransparentStaysTransparent() {
        int input = Color.argb(0, 255, 255, 255);
        int output =
                applyColorMatrix(
                        input, ComplicationRenderer.PaintSet.createSingleColorMatrix(Color.WHITE));

        assertThat(Color.alpha(output)).isEqualTo(0);
    }

    @Test
    public void singleColorMatrixTransparentBlackStaysTransparent() {
        int input = Color.argb(0, 255, 255, 255);
        int output =
                applyColorMatrix(
                        input, ComplicationRenderer.PaintSet.createSingleColorMatrix(Color.BLACK));

        assertThat(Color.alpha(output)).isEqualTo(0);
    }

    @Test
    public void singleColorMatrixMostlyTransparentBecomesTransparent() {
        int input = Color.argb(80, 255, 255, 255);
        int output =
                applyColorMatrix(
                        input, ComplicationRenderer.PaintSet.createSingleColorMatrix(Color.BLACK));

        assertThat(Color.alpha(output)).isEqualTo(0);
    }

    @Test
    public void singleColorMatrixMostlyOpaqueBecomesDesiredColor() {
        int input = Color.argb(200, 5, 200, 150);
        int output =
                applyColorMatrix(
                        input, ComplicationRenderer.PaintSet.createSingleColorMatrix(Color.CYAN));

        assertThat(output).isEqualTo(Color.CYAN);
    }

    @Test
    public void singleColorMatrixSolidColorBecomesDesiredColor() {
        int output =
                applyColorMatrix(
                        Color.YELLOW,
                        ComplicationRenderer.PaintSet.createSingleColorMatrix(Color.CYAN));

        assertThat(output).isEqualTo(Color.CYAN);
    }

    @Test
    public void singleColorMatrixSolidBlackBecomesDesiredColor() {
        int output =
                applyColorMatrix(
                        Color.BLACK,
                        ComplicationRenderer.PaintSet.createSingleColorMatrix(Color.WHITE));

        assertThat(output).isEqualTo(Color.WHITE);
    }

    private Drawable loadIconFromMock(Icon icon) {
        ArgumentCaptor<OnDrawableLoadedListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(OnDrawableLoadedListener.class);
        verify(icon)
                .loadDrawableAsync(
                        eq(ApplicationProvider.getApplicationContext()),
                        listenerArgumentCaptor.capture(),
                        any());

        Drawable loadedDrawable = new VectorDrawable();
        listenerArgumentCaptor.getValue().onDrawableLoaded(loadedDrawable);
        return loadedDrawable;
    }

    private ComplicationRenderer createRendererWithBounds(Rect bounds) {
        ComplicationRenderer renderer =
                new ComplicationRenderer(
                        ApplicationProvider.getApplicationContext(),
                        new ComplicationStyle(),
                        new ComplicationStyle());
        renderer.setBounds(bounds);
        return renderer;
    }

    private static int applyColorMatrix(int input, ColorMatrix matrix) {
        float[] m = matrix.getArray();
        int r = Color.red(input);
        int g = Color.green(input);
        int b = Color.blue(input);
        int a = Color.alpha(input);

        int rOut = clampColor(m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4]);
        int gOut = clampColor(m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9]);
        int bOut = clampColor(m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14]);
        int aOut = clampColor(m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19]);

        return Color.argb(aOut, rOut, gOut, bOut);
    }

    private static int clampColor(float input) {
        return (int) Math.max(0, Math.min(255, input));
    }
}
