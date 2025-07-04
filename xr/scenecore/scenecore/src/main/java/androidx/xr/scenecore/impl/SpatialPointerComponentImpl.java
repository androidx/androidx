/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import android.util.Log;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.SpatialPointerComponent;
import androidx.xr.runtime.internal.SpatialPointerIcon;
import androidx.xr.runtime.internal.SpatialPointerIconType;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeTransaction;

import org.jspecify.annotations.NonNull;

class SpatialPointerComponentImpl implements SpatialPointerComponent {

    private static final String TAG = "Runtime";

    private final XrExtensions mExtensions;
    private AndroidXrEntity mEntity;
    @SpatialPointerIconType private int mSpatialPointerIconType = SpatialPointerIcon.TYPE_DEFAULT;

    SpatialPointerComponentImpl(XrExtensions extensions) {
        mExtensions = extensions;
    }

    @Override
    public boolean onAttach(@NonNull Entity entity) {
        if (mEntity != null) {
            Log.e(TAG, "Already attached to entity " + mEntity);
            return false;
        }
        if (!(entity instanceof AndroidXrEntity)) {
            Log.e(TAG, "Entity is not an AndroidXrEntity.");
            return false;
        }
        mEntity = (AndroidXrEntity) entity;
        setSpatialPointerIcon(SpatialPointerIcon.TYPE_DEFAULT);
        return true;
    }

    @Override
    public void onDetach(@NonNull Entity entity) {
        setSpatialPointerIcon(SpatialPointerIcon.TYPE_DEFAULT);
        mEntity = null;
    }

    @Override
    public void setSpatialPointerIcon(@SpatialPointerIconType int icon) {
        mSpatialPointerIconType = icon;

        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setPointerIcon(
                            mEntity.getNode(), RuntimeUtils.convertSpatialPointerIconType(icon))
                    .apply();
        }
    }

    @Override
    @SpatialPointerIconType
    public int getSpatialPointerIcon() {
        return mSpatialPointerIconType;
    }
}
