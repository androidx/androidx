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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

@SuppressWarnings("BanParcelableUsage")
final class FragmentState implements Parcelable {
    final String mClassName;
    final String mWho;
    final boolean mFromLayout;
    final int mFragmentId;
    final int mContainerId;
    final String mTag;
    final boolean mRetainInstance;
    final boolean mDetached;
    final Bundle mArguments;
    final boolean mHidden;

    Bundle mSavedFragmentState;

    Fragment mInstance;

    FragmentState(Fragment frag) {
        mClassName = frag.getClass().getName();
        mWho = frag.mWho;
        mFromLayout = frag.mFromLayout;
        mFragmentId = frag.mFragmentId;
        mContainerId = frag.mContainerId;
        mTag = frag.mTag;
        mRetainInstance = frag.mRetainInstance;
        mDetached = frag.mDetached;
        mArguments = frag.mArguments;
        mHidden = frag.mHidden;
    }

    FragmentState(Parcel in) {
        mClassName = in.readString();
        mWho = in.readString();
        mFromLayout = in.readInt() != 0;
        mFragmentId = in.readInt();
        mContainerId = in.readInt();
        mTag = in.readString();
        mRetainInstance = in.readInt() != 0;
        mDetached = in.readInt() != 0;
        mArguments = in.readBundle();
        mHidden = in.readInt() != 0;
        mSavedFragmentState = in.readBundle();
    }

    public Fragment instantiate(@NonNull ClassLoader classLoader,
            @NonNull FragmentFactory factory) {
        if (mInstance == null) {
            if (mArguments != null) {
                mArguments.setClassLoader(classLoader);
            }

            mInstance = factory.instantiate(classLoader, mClassName, mArguments);
            mInstance.setArguments(mArguments);

            if (mSavedFragmentState != null) {
                mSavedFragmentState.setClassLoader(classLoader);
                mInstance.mSavedFragmentState = mSavedFragmentState;
            }
            mInstance.mWho = mWho;
            mInstance.mFromLayout = mFromLayout;
            mInstance.mRestored = true;
            mInstance.mFragmentId = mFragmentId;
            mInstance.mContainerId = mContainerId;
            mInstance.mTag = mTag;
            mInstance.mRetainInstance = mRetainInstance;
            mInstance.mDetached = mDetached;
            mInstance.mHidden = mHidden;

            if (FragmentManagerImpl.DEBUG) {
                Log.v(FragmentManagerImpl.TAG, "Instantiated fragment " + mInstance);
            }
        }
        return mInstance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClassName);
        dest.writeString(mWho);
        dest.writeInt(mFromLayout ? 1 : 0);
        dest.writeInt(mFragmentId);
        dest.writeInt(mContainerId);
        dest.writeString(mTag);
        dest.writeInt(mRetainInstance ? 1 : 0);
        dest.writeInt(mDetached ? 1 : 0);
        dest.writeBundle(mArguments);
        dest.writeInt(mHidden ? 1 : 0);
        dest.writeBundle(mSavedFragmentState);
    }

    public static final Parcelable.Creator<FragmentState> CREATOR =
            new Parcelable.Creator<FragmentState>() {
                @Override
                public FragmentState createFromParcel(Parcel in) {
                    return new FragmentState(in);
                }

                @Override
                public FragmentState[] newArray(int size) {
                    return new FragmentState[size];
                }
            };
}
