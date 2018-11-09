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
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media2.exoplayer.external.DefaultRenderersFactory;
import androidx.media2.exoplayer.external.Renderer;
import androidx.media2.exoplayer.external.audio.AudioProcessor;
import androidx.media2.exoplayer.external.audio.AudioRendererEventListener;
import androidx.media2.exoplayer.external.audio.AudioSink;
import androidx.media2.exoplayer.external.audio.MediaCodecAudioRenderer;
import androidx.media2.exoplayer.external.drm.DrmSessionManager;
import androidx.media2.exoplayer.external.drm.FrameworkMediaCrypto;
import androidx.media2.exoplayer.external.mediacodec.MediaCodecSelector;
import androidx.media2.exoplayer.external.text.TextOutput;
import androidx.media2.exoplayer.external.video.MediaCodecVideoRenderer;
import androidx.media2.exoplayer.external.video.VideoRendererEventListener;

import java.util.ArrayList;

/**
 * Factory for renderers for {@link ExoPlayerWrapper}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class RenderersFactory extends DefaultRenderersFactory {

    public static final int VIDEO_RENDERER_INDEX = 0;
    public static final int AUDIO_RENDERER_INDEX = 1;
    public static final int TEXT_RENDERER_INDEX = 2;

    private final AudioSink mAudioSink;
    private final TextRenderer mTextRenderer;

    RenderersFactory(
            Context context,
            AudioSink audioSink,
            TextRenderer textRenderer) {
        super(context);
        mAudioSink = audioSink;
        mTextRenderer = textRenderer;
    }

    @Override
    protected void buildVideoRenderers(
            Context context,
            @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
            long allowedVideoJoiningTimeMs, Handler eventHandler,
            VideoRendererEventListener eventListener, int extensionRendererMode,
            ArrayList<Renderer> out) {
        out.add(
                new MediaCodecVideoRenderer(
                        context,
                        MediaCodecSelector.DEFAULT,
                        allowedVideoJoiningTimeMs,
                        drmSessionManager,
                        /* playClearSamplesWithoutKeys= */ false,
                        eventHandler,
                        eventListener,
                        MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY));
    }

    @Override
    protected void buildAudioRenderers(
            Context context,
            @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
            AudioProcessor[] audioProcessors, Handler eventHandler,
            AudioRendererEventListener eventListener, int extensionRendererMode,
            ArrayList<Renderer> out) {
        out.add(new MediaCodecAudioRenderer(
                context,
                MediaCodecSelector.DEFAULT,
                drmSessionManager,
                /* playClearSamplesWithoutKeys= */ false,
                eventHandler,
                eventListener,
                mAudioSink));
    }

    @Override
    protected void buildTextRenderers(Context context,
            TextOutput output,
            Looper outputLooper,
            int extensionRendererMode,
            ArrayList<Renderer> out) {
        out.add(mTextRenderer);
    }

}
