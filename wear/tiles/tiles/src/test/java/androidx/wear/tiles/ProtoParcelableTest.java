/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import androidx.wear.tiles.proto.RequestProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class ProtoParcelableTest {
    public static class Wrapper extends ProtoParcelable {
        public static final int VERSION = 1;
        public static final Creator<Wrapper> CREATOR = newCreator(Wrapper.class, Wrapper::new);

        Wrapper(byte[] payload, int version) {
            super(payload, version);
        }
    }

    @Test
    public void contentsEqualsAndHashCode() {
        final Wrapper foo1 =
                new Wrapper(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion("foo")
                                .build()
                                .toByteArray(),
                        Wrapper.VERSION);
        final Wrapper foo2 =
                new Wrapper(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion("foo")
                                .build()
                                .toByteArray(),
                        Wrapper.VERSION);
        final Wrapper bar =
                new Wrapper(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion("bar")
                                .build()
                                .toByteArray(),
                        Wrapper.VERSION);
        assertThat(foo1).isEqualTo(foo2);
        assertThat(foo1).isNotEqualTo(bar);
        assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode());
        assertThat(foo1.hashCode()).isNotEqualTo(bar.hashCode());
    }

    @Test
    public void versionEqualsAndHashCode() {
        final Wrapper foo1 =
                new Wrapper(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion("foo")
                                .build()
                                .toByteArray(),
                        Wrapper.VERSION);
        final Wrapper foo2 =
                new Wrapper(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion("foo")
                                .build()
                                .toByteArray(),
                        /* version= */ 2);

        assertThat(foo1).isNotEqualTo(foo2);
        assertThat(foo1.hashCode()).isNotEqualTo(foo2.hashCode());
    }

    @Test
    public void toParcelAndBack() {
        RequestProto.ResourcesRequest wrappedMessage =
                RequestProto.ResourcesRequest.newBuilder().setVersion("foobar").build();
        Wrapper wrapper = new Wrapper(wrappedMessage.toByteArray(), Wrapper.VERSION);

        Parcel parcel = Parcel.obtain();
        wrapper.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        assertThat(Wrapper.CREATOR.createFromParcel(parcel)).isEqualTo(wrapper);
    }

    @Test
    public void arrayCreator() {
        assertThat(Wrapper.CREATOR.newArray(123)).hasLength(123);
    }
}
