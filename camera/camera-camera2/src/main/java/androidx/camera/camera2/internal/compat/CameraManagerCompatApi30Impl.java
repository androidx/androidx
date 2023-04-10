/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Set;

@RequiresApi(30)
class CameraManagerCompatApi30Impl extends CameraManagerCompatApi29Impl {

    CameraManagerCompatApi30Impl(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public Set<Set<String>> getConcurrentCameraIds() throws CameraAccessExceptionCompat {
        try {
            return mCameraManager.getConcurrentCameraIds();
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
    }
}
