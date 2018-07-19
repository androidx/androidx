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

package androidx.work;

/**
 * The current state of a unit of work.
 */
public enum State {

    /**
     * The state for work that is enqueued (hasn't completed and isn't running)
     */
    ENQUEUED,

    /**
     * The state for work that is currently being executed
     */
    RUNNING,

    /**
     * The state for work that has completed successfully
     */
    SUCCEEDED,

    /**
     * The state for work that has completed in a failure state
     */
    FAILED,

    /**
     * The state for work that is currently blocked because its prerequisites haven't finished
     * successfully
     */
    BLOCKED,

    /**
     * The state for work that has been cancelled and will not execute
     */
    CANCELLED;

    /**
     * Returns {@code true} if this State is considered finished.
     *
     * @return {@code true} for {@link #SUCCEEDED}, {@link #FAILED}, and {@link #CANCELLED} states
     */
    public boolean isFinished() {
        return (this == SUCCEEDED || this == FAILED || this == CANCELLED);
    }
}
