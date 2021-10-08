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

package androidx.camera.video.internal.encoder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/** An exception thrown to indicate an error has occurred during configuring an encoder. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class InvalidConfigException extends Exception {

    public InvalidConfigException(@Nullable String message) {
        super(message);
    }

    public InvalidConfigException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigException(@Nullable Throwable cause) {
        super(cause);
    }
}
