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

import static com.google.common.truth.Truth.assertThat;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link CarText}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarTextTest {
    @Test
    public void toCharSequence_noSpans() {
        String text = "";
        CarText carText = CarText.create(text);
        assertThat(carText.toCharSequence().toString()).isEqualTo(text);

        text = "Test string";
        carText = CarText.create(text);
        assertThat(carText.toCharSequence().toString()).isEqualTo(text);
    }

    @Test
    public void toCharSequence_withSpans() {
        String text = "Part of this text is red";
        SpannableString spannable = new SpannableString(text);

        // Add a foreground car color span.
        ForegroundCarColorSpan foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        spannable.setSpan(foregroundCarColorSpan, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Add a duration span
        DurationSpan durationSpan = DurationSpan.create(46);
        spannable.setSpan(durationSpan, 10, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Add a span that will be filtered out.
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(0xffff00);
        spannable.setSpan(foregroundColorSpan, 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Create the car text from the spannable and verify it.
        CarText carText = CarText.create(spannable);

        CharSequence charSequence = carText.toCharSequence();
        assertThat(charSequence.toString()).isEqualTo(text);

        List<CarSpanInfo> carSpans = getCarSpans(charSequence);
        assertThat(carSpans).hasSize(2);

        CarSpanInfo carSpan = carSpans.get(0);
        assertThat(carSpan.mCarSpan instanceof ForegroundCarColorSpan).isTrue();
        assertThat(carSpan.mCarSpan).isEqualTo(foregroundCarColorSpan);
        assertThat(carSpan.mStart).isEqualTo(0);
        assertThat(carSpan.mEnd).isEqualTo(5);
        assertThat(carSpan.mFlags).isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        carSpan = carSpans.get(1);
        assertThat(carSpan.mCarSpan instanceof DurationSpan).isTrue();
        assertThat(carSpan.mCarSpan).isEqualTo(durationSpan);
        assertThat(carSpan.mStart).isEqualTo(10);
        assertThat(carSpan.mEnd).isEqualTo(12);
        assertThat(carSpan.mFlags).isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Test
    public void variants_toCharSequence_withSpans() {
        String text1 = "Part of this text is red";
        SpannableString spannable1 = new SpannableString(text1);
        ForegroundCarColorSpan foregroundCarColorSpan1 =
                ForegroundCarColorSpan.create(CarColor.RED);
        spannable1.setSpan(foregroundCarColorSpan1, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        DurationSpan durationSpan1 = DurationSpan.create(46);
        spannable1.setSpan(durationSpan1, 10, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Create a text where the string is different
        String text2 = "Part of this text is blue";
        SpannableString spannable2 = new SpannableString(text2);
        ForegroundCarColorSpan foregroundCarColorSpan2 =
                ForegroundCarColorSpan.create(CarColor.RED);
        spannable2.setSpan(foregroundCarColorSpan2, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        DurationSpan durationSpan2 = DurationSpan.create(46);
        spannable2.setSpan(durationSpan2, 10, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Create the car text from the spannables and verify it.
        CarText carText = new CarText.Builder(spannable1).addVariant(spannable2).build();

        // Check that we have two variants.
        assertThat(carText.toCharSequence()).isNotNull();
        assertThat(carText.getVariants()).hasSize(1);

        // Check the first variant.
        CharSequence charSequence1 = carText.toCharSequence();
        assertThat(charSequence1.toString()).isEqualTo(text1);

        List<CarSpanInfo> carSpans1 = getCarSpans(charSequence1);
        assertThat(carSpans1).hasSize(2);

        CarSpanInfo carSpan = carSpans1.get(0);
        assertThat(carSpan.mCarSpan instanceof ForegroundCarColorSpan).isTrue();
        assertThat(carSpan.mCarSpan).isEqualTo(foregroundCarColorSpan1);
        assertThat(carSpan.mStart).isEqualTo(0);
        assertThat(carSpan.mEnd).isEqualTo(5);
        assertThat(carSpan.mFlags).isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        carSpan = carSpans1.get(1);
        assertThat(carSpan.mCarSpan instanceof DurationSpan).isTrue();
        assertThat(carSpan.mCarSpan).isEqualTo(durationSpan1);
        assertThat(carSpan.mStart).isEqualTo(10);
        assertThat(carSpan.mEnd).isEqualTo(12);
        assertThat(carSpan.mFlags).isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Check the second variant.
        CharSequence charSequence2 = carText.getVariants().get(0);
        assertThat(charSequence2.toString()).isEqualTo(text2);

        List<CarSpanInfo> carSpans = getCarSpans(charSequence2);
        assertThat(carSpans).hasSize(2);

        carSpan = carSpans.get(0);
        assertThat(carSpan.mCarSpan instanceof ForegroundCarColorSpan).isTrue();
        assertThat(carSpan.mCarSpan).isEqualTo(foregroundCarColorSpan2);
        assertThat(carSpan.mStart).isEqualTo(0);
        assertThat(carSpan.mEnd).isEqualTo(5);
        assertThat(carSpan.mFlags).isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        carSpan = carSpans.get(1);
        assertThat(carSpan.mCarSpan instanceof DurationSpan).isTrue();
        assertThat(carSpan.mCarSpan).isEqualTo(durationSpan2);
        assertThat(carSpan.mStart).isEqualTo(10);
        assertThat(carSpan.mEnd).isEqualTo(12);
        assertThat(carSpan.mFlags).isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Test
    public void equals_and_hashCode() {
        String text = "Part of this text is red";
        SpannableString spannable = new SpannableString(text);
        ForegroundCarColorSpan foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        spannable.setSpan(foregroundCarColorSpan, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        DurationSpan durationSpan = DurationSpan.create(46);
        spannable.setSpan(durationSpan, 10, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        CarText carText1 = CarText.create(spannable);

        text = "Part of this text is red";
        spannable = new SpannableString(text);
        foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        spannable.setSpan(foregroundCarColorSpan, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        durationSpan = DurationSpan.create(46);
        spannable.setSpan(durationSpan, 10, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        CarText carText2 = CarText.create(spannable);

        // Create a text where the string is different
        text = "Part of this text is blue";
        spannable = new SpannableString(text);
        foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        spannable.setSpan(foregroundCarColorSpan, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        durationSpan = DurationSpan.create(46);
        spannable.setSpan(durationSpan, 10, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        CarText carText3 = CarText.create(spannable);

        // Create a text where the spans change
        text = "Part of this text is red";
        spannable = new SpannableString(text);
        foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        spannable.setSpan(foregroundCarColorSpan, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        CarText carText4 = CarText.create(spannable);

        assertThat(carText1).isEqualTo(carText2);
        assertThat(carText1.hashCode()).isEqualTo(carText2.hashCode());

        assertThat(carText1).isEqualTo(carText1);
        assertThat(carText1.hashCode()).isEqualTo(carText1.hashCode());

        assertThat(carText1).isNotEqualTo(carText3);
        assertThat(carText1.hashCode()).isNotEqualTo(carText3.hashCode());

        assertThat(carText2).isNotEqualTo(carText4);
        assertThat(carText2.hashCode()).isNotEqualTo(carText4.hashCode());

        assertThat(carText3).isNotEqualTo(carText4);
        assertThat(carText3.hashCode()).isNotEqualTo(carText4.hashCode());
    }

    private static List<CarSpanInfo> getCarSpans(CharSequence charSequence) {
        Spanned spanned = (Spanned) charSequence;
        List<CarSpanInfo> carSpans = new ArrayList<>();
        for (Object span : spanned.getSpans(0, charSequence.length(), Object.class)) {
            assertThat(span instanceof CarSpan).isTrue();
            CarSpanInfo info = new CarSpanInfo();
            info.mCarSpan = (CarSpan) span;
            info.mStart = spanned.getSpanStart(span);
            info.mEnd = spanned.getSpanEnd(span);
            info.mFlags = spanned.getSpanFlags(span);
            carSpans.add(info);
        }
        return carSpans;
    }

    private static class CarSpanInfo {
        CarSpan mCarSpan;
        int mStart;
        int mEnd;
        int mFlags;
    }
}
