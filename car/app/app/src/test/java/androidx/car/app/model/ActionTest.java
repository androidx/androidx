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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Action}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ActionTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private IOnDoneCallback.Stub mMockOnDoneCallback;

    @Test
    public void create_throws_noTitleOrIcon() {
        assertThrows(
                IllegalStateException.class, () -> new Action.Builder().setOnClickListener(() -> {
                }).build());
        assertThrows(
                IllegalStateException.class,
                () -> new Action.Builder().setOnClickListener(() -> {
                }).setTitle("").build());
    }

    @Test
    public void create_throws_invalid_carIcon() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();

        assertThrows(IllegalArgumentException.class, () -> new Action.Builder().setIcon(carIcon));
    }

    @Test
    public void create_noTitleDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder()
                .setIcon(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .setOnClickListener(onClickListener)
                .build();
        assertThat(action.getTitle()).isNull();
    }

    @Test
    public void create_noIconDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder().setTitle("foo").setOnClickListener(
                onClickListener).build();
        assertThat(action.getIcon()).isNull();
    }

    @Test
    public void create_titleHasTextVariants() {
        CarText title = new CarText.Builder("foo long text").addVariant("foo").build();
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder().setTitle(title).setOnClickListener(
                onClickListener).build();
        assertThat(action.getTitle()).isNotNull();
        assertThat(action.getTitle().toCharSequence().toString()).isEqualTo("foo long text");
        assertThat(action.getTitle().getVariants().get(0).toString()).isEqualTo("foo");
    }

    @Test
    public void create_noBackgroundColorDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder().setTitle("foo").setOnClickListener(
                onClickListener).build();
        assertThat(action.getBackgroundColor()).isEqualTo(CarColor.DEFAULT);
    }

    @Test
    public void createInstance() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Context context = ApplicationProvider.getApplicationContext();
        IconCompat icon = IconCompat.createWithResource(
                context, TestUtils.getTestDrawableResId(context, "ic_test_1"));
        String title = "foo";
        Action action = new Action.Builder()
                .setTitle(title)
                .setIcon(new CarIcon.Builder(icon).build())
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(onClickListener)
                .build();
        assertThat(icon).isEqualTo(action.getIcon().getIcon());
        assertThat(CarText.create(title)).isEqualTo(action.getTitle());
        assertThat(CarColor.BLUE).isEqualTo(action.getBackgroundColor());
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        action.getOnClickDelegate().sendClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void create_panMode() {
        Action action = new Action.Builder(Action.PAN)
                .setIcon(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .build();
        assertThat(action.getTitle()).isNull();
    }

    @Test
    public void create_panMode_hasOnClickListener_throws() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        assertThrows(IllegalStateException.class,
                () -> new Action.Builder(Action.PAN)
                        .setIcon(TestUtils.getTestCarIcon(
                                ApplicationProvider.getApplicationContext(),
                                "ic_test_1"))
                        .setOnClickListener(onClickListener)
                        .build());
    }

    @Test
    public void equals() {
        String title = "foo";
        CarIcon icon = CarIcon.ALERT;

        Action action1 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).build();
        Action action2 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).build();

        assertThat(action2).isEqualTo(action1);
    }

    @Test
    public void notEquals_nonMatchingTitle() {
        String title = "foo";
        Action action1 = new Action.Builder().setOnClickListener(() -> {
        }).setTitle(title).build();
        Action action2 = new Action.Builder().setOnClickListener(() -> {
        }).setTitle("not foo").build();

        assertThat(action2).isNotEqualTo(action1);
    }

    @Test
    public void notEquals_nonMatchingIcon() {
        String title = "foo";
        CarIcon icon1 = CarIcon.ALERT;
        CarIcon icon2 = CarIcon.APP_ICON;

        Action action1 = new Action.Builder().setOnClickListener(() -> {
        }).setTitle(title).setIcon(icon1).build();
        Action action2 = new Action.Builder().setOnClickListener(() -> {
        }).setTitle(title).setIcon(icon2).build();

        assertThat(action2).isNotEqualTo(action1);
    }
}
