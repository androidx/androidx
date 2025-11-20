/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.car.app.dialer;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.SpannableStringBuilder;

import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Header;
import androidx.core.graphics.drawable.IconCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class InCallTemplateTest {
    private final Bitmap mBitmap = Bitmap.createBitmap(666, 666, Bitmap.Config.ARGB_8888);
    private final IconCompat mIcon = IconCompat.createWithBitmap(mBitmap);
    private final String mTitle = "A simple title";
    private final String mSubtitle = "A simple subtitle";

    @Test
    public void build_allSimpleFields_returnsValidTemplate() {
        String headerTitle = "The header title";
        InCallTemplate template =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .setHeader(
                                new Header.Builder()
                                        .setTitle(headerTitle)
                                        .setStartHeaderAction(Action.BACK)
                                        .build())
                        .addText(mSubtitle)
                        .addAction(
                                new Action.Builder()
                                        .setIcon(new CarIcon.Builder(mIcon).build())
                                        .setFlags(Action.FLAG_PRIMARY)
                                        .build())
                        .addAction(
                                new Action.Builder()
                                        .setIcon(new CarIcon.Builder(mIcon).build())
                                        .setOnClickListener(() -> {})
                                        .build())
                        .build();

        assertThat(template.getHeader().getTitle().toString()).isEqualTo(headerTitle);
        assertThat(template.getHeader().getStartHeaderAction()).isEqualTo(Action.BACK);
        assertThat(template.getIcon().getIcon()).isEqualTo(mIcon);
        assertThat(template.getTitle().toString()).isEqualTo(mTitle);
        assertThat(template.getTexts().get(0).toString()).isEqualTo(mSubtitle);
        assertThat(template.getActions()).hasSize(2);
    }

    @Test
    public void build_withInvalidHeader_throws() {
        Header headerWithUnsupportedAction =
                new Header.Builder().setStartHeaderAction(Action.PAN).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InCallTemplate.Builder()
                            .setTitle(mTitle)
                            .setIcon(new CarIcon.Builder(mIcon).build())
                            .setHeader(headerWithUnsupportedAction)
                            .build();
                });
    }

    @Test
    public void build_carTextsWithValidSpans_returnsValidTemplate() {
        DurationSpan durationSpan = DurationSpan.create(1000);
        DistanceSpan distanceSpan = DistanceSpan.create(Distance.create(5.0, Distance.UNIT_FEET));
        ForegroundCarColorSpan foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        SpannableStringBuilder titleSpannableBuilder = new SpannableStringBuilder(mTitle);
        titleSpannableBuilder.setSpan(durationSpan, 0, 4, 0);
        titleSpannableBuilder.setSpan(distanceSpan, 6, 10, 0);
        CarText titleCarText = CarText.create(titleSpannableBuilder);
        SpannableStringBuilder subtitleSpannableBuilder = new SpannableStringBuilder(mSubtitle);
        subtitleSpannableBuilder.setSpan(foregroundCarColorSpan, 0, 4, 0);
        CarText subtitleCarText = CarText.create(mSubtitle);

        InCallTemplate template =
                new InCallTemplate.Builder()
                        .setTitle(titleCarText)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .addText(subtitleCarText)
                        .build();

        assertThat(template.getIcon().getIcon()).isEqualTo(mIcon);
        assertThat(template.getTitle()).isEqualTo(titleCarText);
        assertThat(template.getTitle().toString()).isEqualTo(mTitle);
        assertThat(template.getTexts().get(0)).isEqualTo(subtitleCarText);
        assertThat(template.getTexts().get(0).toString()).isEqualTo(mSubtitle);
    }

    @Test
    public void build_carTextTitleWithInvalidSpan_throws() {
        ForegroundCarColorSpan foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        SpannableStringBuilder titleSpannableBuilder = new SpannableStringBuilder(mTitle);
        titleSpannableBuilder.setSpan(foregroundCarColorSpan, 2, 6, 0);
        CarText titleCarText = CarText.create(titleSpannableBuilder);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InCallTemplate.Builder()
                            .setTitle(titleCarText)
                            .setIcon(new CarIcon.Builder(mIcon).build())
                            .build();
                });
    }

    @Test
    public void build_carTextSubtitleWithInvalidSpan_throws() {
        ForegroundCarColorSpan foregroundCarColorSpan = ForegroundCarColorSpan.create(CarColor.RED);
        SpannableStringBuilder subtitleSpannableBuilder = new SpannableStringBuilder(mSubtitle);
        subtitleSpannableBuilder.setSpan(foregroundCarColorSpan, 2, 6, 0);
        CarText subtitleCarText = CarText.create(subtitleSpannableBuilder);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InCallTemplate.Builder()
                            .setTitle(mTitle)
                            .setIcon(new CarIcon.Builder(mIcon).build())
                            .addText(subtitleCarText)
                            .build();
                });
    }

    @Test
    public void build_iconWithContentUri_throws() {
        IconCompat iconWithContentUri = IconCompat.createWithContentUri(Uri.parse("content://hi"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InCallTemplate.Builder()
                                .setTitle(mTitle)
                                .setIcon(new CarIcon.Builder(iconWithContentUri).build())
                                .build());
    }

    @Test
    public void build_tooManyActions_throws() {
        Action action = new Action.Builder().setIcon(new CarIcon.Builder(mIcon).build()).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InCallTemplate.Builder()
                            .setTitle(mTitle)
                            .setIcon(new CarIcon.Builder(mIcon).build())
                            .addAction(action)
                            .addAction(action)
                            .addAction(action)
                            .addAction(action)
                            .addAction(action)
                            .addAction(action)
                            .build();
                });
    }

    @Test
    public void build_tooManyPrimaryActions_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InCallTemplate.Builder()
                                .setTitle(mTitle)
                                .setIcon(new CarIcon.Builder(mIcon).build())
                                .addAction(
                                        new Action.Builder()
                                                .setIcon(new CarIcon.Builder(mIcon).build())
                                                .setFlags(Action.FLAG_PRIMARY)
                                                .build())
                                .addAction(
                                        new Action.Builder()
                                                .setIcon(new CarIcon.Builder(mIcon).build())
                                                .setFlags(Action.FLAG_PRIMARY)
                                                .build())
                                .build());
    }

    @Test
    public void build_actionWithoutIcon_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new InCallTemplate.Builder()
                                .setTitle(mTitle)
                                .setIcon(new CarIcon.Builder(mIcon).build())
                                .addAction(new Action.Builder().build())
                                .build());
    }

    @Test
    public void build_actionWithTitle_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InCallTemplate.Builder()
                                .setTitle(mTitle)
                                .setIcon(new CarIcon.Builder(mIcon).build())
                                .addAction(
                                        new Action.Builder()
                                                .setTitle("the title")
                                                .setIcon(new CarIcon.Builder(mIcon).build())
                                                .build())
                                .build());
    }

    @Test
    public void build_actionNotCustom_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InCallTemplate.Builder()
                                .setTitle(mTitle)
                                .setIcon(new CarIcon.Builder(mIcon).build())
                                .addAction(Action.BACK)
                                .build());
    }

    @Test
    public void build_loadingTemplate_returnsValidTemplate() {
        InCallTemplate template = new InCallTemplate.Builder().setLoading(true).build();

        assertThat(template.isLoading()).isTrue();
    }

    @Test
    public void build_loadingTemplateWithContent_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new InCallTemplate.Builder()
                                .setLoading(true)
                                .addAction(
                                        new Action.Builder()
                                                .setIcon(new CarIcon.Builder(mIcon).build())
                                                .build())
                                .build());
    }

    @Test
    public void equals_sameTemplates_returnsTrue() {
        InCallTemplate template1 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .addText(mSubtitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .addAction(
                                new Action.Builder()
                                        .setIcon(new CarIcon.Builder(mIcon).build())
                                        .build())
                        .build();
        InCallTemplate template2 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .addText(mSubtitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .addAction(
                                new Action.Builder()
                                        .setIcon(new CarIcon.Builder(mIcon).build())
                                        .build())
                        .build();

        assertThat(template1).isEqualTo(template2);
    }

    @Test
    public void equals_differentTemplates_returnsFalse() {
        InCallTemplate template1 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();
        InCallTemplate template2 =
                new InCallTemplate.Builder()
                        .setTitle("hi")
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();

        assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    public void hashCode_sameTemplates_areNotEqual() {
        InCallTemplate template1 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();
        InCallTemplate template2 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();

        assertThat(template1.hashCode()).isEqualTo(template2.hashCode());
    }

    @Test
    public void hashCode_differentTemplates_areNotEqual() {
        InCallTemplate template1 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();
        InCallTemplate template2 =
                new InCallTemplate.Builder()
                        .setTitle("hi")
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();

        assertThat(template1.hashCode()).isNotEqualTo(template2.hashCode());
    }

    @Test
    public void copy_noChanges_templatesAreEqual() {
        InCallTemplate template1 =
                new InCallTemplate.Builder()
                        .setTitle(mTitle)
                        .setIcon(new CarIcon.Builder(mIcon).build())
                        .build();
        InCallTemplate template2 = new InCallTemplate.Builder(template1).build();

        assertThat(template1).isEqualTo(template2);
    }
}
