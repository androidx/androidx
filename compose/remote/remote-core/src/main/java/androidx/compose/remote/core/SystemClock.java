/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core;
import androidx.annotation.RestrictTo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * An implementation of {@link Clock} which returns the current time in the system default time
 * zone.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemClock extends RemoteClock {
    @Override
    public ZoneId getZone() {
        System.nanoTime();
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return Instant.now();
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }
}
