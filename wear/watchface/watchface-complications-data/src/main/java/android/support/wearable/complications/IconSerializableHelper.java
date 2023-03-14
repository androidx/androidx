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

package android.support.wearable.complications;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

@RequiresApi(api = Build.VERSION_CODES.P)
class IconSerializableHelper implements Serializable {
    int mType;
    String mResourcePackage;
    int mResourceId;
    String mUri;
    byte[] mBitmap;

    private static final String TAG = "IconSerializableHelper";

    @Nullable
    static IconSerializableHelper create(@Nullable Icon icon) {
        if (icon == null) {
            return null;
        }
        return new IconSerializableHelper(icon);
    }

    @Nullable
    static Icon read(@NonNull ObjectInputStream ois) throws IOException, ClassNotFoundException {
        IconSerializableHelper helper = (IconSerializableHelper) ois.readObject();
        if (helper == null) {
            return null;
        }
        return helper.toIcon();
    }

    IconSerializableHelper(@NonNull Icon icon) {
        mType = icon.getType();

        switch (mType) {
            case Icon.TYPE_RESOURCE:
                mResourcePackage = icon.getResPackage();
                mResourceId = icon.getResId();
                break;

            case Icon.TYPE_URI:
            case Icon.TYPE_URI_ADAPTIVE_BITMAP:
                mUri = icon.getUri().toString();
                break;

            case Icon.TYPE_BITMAP:
            case Icon.TYPE_ADAPTIVE_BITMAP:
                try {
                    Method getBitmap = icon.getClass().getDeclaredMethod("getBitmap");
                    @SuppressLint("BanUncheckedReflection")
                    Bitmap bitmap = (Bitmap) getBitmap.invoke(icon);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    mBitmap = baos.toByteArray();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to serialize bitmap", e);
                }
                break;

            default:
                Log.e(TAG, "Failed to serialize icon of type " + mType);
        }
    }

    @Nullable
    Icon toIcon() {
        switch (mType) {
            case Icon.TYPE_RESOURCE:
                return Icon.createWithResource(mResourcePackage, mResourceId);

            case Icon.TYPE_URI:
            case Icon.TYPE_URI_ADAPTIVE_BITMAP:
                return Icon.createWithContentUri(mUri);

            case Icon.TYPE_BITMAP:
            case Icon.TYPE_ADAPTIVE_BITMAP:
                return Icon.createWithBitmap(
                        BitmapFactory.decodeByteArray(mBitmap, 0, mBitmap.length));

            default:
                return null;
        }
    }
}
