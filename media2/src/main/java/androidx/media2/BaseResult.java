/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import androidx.annotation.Nullable;

/**
 * Base interface for all result classes in {@link MediaSession}, {@link MediaController},
 * and {@link SessionPlayer}, for defining result codes in one place with documentation.
 * <p>
 * Error code: Negative integer
 * Success code: 0
 * Info code: Positive integer
 * <p>
 *    0 <  |code| <  100: Session player specific code.
 *  100 <= |code| <  500: Session/Controller specific code.
 *  500 <= |code| < 1000: Browser/Library session specific code.
 * 1000 <= |code|       : Custom session player result code.
 */
interface BaseResult {
    /**
     * Result code representing that the command is successfully completed.
     */
    int RESULT_CODE_SUCCESS = 0;

    /**
     * Result code represents that call is ended with an unknown error.
     */
    int RESULT_CODE_UNKNOWN_ERROR = -1;

    /**
     * Result code representing that the command cannot be completed because the current state is
     * not valid for the command.
     */
    int RESULT_CODE_INVALID_STATE = -2;

    /**
     * Result code representing that an argument is illegal.
     */
    int RESULT_CODE_BAD_VALUE = -3;

    /**
     * Result code representing that the command is not allowed.
     */
    int RESULT_CODE_PERMISSION_DENIED = -4;

    /**
     * Result code representing a file or network related command error.
     */
    int RESULT_CODE_IO_ERROR = -5;

    /**
     * Result code representing that the command is not supported nor implemented.
     */
    int RESULT_CODE_NOT_SUPPORTED = -6;

    /**
     * Result code representing that the command is skipped or canceled. For an example, a seek
     * command can be skipped if it is followed by another seek command.
     */
    int RESULT_CODE_SKIPPED = 1;

    // Subclasses should write its own documentation with @IntDef annotation at the return type.
    int getResultCode();

    // Subclasses should write its own documentation.
    // Should use SystemClock#elapsedRealtime() instead of System#currentTimeMillis() because
    //    1. System#currentTimeMillis() can be unexpectedly set by System#setCurrentTimeMillis() or
    //       changes in the timezone. So receiver cannot know when the command is finished.
    //    2. For matching the timestamp with the PlaybackState(Compat) which uses
    //       SystemClock#elapsedRealtime().
    long getCompletionTime();

    // Subclasses should write its own documentation.
    @Nullable MediaItem getMediaItem();
}
