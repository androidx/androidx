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

import java.util.HashMap;
import java.util.Set;

public class Registry {

    private static final Registry sRegistry = new Registry();

    public static Registry getInstance() {
        return sRegistry;
    }

    private HashMap<String, RegistryCallback> mCallbacks = new HashMap<>();

    // @TODO: add description
    public void register(String name, RegistryCallback callback) {
        mCallbacks.put(name, callback);
    }

    // @TODO: add description
    public void unregister(String name, RegistryCallback callback) {
        mCallbacks.remove(name);
    }

    // @TODO: add description
    public void updateContent(String name, String content) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            callback.onNewMotionScene(content);
        }
    }

    // @TODO: add description
    public void updateProgress(String name, float progress) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            callback.onProgress(progress);
        }
    }

    // @TODO: add description
    public String currentContent(String name) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            return callback.currentMotionScene();
        }
        return null;
    }

    // @TODO: add description
    public String currentLayoutInformation(String name) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            return callback.currentLayoutInformation();
        }
        return null;
    }

    // @TODO: add description
    public void setDrawDebug(String name, int debugMode) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            callback.setDrawDebug(debugMode);
        }
    }

    // @TODO: add description
    public void setLayoutInformationMode(String name, int mode) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            callback.setLayoutInformationMode(mode);
        }
    }

    public Set<String> getLayoutList() {
        return mCallbacks.keySet();
    }

    // @TODO: add description
    public long getLastModified(String name) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            return callback.getLastModified();
        }
        return Long.MAX_VALUE;
    }

    // @TODO: add description
    public void updateDimensions(String name, int width, int height) {
        RegistryCallback callback = mCallbacks.get(name);
        if (callback != null) {
            callback.onDimensions(width, height);
        }
    }
}
