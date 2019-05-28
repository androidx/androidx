/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.player.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media2.exoplayer.external.Renderer;
import androidx.media2.exoplayer.external.audio.AudioRendererEventListener;
import androidx.media2.exoplayer.external.audio.AudioSink;
import androidx.media2.exoplayer.external.audio.MediaCodecAudioRenderer;
import androidx.media2.exoplayer.external.drm.DrmSessionManager;
import androidx.media2.exoplayer.external.drm.FrameworkMediaCrypto;
import androidx.media2.exoplayer.external.mediacodec.MediaCodecSelector;
import androidx.media2.exoplayer.external.metadata.MetadataOutput;
import androidx.media2.exoplayer.external.metadata.MetadataRenderer;
import androidx.media2.exoplayer.external.text.TextOutput;
import androidx.media2.exoplayer.external.video.MediaCodecVideoRenderer;
import androidx.media2.exoplayer.external.video.VideoRendererEventListener;

/**
 * Factory for renderers for {@link ExoPlayerWrapper}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class RenderersFactory
        implements androidx.media2.exoplayer.external.RenderersFactory {

    public static final int VIDEO_RENDERER_INDEX = 0;
    public static final int AUDIO_RENDERER_INDEX = 1;
    public static final int TEXT_RENDERER_INDEX = 2;
    public static final int METADATA_RENDERER_INDEX = 3;

    private static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;
    private static final int MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 50;

    private final Context mContext;
    private final AudioSink mAudioSink;
    private final TextRenderer mTextRenderer;

    RenderersFactory(
            Context context,
            AudioSink audioSink,
            TextRenderer textRenderer) {
        mContext = context;
        mAudioSink = audioSink;
        mTextRenderer = textRenderer;
    }

    @Override
    public Renderer[] createRenderers(
            Handler eventHandler,
            VideoRendererEventListener videoRendererEventListener,
            AudioRendererEventListener audioRendererEventListener,
            TextOutput textRendererOutput,
            MetadataOutput metadataRendererOutput,
            @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        return new Renderer[] {
                new MediaCodecVideoRenderer(
                        mContext,
                        MediaCodecSelector.DEFAULT,
                        DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS,
                        drmSessionManager,
                        /* playClearSamplesWithoutKeys= */ false,
                        eventHandler,
                        videoRendererEventListener,
                        MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY),
                new MediaCodecAudioRenderer(
                        mContext,
                        MediaCodecSelector.DEFAULT,
                        drmSessionManager,
                        /* playClearSamplesWithoutKeys= */ false,
                        eventHandler,
                        audioRendererEventListener,
                        mAudioSink),
                mTextRenderer,
                new MetadataRenderer(
                        metadataRendererOutput,
                        eventHandler.getLooper(),
                        new Id3MetadataDecoderFactory())
        };
    }


}
