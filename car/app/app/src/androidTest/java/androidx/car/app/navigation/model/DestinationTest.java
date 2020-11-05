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

/** Tests for {@link Destination}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class DestinationTest {

    @Test
    public void createInstance() {
        String title = "Google BVE";
        String address = "1120 112th Ave NE";

        Destination destination = Destination.builder().setName(title).setAddress(address).build();

        assertThat(destination.getName().getText()).isEqualTo(title);
        assertThat(destination.getAddress().getText()).isEqualTo(address);
        assertThat(destination.getImage()).isNull();
    }

    @Test
    public void emptyNameAndAddress_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> Destination.builder().setName("").setAddress("").build());
    }

    @Test
    public void emptyNameOrAddress_allowed() {
        Destination destination = Destination.builder().setName("name").setAddress("").build();
        assertThat(destination.getName().getText()).isEqualTo("name");
        assertThat(destination.getAddress().getText()).isEmpty();

        destination = Destination.builder().setName(null).setAddress("address").build();
        assertThat(destination.getAddress().getText()).isEqualTo("address");
        assertThat(destination.getName()).isNull();
    }

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = CarIcon.of(IconCompat.createWithContentUri(iconUri));
        assertThrows(
                IllegalArgumentException.class,
                () -> Destination.builder().setName("hello").setAddress("world").setImage(carIcon));
    }

    @Test
    public void validate_hashcodeAndEquals() {
        Destination destination1 = Destination.builder().setName("name").setAddress(
                "address").build();
        Destination destination2 = Destination.builder().setName("name").setAddress(
                "address1").build();
        Destination destination3 = Destination.builder().setName("name2").setAddress(
                "address").build();
        Destination destination4 = Destination.builder().setName("name").setAddress(
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
        Destination destination = Destination.builder().setName("name").setAddress(
                "address").build();

        assertThat(destination)
                .isEqualTo(Destination.builder().setName("name").setAddress("address").build());
    }

    @Test
    public void notEquals_differentName() {
        Destination destination = Destination.builder().setName("name").setAddress(
                "address").build();

        assertThat(destination)
                .isNotEqualTo(Destination.builder().setName("Rafael").setAddress(
                        "address").build());
    }

    @Test
    public void notEquals_differentAddress() {
        Destination destination = Destination.builder().setName("name").setAddress(
                "address").build();

        assertThat(destination)
                .isNotEqualTo(Destination.builder().setName("name").setAddress(
                        "123 main st.").build());
    }
}
