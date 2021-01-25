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

package androidx.media2.player;

import androidx.annotation.NonNull;

/**
 * Immutable class for describing video size.
 */
public final class VideoSize extends androidx.media2.common.VideoSize {
    VideoSize(@NonNull androidx.media2.common.VideoSize internal) {
        super(internal.getWidth(), internal.getHeight());
    }

    public VideoSize(int width, int height) {
        super(width, height);
    }
}
