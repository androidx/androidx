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

package androidx.wear.complications.rendering;

import static androidx.wear.complications.rendering.ComplicationDrawable.BORDER_STYLE_DASHED;
import static androidx.wear.complications.rendering.ComplicationDrawable.BORDER_STYLE_NONE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.TimeUnit;

/** Tests for {@link ComplicationDrawable}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationDrawableTest {

    private static final int ACTIVE_COLOR = 0x12345678;
    private static final int AMBIENT_COLOR = 0x87654321;
    private static final int ACTIVE_PX = 1;
    private static final int AMBIENT_PX = 1;

    private ComplicationDrawable mComplicationDrawable;
    private ComplicationData mComplicationData;
    private int mDefaultTextSize;

    @Mock
    Canvas mMockCanvas;
    @Mock
    Drawable mMockDrawableActive;
    @Mock
    Drawable mMockDrawableAmbient;
    @Mock
    PendingIntent mMockPendingIntent;
    @Mock
    Drawable.Callback mMockDrawableCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mComplicationDrawable = new ComplicationDrawable();
        mComplicationDrawable.setCallback(mMockDrawableCallback);

        mComplicationData =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("hede"))
                        .build();
        mDefaultTextSize =
                ApplicationProvider.getApplicationContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.complicationDrawable_textSize);
        Robolectric.getForegroundThreadScheduler().pause();
    }

    @Test
    public void callingSetContextWithNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mComplicationDrawable.setContext(null));
    }

    @Test
    public void callingDrawOnCanvasBeforeSetContextThrowsAnException() {
        assertThrows(IllegalStateException.class, () -> mComplicationDrawable.draw(mMockCanvas));
    }

    @Test
    public void callingDrawWithTimeOnCanvasBeforeSetContextThrowsAnException() {
        assertThrows(
                IllegalStateException.class, () -> mComplicationDrawable.draw(mMockCanvas));
    }

    @Test
    public void callingSetComplicationDataBeforeSetContextThrowsAnException() {
        assertThrows(
                IllegalStateException.class,
                () -> mComplicationDrawable.setComplicationData(mComplicationData));
    }

    @Test
    public void setBoundsRectSetsComplicationRendererBounds() {
        Rect bounds = new Rect(100, 200, 400, 800);
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(bounds);
        assertThat(mComplicationDrawable.getComplicationRenderer().getBounds()).isEqualTo(bounds);
    }

    @Test
    public void setBoundsIntIntIntIntSetsComplicationRendererBounds() {
        Rect bounds = new Rect(100, 200, 400, 800);
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
        assertThat(mComplicationDrawable.getComplicationRenderer().getBounds()).isEqualTo(bounds);
    }

    @Test
    public void setContextSetsComplicationRendererBounds() {
        Rect bounds = new Rect(100, 200, 400, 800);
        mComplicationDrawable.setBounds(bounds);
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        assertThat(mComplicationDrawable.getComplicationRenderer().getBounds()).isEqualTo(bounds);
    }

    @Test
    public void callingContextDependentMethodsAfterSetContextDoesNotThrowAnException() {
        // WHEN setContext is called
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        // AND below methods are called afterwards
        mComplicationDrawable.draw(mMockCanvas);
        mComplicationDrawable.setComplicationData(mComplicationData);
        // THEN no exception is thrown
    }

    @Test
    public void setBackgroundColor() {
        mComplicationDrawable.setBackgroundColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setBackgroundColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getBackgroundColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getBackgroundColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setBackgroundDrawable() {
        mComplicationDrawable.setBackgroundDrawableActive(mMockDrawableActive);
        mComplicationDrawable.setBackgroundDrawableAmbient(mMockDrawableAmbient);
        assertThat(mComplicationDrawable.getActiveStyle().getBackgroundDrawable())
                .isEqualTo(mMockDrawableActive);
        assertThat(mComplicationDrawable.getAmbientStyle().getBackgroundDrawable())
                .isEqualTo(mMockDrawableAmbient);
    }

    @Test
    public void setTextColor() {
        mComplicationDrawable.setTextColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setTextColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getTextColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getTextColor()).isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setTitleColor() {
        mComplicationDrawable.setTitleColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setTitleColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getTitleColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getTitleColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setTextTypeface() {
        Typeface activeTf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        Typeface ambientTf = Typeface.create("sans-serif-condensed", Typeface.ITALIC);
        mComplicationDrawable.setTextTypefaceActive(activeTf);
        mComplicationDrawable.setTextTypefaceAmbient(ambientTf);
        assertThat(mComplicationDrawable.getActiveStyle().getTextTypeface()).isEqualTo(activeTf);
        assertThat(mComplicationDrawable.getAmbientStyle().getTextTypeface()).isEqualTo(ambientTf);
    }

    @Test
    public void setTitleTypeface() {
        Typeface activeTf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        Typeface ambientTf = Typeface.create("sans-serif-condensed", Typeface.ITALIC);
        mComplicationDrawable.setTitleTypefaceActive(activeTf);
        mComplicationDrawable.setTitleTypefaceAmbient(ambientTf);
        assertThat(mComplicationDrawable.getActiveStyle().getTitleTypeface()).isEqualTo(activeTf);
        assertThat(mComplicationDrawable.getAmbientStyle().getTitleTypeface()).isEqualTo(ambientTf);
    }

    @Test
    public void setTextSize() {
        mComplicationDrawable.setTextSizeActive(ACTIVE_PX);
        mComplicationDrawable.setTextSizeAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getTextSize()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getTextSize()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setTitleSize() {
        mComplicationDrawable.setTitleSizeActive(ACTIVE_PX);
        mComplicationDrawable.setTitleSizeAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getTitleSize()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getTitleSize()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setIconColor() {
        mComplicationDrawable.setIconColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setIconColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getIconColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getIconColor()).isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setBorderColor() {
        mComplicationDrawable.setBorderColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setBorderColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setBorderRadius() {
        mComplicationDrawable.setBorderRadiusActive(ACTIVE_PX);
        mComplicationDrawable.setBorderRadiusAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderRadius()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderRadius()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setBorderStyle() {
        mComplicationDrawable.setBorderStyleActive(BORDER_STYLE_NONE);
        mComplicationDrawable.setBorderStyleAmbient(ComplicationDrawable.BORDER_STYLE_DASHED);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderStyle())
                .isEqualTo(BORDER_STYLE_NONE);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderStyle())
                .isEqualTo(ComplicationDrawable.BORDER_STYLE_DASHED);
    }

    @Test
    public void setBorderWidth() {
        mComplicationDrawable.setBorderWidthActive(ACTIVE_PX);
        mComplicationDrawable.setBorderWidthAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderWidth()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderWidth()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setBorderDashGap() {
        mComplicationDrawable.setBorderDashGapActive(ACTIVE_PX);
        mComplicationDrawable.setBorderDashGapAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderDashGap()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderDashGap())
                .isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setBorderDashWidth() {
        mComplicationDrawable.setBorderDashWidthActive(ACTIVE_PX);
        mComplicationDrawable.setBorderDashWidthAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderDashWidth())
                .isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderDashWidth())
                .isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setImageColorFilter() {
        ColorFilter activeCF = new PorterDuffColorFilter(ACTIVE_COLOR, Mode.SRC_IN);
        ColorFilter ambientCF = new PorterDuffColorFilter(AMBIENT_COLOR, Mode.SRC_IN);
        mComplicationDrawable.setImageColorFilterActive(activeCF);
        mComplicationDrawable.setImageColorFilterAmbient(ambientCF);
        assertThat(mComplicationDrawable.getActiveStyle().getColorFilter()).isEqualTo(activeCF);
        assertThat(mComplicationDrawable.getAmbientStyle().getColorFilter()).isEqualTo(ambientCF);
    }

    @Test
    public void setRangedValueRingWidth() {
        mComplicationDrawable.setRangedValueRingWidthActive(ACTIVE_PX);
        mComplicationDrawable.setRangedValueRingWidthAmbient(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getRangedValueRingWidth())
                .isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getRangedValueRingWidth())
                .isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setRangedValuePrimaryColor() {
        mComplicationDrawable.setRangedValuePrimaryColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setRangedValuePrimaryColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getRangedValuePrimaryColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getRangedValuePrimaryColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setRangedValueSecondaryColor() {
        mComplicationDrawable.setRangedValueSecondaryColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setRangedValueSecondaryColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getRangedValueSecondaryColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getRangedValueSecondaryColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setHighlightColor() {
        mComplicationDrawable.setHighlightColorActive(ACTIVE_COLOR);
        mComplicationDrawable.setHighlightColorAmbient(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getHighlightColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getHighlightColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void defaultValuesAreLoadedAfterSetContext() {
        mComplicationDrawable = new ComplicationDrawable();
        int textSizeFromConstructor = mComplicationDrawable.getActiveStyle().getTextSize();
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        int textSizeFromResources = mComplicationDrawable.getActiveStyle().getTextSize();
        assertThat(textSizeFromConstructor)
                .isEqualTo(new ComplicationStyle.Builder().build().getTextSize());
        assertThat(textSizeFromResources).isEqualTo(mDefaultTextSize);
    }

    @Test
    public void onTapReturnsFalseIfNoComplicationData() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(null);
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();
    }

    @Test
    public void onTapReturnsFalseIfNoTapAction() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("rofl"))
                        .setShortTitle(ComplicationText.plainText("copter"))
                        .build());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();
    }

    @Test
    public void onTapReturnsFalseIfOutOfBounds() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("rofl"))
                        .setShortTitle(ComplicationText.plainText("copter"))
                        .setTapAction(mMockPendingIntent)
                        .build());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(200, 200)).isFalse();
    }

    @Test
    public void onTapReturnsFalseIfTapActionCanceled() throws CanceledException {
        doThrow(new CanceledException()).when(mMockPendingIntent).send();

        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("rofl"))
                        .setShortTitle(ComplicationText.plainText("copter"))
                        .setTapAction(mMockPendingIntent)
                        .build());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();
    }

    @Test
    public void onTapReturnsTrueIfSuccessfulAndHighlightsComplication() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("rofl"))
                        .setShortTitle(ComplicationText.plainText("copter"))
                        .setTapAction(mMockPendingIntent)
                        .build());
        reset(mMockDrawableCallback);
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isTrue();
        assertThat(mComplicationDrawable.getHighlighted()).isTrue();
        verify(mMockDrawableCallback).invalidateDrawable(mComplicationDrawable);
    }

    @Test
    public void tapHighlightEndsAfterHighlightTime() {
        long highlightDuration = 1000;
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("rofl"))
                        .setShortTitle(ComplicationText.plainText("copter"))
                        .setTapAction(mMockPendingIntent)
                        .build());
        reset(mMockDrawableCallback);

        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setHighlightDuration(highlightDuration);
        mComplicationDrawable.onTap(50, 50);
        assertThat(mComplicationDrawable.getHighlighted()).isTrue();

        verify(mMockDrawableCallback).invalidateDrawable(mComplicationDrawable);

        Robolectric.getForegroundThreadScheduler()
                .advanceBy(highlightDuration - 100, TimeUnit.MILLISECONDS);
        assertThat(mComplicationDrawable.getHighlighted()).isTrue();

        Robolectric.getForegroundThreadScheduler().advanceBy(200, TimeUnit.MILLISECONDS);
        assertThat(mComplicationDrawable.getHighlighted()).isFalse();
        verify(mMockDrawableCallback, times(2)).invalidateDrawable(mComplicationDrawable);
    }

    @Test
    public void settingHighlightDurationToZeroDisablesHighlighting() {
        long highlightDuration = 0;
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("rofl"))
                        .setShortTitle(ComplicationText.plainText("copter"))
                        .setTapAction(mMockPendingIntent)
                        .build());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setHighlightDuration(highlightDuration);
        mComplicationDrawable.onTap(50, 50);

        assertThat(mComplicationDrawable.getHighlighted()).isFalse();
    }

    @Test
    public void setRangedValueProgressHidden() {
        mComplicationDrawable.setRangedValueProgressHidden(true);
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());

        assertThat(mComplicationDrawable.isRangedValueProgressHidden()).isTrue();
        assertThat(mComplicationDrawable.getComplicationRenderer().isRangedValueProgressHidden())
                .isTrue();
    }

    @Test
    public void rangedValueProgressVisibleByDefault() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        assertThat(mComplicationDrawable.isRangedValueProgressHidden()).isFalse();
        assertThat(mComplicationDrawable.getComplicationRenderer().isRangedValueProgressHidden())
                .isFalse();
    }

    // TODO(alexclarke): Add a test to check if onTap requests permission if needed.

    @Test
    public void onTapDoesNotRequestPermissionIfContextIsNotWatchFaceService() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setComplicationData(
                new ComplicationData.Builder(ComplicationData.TYPE_NO_PERMISSION).build());

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();

        Application context = ApplicationProvider.getApplicationContext();
        Intent intent = shadowOf(context).getNextStartedActivity();
        assertThat(intent).isNull();
    }

    @Test
    public void basicParcelUnparcel() {
        Rect bounds = new Rect(12, 24, 34, 56);
        boolean hideRangedProgress = false;

        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(new Rect(12, 24, 34, 56));
        mComplicationDrawable.setRangedValueProgressHidden(hideRangedProgress);

        ComplicationDrawable unparceled = parcelAndUnparcel(mComplicationDrawable);
        assertThat(unparceled.getBounds()).isEqualTo(bounds);
        assertThat(unparceled.isRangedValueProgressHidden()).isEqualTo(hideRangedProgress);
    }

    @Test
    public void parcelUnparcelFieldsNotResetAfterContextSet() {
        Rect bounds = new Rect(12, 24, 34, 56);
        long highlightDuration = 5000;
        String noDataText = "nowt";
        boolean hideRangedProgress = true;

        int bgColor = Color.BLUE;
        int borderColor = Color.RED;
        int borderDashGap = 3;
        int borderDashWidth = 4;
        int borderRadius = 12;
        int borderStyle = BORDER_STYLE_NONE;
        int borderWidth = 8;
        int highlightColor = Color.GREEN;
        int iconColor = Color.YELLOW;
        int rangedValuePrimaryColor = Color.CYAN;
        int rangedValueRingWidth = 3;
        int rangedValueSecondaryColor = Color.BLACK;
        int textColor = Color.WHITE;
        int textSize = 34;
        int titleColor = Color.MAGENTA;
        int titleSize = 18;

        int bgColorAmbient = 128;
        int borderColorAmbient = 244;
        int borderDashGapAmbient = 9;
        int borderDashWidthAmbient = 14;
        int borderRadiusAmbient = 3;
        int borderStyleAmbient = BORDER_STYLE_DASHED;
        int borderWidthAmbient = 18;
        int highlightColorAmbient = 123;
        int iconColorAmbient = 144;
        int rangedValuePrimaryColorAmbient = 24;
        int rangedValueRingWidthAmbient = 34;
        int rangedValueSecondaryColorAmbient = 111;
        int textColorAmbient = 222;
        int textSizeAmbient = 12;
        int titleColorAmbient = 55;
        int titleSizeAmbient = 7;

        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(new Rect(12, 24, 34, 56));
        mComplicationDrawable.setHighlightDuration(highlightDuration);
        mComplicationDrawable.setNoDataText(noDataText);
        mComplicationDrawable.setRangedValueProgressHidden(hideRangedProgress);

        mComplicationDrawable.setBackgroundColorActive(bgColor);
        mComplicationDrawable.setBorderColorActive(borderColor);
        mComplicationDrawable.setBorderDashGapActive(borderDashGap);
        mComplicationDrawable.setBorderDashWidthActive(borderDashWidth);
        mComplicationDrawable.setBorderRadiusActive(borderRadius);
        mComplicationDrawable.setBorderStyleActive(borderStyle);
        mComplicationDrawable.setBorderWidthActive(borderWidth);
        mComplicationDrawable.setHighlightColorActive(highlightColor);
        mComplicationDrawable.setIconColorActive(iconColor);
        mComplicationDrawable.setRangedValuePrimaryColorActive(rangedValuePrimaryColor);
        mComplicationDrawable.setRangedValueRingWidthActive(rangedValueRingWidth);
        mComplicationDrawable.setRangedValueSecondaryColorActive(rangedValueSecondaryColor);
        mComplicationDrawable.setTextColorActive(textColor);
        mComplicationDrawable.setTextSizeActive(textSize);
        mComplicationDrawable.setTitleColorActive(titleColor);
        mComplicationDrawable.setTitleSizeActive(titleSize);

        mComplicationDrawable.setBackgroundColorAmbient(bgColorAmbient);
        mComplicationDrawable.setBorderColorAmbient(borderColorAmbient);
        mComplicationDrawable.setBorderDashGapAmbient(borderDashGapAmbient);
        mComplicationDrawable.setBorderDashWidthAmbient(borderDashWidthAmbient);
        mComplicationDrawable.setBorderRadiusAmbient(borderRadiusAmbient);
        mComplicationDrawable.setBorderStyleAmbient(borderStyleAmbient);
        mComplicationDrawable.setBorderWidthAmbient(borderWidthAmbient);
        mComplicationDrawable.setHighlightColorAmbient(highlightColorAmbient);
        mComplicationDrawable.setIconColorAmbient(iconColorAmbient);
        mComplicationDrawable.setRangedValuePrimaryColorAmbient(rangedValuePrimaryColorAmbient);
        mComplicationDrawable.setRangedValueRingWidthAmbient(rangedValueRingWidthAmbient);
        mComplicationDrawable.setRangedValueSecondaryColorAmbient(rangedValueSecondaryColorAmbient);
        mComplicationDrawable.setTextColorAmbient(textColorAmbient);
        mComplicationDrawable.setTextSizeAmbient(textSizeAmbient);
        mComplicationDrawable.setTitleColorAmbient(titleColorAmbient);
        mComplicationDrawable.setTitleSizeAmbient(titleSizeAmbient);

        ComplicationDrawable unparceled = parcelAndUnparcel(mComplicationDrawable);

        assertThat(unparceled.getBounds()).isEqualTo(bounds);
        assertThat(unparceled.getHighlightDuration()).isEqualTo(highlightDuration);
        assertThat(unparceled.getNoDataText()).isEqualTo(noDataText);
        assertThat(unparceled.isRangedValueProgressHidden()).isEqualTo(hideRangedProgress);

        assertThat(unparceled.getActiveStyle().getBackgroundColor()).isEqualTo(bgColor);
        assertThat(unparceled.getActiveStyle().getBorderColor()).isEqualTo(borderColor);
        assertThat(unparceled.getActiveStyle().getBorderDashGap()).isEqualTo(borderDashGap);
        assertThat(unparceled.getActiveStyle().getBorderDashWidth()).isEqualTo(borderDashWidth);
        assertThat(unparceled.getActiveStyle().getBorderRadius()).isEqualTo(borderRadius);
        assertThat(unparceled.getActiveStyle().getBorderStyle()).isEqualTo(borderStyle);
        assertThat(unparceled.getActiveStyle().getBorderWidth()).isEqualTo(borderWidth);
        assertThat(unparceled.getActiveStyle().getHighlightColor()).isEqualTo(highlightColor);
        assertThat(unparceled.getActiveStyle().getIconColor()).isEqualTo(iconColor);
        assertThat(unparceled.getActiveStyle().getRangedValuePrimaryColor())
                .isEqualTo(rangedValuePrimaryColor);
        assertThat(unparceled.getActiveStyle().getRangedValueRingWidth())
                .isEqualTo(rangedValueRingWidth);
        assertThat(unparceled.getActiveStyle().getRangedValueSecondaryColor())
                .isEqualTo(rangedValueSecondaryColor);
        assertThat(unparceled.getActiveStyle().getTextColor()).isEqualTo(textColor);
        assertThat(unparceled.getActiveStyle().getTextSize()).isEqualTo(textSize);
        assertThat(unparceled.getActiveStyle().getTitleColor()).isEqualTo(titleColor);
        assertThat(unparceled.getActiveStyle().getTitleSize()).isEqualTo(titleSize);

        assertThat(unparceled.getAmbientStyle().getBackgroundColor()).isEqualTo(bgColorAmbient);
        assertThat(unparceled.getAmbientStyle().getBorderColor()).isEqualTo(borderColorAmbient);
        assertThat(unparceled.getAmbientStyle().getBorderDashGap()).isEqualTo(borderDashGapAmbient);
        assertThat(unparceled.getAmbientStyle().getBorderDashWidth())
                .isEqualTo(borderDashWidthAmbient);
        assertThat(unparceled.getAmbientStyle().getBorderRadius()).isEqualTo(borderRadiusAmbient);
        assertThat(unparceled.getAmbientStyle().getBorderStyle()).isEqualTo(borderStyleAmbient);
        assertThat(unparceled.getAmbientStyle().getBorderWidth()).isEqualTo(borderWidthAmbient);
        assertThat(unparceled.getAmbientStyle().getHighlightColor())
                .isEqualTo(highlightColorAmbient);
        assertThat(unparceled.getAmbientStyle().getIconColor()).isEqualTo(iconColorAmbient);
        assertThat(unparceled.getAmbientStyle().getRangedValuePrimaryColor())
                .isEqualTo(rangedValuePrimaryColorAmbient);
        assertThat(unparceled.getAmbientStyle().getRangedValueRingWidth())
                .isEqualTo(rangedValueRingWidthAmbient);
        assertThat(unparceled.getAmbientStyle().getRangedValueSecondaryColor())
                .isEqualTo(rangedValueSecondaryColorAmbient);
        assertThat(unparceled.getAmbientStyle().getTextColor()).isEqualTo(textColorAmbient);
        assertThat(unparceled.getAmbientStyle().getTextSize()).isEqualTo(textSizeAmbient);
        assertThat(unparceled.getAmbientStyle().getTitleColor()).isEqualTo(titleColorAmbient);
        assertThat(unparceled.getAmbientStyle().getTitleSize()).isEqualTo(titleSizeAmbient);
    }

    @Test
    public void copyConstructorCopiesAllFields() {
        Rect bounds = new Rect(12, 24, 34, 56);
        long highlightDuration = 5000;
        String noDataText = "nowt";
        boolean hideRangedProgress = true;

        int bgColor = Color.BLUE;
        int borderColor = Color.RED;
        int borderDashGap = 3;
        int borderDashWidth = 4;
        int borderRadius = 12;
        int borderStyle = BORDER_STYLE_NONE;
        int borderWidth = 8;
        int highlightColor = Color.GREEN;
        int iconColor = Color.YELLOW;
        int rangedValuePrimaryColor = Color.CYAN;
        int rangedValueRingWidth = 3;
        int rangedValueSecondaryColor = Color.BLACK;
        int textColor = Color.WHITE;
        int textSize = 34;
        int titleColor = Color.MAGENTA;
        int titleSize = 18;

        int bgColorAmbient = 128;
        int borderColorAmbient = 244;
        int borderDashGapAmbient = 9;
        int borderDashWidthAmbient = 14;
        int borderRadiusAmbient = 3;
        int borderStyleAmbient = BORDER_STYLE_DASHED;
        int borderWidthAmbient = 18;
        int highlightColorAmbient = 123;
        int iconColorAmbient = 144;
        int rangedValuePrimaryColorAmbient = 24;
        int rangedValueRingWidthAmbient = 34;
        int rangedValueSecondaryColorAmbient = 111;
        int textColorAmbient = 222;
        int textSizeAmbient = 12;
        int titleColorAmbient = 55;
        int titleSizeAmbient = 7;

        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(new Rect(12, 24, 34, 56));
        mComplicationDrawable.setHighlightDuration(highlightDuration);
        mComplicationDrawable.setNoDataText(noDataText);
        mComplicationDrawable.setRangedValueProgressHidden(hideRangedProgress);

        mComplicationDrawable.setBackgroundColorActive(bgColor);
        mComplicationDrawable.setBorderColorActive(borderColor);
        mComplicationDrawable.setBorderDashGapActive(borderDashGap);
        mComplicationDrawable.setBorderDashWidthActive(borderDashWidth);
        mComplicationDrawable.setBorderRadiusActive(borderRadius);
        mComplicationDrawable.setBorderStyleActive(borderStyle);
        mComplicationDrawable.setBorderWidthActive(borderWidth);
        mComplicationDrawable.setHighlightColorActive(highlightColor);
        mComplicationDrawable.setIconColorActive(iconColor);
        mComplicationDrawable.setRangedValuePrimaryColorActive(rangedValuePrimaryColor);
        mComplicationDrawable.setRangedValueRingWidthActive(rangedValueRingWidth);
        mComplicationDrawable.setRangedValueSecondaryColorActive(rangedValueSecondaryColor);
        mComplicationDrawable.setTextColorActive(textColor);
        mComplicationDrawable.setTextSizeActive(textSize);
        mComplicationDrawable.setTitleColorActive(titleColor);
        mComplicationDrawable.setTitleSizeActive(titleSize);

        mComplicationDrawable.setBackgroundColorAmbient(bgColorAmbient);
        mComplicationDrawable.setBorderColorAmbient(borderColorAmbient);
        mComplicationDrawable.setBorderDashGapAmbient(borderDashGapAmbient);
        mComplicationDrawable.setBorderDashWidthAmbient(borderDashWidthAmbient);
        mComplicationDrawable.setBorderRadiusAmbient(borderRadiusAmbient);
        mComplicationDrawable.setBorderStyleAmbient(borderStyleAmbient);
        mComplicationDrawable.setBorderWidthAmbient(borderWidthAmbient);
        mComplicationDrawable.setHighlightColorAmbient(highlightColorAmbient);
        mComplicationDrawable.setIconColorAmbient(iconColorAmbient);
        mComplicationDrawable.setRangedValuePrimaryColorAmbient(rangedValuePrimaryColorAmbient);
        mComplicationDrawable.setRangedValueRingWidthAmbient(rangedValueRingWidthAmbient);
        mComplicationDrawable.setRangedValueSecondaryColorAmbient(rangedValueSecondaryColorAmbient);
        mComplicationDrawable.setTextColorAmbient(textColorAmbient);
        mComplicationDrawable.setTextSizeAmbient(textSizeAmbient);
        mComplicationDrawable.setTitleColorAmbient(titleColorAmbient);
        mComplicationDrawable.setTitleSizeAmbient(titleSizeAmbient);

        ComplicationDrawable copy = new ComplicationDrawable(mComplicationDrawable);
        copy.setContext(ApplicationProvider.getApplicationContext());

        assertThat(copy.getBounds()).isEqualTo(bounds);
        assertThat(copy.getHighlightDuration()).isEqualTo(highlightDuration);
        assertThat(copy.getNoDataText()).isEqualTo(noDataText);
        assertThat(copy.isRangedValueProgressHidden()).isEqualTo(hideRangedProgress);

        assertThat(copy.getActiveStyle().getBackgroundColor()).isEqualTo(bgColor);
        assertThat(copy.getActiveStyle().getBorderColor()).isEqualTo(borderColor);
        assertThat(copy.getActiveStyle().getBorderDashGap()).isEqualTo(borderDashGap);
        assertThat(copy.getActiveStyle().getBorderDashWidth()).isEqualTo(borderDashWidth);
        assertThat(copy.getActiveStyle().getBorderRadius()).isEqualTo(borderRadius);
        assertThat(copy.getActiveStyle().getBorderStyle()).isEqualTo(borderStyle);
        assertThat(copy.getActiveStyle().getBorderWidth()).isEqualTo(borderWidth);
        assertThat(copy.getActiveStyle().getHighlightColor()).isEqualTo(highlightColor);
        assertThat(copy.getActiveStyle().getIconColor()).isEqualTo(iconColor);
        assertThat(copy.getActiveStyle().getRangedValuePrimaryColor())
                .isEqualTo(rangedValuePrimaryColor);
        assertThat(copy.getActiveStyle().getRangedValueRingWidth()).isEqualTo(rangedValueRingWidth);
        assertThat(copy.getActiveStyle().getRangedValueSecondaryColor())
                .isEqualTo(rangedValueSecondaryColor);
        assertThat(copy.getActiveStyle().getTextColor()).isEqualTo(textColor);
        assertThat(copy.getActiveStyle().getTextSize()).isEqualTo(textSize);
        assertThat(copy.getActiveStyle().getTitleColor()).isEqualTo(titleColor);
        assertThat(copy.getActiveStyle().getTitleSize()).isEqualTo(titleSize);

        assertThat(copy.getAmbientStyle().getBackgroundColor()).isEqualTo(bgColorAmbient);
        assertThat(copy.getAmbientStyle().getBorderColor()).isEqualTo(borderColorAmbient);
        assertThat(copy.getAmbientStyle().getBorderDashGap()).isEqualTo(borderDashGapAmbient);
        assertThat(copy.getAmbientStyle().getBorderDashWidth()).isEqualTo(borderDashWidthAmbient);
        assertThat(copy.getAmbientStyle().getBorderRadius()).isEqualTo(borderRadiusAmbient);
        assertThat(copy.getAmbientStyle().getBorderStyle()).isEqualTo(borderStyleAmbient);
        assertThat(copy.getAmbientStyle().getBorderWidth()).isEqualTo(borderWidthAmbient);
        assertThat(copy.getAmbientStyle().getHighlightColor()).isEqualTo(highlightColorAmbient);
        assertThat(copy.getAmbientStyle().getIconColor()).isEqualTo(iconColorAmbient);
        assertThat(copy.getAmbientStyle().getRangedValuePrimaryColor())
                .isEqualTo(rangedValuePrimaryColorAmbient);
        assertThat(copy.getAmbientStyle().getRangedValueRingWidth())
                .isEqualTo(rangedValueRingWidthAmbient);
        assertThat(copy.getAmbientStyle().getRangedValueSecondaryColor())
                .isEqualTo(rangedValueSecondaryColorAmbient);
        assertThat(copy.getAmbientStyle().getTextColor()).isEqualTo(textColorAmbient);
        assertThat(copy.getAmbientStyle().getTextSize()).isEqualTo(textSizeAmbient);
        assertThat(copy.getAmbientStyle().getTitleColor()).isEqualTo(titleColorAmbient);
        assertThat(copy.getAmbientStyle().getTitleSize()).isEqualTo(titleSizeAmbient);
    }

    /** Writes {@code in} to a {@link Parcel} and reads it back, returning the result. */
    @SuppressWarnings("unchecked")
    private static <T extends Parcelable> T parcelAndUnparcel(T in) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeValue(in);
            parcel.setDataPosition(0);
            return (T) parcel.readValue(in.getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
    }
}
