/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */
package android.support.v17.leanback.os;

import android.os.Build;
import android.support.v17.leanback.os.TraceHelperJbmr2;


/**
 * Helper for systrace events.
 * @hide
 */
public final class TraceHelper {

    final static TraceHelperVersionImpl sImpl;

    static interface TraceHelperVersionImpl {
        public void beginSection(String section);
        public void endSection();
    }

    private static final class TraceHelperStubImpl implements TraceHelperVersionImpl {
        @Override
        public void beginSection(String section) {
        }

        @Override
        public void endSection() {
        }
    }

    private static final class TraceHelperJbmr2Impl implements TraceHelperVersionImpl {
        @Override
        public void beginSection(String section) {
            TraceHelperJbmr2.beginSection(section);
        }

        @Override
        public void endSection() {
            TraceHelperJbmr2.endSection();
        }
    }

    private TraceHelper() {
    }

    static {
        if (Build.VERSION.SDK_INT >= 18) {
            sImpl = new TraceHelperJbmr2Impl();
        } else {
            sImpl = new TraceHelperStubImpl();
        }
    }

    public static void beginSection(String section) {
        sImpl.beginSection(section);
    }

    public static void endSection() {
        sImpl.endSection();
    }
}
