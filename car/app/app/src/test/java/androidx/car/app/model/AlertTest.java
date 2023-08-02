/*
 * Copyright 2022 The Android Open Source Project
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
public class AlertTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Test
    public void create_throws_invalidDuration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Alert.Builder(1, CarText.create("title"), 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Alert.Builder(1, CarText.create("title"), -1));
    }

    @Test
    public void create_throws_invalid_carIcon() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        CarText title = CarText.create("title");
        assertThrows(IllegalArgumentException.class,
                () -> new Alert.Builder(1, title, 10).setIcon(carIcon));
    }

    @Test
    public void create_default() {
        CarText title = CarText.create("title");
        Alert alert = new Alert.Builder(123, title, 1234).build();
        assertThat(alert.getId()).isEqualTo(123);
        assertThat(alert.getTitle()).isEqualTo(title);
        assertThat(alert.getDurationMillis()).isEqualTo(1234);
        assertThat(alert.getIcon()).isNull();
        assertThat(alert.getSubtitle()).isNull();
        assertThat(alert.getActions()).isEmpty();
        assertThat(alert.getCallbackDelegate()).isNull();
    }

    @Test
    public void create_setIcon() {
        CarIcon icon = TestUtils.getTestCarIcon(ApplicationProvider.getApplicationContext(),
                "ic_test_1");
        CarText title = CarText.create("title");
        Alert alert = new Alert.Builder(123, title, 1234).setIcon(icon).build();
        assertThat(alert.getIcon()).isEqualTo(icon);
    }

    @Test
    public void create_titleHasTextVariants() {
        CarText title = new CarText.Builder("foo long text").addVariant("foo").build();
        Alert alert = new Alert.Builder(123, title, 1234).build();
        assertThat(alert.getTitle()).isNotNull();
        assertThat(alert.getTitle().toCharSequence().toString()).isEqualTo("foo long text");
        assertThat(alert.getTitle().getVariants().get(0).toString()).isEqualTo("foo");
    }

    @Test
    public void create_withActions() {
        CarText title = CarText.create("title");
        Action action1 = Action.PAN;
        Action action2 = Action.BACK;
        Alert alert = new Alert.Builder(123, title, 1234).addAction(action1)
                .addAction(action2).build();
        assertThat(alert.getActions()).isNotNull();
        assertThat(alert.getActions().size()).isEqualTo(2);
        assertThat(alert.getActions().get(0)).isEqualTo(action1);
        assertThat(alert.getActions().get(1)).isEqualTo(action2);
    }

    @Test
    public void create_throws_tooManyActions() {
        CarText title = CarText.create("title");
        Action action1 = Action.PAN;
        Action action2 = Action.BACK;
        Action action3 = Action.APP_ICON;
        assertThrows(
                IllegalStateException.class,
                () -> new Alert.Builder(1, title, 1234).addAction(action1)
                        .addAction(action2).addAction(action3));
    }

    @Test
    public void onDismiss() {
        AlertCallback alertCallback = mock(AlertCallback.class);
        CarText title = CarText.create("foo");
        Alert alert = new Alert.Builder(123, title, 1234)
                .setCallback(alertCallback)
                .build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        assertThat(alert.getCallbackDelegate()).isNotNull();
        alert.getCallbackDelegate().sendDismiss(onDoneCallback);
        verify(alertCallback).onDismiss();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void equals() {
        CarText title = CarText.create("foo");
        CarText subtitle = CarText.create("sub foo");
        CarIcon icon = CarIcon.ALERT;

        Alert alert1 =
                new Alert.Builder(1, title, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();
        Alert alert2 =
                new Alert.Builder(1, title, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();

        assertThat(alert1).isEqualTo(alert2);
    }

    @Test
    public void notEquals_nonMatchingId() {
        CarText title = CarText.create("foo");
        CarText subtitle = CarText.create("sub foo");
        CarIcon icon = CarIcon.ALERT;

        Alert alert1 =
                new Alert.Builder(1, title, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();
        Alert alert2 =
                new Alert.Builder(2, title, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();

        assertThat(alert1).isNotEqualTo(alert2);
    }

    @Test
    public void equals_nonMatchingTitle() {
        CarText title1 = CarText.create("foo");
        CarText title2 = CarText.create("bar");
        CarText subtitle = CarText.create("sub foo");
        CarIcon icon = CarIcon.ALERT;

        Alert alert1 =
                new Alert.Builder(1, title1, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();
        Alert alert2 =
                new Alert.Builder(1, title2, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();

        assertThat(alert1).isEqualTo(alert2);
    }

    @Test
    public void equals_nonMatchingIcon() {
        CarText title = CarText.create("foo");
        CarText subtitle = CarText.create("sub foo");
        CarIcon icon1 = CarIcon.ALERT;
        CarIcon icon2 = CarIcon.BACK;

        Alert alert1 =
                new Alert.Builder(1, title, 12).setSubtitle(subtitle)
                        .setIcon(icon1).build();
        Alert alert2 =
                new Alert.Builder(1, title, 12).setSubtitle(subtitle)
                        .setIcon(icon2).build();

        assertThat(alert1).isEqualTo(alert2);
    }

    @Test
    public void equals_nonMatchingDuration() {
        CarText title = CarText.create("foo");
        CarText subtitle = CarText.create("sub foo");
        CarIcon icon = CarIcon.ALERT;

        Alert alert1 =
                new Alert.Builder(1, title, 12).setSubtitle(subtitle)
                        .setIcon(icon).build();
        Alert alert2 =
                new Alert.Builder(1, title, 13).setSubtitle(subtitle)
                        .setIcon(icon).build();

        assertThat(alert1).isEqualTo(alert2);
    }


}
