/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SavedStateAccessorHolderTest {

    @UiThreadTest
    @Test
    public void test() {
        SavedStateAccessorHolder h = new SavedStateAccessorHolder(null);
        SavedStateAccessor accessor = h.savedStateAccessor();
        accessor.<String>getLiveData("livedata").setValue("para");
        accessor.set("notlive", 261);
        accessor.set("array", new int[]{2, 3, 9});
        Bundle savedState = h.getSavedState();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Bundle newBundle = Bundle.CREATOR.createFromParcel(parcel);
        SavedStateAccessorHolder newHolder = new SavedStateAccessorHolder(newBundle);
        SavedStateAccessor newAccessor = newHolder.savedStateAccessor();
        assertThat(newAccessor.<String>get("livedata"), is("para"));
        assertThat(newAccessor.<Integer>get("notlive"), is(261));
        assertThat(Arrays.equals(newAccessor.<int[]>get("array"), new int[]{2, 3, 9}), is(true));
    }
}
