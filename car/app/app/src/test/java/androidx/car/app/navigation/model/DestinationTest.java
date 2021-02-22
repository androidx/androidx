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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Destination}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class DestinationTest {

    @Test
    public void createInstance() {
        String title = "Google BVE";
        String address = "1120 112th Ave NE";

        Destination destination = new Destination.Builder().setName(title).setAddress(
                address).build();

        assertThat(destination.getName().toString()).isEqualTo(title);
        assertThat(destination.getAddress().toString()).isEqualTo(address);
        assertThat(destination.getImage()).isNull();
    }

    @Test
    public void emptyNameAndAddress_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> new Destination.Builder().setName("").setAddress("").build());
    }

    @Test
    public void emptyNameOrAddress_allowed() {
        Destination destination = new Destination.Builder().setName("name").setAddress("").build();
        assertThat(destination.getName().toString()).isEqualTo("name");
        assertThat(destination.getAddress().toString()).isEmpty();

        destination = new Destination.Builder().setAddress("address").build();
        assertThat(destination.getAddress().toString()).isEqualTo("address");
        assertThat(destination.getName()).isNull();
    }

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new Destination.Builder().setName("hello").setAddress("world").setImage(
                        carIcon));
    }

    @Test
    public void validate_hashcodeAndEquals() {
        Destination destination1 = new Destination.Builder().setName("name").setAddress(
                "address").build();
        Destination destination2 = new Destination.Builder().setName("name").setAddress(
                "address1").build();
        Destination destination3 = new Destination.Builder().setName("name2").setAddress(
                "address").build();
        Destination destination4 = new Destination.Builder().setName("name").setAddress(
                "address").build();

        assertThat(destination1.hashCode()).isNotEqualTo(destination2.hashCode());
        assertThat(destination1).isNotEqualTo(destination2);
        assertThat(destination1.hashCode()).isNotEqualTo(destination3.hashCode());
        assertThat(destination1).isNotEqualTo(destination3);
        assertThat(destination1.hashCode()).isEqualTo(destination4.hashCode());
        assertThat(destination1).isEqualTo(destination4);
    }

    @Test
    public void equals() {
        Destination destination = new Destination.Builder().setName("name").setAddress(
                "address").build();

        assertThat(destination)
                .isEqualTo(new Destination.Builder().setName("name").setAddress("address").build());
    }

    @Test
    public void notEquals_differentName() {
        Destination destination = new Destination.Builder().setName("name").setAddress(
                "address").build();

        assertThat(destination)
                .isNotEqualTo(new Destination.Builder().setName("Rafael").setAddress(
                        "address").build());
    }

    @Test
    public void notEquals_differentAddress() {
        Destination destination = new Destination.Builder().setName("name").setAddress(
                "address").build();

        assertThat(destination)
                .isNotEqualTo(new Destination.Builder().setName("name").setAddress(
                        "123 main st.").build());
    }
}
