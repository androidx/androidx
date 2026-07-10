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

package androidx.webkit.internal;

import androidx.webkit.StartUpLocation;
import androidx.webkit.WebViewStartUpResult;

import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface.StartUpResultField;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebViewStartupResultImpl implements WebViewStartUpResult,
        BiConsumer<@StartUpResultField Integer, Object> {
    private Long mTotalTimeUiThreadMillis;
    private Long mMaxTimePerTaskUiThreadMillis;
    private List<StartUpLocation> mBlockingStartupLocations = new ArrayList<>();
    private List<StartUpLocation> mAsyncStartupLocations = new ArrayList<>();

    public WebViewStartupResultImpl(
            @NonNull Consumer<BiConsumer<@StartUpResultField Integer, Object>> result) {
        result.accept(this);
    }

    @Override
    public @Nullable Long getTotalTimeInUiThreadMillis() {
        return mTotalTimeUiThreadMillis;
    }

    @Override
    public @Nullable Long getMaxTimePerTaskInUiThreadMillis() {
        return mMaxTimePerTaskUiThreadMillis;
    }

    @Override
    public @Nullable List<StartUpLocation> getUiThreadBlockingStartUpLocations() {
        return mBlockingStartupLocations;
    }

    @Override
    public @Nullable List<StartUpLocation> getNonUiThreadBlockingStartUpLocations() {
        return mAsyncStartupLocations;
    }

    @Override
    public void accept(@StartUpResultField Integer key, Object value) {
        switch (key) {
            case StartUpResultField.TOTAL_TIME_UI_THREAD_MILLIS:
                if (value != null) {
                    mTotalTimeUiThreadMillis = (Long) value;
                }
                break;
            case StartUpResultField.MAX_TIME_PER_TASK_UI_THREAD_MILLIS:
                if (value != null) {
                    mMaxTimePerTaskUiThreadMillis = (Long) value;
                }
                break;
            case StartUpResultField.BLOCKING_START_UP_LOCATION:
                mBlockingStartupLocations.add(new StartUpLocationImpl((Throwable) value));
                break;
            case StartUpResultField.ASYNC_START_UP_LOCATION:
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        mAsyncStartupLocations.add(
                                new StartUpLocationImpl((Throwable) list.get(0)));
                    }
                } else {
                    mAsyncStartupLocations.add(new StartUpLocationImpl((Throwable) value));
                }
                break;
            default:
                if (key < 0) {
                    throw new UnsupportedOperationException(
                            "The current AndroidX version doesn't support this callback value: "
                                    + key);
                }
                // If we get here then it means that there's an optional operation that the
                // current AndroidX version doesn't support and it's safe to ignore.

        }
    }

    private static class StartUpLocationImpl implements StartUpLocation {
        private final Throwable mThrowable;

        StartUpLocationImpl(Throwable t) {
            mThrowable = t;
        }

        /**
         * Gets the stack information depicting the code location.
         */
        @Override
        @NonNull
        public Throwable getStackInformation() {
            return mThrowable;
        }
    }
}
