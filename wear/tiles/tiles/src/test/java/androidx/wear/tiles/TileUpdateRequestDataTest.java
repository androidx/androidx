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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class TileUpdateRequestDataTest {
    @Test
    public void toParcelAndBack() {
        // This payload ends up empty anyway (it's there for future expansion). Just test that it
        // doesn't error out when being parceled and back again.
        TileUpdateRequestData wrapper = new TileUpdateRequestData();

        Parcel parcel = Parcel.obtain();
        wrapper.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        assertThat(TileUpdateRequestData.CREATOR.createFromParcel(parcel)).isEqualTo(wrapper);
    }
}
