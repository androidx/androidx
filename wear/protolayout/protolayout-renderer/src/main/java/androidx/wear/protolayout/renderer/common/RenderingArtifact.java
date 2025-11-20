/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.common;

import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.renderer.common.ProviderStatsLogger.InflaterStatsLogger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Artifacts resulted from the layout rendering. */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface RenderingArtifact {

    /** Creates a {@link RenderingArtifact} instance. */
    static @NonNull RenderingArtifact create(
            @NonNull InflaterStatsLogger inflaterStatsLogger, @Nullable View inflatedParent) {
        return new SuccessfulRenderingArtifact(inflaterStatsLogger, inflatedParent);
    }

    /** Creates a {@link RenderingArtifact} instance for a skipped inflation. */
    static @NonNull RenderingArtifact skipped() {
        return new SkippedRenderingArtifact();
    }

    /** Creates a {@link RenderingArtifact} instance for a failed inflation. */
    static @NonNull RenderingArtifact failed() {
        return new FailedRenderingArtifact();
    }

    /** Artifacts resulted from a successful layout rendering. */
    class SuccessfulRenderingArtifact implements RenderingArtifact {
        private final @NonNull InflaterStatsLogger mInflaterStatsLogger;

        private final @Nullable View mInflatedParent;

        private SuccessfulRenderingArtifact(
                @NonNull InflaterStatsLogger inflaterStatsLogger, @Nullable View inflatedParent) {
            mInflaterStatsLogger = inflaterStatsLogger;
            mInflatedParent = inflatedParent;
        }

        /**
         * Returns the {@link ProviderStatsLogger.InflaterStatsLogger} used log inflation stats.
         * This will return {@code null} if the inflation was skipped or failed.
         */
        public @NonNull InflaterStatsLogger getInflaterStatsLogger() {
            return mInflaterStatsLogger;
        }

        /** Returns the inflated parent view. */
        public @Nullable View getInflatedParent() {
            return mInflatedParent;
        }
    }

    /** Artifacts resulted from a skipped layout rendering. */
    class SkippedRenderingArtifact implements RenderingArtifact {}

    /** Artifacts resulted from a failed layout rendering. */
    class FailedRenderingArtifact implements RenderingArtifact {}
}
