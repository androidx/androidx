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

import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_COLORS;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_END_ANGLE;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_START_ANGLE;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_STROKE_WIDTH;

import static com.google.common.truth.Truth.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.LayoutElementBuilders.Box;
import androidx.wear.protolayout.LayoutElementBuilders.Column;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;

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

        assertProgressIndicator(
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
        StringProp contentDescription = new StringProp.Builder("60 degrees progress").build();
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

        assertProgressIndicator(
                circularProgressIndicator,
                progress,
                startAngle,
                endAngle,
                colors,
                thickness,
                contentDescription);
    }

    @Test
    public void testWrongElement() {
        Column box = new Column.Builder().build();

        assertThat(CircularProgressIndicator.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongBox() {
        Box box = new Box.Builder().build();

        assertThat(CircularProgressIndicator.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testWrongTag() {
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

        assertThat(CircularProgressIndicator.fromLayoutElement(box)).isNull();
    }

    @Test
    public void testDynamicContentDescription() {
        StringProp dynamicContentDescription =
                new StringProp.Builder("static")
                        .setDynamicValue(DynamicString.constant("dynamic"))
                        .build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder()
                        .setContentDescription(dynamicContentDescription)
                        .build();

        assertThat(progressIndicator.getContentDescription().toProto())
                .isEqualTo(dynamicContentDescription.toProto());
    }

    private void assertProgressIndicator(
            @NonNull CircularProgressIndicator actualCircularProgressIndicator,
            float expectedProgress,
            float expectedStartAngle,
            float expectedEndAngle,
            @NonNull ProgressIndicatorColors expectedColors,
            float expectedThickness,
            @Nullable StringProp expectedContentDescription) {
        assertProgressIndicatorIsEqual(
                actualCircularProgressIndicator,
                expectedProgress,
                expectedStartAngle,
                expectedEndAngle,
                expectedColors,
                expectedThickness,
                expectedContentDescription);

        Box box = new Box.Builder().addContent(actualCircularProgressIndicator).build();

        CircularProgressIndicator newCpi =
                CircularProgressIndicator.fromLayoutElement(box.getContents().get(0));

        assertThat(newCpi).isNotNull();
        assertProgressIndicatorIsEqual(
                actualCircularProgressIndicator,
                expectedProgress,
                expectedStartAngle,
                expectedEndAngle,
                expectedColors,
                expectedThickness,
                expectedContentDescription);

        assertThat(CircularProgressIndicator.fromLayoutElement(actualCircularProgressIndicator))
                .isEqualTo(actualCircularProgressIndicator);
    }

    private void assertProgressIndicatorIsEqual(
            @NonNull CircularProgressIndicator actualCircularProgressIndicator,
            float expectedProgress,
            float expectedStartAngle,
            float expectedEndAngle,
            @NonNull ProgressIndicatorColors expectedColors,
            float expectedThickness,
            @Nullable StringProp expectedContentDescription) {
        float total =
                expectedEndAngle
                        + (expectedEndAngle <= expectedStartAngle ? 360 : 0)
                        - expectedStartAngle;
        assertThat(actualCircularProgressIndicator.getMetadataTag())
                .isEqualTo(CircularProgressIndicator.METADATA_TAG);
        assertThat(actualCircularProgressIndicator.getProgress().getValue())
                .isWithin(0.01f)
                .of(expectedProgress * total);
        assertThat(actualCircularProgressIndicator.getStartAngle().getValue())
                .isWithin(0.01f)
                .of(expectedStartAngle);
        assertThat(actualCircularProgressIndicator.getEndAngle().getValue())
                .isWithin(0.01f)
                .of(expectedEndAngle);
        assertThat(
                        actualCircularProgressIndicator
                                .getCircularProgressIndicatorColors()
                                .getIndicatorColor()
                                .getArgb())
                .isEqualTo(expectedColors.getIndicatorColor().getArgb());
        assertThat(
                        actualCircularProgressIndicator
                                .getCircularProgressIndicatorColors()
                                .getTrackColor()
                                .getArgb())
                .isEqualTo(expectedColors.getTrackColor().getArgb());
        assertThat(actualCircularProgressIndicator.getStrokeWidth().getValue())
                .isEqualTo(expectedThickness);

        if (expectedContentDescription == null) {
            assertThat(actualCircularProgressIndicator.getContentDescription()).isNull();
        } else {
            assertThat(actualCircularProgressIndicator.getContentDescription().toProto())
                    .isEqualTo(expectedContentDescription.toProto());
        }
    }
}
