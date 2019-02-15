/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import android.app.Activity;
import android.os.Bundle;

import androidx.camera.core.CameraX;

/** A fake {@link Activity} that checks properties of the CameraX library. */
public class FakeActivity extends Activity {
    private volatile boolean mIsCameraXInitializedAtOnCreate = false;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mIsCameraXInitializedAtOnCreate = CameraX.isInitialized();
    }

    /** Returns true if CameraX is initialized when {@link #onCreate(Bundle)} is called. */
    public boolean isCameraXInitializedAtOnCreate() {
        return mIsCameraXInitializedAtOnCreate;
    }
}
