/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Bundle;
import android.os.Parcel;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SavedStateHandleParcelingTest {

    @UiThreadTest
    @Test
    public void test() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.<String>getLiveData("livedata").setValue("para");
        handle.set("notlive", 261);
        handle.set("array", new int[]{2, 3, 9});
        Bundle savedState = handle.savedStateProvider().saveState();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Bundle newBundle = Bundle.CREATOR.createFromParcel(parcel);
        SavedStateHandle newHandle = SavedStateHandle.createHandle(newBundle, null);
        assertThat(newHandle.<String>get("livedata"), is("para"));
        assertThat(newHandle.<Integer>get("notlive"), is(261));
        assertThat(Arrays.equals(newHandle.<int[]>get("array"), new int[]{2, 3, 9}), is(true));
    }
}
