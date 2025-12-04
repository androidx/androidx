/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.utils;

import androidx.compose.remote.player.core.RemoteDocument;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Utility Interface to represent a doc for the integration test app
 */
@SuppressWarnings("RestrictedApiAndroidX")
public interface RCDoc {
    /**
     * Return base color
     * @return
     */
    int getColor();

    /**
     * convert to string
     * @return
     */
    @NonNull
    @Override
    String toString();

    /**
     * size of the document
     * @return
     */
    int size();

    /**
     * size of the document
     * @return
     */
    int zipSize();

    /**
     * Run the doc
     */
    void run();

    /**
     * get the looper
     * @return
     */
    default @Nullable RemoteDocument getLooper() {
        return null;
    }

    /**
     * get the looper frequency
     * @return
     */
    default int getLooperFreq() {
        return 0;
    }

    /**
     * Return the RemoteComposeDocument
     * @return
     */
    @Nullable
    RemoteDocument getDoc();
}
