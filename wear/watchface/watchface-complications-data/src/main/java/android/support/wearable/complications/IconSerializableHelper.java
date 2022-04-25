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

import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

@RequiresApi(api = Build.VERSION_CODES.P)
class IconSerializableHelper implements Serializable {
    int mType;
    String mResourcePackage;
    int mResourceId;
    String mUri;

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
                mUri = icon.getUri().toString();
                break;
        }

        // We currently don't attempt to serialize any other type of icon. We could render to a
        // bitmap, but the above covers the majority of complication icons.
    }

    @Nullable Icon toIcon() {
        switch (mType) {
            case Icon.TYPE_RESOURCE:
                return Icon.createWithResource(mResourcePackage, mResourceId);

            case Icon.TYPE_URI:
                return Icon.createWithContentUri(mUri);

            default:
                return null;
        }
    }
}
