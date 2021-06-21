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

package androidx.fragment.app;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressLint("BanParcelableUsage")
class BackStackState implements Parcelable {
    final List<String> mFragments;
    final List<BackStackRecordState> mTransactions;

    BackStackState(List<String> fragments,
            List<BackStackRecordState> transactions) {
        mFragments = fragments;
        mTransactions = transactions;
    }

    BackStackState(@NonNull Parcel in) {
        mFragments = in.createStringArrayList();
        mTransactions = in.createTypedArrayList(BackStackRecordState.CREATOR);
    }

    @NonNull
    List<BackStackRecord> instantiate(@NonNull FragmentManager fm) {
        // First instantiate the saved Fragments from state.
        // These will populate the transactions we instantiate.
        HashMap<String, Fragment> fragments = new HashMap<>(mFragments.size());
        for (String fWho : mFragments) {
            Fragment existingFragment = fm.getFragmentStore().findFragmentByWho(fWho);
            if (existingFragment != null) {
                // If the Fragment still exists, this means the saveBackStack()
                // hasn't executed yet, so we can use the existing Fragment directly
                fragments.put(existingFragment.mWho, existingFragment);
                continue;
            }
            // Otherwise, retrieve any saved state, clearing it out for future calls
            FragmentState fragmentState = fm.getFragmentStore().setSavedState(fWho, null);
            if (fragmentState != null) {
                Fragment fragment = fragmentState.instantiate(fm.getFragmentFactory(),
                        fm.getHost().getContext().getClassLoader());
                fragments.put(fragment.mWho, fragment);
            }
        }

        // Now instantiate all of the BackStackRecords
        ArrayList<BackStackRecord> transactions = new ArrayList<>();
        for (BackStackRecordState backStackRecordState : mTransactions) {
            transactions.add(backStackRecordState.instantiate(fm, fragments));
        }
        return transactions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mFragments);
        dest.writeTypedList(mTransactions);
    }

    public static final Parcelable.Creator<BackStackState> CREATOR =
            new Parcelable.Creator<BackStackState>() {
                @Override
                public BackStackState createFromParcel(Parcel in) {
                    return new BackStackState(in);
                }

                @Override
                public BackStackState[] newArray(int size) {
                    return new BackStackState[size];
                }
            };
}
