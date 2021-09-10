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

package androidx.car.app.navigation.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.car.app.TestUtils;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.core.graphics.drawable.IconCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link MessageInfoTest}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class MessageInfoTest {

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageInfo.Builder("Message").setImage(carIcon));
    }

    /** Tests basic construction of a template with a minimal data. */
    @Test
    public void createMinimalInstance() {
        MessageInfo messageInfo = new MessageInfo.Builder("Message").build();
        assertThat(messageInfo.getTitle().toString()).isEqualTo("Message");
        assertThat(messageInfo.getText()).isNull();
        assertThat(messageInfo.getImage()).isNull();
    }

    /** Tests construction of a template with all data. */
    @Test
    public void createFullInstance() {
        MessageInfo messageInfo =
                new MessageInfo.Builder("Message").setImage(CarIcon.APP_ICON).setText(
                        "Secondary").build();
        assertThat(messageInfo.getTitle().toString()).isEqualTo("Message");
        assertThat(messageInfo.getText().toString()).isEqualTo("Secondary");
        assertThat(messageInfo.getImage()).isEqualTo(CarIcon.APP_ICON);
    }

    @Test
    public void no_message_throws() {
        assertThrows(NullPointerException.class,
                () -> new MessageInfo.Builder((CharSequence) null));
    }

    /** Tests construction of a template where title and text have variants. */
    @Test
    public void createInstanceWithTextVariants() {
        CarText title = new CarText.Builder("Message Long").addVariant("Message").build();
        CarText text = new CarText.Builder("Secondary Long").addVariant("Secondary").build();

        MessageInfo messageInfo =
                new MessageInfo.Builder(title).setImage(CarIcon.APP_ICON).setText(
                        text).build();
        assertThat(messageInfo.getTitle().toString()).isEqualTo("Message Long");
        assertThat(messageInfo.getTitle().getVariants().get(0).toString()).isEqualTo(
                "Message");
        assertThat(messageInfo.getText().toString()).isEqualTo("Secondary Long");
        assertThat(messageInfo.getText().getVariants().get(0).toString()).isEqualTo(
                "Secondary");
        assertThat(messageInfo.getImage()).isEqualTo(CarIcon.APP_ICON);
    }

    @Test
    public void title_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithColorSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageInfo.Builder(title));

        // DurationSpan and DistanceSpan do not throw
        CharSequence title2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new MessageInfo.Builder(title2).build();
    }

    @Test
    public void text_unsupportedSpans_throws() {
        CharSequence text = TestUtils.getCharSequenceWithColorSpan("Text");
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageInfo.Builder("title").setText(text));

        // DurationSpan and DistanceSpan do not throw
        CharSequence text2 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Text");
        new MessageInfo.Builder("title").setText(text2).build();
    }

    @Test
    public void equals() {
        final String title = "Primary";
        final String text = "Secondary";

        MessageInfo messageInfo =
                new MessageInfo.Builder(title).setText(text).setImage(CarIcon.APP_ICON).build();

        assertThat(messageInfo)
                .isEqualTo(new MessageInfo.Builder(title).setText(text).setImage(
                        CarIcon.APP_ICON).build());
    }

    @Test
    public void notEquals() {
        final String title = "Primary";
        final String text = "Secondary";

        MessageInfo messageInfo =
                new MessageInfo.Builder(title).setText(text).setImage(CarIcon.APP_ICON).build();

        assertThat(messageInfo)
                .isNotEqualTo(
                        new MessageInfo.Builder("Not Primary").setText(text).setImage(
                                CarIcon.APP_ICON).build());

        assertThat(messageInfo)
                .isNotEqualTo(
                        new MessageInfo.Builder(title).setText("Not Secondary").setImage(
                                CarIcon.APP_ICON).build());

        assertThat(messageInfo)
                .isNotEqualTo(new MessageInfo.Builder(title).setText(text).setImage(
                        CarIcon.ERROR).build());
    }

}
