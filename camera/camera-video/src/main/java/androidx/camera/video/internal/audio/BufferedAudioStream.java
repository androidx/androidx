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

import android.annotation.SuppressLint;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class BufferedAudioStream implements AudioStream {

    private static final String TAG = "BufferedAudioStream";
    private static final int DEFAULT_BUFFER_SIZE_IN_FRAME = 1024;
    private static final int DEFAULT_QUEUE_SIZE = 500;
    private static final int DATA_WAITING_TIME_MILLIS = 1;

    private final AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private final AtomicBoolean mIsReleased = new AtomicBoolean(false);
    @GuardedBy("mLock")
    private final Queue<AudioData> mAudioDataQueue = new ConcurrentLinkedQueue<>();
    private final Executor mProducerExecutor = CameraXExecutors.newSequentialExecutor(
            CameraXExecutors.audioExecutor());
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    @Nullable
    private AudioData mAudioDataNotFullyRead = null;

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
        mSampleRate = audioSettings.getSampleRate();

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
            synchronized (mLock) {
                mAudioDataNotFullyRead = null;
                mAudioDataQueue.clear();
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
            synchronized (mLock) {
                mAudioDataNotFullyRead = null;
                mAudioDataQueue.clear();
            }
        });
    }

    @SuppressLint("BanThreadSleep")
    @NonNull
    @Override
    public PacketInfo read(@NonNull ByteBuffer byteBuffer) {
        checkNotReleasedOrThrow();
        checkStartedOrThrow();

        // Match collection buffer size and read buffer size to improve read efficiency.
        updateCollectionBufferSizeAsync(byteBuffer.remaining());

        // Block the thread till the audio data is actually read.
        boolean isWaitingForData;
        PacketInfo packetInfo = PacketInfo.of(0, 0);
        do {
            synchronized (mLock) {
                AudioData audioData = mAudioDataNotFullyRead;
                mAudioDataNotFullyRead = null;
                if (audioData == null) {
                    audioData = mAudioDataQueue.poll();
                }

                if (audioData != null) {
                    packetInfo = audioData.read(byteBuffer);

                    if (audioData.getRemainingBufferSizeInBytes() > 0) {
                        mAudioDataNotFullyRead = audioData;
                    }
                }
            }

            // Wait for data collection if no data to read and the audio stream is still running.
            isWaitingForData =
                    packetInfo.getSizeInBytes() <= 0 && mIsStarted.get() && !mIsReleased.get();

            // Sleep to prevent busy accessing to variables.
            if (isWaitingForData) {
                try {
                    Thread.sleep(DATA_WAITING_TIME_MILLIS);
                } catch (InterruptedException e) {
                    Logger.w(TAG, "Interruption while waiting for audio data", e);
                    break;
                }
            }
        } while (isWaitingForData);

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
        int queueMaxSize = mQueueMaxSize;
        synchronized (mLock) {
            mAudioDataQueue.offer(audioData);

            // Pop audio data when the queue size exceeds the limit.
            while (mAudioDataQueue.size() > queueMaxSize) {
                mAudioDataQueue.poll();
                Logger.w(TAG, "Drop audio data due to full of queue.");
            }
        }

        // Start next data collection.
        if (mIsCollectingAudioData.get()) {
            mProducerExecutor.execute(this::collectAudioData);
        }
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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
