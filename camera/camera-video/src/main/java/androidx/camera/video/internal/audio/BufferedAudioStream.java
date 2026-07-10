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
import static androidx.camera.video.internal.audio.AudioUtils.sizeToFrameCount;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import androidx.annotation.GuardedBy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The class implements a buffered AudioStream.
 *
 * <p>A BufferedAudioStream adds functionality to another AudioStream, the ability to buffer the
 * input audio data and to decouple audio data producing with consuming. When the
 * BufferedAudioStream is created, an internal buffer queue is created. The queue's size is limited
 * to prevent memory from being overused. When the queue's size exceeds the limit, the oldest
 * cached data will be discarded.
 *
 * <p>This class is not thread safe, it should be used on the same thread.
 */
public class BufferedAudioStream implements AudioStream {

    private static final String TAG = "BufferedAudioStream";
    private static final int DEFAULT_BUFFER_SIZE_IN_FRAME = 1024;
    private static final int DEFAULT_QUEUE_SIZE = 500;
    private static final int DATA_WAITING_TIME_MILLIS = 100;

    private final AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private final AtomicBoolean mIsReleased = new AtomicBoolean(false);
    private final BlockingQueue<AudioData> mAudioDataQueue = new LinkedBlockingQueue<>();
    private final Executor mProducerExecutor = CameraXExecutors.newSequentialExecutor(
            CameraXExecutors.audioExecutor());
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private @Nullable AudioData mAudioDataNotFullyRead = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                      Members only accessed on mProducerExecutor                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final AudioStream mAudioStream;
    private final int mBytesPerFrame;
    private final int mSampleRate;
    private final int mQueueMaxSize;
    private final AtomicBoolean mIsCollectingAudioData = new AtomicBoolean(false);
    private int mBufferSize;

    public BufferedAudioStream(@NonNull AudioStream audioStream,
            @NonNull AudioSettings audioSettings) {
        mAudioStream = audioStream;
        mBytesPerFrame = audioSettings.getBytesPerFrame();
        mSampleRate = audioSettings.getCaptureSampleRate();

        checkArgument(mBytesPerFrame > 0L, "mBytesPerFrame must be greater than 0.");
        checkArgument(mSampleRate > 0L, "mSampleRate must be greater than 0.");

        mQueueMaxSize = DEFAULT_QUEUE_SIZE;
        mBufferSize = DEFAULT_BUFFER_SIZE_IN_FRAME * mBytesPerFrame;
    }

    @Override
    public void start() throws AudioStreamException, IllegalStateException {
        checkNotReleasedOrThrow();
        if (mIsStarted.getAndSet(true)) {
            return;
        }

        // Start internal audio data collection.
        RunnableFuture<Void> startTask = new FutureTask<>(() -> {
            try {
                mAudioStream.start();
                startCollectingAudioData();
            } catch (AudioStreamException e) {
                throw new RuntimeException(e);
            }
        }, null);
        mProducerExecutor.execute(startTask);

        // Wait for the internal audio stream to start.
        try {
            startTask.get();
        } catch (InterruptedException | ExecutionException e) {
            mIsStarted.set(false);
            throw new AudioStreamException(e);
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        checkNotReleasedOrThrow();
        if (!mIsStarted.getAndSet(false)) {
            return;
        }

        // Stop internal audio data collection.
        mProducerExecutor.execute(() -> {
            mIsCollectingAudioData.set(false);
            mAudioStream.stop();
            mAudioDataQueue.clear();
            synchronized (mLock) {
                mAudioDataNotFullyRead = null;
            }
        });
    }

    @Override
    public void release() {
        if (mIsReleased.getAndSet(true)) {
            return;
        }

        mProducerExecutor.execute(() -> {
            mIsCollectingAudioData.set(false);
            mAudioStream.release();
            mAudioDataQueue.clear();
            synchronized (mLock) {
                mAudioDataNotFullyRead = null;
            }
        });
    }

    @Override
    public @NonNull PacketInfo read(@NonNull ByteBuffer byteBuffer) {
        checkNotReleasedOrThrow();
        checkStartedOrThrow();

        // Match collection buffer size and read buffer size to improve read efficiency.
        updateCollectionBufferSizeAsync(byteBuffer.remaining());

        AudioData audioData;
        synchronized (mLock) {
            audioData = mAudioDataNotFullyRead;
            mAudioDataNotFullyRead = null;
        }

        if (audioData == null) {
            while (mIsStarted.get() && !mIsReleased.get()) {
                try {
                    audioData = mAudioDataQueue.poll(DATA_WAITING_TIME_MILLIS,
                            TimeUnit.MILLISECONDS);
                    if (audioData != null) {
                        break;
                    }
                } catch (InterruptedException e) {
                    Logger.w(TAG, "Interruption while waiting for audio data", e);
                    return PacketInfo.of(0, 0);
                }
            }
        }

        if (audioData == null) {
            return PacketInfo.of(0, 0);
        }

        PacketInfo packetInfo = audioData.read(byteBuffer);

        if (audioData.getRemainingBufferSizeInBytes() > 0) {
            synchronized (mLock) {
                mAudioDataNotFullyRead = audioData;
            }
        }

        return packetInfo;
    }

    @Override
    public void setCallback(@Nullable AudioStreamCallback callback, @Nullable Executor executor) {
        checkState(!mIsStarted.get(), "AudioStream can not be started when setCallback.");
        checkNotReleasedOrThrow();
        checkArgument(callback == null || executor != null,
                "executor can't be null with non-null callback.");

        mProducerExecutor.execute(() -> mAudioStream.setCallback(callback, executor));
    }

    private void checkNotReleasedOrThrow() {
        checkState(!mIsReleased.get(), "AudioStream has been released.");
    }

    private void checkStartedOrThrow() {
        checkState(mIsStarted.get(), "AudioStream has not been started.");
    }

    private void updateCollectionBufferSizeAsync(int bufferSize) {
        mProducerExecutor.execute(() -> updateCollectionBufferSize(bufferSize));
    }

    @ExecutedBy("mProducerExecutor")
    private void updateCollectionBufferSize(int bufferSize) {
        if (mBufferSize == bufferSize) {
            return;
        }

        // Ensure buffer size is multiple of the frame size.
        int originalBufferSize = mBufferSize;
        int newFrameSize = bufferSize / mBytesPerFrame;
        mBufferSize = newFrameSize * mBytesPerFrame;

        Logger.d(TAG, "Update buffer size from " + originalBufferSize + " to " + mBufferSize);
    }

    @ExecutedBy("mProducerExecutor")
    private void startCollectingAudioData() {
        if (mIsCollectingAudioData.getAndSet(true)) {
            return;
        }

        collectAudioData();
    }

    @ExecutedBy("mProducerExecutor")
    private void collectAudioData() {
        if (!mIsCollectingAudioData.get()) {
            return;
        }

        // Read audio data.
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mBufferSize);
        PacketInfo packetInfo = mAudioStream.read(byteBuffer);
        AudioData audioData = new AudioData(byteBuffer, packetInfo, mBytesPerFrame, mSampleRate);

        // Push audio data to the queue.
        if (!mAudioDataQueue.offer(audioData)) {
            Logger.w(TAG, "Failed to offer audio data to queue.");
        }

        // Pop audio data when the queue size exceeds the limit.
        while (mAudioDataQueue.size() > mQueueMaxSize) {
            mAudioDataQueue.poll();
            Logger.w(TAG, "Drop audio data due to full of queue.");
        }

        // Start next data collection.
        if (mIsCollectingAudioData.get()) {
            mProducerExecutor.execute(this::collectAudioData);
        }
    }

    private static class AudioData {

        private final int mBytesPerFrame;
        private final int mSampleRate;
        private final ByteBuffer mByteBuffer;
        private long mTimestampNs;

        AudioData(@NonNull ByteBuffer byteBuffer, @NonNull PacketInfo packetInfo,
                int bytesPerFrame, int sampleRate) {
            // Make the buffer ready for reading.
            byteBuffer.rewind();

            // Check if byte buffer match with packet info.
            int bufferSize = byteBuffer.limit() - byteBuffer.position();
            if (bufferSize != packetInfo.getSizeInBytes()) {
                throw new IllegalStateException(
                        "Byte buffer size is not match with packet info: " + bufferSize + " != "
                                + packetInfo.getSizeInBytes());
            }

            mBytesPerFrame = bytesPerFrame;
            mSampleRate = sampleRate;
            mByteBuffer = byteBuffer;
            mTimestampNs = packetInfo.getTimestampNs();
        }

        public int getRemainingBufferSizeInBytes() {
            return mByteBuffer.remaining();
        }

        public PacketInfo read(@NonNull ByteBuffer byteBuffer) {
            long timestampNs = mTimestampNs;

            // Check the read size, read data and handle timestamp for the remaining data.
            int readSizeInBytes;
            int originalSourcePosition = mByteBuffer.position();
            int originalDestinationPosition = byteBuffer.position();
            if (mByteBuffer.remaining() > byteBuffer.remaining()) {
                readSizeInBytes = byteBuffer.remaining();

                // Update the next timestamp to the start of the unread part.
                long readFrames = sizeToFrameCount(readSizeInBytes, mBytesPerFrame);
                long readDurationNs = frameCountToDurationNs(readFrames, mSampleRate);
                mTimestampNs += readDurationNs;

                // Use the duplicated byte buffer to put data into the destination to limit the
                // read size and to not corrupt the source.
                ByteBuffer duplicatedByteBuffer = mByteBuffer.duplicate();
                duplicatedByteBuffer.position(originalSourcePosition)
                        .limit(originalSourcePosition + readSizeInBytes);
                byteBuffer.put(duplicatedByteBuffer)
                        .limit(originalDestinationPosition + readSizeInBytes)
                        .position(originalDestinationPosition);

            } else {
                readSizeInBytes = mByteBuffer.remaining();

                // Put data into byte buffer.
                byteBuffer.put(mByteBuffer)
                        .limit(originalDestinationPosition + readSizeInBytes)
                        .position(originalDestinationPosition);
            }

            // Point to the start of the unread part.
            mByteBuffer.position(originalSourcePosition + readSizeInBytes);

            return PacketInfo.of(readSizeInBytes, timestampNs);
        }
    }
}
