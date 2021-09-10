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

package androidx.media2.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Helper classes to avoid ClassVerificationFailure.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class ClassVerificationHelper {

    /** Helper class for {@link android.media.AudioManager}. */
    public static final class AudioManager {

        /** Helper methods for {@link android.media.AudioManager} APIs added in API level 21. */
        @RequiresApi(21)
        public static final class Api21 {

            // TODO(b/194239360): Replace it with AudioManagerCompat#isVolumeFixed.
            /** Helper method to call {@link android.media.AudioManager#isVolumeFixed()}. */
            @DoNotInline
            public static boolean isVolumeFixed(@NonNull android.media.AudioManager manager) {
                return manager.isVolumeFixed();
            }

            private Api21() {}
        }

        private AudioManager() {}
    }

    /** Helper class for {@link android.os.HandlerThread}. */
    public static final class HandlerThread {

        /** Helper methods for {@link android.os.HandlerThread} APIs added in API level 18. */
        @RequiresApi(18)
        public static final class Api18 {

            /** Helper method to call {@link android.os.HandlerThread#quitSafely()}. */
            @DoNotInline
            public static boolean quitSafely(@NonNull android.os.HandlerThread handlerThread) {
                return handlerThread.quitSafely();
            }

            private Api18() {}
        }

        private HandlerThread() {}
    }

    /** Helper class for {@link android.app.PendingIntent}. */
    public static final class PendingIntent {

        /** Helper method for {@link android.app.PendingIntent} APIs added in API level 26. */
        @RequiresApi(26)
        public static final class Api26 {

            /** Helper method to call {@link android.app.PendingIntent#getForegroundService}. */
            @DoNotInline
            @Nullable
            public static android.app.PendingIntent getForegroundService(
                    @NonNull android.content.Context context, int requestCode,
                    @NonNull android.content.Intent intent, int flags) {
                return android.app.PendingIntent.getForegroundService(context, requestCode, intent,
                        flags);
            }

            private Api26() {}
        }

        private PendingIntent() {}
    }

    private ClassVerificationHelper() {}
}
