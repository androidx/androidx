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

import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_COLORS;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_END_ANGLE;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_START_ANGLE;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_STROKE_WIDTH;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class CircularProgressIndicatorTest {
    @Test
    public void testOpenRingIndicatorDefault() {
        CircularProgressIndicator circularProgressIndicator =
                new CircularProgressIndicator.Builder().build();

        assertProgressIndicatorIsEqual(
                circularProgressIndicator,
                0,
                DEFAULT_START_ANGLE,
                DEFAULT_END_ANGLE,
                DEFAULT_COLORS,
                DEFAULT_STROKE_WIDTH.getValue(),
                null);
    }

    @Test
    public void testProgressIndicatorCustom() {
        float progress = 0.25f;
        String contentDescription = "60 degrees progress";
        ProgressIndicatorColors colors = new ProgressIndicatorColors(Color.YELLOW, Color.BLACK);
        int thickness = 16;
        float startAngle = -24;
        float endAngle = 24;

        CircularProgressIndicator circularProgressIndicator =
                new CircularProgressIndicator.Builder()
                        .setProgress(progress)
                        .setStartAngle(startAngle)
                        .setEndAngle(endAngle)
                        .setCircularProgressIndicatorColors(colors)
                        .setStrokeWidth(thickness)
                        .setContentDescription(contentDescription)
                        .build();

        assertProgressIndicatorIsEqual(
                circularProgressIndicator,
                progress,
                startAngle,
                endAngle,
                colors,
                thickness,
                contentDescription);
    }

    private void assertProgressIndicatorIsEqual(
            @NonNull CircularProgressIndicator circularProgressIndicator,
            float progress,
            float startAngle,
            float endAngle,
            @NonNull ProgressIndicatorColors colors,
            float thickness,
            @Nullable String contentDescription) {
        float total = endAngle + (endAngle <= startAngle ? 360 : 0) - startAngle;
        assertThat(circularProgressIndicator.getProgress().getValue())
                .isWithin(0.01f)
                .of(progress * total);
        assertThat(circularProgressIndicator.getStartAngle().getValue())
                .isWithin(0.01f)
                .of(startAngle);
        assertThat(circularProgressIndicator.getEndAngle().getValue()).isWithin(0.01f).of(endAngle);
        assertThat(
                circularProgressIndicator
                        .getCircularProgressIndicatorColors()
                        .getIndicatorColor()
                        .getArgb())
                .isEqualTo(colors.getIndicatorColor().getArgb());
        assertThat(
                circularProgressIndicator
                        .getCircularProgressIndicatorColors()
                        .getTrackColor()
                        .getArgb())
                .isEqualTo(colors.getTrackColor().getArgb());
        assertThat(circularProgressIndicator.getStrokeWidth().getValue()).isEqualTo(thickness);

        if (contentDescription == null) {
            assertThat(circularProgressIndicator.getContentDescription()).isNull();
        } else {
            assertThat(circularProgressIndicator.getContentDescription())
                    .isEqualTo(contentDescription);
        }
    }
}
