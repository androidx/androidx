/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLog.LogItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class LoggerTest {

    private static final String TAG = "LoggerTestTag";
    private static final String LONG_TAG = "LoggerTestExcessivelyLongTag";
    private static final String LOG_MESSAGE = "this is a log message!";
    private static final String ANOTHER_LOG_MESSAGE = "this is another log message!";
    private static final String YET_ANOTHER_LOG_MESSAGE = "this is yet another log message!";
    private static final Throwable ANY_THROWABLE = new RuntimeException();

    @ParameterizedRobolectricTestRunner.Parameters(name = "level={1}")
    public static Collection<Object[]> data() {
        final List<Object[]> logLevels = new ArrayList<>();
        logLevels.add(new Object[]{Log.DEBUG, "Debug"});
        logLevels.add(new Object[]{Log.INFO, "Info"});
        logLevels.add(new Object[]{Log.WARN, "Warn"});
        logLevels.add(new Object[]{Log.ERROR, "Error"});
        return logLevels;
    }

    private final int mLevel;

    // Used to make the test name more readable by writing the level name next to it
    @SuppressWarnings("FieldCanBeLocal")
    private final String mLevelLiteral;

    public LoggerTest(int level, String levelLiteral) {
        mLevel = level;
        mLevelLiteral = levelLiteral;
    }

    @Before
    public void clearLogs() {
        ShadowLog.clear();
    }

    @Test
    public void isLoggable_minLogLevel() {
        Logger.setMinLogLevel(mLevel);
        assertThat(isLoggable(mLevel, TAG)).isTrue();
    }

    @Test
    public void isLoggable_tagLoggable() {
        ShadowLog.setLoggable(TAG, mLevel);
        assertThat(isLoggable(mLevel, TAG)).isTrue();
    }

    @Test
    public void isNotLoggable_whenMinLogLevelGreaterThanLevel_andTagNotLoggable() {
        Logger.setMinLogLevel(mLevel + 1);
        ShadowLog.setLoggable(TAG, mLevel + 1);

        assertThat(isLoggable(mLevel, TAG)).isFalse();
    }

    @Test
    public void logOneTime() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(TAG, mLevel);

        log(mLevel, TAG, LOG_MESSAGE);

        assertLog()
                .hasMessage(mLevel, TAG, LOG_MESSAGE)
                .hasNoMoreMessages(TAG);
    }

    @Test
    public void logMultipleTimes() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(TAG, mLevel);

        log(mLevel, TAG, LOG_MESSAGE);
        log(mLevel, TAG, ANOTHER_LOG_MESSAGE);
        log(mLevel, TAG, YET_ANOTHER_LOG_MESSAGE);

        assertLog()
                .hasMessage(mLevel, TAG, LOG_MESSAGE)
                .hasMessage(mLevel, TAG, ANOTHER_LOG_MESSAGE)
                .hasMessage(mLevel, TAG, YET_ANOTHER_LOG_MESSAGE)
                .hasNoMoreMessages(TAG);
    }

    @Test
    public void logWithThrowable() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(TAG, mLevel);

        log(mLevel, TAG, LOG_MESSAGE, ANY_THROWABLE);

        assertLog()
                .hasMessage(mLevel, TAG, LOG_MESSAGE, ANY_THROWABLE)
                .hasNoMoreMessages(TAG);
    }

    @Config(maxSdk = 25)
    @Test
    public void log_truncateLongTag() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(LONG_TAG, mLevel);

        log(mLevel, LONG_TAG, LOG_MESSAGE);

        assertLog()
                .hasMessage(mLevel, LONG_TAG.substring(0, 23), LOG_MESSAGE)
                .hasNoMoreMessages(LONG_TAG);
    }

    @Config(minSdk = 26)
    @Test
    public void log_doNotTruncateLongTag() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(LONG_TAG, mLevel);

        log(mLevel, LONG_TAG, LOG_MESSAGE);

        assertLog()
                .hasMessage(mLevel, LONG_TAG, LOG_MESSAGE)
                .hasNoMoreMessages(LONG_TAG);
    }

    @Test
    public void doNotLog_whenMinLogLevelGreaterThanLevel_andTagNotLoggable() {
        Logger.setMinLogLevel(mLevel + 1);
        ShadowLog.setLoggable(TAG, mLevel + 1);

        log(mLevel, TAG, LOG_MESSAGE);

        assertLog().hasNoMoreMessages(TAG);
    }

    private boolean isLoggable(int level, String tag) {
        switch (level) {
            case Log.DEBUG:
                return Logger.isDebugEnabled(tag);
            case Log.INFO:
                return Logger.isInfoEnabled(tag);
            case Log.WARN:
                return Logger.isWarnEnabled(tag);
            case Log.ERROR:
                return Logger.isErrorEnabled(tag);
            default:
                return false;
        }
    }

    private void log(int level, String tag, String message) {
        log(level, tag, message, null);
    }

    private void log(int level, String tag, String message, @Nullable Throwable throwable) {
        if (!isLoggable(level, tag)) {
            return;
        }
        switch (level) {
            case Log.DEBUG:
                Logger.d(tag, message, throwable);
                break;
            case Log.INFO:
                Logger.i(tag, message, throwable);
                break;
            case Log.WARN:
                Logger.w(tag, message, throwable);
                break;
            case Log.ERROR:
                Logger.e(tag, message, throwable);
                break;
        }
    }

    private LogAssert assertLog() {
        return new LogAssert(ShadowLog.getLogs());
    }

    private static class LogAssert {

        private final List<LogItem> mLogItems;
        private int mIndex = 0;

        LogAssert(@NonNull final List<LogItem> logItems) {
            mLogItems = new ArrayList<>(logItems);
        }

        LogAssert hasMessage(int priority, @NonNull String tag, @NonNull String message) {
            return hasMessage(priority, tag, message, null);
        }

        LogAssert hasMessage(int priority, @NonNull String tag, @NonNull String message,
                @Nullable Throwable throwable) {
            int index = findLogItemIndexByTag(tag, mIndex);
            final LogItem item = mLogItems.get(index);

            try {
                assertThat(item.type).isEqualTo(priority);
                assertThat(item.tag).isEqualTo(tag);
                assertThat(item.msg).isEqualTo(message);
                assertThat(item.throwable).isEqualTo(throwable);
            } catch (Throwable e) {
                // TODO(b/208252595): Dump the log info for the issue clarification.
                throw new RuntimeException(collectLogItemsInfo() + "\n" + e);
            }

            mIndex = index + 1;

            return this;
        }

        /**
         * Checks no more messages with the specified tag.
         */
        void hasNoMoreMessages(@NonNull String tag) {
            try {
                assertThat(findLogItemIndexByTag(tag, mIndex)).isEqualTo(mLogItems.size());
            } catch (Throwable e) {
                // TODO(b/207674161): Dump the log info for the issue clarification.
                throw new RuntimeException(collectLogItemsInfo() + "\n" + e);
            }
        }

        /**
         * Returns the log item index with the specified tag. Returns mLogItems.size() if no item
         * can be found.
         */
        private int findLogItemIndexByTag(@NonNull String tag, int startIndex) {
            for (int i = startIndex; i < mLogItems.size(); i++) {
                if (tag.equals(mLogItems.get(i).tag)) {
                    return i;
                }
            }

            return mLogItems.size();
        }

        private String collectLogItemsInfo() {
            int counter = 0;
            String logItemsInfo =
                    "Log items count is " + mLogItems.size() + ", mIndex is " + mIndex + "\n";

            for (int i = mIndex; i >= 0; i--) {
                if (i >= mLogItems.size()) {
                    continue;
                }

                LogItem item = mLogItems.get(i);
                logItemsInfo +=
                        "index: " + i + ", item.type: " + item.type + ", item.tag: " + item.tag
                                + ", item.msg: " + item.msg + ", item.throwable: "
                                + item.throwable + "\n";

                // Prints five items at most in case too many items exist to cause problem.
                if (counter++ > 5) {
                    break;
                }
            }

            return logItemsInfo;
        }
    }
}
