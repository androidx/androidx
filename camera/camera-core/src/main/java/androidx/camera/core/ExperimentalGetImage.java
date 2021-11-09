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

package androidx.camera.core;

import static java.lang.annotation.RetentionPolicy.CLASS;

import android.media.Image;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses the experimental {@link ImageProxy#getImage()} method.
 *
 * <p> The getImage() method makes the assumptions that each {@link ImageProxy} is the sole owner
 * of the underlying {@link android.media.Image} which might not be the case. In the case where
 * the Image is shared by multiple ImageProxy, if the Image is closed then it will invalidate
 * multiple ImageProxy without a way to clearly indicate this has occurred.
 *
 * <p> When using this method it would be recommended to not close the Image via
 * {@link Image#close()}. Instead when the Image needs to be closed, {@link ImageProxy#close()}
 * should be called on the ImageProxy from which the Image was retrieved.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalGetImage {
}
