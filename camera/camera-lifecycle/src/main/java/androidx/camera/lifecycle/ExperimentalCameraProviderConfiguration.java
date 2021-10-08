/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.lifecycle;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses an experimental path for configuring a camera provider.
 *
 * <p>In future releases, this experimental path may be modified or removed completely.
 *
 * <p>Non-experimental methods for initializing a camera provider include implementing
 * {@link androidx.camera.core.CameraXConfig.Provider} in the application's
 * {@link android.app.Application} class, or by simply using
 * {@link ProcessCameraProvider#getInstance(android.content.Context)}, which will use a default
 * configuration if none is explicitly configured.
 *
 * @see androidx.camera.core.CameraXConfig.Provider
 */
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalCameraProviderConfiguration {
}
