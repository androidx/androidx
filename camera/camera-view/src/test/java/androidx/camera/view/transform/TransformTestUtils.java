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

package androidx.camera.view.transform;

import android.graphics.Rect;

import androidx.camera.core.ImageProxy;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;


/**
 * Shared code for transform tests.
 */
class TransformTestUtils {

    static ImageProxy createFakeImageProxy(int width, int height,
            int rotationDegrees, Rect cropRect) {
        FakeImageInfo fakeImageInfo = new FakeImageInfo();
        fakeImageInfo.setRotationDegrees(rotationDegrees);
        FakeImageProxy imageProxy = new FakeImageProxy(fakeImageInfo);
        imageProxy.setHeight(height);
        imageProxy.setWidth(width);
        imageProxy.setCropRect(cropRect);
        return imageProxy;
    }
}
