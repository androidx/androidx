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

package androidx.fragment.app;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Map;

@SuppressLint("BanParcelableUsage")
final class BackStackRecordState implements Parcelable {
    private static final String TAG = FragmentManager.TAG;

    final int[] mOps;
    final ArrayList<String> mFragmentWhos;
    final int[] mOldMaxLifecycleStates;
    final int[] mCurrentMaxLifecycleStates;
    final int mTransition;
    final String mName;
    final int mIndex;
    final int mBreadCrumbTitleRes;
    final CharSequence mBreadCrumbTitleText;
    final int mBreadCrumbShortTitleRes;
    final CharSequence mBreadCrumbShortTitleText;
    final ArrayList<String> mSharedElementSourceNames;
    final ArrayList<String> mSharedElementTargetNames;
    final boolean mReorderingAllowed;

    BackStackRecordState(BackStackRecord bse) {
        final int numOps = bse.mOps.size();
        mOps = new int[numOps * 6];

        if (!bse.mAddToBackStack) {
            throw new IllegalStateException("Not on back stack");
        }

        mFragmentWhos = new ArrayList<>(numOps);
        mOldMaxLifecycleStates = new int[numOps];
        mCurrentMaxLifecycleStates = new int[numOps];
        int pos = 0;
        for (int opNum = 0; opNum < numOps; opNum++) {
            final BackStackRecord.Op op = bse.mOps.get(opNum);
            mOps[pos++] = op.mCmd;
            mFragmentWhos.add(op.mFragment != null ? op.mFragment.mWho : null);
            mOps[pos++] = op.mFromExpandedOp ? 1 : 0;
            mOps[pos++] = op.mEnterAnim;
            mOps[pos++] = op.mExitAnim;
            mOps[pos++] = op.mPopEnterAnim;
            mOps[pos++] = op.mPopExitAnim;
            mOldMaxLifecycleStates[opNum] = op.mOldMaxState.ordinal();
            mCurrentMaxLifecycleStates[opNum] = op.mCurrentMaxState.ordinal();
        }
        mTransition = bse.mTransition;
        mName = bse.mName;
        mIndex = bse.mIndex;
        mBreadCrumbTitleRes = bse.mBreadCrumbTitleRes;
        mBreadCrumbTitleText = bse.mBreadCrumbTitleText;
        mBreadCrumbShortTitleRes = bse.mBreadCrumbShortTitleRes;
        mBreadCrumbShortTitleText = bse.mBreadCrumbShortTitleText;
        mSharedElementSourceNames = bse.mSharedElementSourceNames;
        mSharedElementTargetNames = bse.mSharedElementTargetNames;
        mReorderingAllowed = bse.mReorderingAllowed;
    }

    BackStackRecordState(Parcel in) {
        mOps = in.createIntArray();
        mFragmentWhos = in.createStringArrayList();
        mOldMaxLifecycleStates = in.createIntArray();
        mCurrentMaxLifecycleStates = in.createIntArray();
        mTransition = in.readInt();
        mName = in.readString();
        mIndex = in.readInt();
        mBreadCrumbTitleRes = in.readInt();
        mBreadCrumbTitleText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mBreadCrumbShortTitleRes = in.readInt();
        mBreadCrumbShortTitleText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mSharedElementSourceNames = in.createStringArrayList();
        mSharedElementTargetNames = in.createStringArrayList();
        mReorderingAllowed = in.readInt() != 0;
    }

    /**
     * Instantiates a {@link BackStackRecord} from this state that mirrors the
     * exact state it was in when the FragmentManager's state was saved. This
     * assumes that all fragments included in this transactions are already
     * added as active fragments to the FragmentManager.
     */
    @NonNull
    public BackStackRecord instantiate(@NonNull FragmentManager fm) {
        BackStackRecord bse = new BackStackRecord(fm);
        fillInBackStackRecord(bse);
        bse.mIndex = mIndex;
        for (int num = 0; num < mFragmentWhos.size(); num++) {
            String fWho = mFragmentWhos.get(num);
            if (fWho != null) {
                bse.mOps.get(num).mFragment = fm.findActiveFragment(fWho);
            }
        }
        bse.bumpBackStackNesting(1);
        return bse;
    }

    /**
     * Instantiates a {@link BackStackRecord} from a saved state that allows
     * the returned BackStackRecord to be applied to the given FragmentManager
     * as if it was a new transaction. Any fragments in the transactions will
     * be pulled from the provided fragments map.
     */
    @NonNull
    public BackStackRecord instantiate(@NonNull FragmentManager fm,
            @NonNull Map<String, Fragment> fragments) {
        BackStackRecord bse = new BackStackRecord(fm);
        fillInBackStackRecord(bse);

        for (int num = 0; num < mFragmentWhos.size(); num++) {
            String fWho = mFragmentWhos.get(num);
            if (fWho != null) {
                Fragment fragment = fragments.get(fWho);
                if (fragment != null) {
                    bse.mOps.get(num).mFragment = fragment;
                } else {
                    throw new IllegalStateException("Restoring FragmentTransaction "
                            + mName + " failed due to missing saved state for Fragment ("
                            + fWho + ")");
                }
            }
        }
        return bse;
    }

    private void fillInBackStackRecord(@NonNull BackStackRecord bse) {
        int pos = 0;
        int num = 0;
        while (pos < mOps.length) {
            BackStackRecord.Op op = new BackStackRecord.Op();
            op.mCmd = mOps[pos++];
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(TAG, "Instantiate " + bse
                        + " op #" + num + " base fragment #" + mOps[pos]);
            }
            op.mOldMaxState = Lifecycle.State.values()[mOldMaxLifecycleStates[num]];
            op.mCurrentMaxState = Lifecycle.State.values()[mCurrentMaxLifecycleStates[num]];
            op.mFromExpandedOp = mOps[pos++] != 0;
            op.mEnterAnim = mOps[pos++];
            op.mExitAnim = mOps[pos++];
            op.mPopEnterAnim = mOps[pos++];
            op.mPopExitAnim = mOps[pos++];
            bse.mEnterAnim = op.mEnterAnim;
            bse.mExitAnim = op.mExitAnim;
            bse.mPopEnterAnim = op.mPopEnterAnim;
            bse.mPopExitAnim = op.mPopExitAnim;
            bse.addOp(op);
            num++;
        }
        bse.mTransition = mTransition;
        bse.mName = mName;
        bse.mAddToBackStack = true;
        bse.mBreadCrumbTitleRes = mBreadCrumbTitleRes;
        bse.mBreadCrumbTitleText = mBreadCrumbTitleText;
        bse.mBreadCrumbShortTitleRes = mBreadCrumbShortTitleRes;
        bse.mBreadCrumbShortTitleText = mBreadCrumbShortTitleText;
        bse.mSharedElementSourceNames = mSharedElementSourceNames;
        bse.mSharedElementTargetNames = mSharedElementTargetNames;
        bse.mReorderingAllowed = mReorderingAllowed;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(mOps);
        dest.writeStringList(mFragmentWhos);
        dest.writeIntArray(mOldMaxLifecycleStates);
        dest.writeIntArray(mCurrentMaxLifecycleStates);
        dest.writeInt(mTransition);
        dest.writeString(mName);
        dest.writeInt(mIndex);
        dest.writeInt(mBreadCrumbTitleRes);
        TextUtils.writeToParcel(mBreadCrumbTitleText, dest, 0);
        dest.writeInt(mBreadCrumbShortTitleRes);
        TextUtils.writeToParcel(mBreadCrumbShortTitleText, dest, 0);
        dest.writeStringList(mSharedElementSourceNames);
        dest.writeStringList(mSharedElementTargetNames);
        dest.writeInt(mReorderingAllowed ? 1 : 0);
    }

    public static final Parcelable.Creator<BackStackRecordState> CREATOR =
            new Parcelable.Creator<BackStackRecordState>() {
        @Override
        public BackStackRecordState createFromParcel(Parcel in) {
            return new BackStackRecordState(in);
        }

        @Override
        public BackStackRecordState[] newArray(int size) {
            return new BackStackRecordState[size];
        }
    };
}
