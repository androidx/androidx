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
package androidx.media2.widget;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

// Built on top of com.android.compatibility.common.util.MediaUtils
class TestUtils {
    private static final String TAG = "TestUtils";

    /**
     * Returns true iff all audio and video tracks are supported
     */
    static boolean hasCodecsForResource(Context context, int resourceId) {
        try {
            AssetFileDescriptor afd = null;
            MediaExtractor ex = null;
            try {
                afd = context.getResources().openRawResourceFd(resourceId);
                ex = new MediaExtractor();
                ex.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return hasCodecsForMedia(ex);
            } finally {
                if (ex != null) {
                    ex.release();
                }
                if (afd != null) {
                    afd.close();
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open resource");
        }
        return false;
    }

    /**
     * Returns true iff all audio and video tracks are supported
     */
    static boolean hasCodecsForUri(Context context, Uri uri) {
        try {
            MediaExtractor ex = null;
            try {
                ex = new MediaExtractor();
                ex.setDataSource(context, uri, null);
                return hasCodecsForMedia(ex);
            } finally {
                if (ex != null) {
                    ex.release();
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open resource");
        }
        return false;
    }

    /**
     * Returns true iff all audio and video tracks are supported
     */
    static boolean hasCodecsForFileDescriptor(AssetFileDescriptor afd) {
        try {
            MediaExtractor ex = null;
            try {
                ex = new MediaExtractor();
                ex.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return hasCodecsForMedia(ex);
            } finally {
                if (ex != null) {
                    ex.release();
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open resource");
        }
        return false;
    }

    /**
     * Returns true iff all audio and video tracks are supported
     */
    static boolean hasCodecsForMedia(MediaExtractor ex) {
        for (int i = 0; i < ex.getTrackCount(); ++i) {
            MediaFormat format = ex.getTrackFormat(i);
            // only check for audio and video codecs
            String mime = format.getString(MediaFormat.KEY_MIME).toLowerCase();
            if (!mime.startsWith("audio/") && !mime.startsWith("video/")) {
                continue;
            }
            if (!canDecode(format)) {
                return false;
            }
        }
        return true;
    }

    static boolean canDecode(MediaFormat format) {
        if (Build.VERSION.SDK_INT <= 20) {
            // For build versions <= 20, the secure codecs may not be listed in MediaCodecList
            // but still be available on the system. Check that the codec that is needed for
            // decoding the test file exists by instantiating it by name and making sure that it
            // does not throw an IOException.
            // See https://developer.android.com/reference/android/media/MediaCodec#creation
            String codec = "OMX.qcom.video.decoder.avc.secure";
            try {
                MediaCodec mediaCodec = MediaCodec.createByCodecName(codec);
                if (mediaCodec != null) {
                    return true;
                }
            } catch (IOException e) {
                // Skip to end of method.
            }
        } else {
            final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            if (sMCL.findDecoderForFormat(format) != null) {
                return true;
            }
        }
        Log.i(TAG, "no decoder for " + format);
        return false;
    }
}
