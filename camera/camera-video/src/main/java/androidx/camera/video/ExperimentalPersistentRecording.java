/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses the experimental methods which allows a recording to be
 * persistent.
 *
 * <p>A persistent recording will only be stopped by explicitly calling {@link Recording#stop()}
 * or {@link Recording#close()} and will ignore events that would normally cause recording to
 * stop, such as lifecycle events or explicit unbinding of a {@link VideoCapture} use case that
 * the recording's {@link Recorder} is attached to.
 */
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalPersistentRecording {
}
