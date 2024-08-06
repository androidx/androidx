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
import androidx.car.app.TestUtils;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link Row}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RowTest {
    @Test
    public void create_defaultValues() {
        Row row = new Row.Builder().setTitle("Title").build();
        assertThat(row.getTitle().toString()).isEqualTo("Title");
        assertThat(row.getTexts()).isEmpty();
        assertThat(row.getImage()).isNull();
        assertThat(row.getOnClickDelegate()).isNull();
        assertThat(row.isBrowsable()).isFalse();
        assertThat(row.getMetadata()).isEqualTo(Metadata.EMPTY_METADATA);
        assertThat(row.getRowImageType()).isEqualTo(Row.IMAGE_TYPE_SMALL);
        assertThat(row.isEnabled()).isTrue();
    }

    @Test
    public void title_charSequence() {
        String title = "foo";
        Row row = new Row.Builder().setTitle(title).build();
        assertThat(CarText.create(title)).isEqualTo(row.getTitle());
    }

    @Test
    public void text_charSequence() {
        String text1 = "foo";
        String text2 = "bar";
        Row row = new Row.Builder().setTitle("Title").addText(text1).addText(text2).build();
        assertThat(row.getTexts()).containsExactly(CarText.create(text1), CarText.create(text2));
    }

    @Test
    public void title_text_variants() {
        List<CharSequence> titleVariants = new ArrayList<>();
        titleVariants.add("foo");
        titleVariants.add("foo long");

        List<CharSequence> textVariants = new ArrayList<>();
        textVariants.add("bar");
        textVariants.add("bar long");

        CarText title =
                new CarText.Builder(titleVariants.get(0)).addVariant(titleVariants.get(1)).build();
        CarText text = new CarText.Builder(textVariants.get(0)).addVariant(
                textVariants.get(1)).build();
        Row row = new Row.Builder().setTitle(title).addText(text).build();
        assertThat(title).isEqualTo(row.getTitle());
        assertThat(row.getTexts()).containsExactly(text);
    }

    @Test
    public void title_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle(title).build());
        CarText title2 = TestUtils.getCarTextVariantsWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle(title2).build());

        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new Row.Builder().setTitle(title3).build();
        CarText title4 = TestUtils.getCarTextVariantsWithDistanceAndDurationSpans("Title");
        new Row.Builder().setTitle(title4).build();
    }

    @Test
    public void text_unsupportedSpans_throws() {
        CharSequence text = TestUtils.getCharSequenceWithClickableSpan("Text");
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title").addText(text).build());
        CarText text2 = TestUtils.getCarTextVariantsWithClickableSpan("Text");
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title").addText(text2).build());

        // DurationSpan and DistanceSpan do not throw
        CharSequence text3 = TestUtils.getCharSequenceWithColorSpan("Text");
        new Row.Builder().setTitle("Title").addText(text3).build();
        CarText text4 = TestUtils.getCarTextVariantsWithColorSpan("Text");
        new Row.Builder().setTitle("Title").addText(text4).build();
    }

    @Test
    public void setImage() {
        CarIcon image1 = BACK;
        Row row = new Row.Builder().setTitle("Title").setImage(image1).build();
        assertThat(image1).isEqualTo(row.getImage());
    }

    @Test
    public void setExtraSmallImage() {
        CarIcon image1 = BACK;
        Row row = new Row.Builder().setTitle("Title")
                .setImage(image1, Row.IMAGE_TYPE_EXTRA_SMALL).build();
        assertThat(image1).isEqualTo(row.getImage());
    }

    @Test
    public void setDecoration_positiveValue() {
        int decoration = 5;
        Row row = new Row.Builder().setTitle("Title").setNumericDecoration(decoration).build();
        assertThat(decoration).isEqualTo(row.getNumericDecoration());
    }

    @Test
    public void setDecoration_zero() {
        int decoration = 0;
        Row row = new Row.Builder().setTitle("Title").setNumericDecoration(decoration).build();
        assertThat(decoration).isEqualTo(row.getNumericDecoration());
    }

    @Test
    public void setDecoration_noDecoration() {
        int decoration = Row.NO_DECORATION;
        Row row = new Row.Builder().setTitle("Title").setNumericDecoration(decoration).build();
        assertThat(decoration).isEqualTo(row.getNumericDecoration());
    }

    @Test
    public void setDecoration_negative_throws() {
        int decoration = -123;
        Row.Builder rowBuilder =
                new Row.Builder().setTitle("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> rowBuilder.setNumericDecoration(decoration)
        );
    }

    public void setDecoration_withToggle_throws() {
        Toggle toggle = new Toggle.Builder(isChecked -> {}).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title")
                        .setToggle(toggle)
                        .setNumericDecoration(5)
                        .build());
    }

    @Test
    public void setToggle() {
        Toggle toggle1 = new Toggle.Builder(isChecked -> {
        }).build();
        Row row = new Row.Builder().setTitle("Title").setToggle(toggle1).build();
        assertThat(toggle1).isEqualTo(row.getToggle());
    }

    @Test
    public void setOnClickListenerAndToggle_throws() {
        Toggle toggle1 = new Toggle.Builder(isChecked -> {
        }).build();
        assertThrows(
                IllegalStateException.class,
                () ->
                        new Row.Builder()
                                .setTitle("Title")
                                .setOnClickListener(() -> {
                                })
                                .setToggle(toggle1)
                                .build());
    }

    @Test
    public void clickListener() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Row row = new Row.Builder().setTitle("Title").setOnClickListener(onClickListener).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        row.getOnClickDelegate().sendClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void clickDelegate() {
        OnClickDelegate onClickDelegate = mock(OnClickDelegate.class);
        Row row = new Row.Builder().setTitle("Title").setOnClickDelegate(onClickDelegate).build();

        assertThat(row.getOnClickDelegate()).isEqualTo(onClickDelegate);
    }

    @Test
    public void addAction() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = new Action.Builder().setIcon(icon).build();
        Row row = new Row.Builder()
                .setTitle("Title")
                .addAction(customAction)
                .build();
        assertThat(row.getActions()).containsExactly(customAction);
    }

    @Test
    public void addAction_appIconInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title").addAction(Action.APP_ICON).build());
    }

    @Test
    public void addAction_backInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title").addAction(Action.BACK).build());
    }

    @Test
    public void addAction_panInvalid_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title").addAction(Action.PAN).build());
    }

    @Test
    public void addAction_threeActions_throws() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction("Title", carIcon);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title")
                        .addAction(customAction)
                        .addAction(customAction)
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void addAction_twoActionsWithOnePrimary_doesNotThrow() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action primaryAction = new Action.Builder().setTitle("Title").setFlags(
                Action.FLAG_PRIMARY).build();
        Action customAction = TestUtils.createAction("Title", carIcon);
        Row row = new Row.Builder().setTitle("Title")
                .addAction(customAction)
                .addAction(primaryAction)
                .build();

        assertThat(row.getActions().size()).isEqualTo(2);
    }

    @Test
    public void addAction_twoActionsWithOneTimed_doesNotThrow() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action defaultAction = new Action.Builder().setTitle("Title").setFlags(
                Action.FLAG_DEFAULT).build();
        Action customAction = TestUtils.createAction("Title", carIcon);
        Row row = new Row.Builder().setTitle("Title")
                .addAction(customAction)
                .addAction(defaultAction)
                .build();

        assertThat(row.getActions().size()).isEqualTo(2);
    }

    @Test
    public void addAction_textOnlyActionNullIcon_doesNotThrow() {
        Action customAction = TestUtils.createAction("Title", null);
        Row row = new Row.Builder().setTitle("Title")
                .addAction(customAction)
                .build();
        assertThat(row.getActions().get(0)).isEqualTo(customAction);
    }

    public void addAction_browsableRow_throws() {
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction("Title", carIcon);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title")
                        .setBrowsable(true)
                        .addAction(customAction)
                        .build());
    }

    public void addAction_withToggle_throws() {
        Toggle toggle = new Toggle.Builder(isChecked -> {}).build();
        CarIcon carIcon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        Action customAction = TestUtils.createAction("Title", carIcon);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Row.Builder().setTitle("Title")
                        .setToggle(toggle)
                        .addAction(customAction)
                        .build());
    }

    @Test
    public void setMetadata() {
        Metadata metadata =
                new Metadata.Builder().setPlace(
                        new Place.Builder(CarLocation.create(1, 1)).build()).build();
        Row row = new Row.Builder().setTitle("Title").setMetadata(metadata).build();
        assertThat(row.getMetadata()).isEqualTo(metadata);
    }

    @Test
    public void setEnabledState() {
        Row row = new Row.Builder().setTitle("Title").setEnabled(false).build();
        assertThat(row.isEnabled()).isFalse();
    }

    @Test
    public void setIsBrowsable_noListener_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Row.Builder().setTitle("Title").setBrowsable(true).build());

        // Positive case.
        new Row.Builder().setTitle("Title").setBrowsable(false).build();
    }

    @Test
    public void setIsBrowsable_notExclusivelyTextOrImage_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new Row.Builder()
                                .setTitle("Title")
                                .setBrowsable(true)
                                .setToggle(new Toggle.Builder(state -> {
                                }).build())
                                .build());

        // Positive case.
        new Row.Builder()
                .setBrowsable(true)
                .setOnClickListener(() -> {
                })
                .setTitle("Title")
                .addText("Text")
                .setImage(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .build();
    }

    @Test
    public void equals() {
        String title = "title";

        Row row =
                new Row.Builder()
                        .setTitle(title)
                        .setImage(BACK)
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(false)
                        .setMetadata(Metadata.EMPTY_METADATA)
                        .setEnabled(true)
                        .addText(title)
                        .build();

        assertThat(
                new Row.Builder()
                        .setTitle(title)
                        .setImage(BACK)
                        .setOnClickListener(() -> {
                        })
                        .setBrowsable(false)
                        .setMetadata(Metadata.EMPTY_METADATA)
                        .setEnabled(true)
                        .addText(title)
                        .build())
                .isEqualTo(row);
    }

    @Test
    public void notEquals_differentTitle() {
        String title = "title";

        Row row = new Row.Builder().setTitle(title).build();

        assertThat(new Row.Builder().setTitle("foo").build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentImage() {
        Row row = new Row.Builder().setTitle("Title").setImage(BACK).build();

        assertThat(new Row.Builder().setTitle("Title").setImage(ALERT).build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentEnabledState() {
        Row row = new Row.Builder().setTitle("Title").setEnabled(true).build();

        assertThat(new Row.Builder().setTitle("Title").setEnabled(false).build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_oneHasNoCallback() {
        Row row = new Row.Builder().setTitle("Title").setOnClickListener(() -> {
        }).build();

        assertThat(new Row.Builder().setTitle("Title").build()).isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentBrowsable() {
        Row row =
                new Row.Builder().setTitle("Title").setBrowsable(false).setOnClickListener(() -> {
                }).build();

        assertThat(
                new Row.Builder()
                        .setTitle("Title")
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .build())
                .isNotEqualTo(row);
    }

    @Test
    public void notEquals_differentMetadata() {
        Row row = new Row.Builder().setTitle("Title").setMetadata(Metadata.EMPTY_METADATA).build();

        assertThat(
                new Row.Builder()
                        .setTitle("Title")
                        .setMetadata(
                                new Metadata.Builder()
                                        .setPlace(
                                                new Place.Builder(CarLocation.create(/* latitude= */
                                                        1f, /* longitude= */ 1f))
                                                        .build())
                                        .build())
                        .build())
                .isNotEqualTo(row);
    }

    @Test
    public void notEquals_differenText() {
        Row row = new Row.Builder().setTitle("Title").addText("foo").build();

        assertThat(new Row.Builder().setTitle("Title").addText("bar").build()).isNotEqualTo(row);
    }
}
