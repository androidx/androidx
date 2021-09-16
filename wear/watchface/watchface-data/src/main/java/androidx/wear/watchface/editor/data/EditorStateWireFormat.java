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

package androidx.wear.watchface.editor.data;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat;
import androidx.wear.watchface.style.data.UserStyleWireFormat;

import java.util.List;

/**
 * Data sent over AIDL for {@link IEditorListener#onEditorStateChange}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize(allowSerialization = true)
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class EditorStateWireFormat implements VersionedParcelable, Parcelable {
    @ParcelField(1)
    @Nullable
    String mWatchFaceInstanceId;

    @ParcelField(2)
    @NonNull
    UserStyleWireFormat mUserStyle;

    @ParcelField(3)
    @NonNull
    List<IdAndComplicationDataWireFormat> mPreviewComplicationData;

    @ParcelField(4)
    boolean mCommitChanges;

    @ParcelField(5)
    @Nullable
    Bundle mPreviewImageBundle;

    /** Used by VersionedParcelable. */
    EditorStateWireFormat() {
    }

    public EditorStateWireFormat(
            @Nullable String watchFaceInstanceId,
            @NonNull UserStyleWireFormat userStyle,
            @NonNull List<IdAndComplicationDataWireFormat> previewComplicationData,
            boolean commitChanges,
            @Nullable Bundle previewImageBundle) {
        mWatchFaceInstanceId = watchFaceInstanceId;
        mUserStyle = userStyle;
        mPreviewComplicationData = previewComplicationData;
        mCommitChanges = commitChanges;
        mPreviewImageBundle = previewImageBundle;
    }

    @Nullable
    public String getWatchFaceInstanceId() {
        return mWatchFaceInstanceId;
    }

    @NonNull
    public UserStyleWireFormat getUserStyle() {
        return mUserStyle;
    }

    @NonNull
    public List<IdAndComplicationDataWireFormat> getPreviewComplicationData() {
        return mPreviewComplicationData;
    }

    public boolean getCommitChanges() {
        return mCommitChanges;
    }

    @Nullable
    public Bundle getPreviewImageBundle() {
        return mPreviewImageBundle;
    }

    /** Serializes this EditorState to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<EditorStateWireFormat> CREATOR =
            new Parcelable.Creator<EditorStateWireFormat>() {
                @Override
                public EditorStateWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public EditorStateWireFormat[] newArray(int size) {
                    return new EditorStateWireFormat[size];
                }
            };
}
