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

import androidx.car.app.model.CarIcon;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link MessageInfoTest}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class MessageInfoTest {

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));
        assertThrows(
                IllegalArgumentException.class,
                () -> MessageInfo.builder("Message").setImage(carIcon));
    }

    /** Tests basic construction of a template with a minimal data. */
    @Test
    public void createMinimalInstance() {
        MessageInfo messageInfo = MessageInfo.builder("Message").build();
        assertThat(messageInfo.getTitle().getText()).isEqualTo("Message");
        assertThat(messageInfo.getText()).isNull();
        assertThat(messageInfo.getImage()).isNull();
    }

    /** Tests construction of a template with all data. */
    @Test
    public void createFullInstance() {
        MessageInfo messageInfo =
                MessageInfo.builder("Message").setImage(CarIcon.APP_ICON).setText(
                        "Secondary").build();
        assertThat(messageInfo.getTitle().getText()).isEqualTo("Message");
        assertThat(messageInfo.getText().getText()).isEqualTo("Secondary");
        assertThat(messageInfo.getImage()).isEqualTo(CarIcon.APP_ICON);
    }

    @Test
    public void no_message_throws() {
        assertThrows(NullPointerException.class, () -> MessageInfo.builder(null));
    }

    @Test
    public void equals() {
        final String title = "Primary";
        final String text = "Secondary";

        MessageInfo messageInfo =
                MessageInfo.builder(title).setText(text).setImage(CarIcon.APP_ICON).build();

        assertThat(messageInfo)
                .isEqualTo(MessageInfo.builder(title).setText(text).setImage(
                        CarIcon.APP_ICON).build());
    }

    @Test
    public void notEquals() {
        final String title = "Primary";
        final String text = "Secondary";

        MessageInfo messageInfo =
                MessageInfo.builder(title).setText(text).setImage(CarIcon.APP_ICON).build();

        assertThat(messageInfo)
                .isNotEqualTo(
                        MessageInfo.builder("Not Primary").setText(text).setImage(
                                CarIcon.APP_ICON).build());

        assertThat(messageInfo)
                .isNotEqualTo(
                        MessageInfo.builder(title).setText("Not Secondary").setImage(
                                CarIcon.APP_ICON).build());

        assertThat(messageInfo)
                .isNotEqualTo(MessageInfo.builder(title).setText(text).setImage(
                        CarIcon.ERROR).build());
    }

}
