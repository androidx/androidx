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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplyTouchUp extends TestOperation {

    private final float mX;
    private final float mY;
    private final float mDx;
    private final float mDy;
    private final long mTimeMillis;

    public ApplyTouchUp(float x, float y) {
        this(x, y, 0f, 0f, 0);
    }

    public ApplyTouchUp(float x, float y, float dx, float dy) {
        this(x, y, dx, dy, 0);
    }

    public ApplyTouchUp(float x, float y, long timeMillis) {
        this(x, y, 0f, 0f, timeMillis);
    }

    public ApplyTouchUp(float x, float y, float dx, float dy, long timeMillis) {
        mX = x;
        mY = y;
        mDx = dx;
        mDy = dy;
        mTimeMillis = timeMillis;
    }

    @Override
    public boolean apply(@NonNull RemoteContext context, @NonNull CoreDocument document,
            @NonNull TestParameters testParameters, @Nullable List<Map<String, Object>> commands) {
        if (commands != null) {
            Map<String, Object> applyTouchUp = new LinkedHashMap<>();
            applyTouchUp.put("x", mX);
            applyTouchUp.put("y", mY);
            applyTouchUp.put("dx", mDx);
            applyTouchUp.put("dy", mDy);
            if (mTimeMillis != 0) {
                applyTouchUp.put("timeMillis", mTimeMillis);
            }
            Map<String, Object> testResult = new LinkedHashMap<>();
            commands.add(command("Apply TouchUp", applyTouchUp, testResult));
        }
        context.currentTime += mTimeMillis;
        context.setAnimationTime(context.currentTime / 1000f);
        document.touchUp(context, mX, mY, mDx, mDy);
        return false;
    }
}