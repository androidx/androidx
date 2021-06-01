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

import static androidx.wear.watchface.complications.rendering.ComplicationStyle.BORDER_STYLE_DASHED;
import static androidx.wear.watchface.complications.rendering.ComplicationStyle.BORDER_STYLE_NONE;

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
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.wear.complications.ComplicationHelperActivity;
import androidx.wear.complications.data.DataKt;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.TimeUnit;

import kotlin.coroutines.Continuation;

/** Tests for {@link ComplicationDrawable}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationDrawableTest {

    private static final int ACTIVE_COLOR = 0x12345678;
    private static final int AMBIENT_COLOR = 0x87654321;
    private static final int ACTIVE_PX = 1;
    private static final int AMBIENT_PX = 1;

    private ComplicationDrawable mComplicationDrawable;
    private androidx.wear.complications.data.ComplicationData mComplicationData;
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

        ComplicationData complicationData =
                new ComplicationData.Builder(
                        ComplicationData.TYPE_SHORT_TEXT
                ).setShortText(ComplicationText.plainText("hede"))
                        .build();
        mComplicationData = DataKt.toApiComplicationData(complicationData);
        mDefaultTextSize =
                ApplicationProvider.getApplicationContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.complicationDrawable_textSize);
        Robolectric.getForegroundThreadScheduler().pause();
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
                () -> mComplicationDrawable.setComplicationData(mComplicationData, true));
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
        mComplicationDrawable.setComplicationData(mComplicationData, true);
        // THEN no exception is thrown
    }

    @Test
    public void setBackgroundColor() {
        mComplicationDrawable.getActiveStyle().setBackgroundColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setBackgroundColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getBackgroundColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getBackgroundColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setBackgroundDrawable() {
        mComplicationDrawable.getActiveStyle().setBackgroundDrawable(mMockDrawableActive);
        mComplicationDrawable.getAmbientStyle().setBackgroundDrawable(mMockDrawableAmbient);
        assertThat(mComplicationDrawable.getActiveStyle().getBackgroundDrawable())
                .isEqualTo(mMockDrawableActive);
        assertThat(mComplicationDrawable.getAmbientStyle().getBackgroundDrawable())
                .isEqualTo(mMockDrawableAmbient);
    }

    @Test
    public void setTextColor() {
        mComplicationDrawable.getActiveStyle().setTextColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setTextColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getTextColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getTextColor()).isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setTitleColor() {
        mComplicationDrawable.getActiveStyle().setTitleColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setTitleColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getTitleColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getTitleColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setTextTypeface() {
        Typeface activeTf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        Typeface ambientTf = Typeface.create("sans-serif-condensed", Typeface.ITALIC);
        mComplicationDrawable.getActiveStyle().setTextTypeface(activeTf);
        mComplicationDrawable.getAmbientStyle().setTextTypeface(ambientTf);
        assertThat(mComplicationDrawable.getActiveStyle().getTextTypeface()).isEqualTo(activeTf);
        assertThat(mComplicationDrawable.getAmbientStyle().getTextTypeface()).isEqualTo(ambientTf);
    }

    @Test
    public void setTitleTypeface() {
        Typeface activeTf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        Typeface ambientTf = Typeface.create("sans-serif-condensed", Typeface.ITALIC);
        mComplicationDrawable.getActiveStyle().setTitleTypeface(activeTf);
        mComplicationDrawable.getAmbientStyle().setTitleTypeface(ambientTf);
        assertThat(mComplicationDrawable.getActiveStyle().getTitleTypeface()).isEqualTo(activeTf);
        assertThat(mComplicationDrawable.getAmbientStyle().getTitleTypeface()).isEqualTo(ambientTf);
    }

    @Test
    public void setTextSize() {
        mComplicationDrawable.getActiveStyle().setTextSize(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setTextSize(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getTextSize()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getTextSize()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setTitleSize() {
        mComplicationDrawable.getActiveStyle().setTitleSize(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setTitleSize(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getTitleSize()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getTitleSize()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setIconColor() {
        mComplicationDrawable.getActiveStyle().setIconColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setIconColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getIconColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getIconColor()).isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setBorderColor() {
        mComplicationDrawable.getActiveStyle().setBorderColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setBorderColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderColor()).isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setBorderRadius() {
        mComplicationDrawable.getActiveStyle().setBorderRadius(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setBorderRadius(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderRadius()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderRadius()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setBorderStyle() {
        mComplicationDrawable.getActiveStyle().setBorderStyle(BORDER_STYLE_NONE);
        mComplicationDrawable.getAmbientStyle().setBorderStyle(BORDER_STYLE_DASHED);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderStyle())
                .isEqualTo(BORDER_STYLE_NONE);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderStyle())
                .isEqualTo(BORDER_STYLE_DASHED);
    }

    @Test
    public void setBorderWidth() {
        mComplicationDrawable.getActiveStyle().setBorderWidth(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setBorderWidth(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderWidth()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderWidth()).isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setBorderDashGap() {
        mComplicationDrawable.getActiveStyle().setBorderDashGap(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setBorderDashGap(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderDashGap()).isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderDashGap())
                .isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setBorderDashWidth() {
        mComplicationDrawable.getActiveStyle().setBorderDashWidth(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setBorderDashWidth(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getBorderDashWidth())
                .isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getBorderDashWidth())
                .isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setImageColorFilter() {
        ColorFilter activeCF = new PorterDuffColorFilter(ACTIVE_COLOR, Mode.SRC_IN);
        ColorFilter ambientCF = new PorterDuffColorFilter(AMBIENT_COLOR, Mode.SRC_IN);
        mComplicationDrawable.getActiveStyle().setImageColorFilter(activeCF);
        mComplicationDrawable.getAmbientStyle().setImageColorFilter(ambientCF);
        assertThat(mComplicationDrawable.getActiveStyle().getImageColorFilter()).isEqualTo(
                activeCF);
        assertThat(mComplicationDrawable.getAmbientStyle().getImageColorFilter()).isEqualTo(
                ambientCF);
    }

    @Test
    public void setRangedValueRingWidth() {
        mComplicationDrawable.getActiveStyle().setRangedValueRingWidth(ACTIVE_PX);
        mComplicationDrawable.getAmbientStyle().setRangedValueRingWidth(AMBIENT_PX);
        assertThat(mComplicationDrawable.getActiveStyle().getRangedValueRingWidth())
                .isEqualTo(ACTIVE_PX);
        assertThat(mComplicationDrawable.getAmbientStyle().getRangedValueRingWidth())
                .isEqualTo(AMBIENT_PX);
    }

    @Test
    public void setRangedValuePrimaryColor() {
        mComplicationDrawable.getActiveStyle().setRangedValuePrimaryColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setRangedValuePrimaryColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getRangedValuePrimaryColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getRangedValuePrimaryColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setRangedValueSecondaryColor() {
        mComplicationDrawable.getActiveStyle().setRangedValueSecondaryColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setRangedValueSecondaryColor(AMBIENT_COLOR);
        assertThat(mComplicationDrawable.getActiveStyle().getRangedValueSecondaryColor())
                .isEqualTo(ACTIVE_COLOR);
        assertThat(mComplicationDrawable.getAmbientStyle().getRangedValueSecondaryColor())
                .isEqualTo(AMBIENT_COLOR);
    }

    @Test
    public void setHighlightColor() {
        mComplicationDrawable.getActiveStyle().setHighlightColor(ACTIVE_COLOR);
        mComplicationDrawable.getAmbientStyle().setHighlightColor(AMBIENT_COLOR);
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
                .isEqualTo(new ComplicationStyle().getTextSize());
        assertThat(textSizeFromResources).isEqualTo(mDefaultTextSize);
    }

    @Test
    public void onTapReturnsFalseIfNoComplicationData() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(null, true);
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();
    }

    @Test
    public void onTapReturnsFalseIfNoTapAction() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("rofl"))
                            .setShortTitle(ComplicationText.plainText("copter"))
                            .build()
                    ),
                true
        );
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();
    }

    @Test
    public void onTapReturnsFalseIfOutOfBounds() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("rofl"))
                            .setShortTitle(ComplicationText.plainText("copter"))
                            .setTapAction(mMockPendingIntent)
                            .build()
                ),
                true
        );
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(200, 200)).isFalse();
    }

    @Test
    public void onTapReturnsFalseIfTapActionCanceled() throws CanceledException {
        doThrow(new CanceledException()).when(mMockPendingIntent).send();

        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("rofl"))
                            .setShortTitle(ComplicationText.plainText("copter"))
                            .setTapAction(mMockPendingIntent)
                        .build()
                ),
                true
        );
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();
    }

    @Test
    public void onTapReturnsTrueIfSuccessfulAndHighlightsComplication() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("rofl"))
                            .setShortTitle(ComplicationText.plainText("copter"))
                            .setTapAction(mMockPendingIntent)
                        .build()
                ),
                true
        );
        reset(mMockDrawableCallback);
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        assertThat(mComplicationDrawable.onTap(50, 50)).isTrue();
        assertThat(mComplicationDrawable.isHighlighted()).isTrue();
        verify(mMockDrawableCallback).invalidateDrawable(mComplicationDrawable);
    }

    @Test
    public void tapHighlightEndsAfterHighlightTime() {
        long highlightDuration = 1000;
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("rofl"))
                            .setShortTitle(ComplicationText.plainText("copter"))
                            .setTapAction(mMockPendingIntent)
                            .build()
                ),
                true
        );
        reset(mMockDrawableCallback);

        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setHighlightDuration(highlightDuration);
        mComplicationDrawable.onTap(50, 50);
        assertThat(mComplicationDrawable.isHighlighted()).isTrue();

        verify(mMockDrawableCallback).invalidateDrawable(mComplicationDrawable);

        Robolectric.getForegroundThreadScheduler()
                .advanceBy(highlightDuration - 100, TimeUnit.MILLISECONDS);
        assertThat(mComplicationDrawable.isHighlighted()).isTrue();

        Robolectric.getForegroundThreadScheduler().advanceBy(200, TimeUnit.MILLISECONDS);
        assertThat(mComplicationDrawable.isHighlighted()).isFalse();
        verify(mMockDrawableCallback, times(2)).invalidateDrawable(mComplicationDrawable);
    }

    @Test
    public void settingHighlightDurationToZeroDisablesHighlighting() {
        long highlightDuration = 0;
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("rofl"))
                            .setShortTitle(ComplicationText.plainText("copter"))
                            .setTapAction(mMockPendingIntent)
                            .build()
                ),
                true
        );
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setHighlightDuration(highlightDuration);
        mComplicationDrawable.onTap(50, 50);

        assertThat(mComplicationDrawable.isHighlighted()).isFalse();
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

    @Test
    @Ignore("Mysteriously crashes on one bot")
    public void onTapRequestsPermissionIfNeeded() {
        mComplicationDrawable = new ComplicationDrawable(new FakeWatchFaceService());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                        new ComplicationData.Builder(ComplicationData.TYPE_NO_PERMISSION).build()
                ),
                true
        );

        assertThat(mComplicationDrawable.onTap(50, 50)).isTrue();

        Application context = ApplicationProvider.getApplicationContext();
        Intent expected =
                ComplicationHelperActivity.createPermissionRequestHelperIntent(
                        context, new ComponentName(context, context.getClass()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent actual = shadowOf(context).getNextStartedActivity();

        assertThat(actual.getAction()).isEqualTo(expected.getAction());
        assertThat(actual.getComponent()).isEqualTo(expected.getComponent());
    }

    @Test
    @Ignore("Mysteriously crashes on one bot")
    public void onTapDoesNotRequestPermissionIfContextIsNotWatchFaceService() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.setBounds(new Rect(0, 0, 100, 100));

        mComplicationDrawable.setComplicationData(
                DataKt.toApiComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_NO_PERMISSION).build()
                ),
                true
        );

        assertThat(mComplicationDrawable.onTap(50, 50)).isFalse();

        Application context = ApplicationProvider.getApplicationContext();
        Intent intent = shadowOf(context).getNextStartedActivity();
        assertThat(intent).isNull();
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

        mComplicationDrawable.getActiveStyle().setBackgroundColor(bgColor);
        mComplicationDrawable.getActiveStyle().setBorderColor(borderColor);
        mComplicationDrawable.getActiveStyle().setBorderDashGap(borderDashGap);
        mComplicationDrawable.getActiveStyle().setBorderDashWidth(borderDashWidth);
        mComplicationDrawable.getActiveStyle().setBorderRadius(borderRadius);
        mComplicationDrawable.getActiveStyle().setBorderStyle(borderStyle);
        mComplicationDrawable.getActiveStyle().setBorderWidth(borderWidth);
        mComplicationDrawable.getActiveStyle().setHighlightColor(highlightColor);
        mComplicationDrawable.getActiveStyle().setIconColor(iconColor);
        mComplicationDrawable.getActiveStyle().setRangedValuePrimaryColor(
                rangedValuePrimaryColor);
        mComplicationDrawable.getActiveStyle().setRangedValueRingWidth(rangedValueRingWidth);
        mComplicationDrawable.getActiveStyle().setRangedValueSecondaryColor(
                rangedValueSecondaryColor);
        mComplicationDrawable.getActiveStyle().setTextColor(textColor);
        mComplicationDrawable.getActiveStyle().setTextSize(textSize);
        mComplicationDrawable.getActiveStyle().setTitleColor(titleColor);
        mComplicationDrawable.getActiveStyle().setTitleSize(titleSize);

        mComplicationDrawable.getAmbientStyle().setBackgroundColor(bgColorAmbient);
        mComplicationDrawable.getAmbientStyle().setBorderColor(borderColorAmbient);
        mComplicationDrawable.getAmbientStyle().setBorderDashGap(borderDashGapAmbient);
        mComplicationDrawable.getAmbientStyle().setBorderDashWidth(borderDashWidthAmbient);
        mComplicationDrawable.getAmbientStyle().setBorderRadius(borderRadiusAmbient);
        mComplicationDrawable.getAmbientStyle().setBorderStyle(borderStyleAmbient);
        mComplicationDrawable.getAmbientStyle().setBorderWidth(borderWidthAmbient);
        mComplicationDrawable.getAmbientStyle().setHighlightColor(highlightColorAmbient);
        mComplicationDrawable.getAmbientStyle().setIconColor(iconColorAmbient);
        mComplicationDrawable.getAmbientStyle().setRangedValuePrimaryColor(
                rangedValuePrimaryColorAmbient);
        mComplicationDrawable.getAmbientStyle().setRangedValueRingWidth(
                rangedValueRingWidthAmbient);
        mComplicationDrawable.getAmbientStyle().setRangedValueSecondaryColor(
                rangedValueSecondaryColorAmbient);
        mComplicationDrawable.getAmbientStyle().setTextColor(textColorAmbient);
        mComplicationDrawable.getAmbientStyle().setTextSize(textSizeAmbient);
        mComplicationDrawable.getAmbientStyle().setTitleColor(titleColorAmbient);
        mComplicationDrawable.getAmbientStyle().setTitleSize(titleSizeAmbient);

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

    @Test
    public void testUpdateStyleIfRequired() {
        mComplicationDrawable.setContext(ApplicationProvider.getApplicationContext());
        mComplicationDrawable.getActiveStyle().setBackgroundColor(123);

        assertThat(mComplicationDrawable.getComplicationRenderer().mActivePaintSet
                .mBackgroundPaint.getColor()).isNotEqualTo(123);

        mComplicationDrawable.updateStyleIfRequired();

        assertThat(mComplicationDrawable.getComplicationRenderer().mActivePaintSet
                .mBackgroundPaint.getColor()).isEqualTo(123);
    }

    /** Proxies necessary methods to Robolectric application. */
    private static class FakeWatchFaceService extends WatchFaceService {

        @Override
        public Resources getResources() {
            return ApplicationProvider.getApplicationContext().getResources();
        }

        @Override
        public String getPackageName() {
            return ApplicationProvider.getApplicationContext().getPackageName();
        }

        @Override
        public void startActivity(Intent intent) {
            ApplicationProvider.getApplicationContext().startActivity(intent);
        }

        @Nullable
        @Override
        protected Object createWatchFace(@NonNull SurfaceHolder surfaceHolder,
                @NonNull WatchState watchState,
                @NonNull ComplicationSlotsManager complicationSlotsManager,
                @NonNull CurrentUserStyleRepository currentUserStyleRepository,
                @NonNull Continuation<? super WatchFace> completion) {
            return new WatchFace(
                    WatchFaceType.ANALOG,
                    new Renderer.CanvasRenderer(
                            surfaceHolder, currentUserStyleRepository, watchState,
                            CanvasType.SOFTWARE, 16L) {
                        @Override
                        public void renderHighlightLayer(@NonNull Canvas canvas,
                                @NonNull Rect bounds, @NonNull Calendar calendar) {}

                        @Override
                        public void render(@NonNull Canvas canvas, @NonNull Rect bounds,
                                @NonNull Calendar calendar) {}
                    }
            );
        }
    }
}
