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

package androidx.work.multiprocess.parcelable;

import static androidx.work.multiprocess.parcelable.ParcelUtils.readBooleanValue;
import static androidx.work.multiprocess.parcelable.ParcelUtils.writeBooleanValue;

import android.annotation.SuppressLint;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.work.WorkerParameters;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link WorkerParameters.RuntimeExtras}, but parcelable.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableRuntimeExtras implements Parcelable {
    private WorkerParameters.RuntimeExtras mRuntimeExtras;

    public ParcelableRuntimeExtras(WorkerParameters.@NonNull RuntimeExtras runtimeExtras) {
        mRuntimeExtras = runtimeExtras;
    }

    @SuppressWarnings("deprecation")
    public ParcelableRuntimeExtras(@NonNull Parcel in) {
        ClassLoader loader = getClass().getClassLoader();
        // network
        Network network = null;
        boolean hasNetwork = readBooleanValue(in);
        if (hasNetwork) {
            network = in.readParcelable(loader);
        }
        // triggeredContentUris
        List<Uri> triggeredContentUris = null;
        boolean hasContentUris = readBooleanValue(in);
        if (hasContentUris) {
            Parcelable[] parceledUris = in.readParcelableArray(loader);
            triggeredContentUris = new ArrayList<>(parceledUris.length);
            for (Parcelable parcelable : parceledUris) {
                triggeredContentUris.add((Uri) parcelable);
            }
        }
        // triggeredContentAuthorities
        List<String> triggeredContentAuthorities = null;
        boolean hasContentAuthorities = readBooleanValue(in);
        if (hasContentAuthorities) {
            triggeredContentAuthorities = in.createStringArrayList();
        }
        mRuntimeExtras = new WorkerParameters.RuntimeExtras();
        if (Build.VERSION.SDK_INT >= 28) {
            mRuntimeExtras.network = network;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            if (triggeredContentUris != null) {
                mRuntimeExtras.triggeredContentUris = triggeredContentUris;
            }
            if (triggeredContentAuthorities != null) {
                mRuntimeExtras.triggeredContentAuthorities = triggeredContentAuthorities;
            }
        }
    }

    public static final Creator<ParcelableRuntimeExtras> CREATOR =
            new Creator<ParcelableRuntimeExtras>() {
                @Override
                public @NonNull ParcelableRuntimeExtras createFromParcel(Parcel in) {
                    return new ParcelableRuntimeExtras(in);
                }

                @Override
                public ParcelableRuntimeExtras[] newArray(int size) {
                    return new ParcelableRuntimeExtras[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    @SuppressLint("NewApi")
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        Network network = null;
        if (Build.VERSION.SDK_INT >= 28) {
            network = mRuntimeExtras.network;
        }
        // network
        boolean hasNetwork = network != null;
        writeBooleanValue(parcel, hasNetwork);
        if (hasNetwork) {
            parcel.writeParcelable(Api21Impl.castToParcelable(network), flags);
        }

        List<Uri> triggeredContentUris = null;
        List<String> triggeredAuthorities = null;
        if (Build.VERSION.SDK_INT >= 24) {
            triggeredContentUris = mRuntimeExtras.triggeredContentUris;
            triggeredAuthorities = mRuntimeExtras.triggeredContentAuthorities;
        }
        // triggeredContentUris
        boolean hasContentUris = triggeredContentUris != null && !triggeredContentUris.isEmpty();
        writeBooleanValue(parcel, hasContentUris);
        if (hasContentUris) {
            Uri[] contentUriArray = new Uri[triggeredContentUris.size()];
            for (int i = 0; i < contentUriArray.length; i++) {
                contentUriArray[i] = triggeredContentUris.get(i);
            }
            parcel.writeParcelableArray(contentUriArray, flags);
        }
        // triggeredContentAuthorities
        boolean hasContentAuthorities =
                triggeredAuthorities != null && !triggeredAuthorities.isEmpty();
        writeBooleanValue(parcel, hasContentAuthorities);
        if (hasContentAuthorities) {
            parcel.writeStringList(triggeredAuthorities);
        }
    }

    public WorkerParameters.@NonNull RuntimeExtras getRuntimeExtras() {
        return mRuntimeExtras;
    }

    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        static Parcelable castToParcelable(Network network) {
            return network;
        }
    }
}
