/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.CaptureStage;

/** A fake {@link CaptureStage} where the values can be set. */
public class FakeCaptureStage implements CaptureStage {

    private final int mId;
    private final CaptureRequestConfiguration mCaptureRequestConfiguration;

    /** Create a FakeCaptureStage with the given parameters. */
    public FakeCaptureStage(int id, CaptureRequestConfiguration captureRequestConfiguration) {
        mId = id;
        mCaptureRequestConfiguration = captureRequestConfiguration;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public CaptureRequestConfiguration getCaptureRequestConfiguration() {
        return mCaptureRequestConfiguration;
    }
}
