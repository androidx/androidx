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

package androidx.car.app.model;

import static org.junit.Assert.assertThrows;

import android.text.SpannableString;

import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PlaceListMapTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ModelUtilsTest {
    @Test
    public void validateAllNonBrowsableRowsHaveDistances() {
        DistanceSpan span = DistanceSpan.create(
                Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));
        SpannableString stringWithDistance = new SpannableString("Test");
        stringWithDistance.setSpan(span, /* start= */ 0, /* end= */ 1, /* flags= */ 0);
        SpannableString stringWithInvalidDistance = new SpannableString("Test");
        // 0-length span is not allowed.
        stringWithInvalidDistance.setSpan(span, /* start= */ 0, /* end= */ 0, /* flags= */ 0);

        Row rowWithDistance = new Row.Builder().setTitle(stringWithDistance).build();
        Row rowWithDistance2 = new Row.Builder().setTitle("Title").addText(
                stringWithDistance).build();
        Row rowWithInvalidDistance = new Row.Builder().setTitle(stringWithInvalidDistance).build();
        Row rowWithoutDistance = new Row.Builder().setTitle("Test").build();
        Row browsableRowWithoutPlace =
                new Row.Builder().setTitle("Test").setBrowsable(true).setOnClickListener(() -> {
                }).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> ModelUtils.validateAllNonBrowsableRowsHaveDistance(
                        ImmutableList.of(rowWithDistance, rowWithInvalidDistance)));
        assertThrows(
                IllegalArgumentException.class,
                () -> ModelUtils.validateAllNonBrowsableRowsHaveDistance(
                        ImmutableList.of(rowWithDistance, rowWithoutDistance)));

        // Positive cases
        ModelUtils.validateAllNonBrowsableRowsHaveDistance(ImmutableList.of());
        ModelUtils.validateAllNonBrowsableRowsHaveDistance(ImmutableList.of(rowWithDistance));
        ModelUtils.validateAllNonBrowsableRowsHaveDistance(
                ImmutableList.of(rowWithDistance, rowWithDistance2));
        ModelUtils.validateAllNonBrowsableRowsHaveDistance(
                ImmutableList.of(rowWithDistance, browsableRowWithoutPlace));
        ModelUtils.validateAllNonBrowsableRowsHaveDistance(
                ImmutableList.of(browsableRowWithoutPlace));
    }

    @Test
    public void validateAllRowsHaveDurationsOrDistances() {
        DistanceSpan distanceSpan =
                DistanceSpan.create(
                        Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));
        DurationSpan durationSpan = DurationSpan.create(1);

        SpannableString stringWithDistance = new SpannableString("Test");
        stringWithDistance.setSpan(distanceSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);

        SpannableString stringWithDuration = new SpannableString("Test");
        stringWithDuration.setSpan(durationSpan, /* start= */ 0, /* end= */ 1, /* flags= */ 0);

        SpannableString stringWithInvalidDuration = new SpannableString("Test");
        // 0-length span is not allowed.
        stringWithInvalidDuration.setSpan(durationSpan, /* start= */ 0, /* end= */ 0, /* flags= */
                0);

        Row rowWithDistance = new Row.Builder().setTitle(stringWithDistance).build();
        Row rowWithDuration = new Row.Builder().setTitle(stringWithDuration).build();
        Row rowWithDuration2 = new Row.Builder().setTitle("Title").addText(
                stringWithDuration).build();
        Row rowWithInvalidDuration = new Row.Builder().setTitle(stringWithInvalidDuration).build();
        Row plainRow = new Row.Builder().setTitle("Test").build();

        assertThrows(
                IllegalArgumentException.class,
                () -> ModelUtils.validateAllRowsHaveDistanceOrDuration(
                        ImmutableList.of(rowWithDuration, rowWithInvalidDuration)));
        assertThrows(
                IllegalArgumentException.class,
                () -> ModelUtils.validateAllRowsHaveDistanceOrDuration(
                        ImmutableList.of(rowWithDuration, plainRow)));

        // Positive cases.
        ModelUtils.validateAllRowsHaveDistanceOrDuration(ImmutableList.of());
        ModelUtils.validateAllRowsHaveDistanceOrDuration(ImmutableList.of(rowWithDistance));
        ModelUtils.validateAllRowsHaveDistanceOrDuration(ImmutableList.of(rowWithDuration));
        ModelUtils.validateAllRowsHaveDistanceOrDuration(
                ImmutableList.of(rowWithDuration, rowWithDuration2));
        ModelUtils.validateAllRowsHaveDistanceOrDuration(
                ImmutableList.of(rowWithDuration, rowWithDistance));
    }

    @Test
    public void validateAllRowsHaveOnlySmallSizedImages() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Row rowWithNoImage = new Row.Builder().setTitle("title1").build();
        Row rowWithSmallImage =
                new Row.Builder().setTitle("title2").setImage(carIcon,
                        Row.IMAGE_TYPE_SMALL).build();
        Row rowWithLargeImage =
                new Row.Builder().setTitle("title3").setImage(carIcon,
                        Row.IMAGE_TYPE_LARGE).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> ModelUtils.validateAllRowsHaveOnlySmallImages(
                        ImmutableList.of(rowWithLargeImage)));
        assertThrows(
                IllegalArgumentException.class,
                () -> ModelUtils.validateAllRowsHaveOnlySmallImages(
                        ImmutableList.of(rowWithNoImage, rowWithLargeImage)));

        // Positive cases
        ModelUtils.validateAllRowsHaveOnlySmallImages(ImmutableList.of());
        ModelUtils.validateAllRowsHaveOnlySmallImages(ImmutableList.of(rowWithNoImage));
        ModelUtils.validateAllRowsHaveOnlySmallImages(ImmutableList.of(rowWithSmallImage));
        ModelUtils.validateAllRowsHaveOnlySmallImages(
                ImmutableList.of(rowWithNoImage, rowWithSmallImage));
    }
}
