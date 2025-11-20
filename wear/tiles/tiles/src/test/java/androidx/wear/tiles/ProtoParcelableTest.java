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

import android.os.Bundle;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.proto.RequestProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument // See http://g/robolectric-users/fTi2FRXgyGA/m/PkB0wYuwBgAJ
public final class ProtoParcelableTest {
    private static class Wrapper extends ProtoParcelable {
        public static final int VERSION = 1;
        public static final Creator<Wrapper> CREATOR = newCreator(Wrapper.class, Wrapper::new);

        Wrapper(byte[] payload, int version) {
            super(payload, version);
        }
    }

    private static class WrapperV2 extends ProtoParcelable {
        public static final int VERSION = 2;
        public static final Creator<WrapperV2> CREATOR =
                newCreator(WrapperV2.class, WrapperV2::new);

        WrapperV2(byte[] payload, Bundle extras, int version) {
            super(payload, extras, version);
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
    public void extrasEqualsAndHashCode() {
        Bundle bundle1 = new Bundle();
        bundle1.putInt("foo1", 111);
        Bundle bundle2 = new Bundle();
        bundle2.putInt("foo1", 111);
        bundle2.putString("bar2", "Baz");
        byte[] reqProto =
                RequestProto.ResourcesRequest.newBuilder().setVersion("foo").build().toByteArray();

        final WrapperV2 foo1 = new WrapperV2(reqProto, bundle1, WrapperV2.VERSION);
        final WrapperV2 foo2 = new WrapperV2(reqProto, bundle1, WrapperV2.VERSION);
        final WrapperV2 bar = new WrapperV2(reqProto, bundle2, WrapperV2.VERSION);

        assertThat(foo1).isEqualTo(foo2);
        assertThat(foo1).isNotEqualTo(bar);
        assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode());
        assertThat(foo1.hashCode()).isNotEqualTo(bar.hashCode());
    }

    @Test
    public void extrasEqualsAndHashCode_withDifferentKeyOrder() {
        String key1 = "key1";
        String key2 = "key2";
        int val1 = 123;
        String val2 = "value2";
        Bundle bundleA1 = new Bundle();
        bundleA1.putInt(key1, val1);
        bundleA1.putString(key2, val2);
        Bundle bundleA2 = new Bundle();
        bundleA2.putString(key2, val2);
        bundleA2.putInt(key1, val1);
        Bundle bundleB = new Bundle();
        bundleB.putString("another_key", "another_value");
        byte[] reqProto =
                RequestProto.ResourcesRequest.newBuilder().setVersion("foo").build().toByteArray();

        WrapperV2 wrapperA1 = new WrapperV2(reqProto, bundleA1, WrapperV2.VERSION);
        WrapperV2 wrapperA2 = new WrapperV2(reqProto, bundleA2, WrapperV2.VERSION);
        WrapperV2 wrapperB = new WrapperV2(reqProto, bundleB, WrapperV2.VERSION);

        assertThat(wrapperA1).isEqualTo(wrapperA2);
        assertThat(wrapperA1).isNotEqualTo(wrapperB);
        assertThat(wrapperA1.hashCode()).isEqualTo(wrapperA2.hashCode());
        assertThat(wrapperA1.hashCode()).isNotEqualTo(wrapperB.hashCode());
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
    public void toParcelAndBackV2() {
        RequestProto.ResourcesRequest wrappedMessage =
                RequestProto.ResourcesRequest.newBuilder().setVersion("foobar").build();
        Bundle extras = new Bundle();
        extras.putInt("foo1", 111);
        extras.putString("bar2", "Baz");
        WrapperV2 wrapper = new WrapperV2(wrappedMessage.toByteArray(), extras, WrapperV2.VERSION);

        Parcel parcel = Parcel.obtain();
        wrapper.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        assertThat(WrapperV2.CREATOR.createFromParcel(parcel)).isEqualTo(wrapper);
    }

    @Test
    public void arrayCreator() {
        assertThat(Wrapper.CREATOR.newArray(123)).hasLength(123);
    }

    @Test
    public void arrayCreatorV2() {
        assertThat(WrapperV2.CREATOR.newArray(123)).hasLength(123);
    }
}
