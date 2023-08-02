/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video.internal.audio;

import static androidx.camera.video.internal.audio.AudioUtils.frameCountToDurationNs;
import static androidx.camera.video.internal.audio.AudioUtils.frameCountToSize;
import static androidx.camera.video.internal.audio.AudioUtils.sizeToFrameCount;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An AudioStream that only outputs silent audio.
 *
 * <p>This class is not thread safe, it should be used on the same thread.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SilentAudioStream implements AudioStream {
    private static final String TAG = "SilentAudioStream";

    private final AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private final AtomicBoolean mIsReleased = new AtomicBoolean(false);
    private final int mBytesPerFrame;
    private final int mSampleRate;
    @Nullable
    private byte[] mZeroBytes;
    private long mCurrentReadTimeNs;
    @Nullable
    private AudioStreamCallback mAudioStreamCallback;
    @Nullable
    private Executor mCallbackExecutor;

    /**
     * Constructs the instance.
     *
     * @param audioSettings the audio settings.
     */
    public SilentAudioStream(@NonNull AudioSettings audioSettings) {
        mBytesPerFrame = audioSettings.getBytesPerFrame();
        mSampleRate = audioSettings.getSampleRate();
    }

    @Override
    public void setCallback(@Nullable AudioStreamCallback callback, @Nullable Executor executor) {
        checkState(!mIsStarted.get(), "AudioStream can not be started when setCallback.");
        checkNotReleasedOrThrow();
        checkArgument(callback == null || executor != null,
                "executor can't be null with non-null callback.");
        mAudioStreamCallback = callback;
        mCallbackExecutor = executor;
    }

    @Override
    public void start() {
        checkNotReleasedOrThrow();
        if (mIsStarted.getAndSet(true)) {
            return;
        }
        mCurrentReadTimeNs = currentSystemTimeNs();
        notifySilenced();
    }

    @Override
    public void stop() {
        checkNotReleasedOrThrow();
        mIsStarted.set(false);
    }

    @Override
    public void release() {
        mIsReleased.getAndSet(true);
    }

    @NonNull
    @Override
    public PacketInfo read(@NonNull ByteBuffer byteBuffer) {
        checkNotReleasedOrThrow();
        checkStartedOrThrow();
        long requiredFrameCount = sizeToFrameCount(byteBuffer.remaining(), mBytesPerFrame);
        int requiredSize = (int) frameCountToSize(requiredFrameCount, mBytesPerFrame);
        if (requiredSize <= 0) {
            return PacketInfo.of(0, mCurrentReadTimeNs);
        }
        long requiredDurationNs = frameCountToDurationNs(requiredFrameCount, mSampleRate);
        long nextReadTimeNs = mCurrentReadTimeNs + requiredDurationNs;
        blockUntilSystemTimeReached(nextReadTimeNs);
        writeSilenceToBuffer(byteBuffer, requiredSize);
        PacketInfo packetInfo = PacketInfo.of(requiredSize, mCurrentReadTimeNs);
        mCurrentReadTimeNs = nextReadTimeNs;
        return packetInfo;
    }

    private void writeSilenceToBuffer(@NonNull ByteBuffer byteBuffer, int sizeInBytes) {
        checkState(sizeInBytes <= byteBuffer.remaining());
        if (mZeroBytes == null || mZeroBytes.length < sizeInBytes) {
            mZeroBytes = new byte[sizeInBytes];
        }
        int originalPosition = byteBuffer.position();
        byteBuffer.put(mZeroBytes, 0, sizeInBytes)
                .limit(originalPosition + sizeInBytes)
                .position(originalPosition);
    }

    private void notifySilenced() {
        AudioStreamCallback callback = mAudioStreamCallback;
        Executor executor = mCallbackExecutor;
        if (callback != null && executor != null) {
            executor.execute(() -> callback.onSilenceStateChanged(true));
        }
    }

    private void checkNotReleasedOrThrow() {
        checkState(!mIsReleased.get(), "AudioStream has been released.");
    }

    private void checkStartedOrThrow() {
        checkState(mIsStarted.get(), "AudioStream has not been started.");
    }

    private static long currentSystemTimeNs() {
        return System.nanoTime();
    }

    // To avoid writing silence too fast, delay a while if the current system time haven't reach
    // the next read time.
    private static void blockUntilSystemTimeReached(long nextReadTimeNs) {
        long requiredBlockTimeNs = nextReadTimeNs - currentSystemTimeNs();
        if (requiredBlockTimeNs > 0L) {
            try {
                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(requiredBlockTimeNs));
            } catch (InterruptedException e) {
                Logger.w(TAG, "Ignore interruption", e);
            }
        }
    }
}
