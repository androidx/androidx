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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media2.exoplayer.external.BaseRenderer;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.ExoPlaybackException;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.FormatHolder;
import androidx.media2.exoplayer.external.text.SubtitleInputBuffer;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.ParsableByteArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Outputs encoded text data from a selected text type and channel.
 *
 * <p>The decoding process implemented here should match NuPlayer2CCDecoder.cpp in the framework.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ class TextRenderer extends BaseRenderer {

    /** Interface for text renderer outputs. */
    public interface Output {
        /** Called when a channel becomes available for selection via {@link #select(int, int)}. */
        void onChannelAvailable(int type, int channel);
        /** Called when a buffer of text data is output. */
        void onCcData(byte[] data, long timeUs);
    }

    @IntDef(/*prefix = "TRACK_TYPE",*/ value = {
            TRACK_TYPE_CEA608,
            TRACK_TYPE_CEA708,
            TRACK_TYPE_UNSET,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextTrackType {}
    public static final int TRACK_TYPE_CEA608 = 0;
    public static final int TRACK_TYPE_CEA708 = 1;
    public static final int TRACK_TYPE_UNSET = -1;

    /**
     * The maximum time read ahead distance in microseconds. This matches the early buffer threshold
     * in ExoPlayer's video renderer.
     */
    private static final int READ_AHEAD_THRESHOLD_US = 110000;
    private static final int PACKET_LENGTH = 3;
    private static final int CHANNEL_UNSET = -1;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Output mOutput;
    private final Handler mHandler;
    private final ParsableByteArray mCcData;
    private final SortedMap<Long, byte[]> mCcMap;
    private final FormatHolder mFormatHolder;
    private final SubtitleInputBuffer mInputBuffer;
    private final DataBuilder mLine21DataBuilder;
    private final DataBuilder mDtvDataBuilder;
    private final int[] mLine21Channels;
    private final ParsableByteArray mScratch;

    private boolean mHasPendingInputBuffer;
    private boolean mInputStreamEnded;
    private boolean[] mIsTypeAndChannelAdvertised;
    @TextTrackType
    private int mSelectedType;
    private int mSelectedChannel;

    TextRenderer(Output output) {
        super(C.TRACK_TYPE_TEXT);
        mOutput = output;
        mHandler = new Handler(Looper.myLooper());
        mCcData = new ParsableByteArray();
        mCcMap = new TreeMap<>();
        mFormatHolder = new FormatHolder();
        mInputBuffer = new SubtitleInputBuffer();
        mLine21DataBuilder = new DataBuilder();
        mDtvDataBuilder = new DataBuilder();
        mLine21Channels = new int[2];
        mScratch = new ParsableByteArray();
        mSelectedType = TRACK_TYPE_UNSET;
        mSelectedChannel = CHANNEL_UNSET;
    }

    // BaseRenderer implementation

    @Override
    public int supportsFormat(Format format) {
        String mimeType = format.sampleMimeType;
        if (MimeTypes.APPLICATION_CEA608.equals(mimeType)
                || MimeTypes.APPLICATION_CEA708.equals(mimeType)
                || MimeTypes.TEXT_VTT.equals(mimeType)) {
            return FORMAT_HANDLED;
        } else if (MimeTypes.isText(mimeType)) {
            return FORMAT_UNSUPPORTED_SUBTYPE;
        } else {
            return FORMAT_UNSUPPORTED_TYPE;
        }
    }

    @Override
    protected void onStreamChanged(Format[] formats, long offsetUs) throws ExoPlaybackException {
        super.onStreamChanged(formats, offsetUs);
        mIsTypeAndChannelAdvertised = new boolean[128];
    }

    @Override
    protected synchronized void onPositionReset(long positionUs, boolean joining) {
        flush();
    }

    @Override
    public synchronized void render(long positionUs, long elapsedRealtimeUs) {
        if (getState() != STATE_STARTED) {
            return;
        }

        // Display any pending subtitles.
        display(positionUs);

        // Get an input buffer to parse.
        if (!mHasPendingInputBuffer) {
            // Try to read more subtitles from the source.
            int result = readSource(mFormatHolder, mInputBuffer, /* formatRequired= */ false);
            if (result == C.RESULT_NOTHING_READ || result == C.RESULT_FORMAT_READ) {
                return;
            }
            if (mInputBuffer.isEndOfStream()) {
                mInputStreamEnded = true;
                return;
            }
            mHasPendingInputBuffer = true;
            mInputBuffer.flip();
        }
        if (mInputBuffer.timeUs - positionUs > READ_AHEAD_THRESHOLD_US) {
            // We aren't ready to parse this buffer yet.
            return;
        }
        mHasPendingInputBuffer = false;
        mCcData.reset(mInputBuffer.data.array(), mInputBuffer.data.limit());
        mLine21DataBuilder.clear();
        while (mCcData.bytesLeft() >= PACKET_LENGTH) {
            byte ccDataHeader = (byte) mCcData.readUnsignedByte();
            byte ccData1 = (byte) mCcData.readUnsignedByte();
            byte ccData2 = (byte) mCcData.readUnsignedByte();

            boolean ccValid = (ccDataHeader & 0x04) != 0;
            int ccType = ccDataHeader & 0x03;
            if (ccValid) {
                if (ccType == 3) {
                    if (mDtvDataBuilder.hasData()) {
                        handleDtvPacket(mDtvDataBuilder, mInputBuffer.timeUs);
                    }
                    mDtvDataBuilder.append(ccData1, ccData2);
                } else if (mDtvDataBuilder.mLength > 0 && ccType == 2) {
                    mDtvDataBuilder.append(ccData1, ccData2);
                } else if (ccType == 0 || ccType == 1) {
                    ccData1 = (byte) (ccData1 & 0x7F);
                    ccData2 = (byte) (ccData2 & 0x7F);
                    if (ccData1 < 0x10 && ccData2 < 0x10) {
                        // Null padding.
                        continue;
                    }
                    if (ccData1 >= 0x10 && ccData1 <= 0x1F) {
                        int channel = (ccData1 >= 0x18 ? 1 : 0) + (ccDataHeader != 0 ? 2 : 0);
                        mLine21Channels[ccType] = channel;
                        maybeAdvertiseChannel(TRACK_TYPE_CEA608, channel);
                    }
                    if (mSelectedType == TRACK_TYPE_CEA608
                            && mSelectedChannel == mLine21Channels[ccType]) {
                        mLine21DataBuilder.append((byte) ccType, ccData1, ccData2);
                    }
                }
            } else if ((ccType == 3 || ccType == 2) && mDtvDataBuilder.hasData()) {
                handleDtvPacket(mDtvDataBuilder, mInputBuffer.timeUs);
            }
        }

        if (mSelectedType == TRACK_TYPE_CEA608 && mLine21DataBuilder.hasData()) {
            handleLine21Packet(mLine21DataBuilder, mInputBuffer.timeUs);
        }
    }

    @Override
    public boolean isEnded() {
        return mInputStreamEnded && mCcMap.isEmpty();
    }

    @Override
    public boolean isReady() {
        // Don't block playback whilst subtitles are loading.
        // Note: To change this behavior, it will be necessary to consider [Internal: b/12949941].
        return true;
    }

    // Track selection.

    /** Clears any previous selection. */
    public synchronized void clearSelection() {
        select(TRACK_TYPE_UNSET, CHANNEL_UNSET);
    }

    /** Selects the specified track type/channel for extraction and rendering. */
    public synchronized void select(@TextTrackType int type, int channel) {
        mSelectedType = type;
        mSelectedChannel = channel;
        flush();
    }

    // Internal methods.

    private void flush() {
        mCcMap.clear();
        mLine21DataBuilder.clear();
        mDtvDataBuilder.clear();
        mInputStreamEnded = false;
        mHasPendingInputBuffer = false;
    }

    private void maybeAdvertiseChannel(final int type, final int channel) {
        int typeAndChannel = (type << 6) + channel;
        if (!mIsTypeAndChannelAdvertised[typeAndChannel]) {
            mIsTypeAndChannelAdvertised[typeAndChannel] = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOutput.onChannelAvailable(type, channel);
                }
            });
        }
    }

    private void handleDtvPacket(DataBuilder dataBuilder, long timeUs) {
        mScratch.reset(dataBuilder.mData, dataBuilder.mLength);
        dataBuilder.clear();
        int size = mScratch.readUnsignedByte() & 0x1F;
        if (size == 0) {
            size = 64;
        }
        if (mScratch.limit() != size * 2) {
            return;
        }
        while (mScratch.bytesLeft() >= 2) {
            int value = mScratch.readUnsignedByte();
            int serviceNumber = (value & 0xE0) >> 5;
            int blockSize = (value & 0x1F);
            if (serviceNumber == 7) {
                serviceNumber = mScratch.readUnsignedByte() & 0x3F;
                if (serviceNumber < 7) {
                    return;
                }
            }
            if (mScratch.bytesLeft() < blockSize) {
                return;
            }
            if (blockSize > 0) {
                maybeAdvertiseChannel(TRACK_TYPE_CEA708, serviceNumber);
                if (mSelectedType == TRACK_TYPE_CEA708 && mSelectedChannel == serviceNumber) {
                    byte[] data = new byte[blockSize];
                    mScratch.readBytes(data, 0, blockSize);
                    mCcMap.put(timeUs, data);
                    continue;
                }
                mScratch.skipBytes(blockSize);
            }
        }
    }

    private void handleLine21Packet(DataBuilder dataBuilder, long timeUs) {
        mCcMap.put(timeUs, Arrays.copyOf(dataBuilder.mData, dataBuilder.mLength));
        dataBuilder.clear();
    }

    private void display(long timeUs) {
        if (mSelectedType == TRACK_TYPE_UNSET || mSelectedChannel == CHANNEL_UNSET) {
            // Nothing is selected for output.
            return;
        }
        byte[] data = new byte[0];
        long displayTimeUs = C.TIME_UNSET;
        while (!mCcMap.isEmpty()) {
            long ccTimeUs = mCcMap.firstKey();
            if (timeUs < ccTimeUs) {
                break;
            }
            byte[] ccData = Preconditions.checkNotNull(mCcMap.get(ccTimeUs));
            displayTimeUs = ccTimeUs;
            int offset = data.length;
            data = Arrays.copyOf(data, offset + ccData.length);
            System.arraycopy(ccData, 0, data, offset, ccData.length);
            mCcMap.remove(mCcMap.firstKey());
        }
        if (data.length > 0) {
            mOutput.onCcData(data, displayTimeUs);
        }
    }

    /** Utility for building a byte array by appending. */
    private static final class DataBuilder {

        public byte[] mData;
        public int mLength;

        DataBuilder() {
            mData = new byte[PACKET_LENGTH];
        }

        public void append(byte cc0, byte cc1) {
            if (mLength + 2 > mData.length) {
                // Double the size each time we run out of space to avoid frequent size increases.
                mData = Arrays.copyOf(mData, mData.length * 2);
            }
            mData[mLength++] = cc0;
            mData[mLength++] = cc1;
        }

        public void append(byte cc0, byte cc1, byte cc2) {
            if (mLength + 3 > mData.length) {
                mData = Arrays.copyOf(mData, mData.length * 2);
            }
            mData[mLength++] = cc0;
            mData[mLength++] = cc1;
            mData[mLength++] = cc2;
        }

        public boolean hasData() {
            return mLength > 0;
        }

        public void clear() {
            mLength = 0;
        }

    }

}
