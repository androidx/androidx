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

import static androidx.car.app.model.Action.FLAG_DEFAULT;
import static androidx.car.app.model.Action.FLAG_IS_PERSISTENT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    public void createComposeMessage_throws_hasListener() {
        assertThrows(
                IllegalStateException.class,
                () -> new Action.Builder(Action.COMPOSE_MESSAGE).setOnClickListener(() -> {
                }).build());
    }

    @Test
    public void createComposeMessage_throws_hasTitle() {
        assertThrows(
                IllegalStateException.class,
                () -> new Action.Builder(Action.COMPOSE_MESSAGE).setTitle("foo").build());
    }

    @Test
    public void createComposeMessage_setCustomizedIcon() {
        Context context = ApplicationProvider.getApplicationContext();
        IconCompat icon = IconCompat.createWithResource(
                context, TestUtils.getTestDrawableResId(context, "ic_test_1"));
        Action action = new Action.Builder()
                .setIcon(new CarIcon.Builder(icon).build())
                .build();
        assertThat(icon).isEqualTo(action.getIcon().getIcon());
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
    public void create_enabledStateDefault() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder()
                .setIcon(TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                        "ic_test_1"))
                .setTitle("foo")
                .setOnClickListener(onClickListener)
                .build();
        assertThat(action.isEnabled()).isTrue();
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
    public void create_primaryIsTrue() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder().setTitle("foo").setOnClickListener(
                onClickListener).setFlags(FLAG_PRIMARY).build();
        assertThat(action.getFlags() & FLAG_PRIMARY).isEqualTo(FLAG_PRIMARY);
    }

    @Test
    public void create_persistentIsTrue() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder().setTitle("foo").setOnClickListener(
                onClickListener).setFlags(FLAG_IS_PERSISTENT).build();
        assertThat(action.getFlags() & FLAG_IS_PERSISTENT).isEqualTo(FLAG_IS_PERSISTENT);
    }

    @Test
    public void create_defaultIsTrue() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Action action = new Action.Builder().setTitle("foo").setOnClickListener(
                onClickListener).setFlags(FLAG_DEFAULT).build();
        assertThat(action.getFlags() & FLAG_DEFAULT).isEqualTo(FLAG_DEFAULT);
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
                .setEnabled(true)
                .build();
        assertThat(icon).isEqualTo(action.getIcon().getIcon());
        assertThat(CarText.create(title)).isEqualTo(action.getTitle());
        assertThat(CarColor.BLUE).isEqualTo(action.getBackgroundColor());
        assertThat(action.isEnabled()).isTrue();
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
                }).setTitle(title).setIcon(icon).setFlags(FLAG_PRIMARY).setEnabled(true).build();
        Action action2 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).setFlags(FLAG_PRIMARY).setEnabled(true).build();

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

    @Test
    public void notEquals_nonMatchingFlags() {
        String title = "foo";
        CarIcon icon = CarIcon.ALERT;

        Action action1 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).setFlags(FLAG_PRIMARY).build();
        Action action2 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).build();

        assertThat(action2).isNotEqualTo(action1);
    }

    @Test
    public void notEquals_nonMatchingEnabledState() {
        String title = "foo";
        CarIcon icon = CarIcon.ALERT;

        Action action1 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).setEnabled(true).build();
        Action action2 =
                new Action.Builder().setOnClickListener(() -> {
                }).setTitle(title).setIcon(icon).setEnabled(false).build();

        assertThat(action2).isNotEqualTo(action1);
    }
}
