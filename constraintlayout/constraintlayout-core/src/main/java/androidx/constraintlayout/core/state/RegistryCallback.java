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
package androidx.constraintlayout.core.state;

public interface RegistryCallback {
    // @TODO: add description
    void onNewMotionScene(String content);

    // @TODO: add description
    void onProgress(float progress);

    // @TODO: add description
    void onDimensions(int width, int height);

    // @TODO: add description
    String currentMotionScene();

    // @TODO: add description
    void setDrawDebug(int debugMode);

    // @TODO: add description
    String currentLayoutInformation();

    // @TODO: add description
    void setLayoutInformationMode(int layoutInformationMode);

    // @TODO: add description
    long getLastModified();
}
