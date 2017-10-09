/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.os.Parcel;
import android.os.Parcelable;

public class PhotoItem implements Parcelable {
    private int mId;
    private String mTitle;
    private String mContent;
    private int mImageResourceId;

    public PhotoItem(String title, int imageResourceId) {
        this(title, null, imageResourceId);
    }

    public PhotoItem(String title, int imageResourceId, int id) {
        this(title, imageResourceId);
        mId = id;
    }

    public PhotoItem(String title, String content, int imageResourceId) {
        mTitle = title;
        mContent = content;
        mImageResourceId = imageResourceId;
        // the id was set to -1 if user don't provide this parameter
        mId = -1;
    }

    public PhotoItem(String title, String content, int imageResourceId, int id) {
        this(title, content, imageResourceId);
        mId = id;
    }

    public int getImageResourceId() {
        return mImageResourceId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getContent() {
        return mContent;
    }

    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeInt(mImageResourceId);
    }

    public static final Parcelable.Creator<PhotoItem> CREATOR
            = new Parcelable.Creator<PhotoItem>() {
        @Override
        public PhotoItem createFromParcel(Parcel in) {
            return new PhotoItem(in);
        }

        @Override
        public PhotoItem[] newArray(int size) {
            return new PhotoItem[size];
        }
    };

    public int getId() {
        return this.mId;
    }

    private PhotoItem(Parcel in) {
        mTitle = in.readString();
        mImageResourceId = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoItem photoItem = (PhotoItem) o;
        if (mId != photoItem.mId) return false;
        if (mImageResourceId != photoItem.mImageResourceId) return false;
        if (mTitle != null ? !mTitle.equals(photoItem.mTitle) : photoItem.mTitle != null) {
            return false;
        }
        return mContent != null ? mContent.equals(photoItem.mContent) : photoItem.mContent == null;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mContent != null ? mContent.hashCode() : 0);
        result = 31 * result + mImageResourceId;
        return result;
    }
}