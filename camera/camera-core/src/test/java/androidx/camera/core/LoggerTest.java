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

    private static final String TAG = "aTag";
    private static final String LONG_TAG = "thisIsAnExcessivelyLongTag";
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
                .hasNoMoreMessages();
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
                .hasNoMoreMessages();
    }

    @Test
    public void logWithThrowable() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(TAG, mLevel);

        log(mLevel, TAG, LOG_MESSAGE, ANY_THROWABLE);

        assertLog()
                .hasMessage(mLevel, TAG, LOG_MESSAGE, ANY_THROWABLE)
                .hasNoMoreMessages();
    }

    @Config(maxSdk = 23)
    @Test
    public void log_truncateLongTag() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(LONG_TAG, mLevel);

        log(mLevel, LONG_TAG, LOG_MESSAGE);

        assertLog()
                .hasMessage(mLevel, LONG_TAG.substring(0, 23), LOG_MESSAGE)
                .hasNoMoreMessages();
    }

    @Config(minSdk = 24)
    @Test
    public void log_doNotTruncateLongTag() {
        Logger.setMinLogLevel(mLevel);
        ShadowLog.setLoggable(LONG_TAG, mLevel);

        log(mLevel, LONG_TAG, LOG_MESSAGE);

        assertLog()
                .hasMessage(mLevel, LONG_TAG, LOG_MESSAGE)
                .hasNoMoreMessages();
    }

    @Test
    public void doNotLog_whenMinLogLevelGreaterThanLevel_andTagNotLoggable() {
        Logger.setMinLogLevel(mLevel + 1);
        ShadowLog.setLoggable(TAG, mLevel + 1);

        log(mLevel, TAG, LOG_MESSAGE);

        assertLog().hasNoMoreMessages();
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

        LogAssert hasMessage(int priority, String tag, String message) {
            return hasMessage(priority, tag, message, null);
        }

        LogAssert hasMessage(int priority, String tag, String message,
                @Nullable Throwable throwable) {
            final LogItem item = mLogItems.get(mIndex++);
            assertThat(item.type).isEqualTo(priority);
            assertThat(item.tag).isEqualTo(tag);
            assertThat(item.msg).isEqualTo(message);
            assertThat(item.throwable).isEqualTo(throwable);
            return this;
        }

        void hasNoMoreMessages() {
            assertThat(mIndex).isEqualTo(mLogItems.size());
        }
    }
}
