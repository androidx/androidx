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

package androidx.compose.remote.core.layout;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.layout.Component;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

class TestComponentOperation extends TestOperation {

    private final int mId;
    private RemoteContext mContext;
    private CoreDocument mDocument;

    private TestParameters mTestParameters;
    private @Nullable List<Map<String, Object>> mCommands;

    TestComponentOperation(int id) {
        mId = id;
    }

    @Override
    public boolean apply(@NonNull RemoteContext context, @NonNull CoreDocument document,
            @NonNull TestParameters testParameters,
            @Nullable List<Map<String, Object>> commands) {
        mContext = context;
        mDocument = document;
        mTestParameters = testParameters;
        mCommands = commands;
        Component component = document.getComponent(mId);
        return apply(component);
    }

    protected RemoteContext getContext() {
        return mContext;
    }

    protected CoreDocument getDocument() {
        return mDocument;
    }

    protected TestParameters getTestParameters() {
        return mTestParameters;
    }

    protected @Nullable List<Map<String, Object>> getCommands() {
        return mCommands;
    }

    public boolean apply(Component component) {
        return false;
    }

    public int getId() {
        return mId;
    }
}
