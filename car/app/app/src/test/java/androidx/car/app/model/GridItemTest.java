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

import android.os.RemoteException;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link GridItem}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class GridItemTest {

    @Test
    public void create_defaultValues() {
        GridItem gridItem = new GridItem.Builder().setTitle("Title").setImage(BACK).build();

        assertThat(BACK).isEqualTo(gridItem.getImage());
        assertThat(gridItem.getImageType()).isEqualTo(GridItem.IMAGE_TYPE_LARGE);
        assertThat(gridItem.getTitle()).isNotNull();
        assertThat(gridItem.getText()).isNull();
    }

    @Test
    public void create_isLoading() {
        GridItem gridItem = new GridItem.Builder().setTitle("Title").setLoading(true).build();
        assertThat(gridItem.isLoading()).isTrue();
    }

    @Test
    public void title_charSequence() {
        String title = "foo";
        GridItem gridItem = new GridItem.Builder().setTitle(title).setImage(BACK).build();

        assertThat(CarText.create(title)).isEqualTo(gridItem.getTitle());
    }

    @Test
    public void title_variants() {
        CarText title = new CarText.Builder("Foo Long").addVariant("Foo").build();
        GridItem gridItem = new GridItem.Builder().setTitle(title).setImage(BACK).build();

        assertThat(gridItem.getTitle().toString()).isEqualTo("Foo Long");
        assertThat(gridItem.getTitle().getVariants().get(0).toString()).isEqualTo("Foo");
    }

    @Test
    public void title_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridItem.Builder().setTitle(title));
        CarText title2 = TestUtils.getCarTextVariantsWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridItem.Builder().setTitle(title2));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new GridItem.Builder().setTitle(title3).setImage(BACK).build();
        CarText title4 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        new GridItem.Builder().setTitle(title4).setImage(BACK).build();
    }

    @Test
    public void title_throwsIfNotSet() {
        // Not set
        assertThrows(IllegalStateException.class,
                () -> new GridItem.Builder().setImage(BACK).build());

        // Not set
        assertThrows(
                IllegalArgumentException.class, () -> new GridItem.Builder().setTitle("").setImage(
                        BACK).build());
    }

    @Test
    public void text_charSequence() {
        String text = "foo";
        GridItem gridItem = new GridItem.Builder().setTitle("title").setText(text).setImage(
                BACK).build();

        assertThat(CarText.create(text)).isEqualTo(gridItem.getText());
    }

    @Test
    public void text_variants() {
        CarText text = new CarText.Builder("Foo Long").addVariant("Foo").build();
        GridItem gridItem = new GridItem.Builder().setTitle("title").setText(text).setImage(
                BACK).build();

        assertThat(gridItem.getText().toString()).isEqualTo("Foo Long");
        assertThat(gridItem.getText().getVariants().get(0).toString()).isEqualTo("Foo");
    }

    @Test
    public void textWithoutTitle_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new GridItem.Builder().setText("text").setImage(BACK).build());
    }

    @Test
    public void text_unsupportedSpans_throws() {
        CharSequence text = TestUtils.getCharSequenceWithClickableSpan("Text");
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridItem.Builder().setTitle("Title").setText(text));
        CarText text2 = TestUtils.getCarTextVariantsWithClickableSpan("Text");
        assertThrows(
                IllegalArgumentException.class,
                () -> new GridItem.Builder().setTitle("Title").setText(text2));

        // DurationSpan and DistanceSpan do not throw
        CharSequence text3 = TestUtils.getCharSequenceWithColorSpan("Text");
        new GridItem.Builder().setTitle("Title").setText(text3).setImage(BACK).build();
        CarText text4 = TestUtils.getCarTextVariantsWithColorSpan("Text");
        new GridItem.Builder().setTitle("Title").setText(text4).setImage(BACK).build();
    }

    @Test
    public void setIsLoading_contentsSet_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new GridItem.Builder().setLoading(true).setTitle("foo").setImage(
                        BACK).build());
    }

    @Test
    public void create_noImage_throws() {
        assertThrows(IllegalStateException.class,
                () -> new GridItem.Builder().setTitle("foo").build());
    }

    @Test
    public void equals() {
        String title = "title";
        String text = "text";
        GridItem gridItem = new GridItem.Builder().setTitle(title).setText(text).setImage(
                BACK).build();

        assertThat(new GridItem.Builder().setTitle(title).setText(text).setImage(BACK).build())
                .isEqualTo(gridItem);
    }

    @Test
    public void notEquals_differentTitle() {
        String title = "title";
        GridItem gridItem = new GridItem.Builder().setTitle(title).setImage(BACK).build();

        assertThat(new GridItem.Builder().setTitle("foo").setImage(BACK).build()).isNotEqualTo(
                gridItem);
    }

    @Test
    public void notEquals_differentText() {
        String title = "title";
        String text = "text";
        GridItem gridItem = new GridItem.Builder().setTitle(title).setText(text).setImage(
                BACK).build();

        assertThat(new GridItem.Builder().setTitle(title).setText("foo").setImage(BACK).build())
                .isNotEqualTo(gridItem);
    }

    @Test
    public void notEquals_differentImage() {
        GridItem gridItem = new GridItem.Builder().setTitle("Title").setImage(BACK).build();

        assertThat(new GridItem.Builder().setImage(ALERT).setTitle("Title").build()).isNotEqualTo(
                gridItem);
    }

    @Test
    public void clickListener() throws RemoteException {
        OnClickListener onClickListener = mock(OnClickListener.class);
        GridItem gridItem =
                new GridItem.Builder().setTitle("Title").setImage(BACK).setOnClickListener(
                        onClickListener).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        gridItem.getOnClickDelegate().sendClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }
}
