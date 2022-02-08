/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.mlkit.vision;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A wrapper around MLKit's Detector.
 *
 * TODO(b/198984186): implement this class.
 * TODO(b/198984186): throw exception if the type == segmentation and the matrix is not identity.
 *
 * @param <T> the type of the detected result. For {@code BarcodeScanner}, it is {@code List
 *            <Barcode>}.
 * @hide
 */
@RequiresApi(21)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MlKitDetector<T> {

}
