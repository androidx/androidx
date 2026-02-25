/*
 * Copyright 2026 The Android Open Source Project
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A test operation that resizes the document and sets a new origin.
 */
public class ResizeWithOrigin extends TestOperation {
    private final int mWidth;
    private final int mHeight;
    private final float mOriginX;
    private final float mOriginY;

    public ResizeWithOrigin(int width, int height, float originX, float originY) {
        mWidth = width;
        mHeight = height;
        mOriginX = originX;
        mOriginY = originY;
    }

    @Override
    public boolean apply(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull TestParameters testParameters,
            @Nullable List<Map<String, Object>> commands) {
        document.setOrigin(mOriginX, mOriginY);
        document.measure(context, 0, mWidth, 0, mHeight);
        return true;
    }
}
