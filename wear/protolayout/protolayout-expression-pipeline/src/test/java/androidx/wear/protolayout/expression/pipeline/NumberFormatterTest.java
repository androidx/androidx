/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.R;

import static com.google.common.truth.Truth.assertThat;

import android.icu.util.ULocale;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatFormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32FormatOp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@Config(sdk = {P, R})
@RunWith(AndroidJUnit4.class)
public class NumberFormatterTest {

    @Test
    public void formatInt_default() {
        Int32FormatOp formatOp = Int32FormatOp.newBuilder().build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5)).isEqualTo("5");
    }

    @Test
    public void formatInt_grouping() {
        Int32FormatOp formatOp =
                Int32FormatOp.newBuilder().setMinIntegerDigits(5).setGroupingUsed(true).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5)).isEqualTo("00,005");
    }

    @Test
    public void formatInt_noGrouping() {
        Int32FormatOp formatOp =
                Int32FormatOp.newBuilder().setMinIntegerDigits(5).setGroupingUsed(false).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5)).isEqualTo("00005");
    }

    @Test
    public void formatInt_grouping_noMinIntegerDigits() {
        Int32FormatOp formatOp = Int32FormatOp.newBuilder().setGroupingUsed(false).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(1234)).isEqualTo("1234");
    }

    @Test
    public void formatInt_floatInput() {
        Int32FormatOp formatOp =
                Int32FormatOp.newBuilder().setMinIntegerDigits(5).setGroupingUsed(true).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.123f)).isEqualTo("00,005");
    }

    @Test
    public void formatFloat_default() {
        FloatFormatOp formatOp = FloatFormatOp.newBuilder().build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.123456f)).isEqualTo("5.123");
    }

    @Test
    public void formatFloat_largeMinFractionDigits_useMaxFractionPartLength() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMinFractionDigits(NumberFormatter.MAX_FRACTION_PART_LENGTH + 1)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(1))
                .isEqualTo("1." + concatZeros(NumberFormatter.MAX_FRACTION_PART_LENGTH));
    }

    @Test
    public void formatFloat_largeMaxFractionDigits_useMaxFractionPartLength() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMaxFractionDigits(NumberFormatter.MAX_FRACTION_PART_LENGTH + 1)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        // Only 15 fraction digits should appear.
        assertThat(numberFormatter.format((float) Math.PI)).isEqualTo("3.141592741012573");
    }

    @Test
    public void formatFloat_largeMinIntegerDigits_useMaxIntegerPartLength() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMinIntegerDigits(NumberFormatter.MAX_INTEGER_PART_LENGTH + 1)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(0))
                .isEqualTo(concatZeros(NumberFormatter.MAX_INTEGER_PART_LENGTH));
    }

    @Test
    public void formatInt_largeMinIntegerDigits_useMaxIntegerPartLength() {
        Int32FormatOp formatOp =
                Int32FormatOp.newBuilder()
                        .setMinIntegerDigits(NumberFormatter.MAX_INTEGER_PART_LENGTH + 1)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(0))
                .isEqualTo(concatZeros(NumberFormatter.MAX_INTEGER_PART_LENGTH));
    }

    @Test
    public void formatFloat_noGrouping() {
        FloatFormatOp formatOp = FloatFormatOp.newBuilder().setMinIntegerDigits(5).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.1234f)).isEqualTo("00005.123");
    }

    @Test
    public void formatFloat_grouping() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMinIntegerDigits(5)
                        .setMinFractionDigits(5)
                        .setGroupingUsed(true)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.1234f)).isEqualTo("00,005.12340");
    }

    @Test
    public void formatFloat_grouping_noMinIntegerDigits() {
        FloatFormatOp formatOp = FloatFormatOp.newBuilder().setGroupingUsed(true).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.1234f)).isEqualTo("5.123");
    }

    @Test
    public void formatFloat_maxFractionDigits() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder().setMaxFractionDigits(2).setGroupingUsed(true).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.1234f)).isEqualTo("5.12");
    }

    @Test
    public void formatFloat_minFractionDigits() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder().setMinFractionDigits(6).setGroupingUsed(true).build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.1234f)).isEqualTo("5.123400");
    }

    @Test
    public void formatFloat_minFractionDigitsLargerThanMinFractionDigits() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMaxFractionDigits(4)
                        .setMinFractionDigits(6)
                        .setGroupingUsed(true)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(5.1234f)).isEqualTo("5.123400");
    }

    @Test
    public void formatFloat_intInput() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMinIntegerDigits(5)
                        .setMinFractionDigits(5)
                        .setGroupingUsed(true)
                        .build();
        NumberFormatter numberFormatter = new NumberFormatter(formatOp, ULocale.UK);
        assertThat(numberFormatter.format(12)).isEqualTo("00,012.00000");
    }

    @Test
    public void numberFormatter_localChanged() {
        FloatFormatOp formatOp =
                FloatFormatOp.newBuilder()
                        .setMinIntegerDigits(5)
                        .setMinFractionDigits(5)
                        .setGroupingUsed(true)
                        .build();
        assertThat(new NumberFormatter(formatOp, ULocale.UK).format(12.345f))
                .isEqualTo("00,012.34500");
        assertThat(new NumberFormatter(formatOp, ULocale.ITALY).format(12.345f))
                .isEqualTo("00.012,34500");
    }

    private static String concatZeros(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('0');
        }
        return sb.toString();
    }
}
