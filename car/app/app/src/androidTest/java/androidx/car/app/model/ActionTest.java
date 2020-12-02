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
import android.net.Uri;

import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.test.R;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Tests for {@link Action}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ActionTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private IOnDoneCallback.Stub mMockOnDoneCallback;

    @Test
    public void create_throws_noTitleOrIcon() {
        assertThrows(
                IllegalStateException.class, () -> Action.builder().setOnClickListener(() -> {
                }).build());
        assertThrows(
                IllegalStateException.class,
                () -> Action.builder().setOnClickListener(() -> {
                }).setTitle("").build());
    }

    @Test
    public void create_throws_invalid_carIcon() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));

        assertThrows(IllegalArgumentException.class, () -> Action.builder().setIcon(carIcon));
    }

    @Test
    public void create_throws_customBackgroundColor() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        assertThrows(
                IllegalArgumentException.class,
                () -> Action.builder()
                                .setTitle("foo")
                                .setOnClickListener(onClickListener)
                                .setBackgroundColor(CarColor.createCustom(0xdead, 0xbeef))
                                .build());
    }

    @Test
    public void create_noTitleDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = Action.builder()
                        .setIcon(
                                CarIcon.of(
                                        IconCompat.createWithResource(
                                                ApplicationProvider.getApplicationContext(),
                                                R.drawable.ic_test_1)))
                        .setOnClickListener(onClickListener)
                        .build();
        assertThat(action.getTitle()).isNull();
    }

    @Test
    public void create_noIconDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = Action.builder().setTitle("foo").setOnClickListener(
                onClickListener).build();
        assertThat(action.getIcon()).isNull();
    }

    @Test
    public void create_noBackgroundColorDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = Action.builder().setTitle("foo").setOnClickListener(
                onClickListener).build();
        assertThat(action.getBackgroundColor()).isEqualTo(CarColor.DEFAULT);
    }

    @Test
    @UiThreadTest
    public void createInstance() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        IconCompat icon =
                IconCompat.createWithResource(
                        ApplicationProvider.getApplicationContext(), R.drawable.ic_test_1);
        String title = "foo";
        Action action = Action.builder()
                        .setTitle(title)
                        .setIcon(CarIcon.of(icon))
                        .setBackgroundColor(CarColor.BLUE)
                        .setOnClickListener(onClickListener)
                        .build();
        assertThat(icon).isEqualTo(action.getIcon().getIcon());
        assertThat(CarText.create(title)).isEqualTo(action.getTitle());
        assertThat(CarColor.BLUE).isEqualTo(action.getBackgroundColor());
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        action.getOnClickListener().onClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void create_invalidSetOnBackThrows() {
        assertThrows(
                IllegalStateException.class,
                () -> Action.BACK.newBuilder().setOnClickListener(() -> {
                }).build());
        assertThrows(
                IllegalStateException.class,
                () -> Action.BACK.newBuilder().setTitle("BACK").build());
        assertThrows(
                IllegalStateException.class,
                () -> Action.BACK.newBuilder().setIcon(CarIcon.ALERT).build());
    }

    @Test
    public void create_invalidSetOnAppIconThrows() {
        assertThrows(
                IllegalStateException.class,
                () -> Action.APP_ICON.newBuilder().setOnClickListener(() -> {
                }).build());
        assertThrows(
                IllegalStateException.class,
                () -> Action.APP_ICON.newBuilder().setTitle("APP").build());
        assertThrows(
                IllegalStateException.class,
                () -> Action.APP_ICON.newBuilder().setIcon(CarIcon.ALERT).build());
    }

    @Test
    public void equals() {
        String title = "foo";
        CarIcon icon = CarIcon.ALERT;

        Action action1 =
                Action.builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).build();
        Action action2 =
                Action.builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).build();

        assertThat(action2).isEqualTo(action1);
    }

    @Test
    public void notEquals_nonMatchingTitle() {
        String title = "foo";
        Action action1 = Action.builder().setOnClickListener(() -> {
        }).setTitle(title).build();
        Action action2 = Action.builder().setOnClickListener(() -> {
        }).setTitle("not foo").build();

        assertThat(action2).isNotEqualTo(action1);
    }

    @Test
    public void notEquals_nonMatchingIcon() {
        String title = "foo";
        CarIcon icon1 = CarIcon.ALERT;
        CarIcon icon2 = CarIcon.APP_ICON;

        Action action1 = Action.builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon1).build();
        Action action2 = Action.builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon2).build();

        assertThat(action2).isNotEqualTo(action1);
    }
}
