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

package androidx.camera.core.impl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A placeholder service to avoid adding application-level metadata. The service is only used to
 * expose metadata defined in the library's manifest. It is never invoked.
 */
public class MetadataHolderService extends Service {
    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        throw new UnsupportedOperationException();
    }

    private MetadataHolderService() {}
}
