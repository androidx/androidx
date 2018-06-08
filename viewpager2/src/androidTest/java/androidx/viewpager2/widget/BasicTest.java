/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.UUID;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BasicTest {
    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void test_recyclerViewAdapter_pageFillEnforced() {
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage(
                "Item's root view must fill the whole ViewPager2 (use match_parent)");

        ViewPager2 viewPager = new ViewPager2(InstrumentationRegistry.getContext());
        viewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                View view = new View(parent.getContext());
                view.setLayoutParams(new ViewGroup.LayoutParams(50, 50)); // arbitrary fixed size
                return new RecyclerView.ViewHolder(view) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // do nothing
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });

        viewPager.measure(0, 0); // equivalent of unspecified
    }

    @Test
    public void test_childrenNotAllowed() throws Exception {
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("ViewPager2 does not support direct child views");

        Context context = InstrumentationRegistry.getContext();
        ViewPager2 viewPager = new ViewPager2(context);
        viewPager.addView(new View(context));
    }

    @Test
    public void test_saveStateParcel_createRestore() throws Throwable {
        // given
        Bundle superState = createIntBundle(42);
        ViewPager2.SavedState state = new ViewPager2.SavedState(superState);
        state.mRecyclerViewId = 700;
        state.mAdapterState = new Parcelable[]{createIntBundle(1), createIntBundle(2),
                createIntBundle(3)};

        // when
        Parcel parcel = Parcel.obtain();
        state.writeToParcel(parcel, 0);
        final String parcelSuffix = UUID.randomUUID().toString();
        parcel.writeString(parcelSuffix); // to verify parcel boundaries
        parcel.setDataPosition(0);
        ViewPager2.SavedState recreatedState = ViewPager2.SavedState.CREATOR.createFromParcel(
                parcel);

        // then
        assertThat("Parcel reading should not go out of bounds", parcel.readString(),
                equalTo(parcelSuffix));
        assertThat("All of the parcel should be read", parcel.dataAvail(), equalTo(0));
        assertThat(recreatedState.mRecyclerViewId, equalTo(700));
        assertThat(recreatedState.mAdapterState, arrayWithSize(3));
        assertThat((int) ((Bundle) recreatedState.getSuperState()).get("key"), equalTo(42));
        assertThat((int) ((Bundle) recreatedState.mAdapterState[0]).get("key"), equalTo(1));
        assertThat((int) ((Bundle) recreatedState.mAdapterState[1]).get("key"), equalTo(2));
        assertThat((int) ((Bundle) recreatedState.mAdapterState[2]).get("key"), equalTo(3));
    }

    private Bundle createIntBundle(int value) {
        Bundle bundle = new Bundle(1);
        bundle.putInt("key", value);
        return bundle;
    }
}
