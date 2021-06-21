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

package androidx.camera.view;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/**
 * Denotes that the API uses experimental transform APIs.
 *
 * <p> Ideally the transform APIs should be in the core artifact. However since there is a
 * version mismatch between core and view, if we add the API in the core, the view cannot depend
 * on it. So as a workaround, we temporarily put the transform API in the view artifact as an
 * experimental API until the versions are in sync.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(CLASS)
@RequiresOptIn
public @interface TransformExperimental {
}
