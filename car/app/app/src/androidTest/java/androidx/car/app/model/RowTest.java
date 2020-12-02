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

import static androidx.car.app.model.CarIcon.ALERT;
import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link Row}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class RowTest {
    @Test
    public void create_defaultValues() {
        Row row = Row.builder().setTitle("Title").build();
        assertThat(row.getTitle().getText()).isEqualTo("Title");
        assertThat(row.getTexts()).isEmpty();
        assertThat(row.getImage()).isNull();
        assertThat(row.getOnClickListener()).isNull();
        assertThat(row.isBrowsable()).isFalse();
        assertThat(row.getMetadata()).isEqualTo(Metadata.EMPTY_METADATA);
        assertThat(row.getRowImageType()).isEqualTo(Row.IMAGE_TYPE_SMALL);
    }

    @Test
    public void title_charSequence() {
        String title = "foo";
        Row row = Row.builder().setTitle(title).build();
        assertThat(CarText.create(title)).isEqualTo(row.getTitle());
    }

    @Test
    public void text_charSequence() {
        String text1 = "foo";
        String text2 = "bar";
        Row row = Row.builder().setTitle("Title").addText(text1).addText(text2).build();
        assertThat(row.getTexts()).containsExactly(CarText.create(text1), CarText.create(text2));
    }

    @Test
    public void setImage() {
        CarIcon image1 = BACK;
        Row row = Row.builder().setTitle("Title").setImage(image1).build();
        assertThat(image1).isEqualTo(row.getImage());
    }

    @Test
    public void setToggle() {
        Toggle toggle1 = Toggle.builder(isChecked -> {
        }).build();
        Row row = Row.builder().setTitle("Title").setToggle(toggle1).build();
        assertThat(toggle1).isEqualTo(row.getToggle());
    }

    @Test
    public void setOnClickListenerAndToggle_throws() {
        Toggle toggle1 = Toggle.builder(isChecked -> {
        }).build();
        assertThrows(
                IllegalStateException.class,
                () ->
                        Row.builder()
                                .setTitle("Title")
                                .setOnClickListener(() -> {
                                })
                                .setToggle(toggle1)
                                .build());
    }

    @Test
    @UiThreadTest
    public void clickListener() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Row row = Row.builder().setTitle("Title").setOnClickListener(onClickListener).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        row.getOnClickListener().onClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void setMetadata() {
        Metadata metadata = Metadata.ofPlace(Place.builder(LatLng.create(1, 1)).build());

        Row row = Row.builder().setTitle("Title").setMetadata(metadata).build();
        assertThat(row.getMetadata()).isEqualTo(metadata);
    }

    @Test
    public void setIsBrowsable_noListener_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> Row.builder().setTitle("Title").setBrowsable(true).build());

        // Positive case.
        Row.builder().setTitle("Title").setBrowsable(false).build();
    }

    @Test
    public void setIsBrowsable_notExclusivelyTextOrImage_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        Row.builder()
                                .setTitle("Title")
                                .setBrowsable(true)
                                .setToggle(Toggle.builder(state -> {
                                }).build())
                                .build());

        // Positive case.
        Row.builder()
                .setBrowsable(true)
                .setOnClickListener(() -> {
                })
                .setTitle("Title")
                .addText("Text")
                .setImage(
                        CarIcon.of(
                                IconCompat.createWithResource(
                                        ApplicationProvider.getApplicationContext(),
                                        R.drawable.ic_test_1)))
                .build();
    }

    @Test
    public void equals() {
        String title = "title";

        Row row =
                Row.builder()
                        .setTitle(title)
                        .setImage(BACK)
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(false)
                        .setMetadata(Metadata.EMPTY_METADATA)
                        .addText(title)
                        .build();

        assertThat(
                Row.builder()
                        .setTitle(title)
                        .setImage(BACK)
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(false)
                        .setMetadata(Metadata.EMPTY_METADATA)
                        .addText(title)
                        .build())
                .isEqualTo(row);
    }

    @Test
    public void notEquals_differentTitle() {
        String title = "title";

        Row row = Row.builder().setTitle(title).build();

        assertThat(Row.builder().setTitle("foo").build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentImage() {
        Row row = Row.builder().setTitle("Title").setImage(BACK).build();

        assertThat(Row.builder().setTitle("Title").setImage(ALERT).build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_oneHasNoCallback() {
        Row row = Row.builder().setTitle("Title").setOnClickListener(() -> {
        }).build();

        assertThat(Row.builder().setTitle("Title").build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentBrowsable() {
        Row row =
                Row.builder().setTitle("Title").setBrowsable(false).setOnClickListener(() -> {
                }).build();

        assertThat(
                Row.builder()
                        .setTitle("Title")
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .build())
                .isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentMetadata() {
        Row row = Row.builder().setTitle("Title").setMetadata(Metadata.EMPTY_METADATA).build();

        assertThat(
                Row.builder()
                        .setTitle("Title")
                        .setMetadata(
                                Metadata.builder()
                                        .setPlace(
                                                Place.builder(LatLng.create(/* latitude= */
                                                        1f, /* longitude= */ 1f))
                                                        .build())
                                        .build())
                        .build())
                .isNotEqualTo(row);
    }

    @Test
    public void notEquals_differenText() {
        Row row = Row.builder().setTitle("Title").addText("foo").build();

        assertThat(Row.builder().setTitle("Title").addText("bar").build()).isNotEqualTo(row);
    }
}
