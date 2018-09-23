# Copyright (C) 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Constructors accessed via reflection in DefaultRenderersFactory
-dontnote androidx.media2.exoplayer.external.ext.vp9.LibvpxVideoRenderer
-keepclassmembers class androidx.media2.exoplayer.external.ext.vp9.LibvpxVideoRenderer {
  <init>(boolean, long, android.os.Handler, androidx.media2.exoplayer.external.video.VideoRendererEventListener, int);
}
-dontnote androidx.media2.exoplayer.external.ext.opus.LibopusAudioRenderer
-keepclassmembers class androidx.media2.exoplayer.external.ext.opus.LibopusAudioRenderer {
  <init>(android.os.Handler, androidx.media2.exoplayer.external.audio.AudioRendererEventListener, androidx.media2.exoplayer.external.audio.AudioProcessor[]);
}
-dontnote androidx.media2.exoplayer.external.ext.flac.LibflacAudioRenderer
-keepclassmembers class androidx.media2.exoplayer.external.ext.flac.LibflacAudioRenderer {
  <init>(android.os.Handler, androidx.media2.exoplayer.external.audio.AudioRendererEventListener, androidx.media2.exoplayer.external.audio.AudioProcessor[]);
}
-dontnote androidx.media2.exoplayer.external.ext.ffmpeg.FfmpegAudioRenderer
-keepclassmembers class androidx.media2.exoplayer.external.ext.ffmpeg.FfmpegAudioRenderer {
  <init>(android.os.Handler, androidx.media2.exoplayer.external.audio.AudioRendererEventListener, androidx.media2.exoplayer.external.audio.AudioProcessor[]);
}

# Constructors accessed via reflection in DefaultExtractorsFactory
-dontnote androidx.media2.exoplayer.external.ext.flac.FlacExtractor
-keepclassmembers class androidx.media2.exoplayer.external.ext.flac.FlacExtractor {
  <init>();
}

# Constructors accessed via reflection in DefaultDataSource
-dontnote androidx.media2.exoplayer.external.ext.rtmp.RtmpDataSource
-keepclassmembers class androidx.media2.exoplayer.external.ext.rtmp.RtmpDataSource {
  <init>();
}

# Constructors accessed via reflection in DownloadAction
-dontnote androidx.media2.exoplayer.external.source.dash.offline.DashDownloadAction
-dontnote androidx.media2.exoplayer.external.source.hls.offline.HlsDownloadAction
-dontnote androidx.media2.exoplayer.external.source.smoothstreaming.offline.SsDownloadAction
