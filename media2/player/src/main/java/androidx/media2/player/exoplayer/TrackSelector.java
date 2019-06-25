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
import static androidx.media2.player.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_AUDIO;
import static androidx.media2.player.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_METADATA;
import static androidx.media2.player.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;
import static androidx.media2.player.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN;
import static androidx.media2.player.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_VIDEO;
import static androidx.media2.player.exoplayer.RenderersFactory.AUDIO_RENDERER_INDEX;
import static androidx.media2.player.exoplayer.RenderersFactory.METADATA_RENDERER_INDEX;
import static androidx.media2.player.exoplayer.RenderersFactory.TEXT_RENDERER_INDEX;
import static androidx.media2.player.exoplayer.RenderersFactory.VIDEO_RENDERER_INDEX;
import static androidx.media2.player.exoplayer.TextRenderer.TRACK_TYPE_CEA608;
import static androidx.media2.player.exoplayer.TextRenderer.TRACK_TYPE_CEA708;
import static androidx.media2.player.exoplayer.TextRenderer.TRACK_TYPE_WEBVTT;
import static androidx.media2.player.exoplayer.TrackSelector.InternalTextTrackInfo.UNSET;

import android.annotation.SuppressLint;
import android.media.MediaFormat;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.DefaultTrackSelector;
import androidx.media2.exoplayer.external.trackselection.MappingTrackSelector;
import androidx.media2.exoplayer.external.trackselection.TrackSelection;
import androidx.media2.exoplayer.external.trackselection.TrackSelectionArray;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.player.MediaPlayer2;
import androidx.media2.player.common.TrackInfoImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages track selection for {@link ExoPlayerWrapper}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class TrackSelector {

    private static final int TRACK_INDEX_UNSET = -1;

    private final TextRenderer mTextRenderer;
    private final DefaultTrackSelector mDefaultTrackSelector;
    private final List<MediaPlayer2.TrackInfo> mAudioTrackInfos;
    private final List<MediaPlayer2.TrackInfo> mVideoTrackInfos;
    private final List<MediaPlayer2.TrackInfo> mMetadataTrackInfos;
    private final List<MediaPlayer2.TrackInfo> mTextTrackInfos;
    private final List<InternalTextTrackInfo> mInternalTextTrackInfos;

    private boolean mPendingMetadataUpdate;
    private int mSelectedAudioTrackIndex;
    private int mSelectedVideoTrackIndex;
    private int mSelectedMetadataTrackIndex;
    private int mPlayerTextTrackIndex;
    private int mSelectedTextTrackIndex;

    TrackSelector(TextRenderer textRenderer) {
        mTextRenderer = textRenderer;
        mDefaultTrackSelector = new DefaultTrackSelector();
        mAudioTrackInfos = new ArrayList<>();
        mVideoTrackInfos = new ArrayList<>();
        mMetadataTrackInfos = new ArrayList<>();
        mTextTrackInfos = new ArrayList<>();
        mInternalTextTrackInfos = new ArrayList<>();
        mSelectedAudioTrackIndex = TRACK_INDEX_UNSET;
        mSelectedVideoTrackIndex = TRACK_INDEX_UNSET;
        mSelectedMetadataTrackIndex = TRACK_INDEX_UNSET;
        mPlayerTextTrackIndex = TRACK_INDEX_UNSET;
        mSelectedTextTrackIndex = TRACK_INDEX_UNSET;
        // Ensure undetermined text tracks are selected so that CEA-608/708 streams are sent to the
        // text renderer. By default, metadata tracks are not selected.
        mDefaultTrackSelector.setParameters(
                new DefaultTrackSelector.ParametersBuilder()
                        .setSelectUndeterminedTextLanguage(true)
                        .setRendererDisabled(METADATA_RENDERER_INDEX, /* disabled= */ true));
    }

    public DefaultTrackSelector getPlayerTrackSelector() {
        return mDefaultTrackSelector;
    }

    public void handlePlayerTracksChanged(Player player) {
        mPendingMetadataUpdate = true;

        // Clear all selection state.
        mDefaultTrackSelector.setParameters(
                mDefaultTrackSelector.buildUponParameters().clearSelectionOverrides());
        mSelectedAudioTrackIndex = TRACK_INDEX_UNSET;
        mSelectedVideoTrackIndex = TRACK_INDEX_UNSET;
        mSelectedMetadataTrackIndex = TRACK_INDEX_UNSET;
        mPlayerTextTrackIndex = TRACK_INDEX_UNSET;
        mSelectedTextTrackIndex = TRACK_INDEX_UNSET;
        mAudioTrackInfos.clear();
        mVideoTrackInfos.clear();
        mMetadataTrackInfos.clear();
        mInternalTextTrackInfos.clear();
        mTextRenderer.clearSelection();
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                mDefaultTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        // Enumerate track information.
        TrackGroupArray audioTrackGroups = mappedTrackInfo.getTrackGroups(AUDIO_RENDERER_INDEX);
        for (int i = 0; i < audioTrackGroups.length; i++) {
            TrackGroup trackGroup = audioTrackGroups.get(i);
            TrackInfoImpl trackInfo = new TrackInfoImpl(
                    MEDIA_TRACK_TYPE_AUDIO, ExoPlayerUtils.getMediaFormat(trackGroup.getFormat(0)));
            mAudioTrackInfos.add(trackInfo);
        }
        TrackGroupArray videoTrackGroups = mappedTrackInfo.getTrackGroups(VIDEO_RENDERER_INDEX);
        for (int i = 0; i < videoTrackGroups.length; i++) {
            TrackGroup trackGroup = videoTrackGroups.get(i);
            TrackInfoImpl trackInfo = new TrackInfoImpl(
                    MEDIA_TRACK_TYPE_VIDEO, ExoPlayerUtils.getMediaFormat(trackGroup.getFormat(0)));
            mVideoTrackInfos.add(trackInfo);
        }
        TrackGroupArray metadataTrackGroups =
                mappedTrackInfo.getTrackGroups(METADATA_RENDERER_INDEX);
        for (int i = 0; i < metadataTrackGroups.length; i++) {
            TrackGroup trackGroup = metadataTrackGroups.get(i);
            TrackInfoImpl trackInfo = new TrackInfoImpl(
                    MEDIA_TRACK_TYPE_METADATA,
                    ExoPlayerUtils.getMediaFormat(trackGroup.getFormat(0)));
            mMetadataTrackInfos.add(trackInfo);
        }

        // Determine selected track indices for audio and video.
        TrackSelectionArray trackSelections = player.getCurrentTrackSelections();
        TrackSelection audioTrackSelection = trackSelections.get(AUDIO_RENDERER_INDEX);
        mSelectedAudioTrackIndex = audioTrackSelection == null
                ? TRACK_INDEX_UNSET : audioTrackGroups.indexOf(audioTrackSelection.getTrackGroup());
        TrackSelection videoTrackSelection = trackSelections.get(VIDEO_RENDERER_INDEX);
        mSelectedVideoTrackIndex = videoTrackSelection == null
                ? TRACK_INDEX_UNSET : videoTrackGroups.indexOf(videoTrackSelection.getTrackGroup());
        TrackSelection metadataTrackSelection = trackSelections.get(METADATA_RENDERER_INDEX);
        mSelectedMetadataTrackIndex = metadataTrackSelection == null
                ? TRACK_INDEX_UNSET : metadataTrackGroups.indexOf(
                        metadataTrackSelection.getTrackGroup());

        // The text renderer exposes information about text tracks, but we may have preliminary
        // information from the player.
        TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(TEXT_RENDERER_INDEX);
        for (int i = 0; i < textTrackGroups.length; i++) {
            TrackGroup trackGroup = textTrackGroups.get(i);
            Format format = Preconditions.checkNotNull(trackGroup.getFormat(0));
            int type = getTextTrackType(format.sampleMimeType);
            InternalTextTrackInfo internalTextTrackInfo =
                    new InternalTextTrackInfo(i, type, format, UNSET);
            mInternalTextTrackInfos.add(internalTextTrackInfo);
            mTextTrackInfos.add(internalTextTrackInfo.mTrackInfo);
        }
        TrackSelection textTrackSelection = trackSelections.get(TEXT_RENDERER_INDEX);
        mPlayerTextTrackIndex = textTrackSelection == null
                ? TRACK_INDEX_UNSET : textTrackGroups.indexOf(textTrackSelection.getTrackGroup());
    }

    public void handleTextRendererChannelAvailable(int type, int channel) {
        // We may already be advertising a track for this type. If so, associate the existing text
        // track with the channel. Otherwise create a new text track info.
        boolean populatedExistingTrack = false;
        for (int i = 0; i < mInternalTextTrackInfos.size(); i++) {
            InternalTextTrackInfo internalTextTrackInfo = mInternalTextTrackInfos.get(i);
            if (internalTextTrackInfo.mType == type && internalTextTrackInfo.mChannel == UNSET) {
                // Associate the existing text track with this channel.
                InternalTextTrackInfo replacementTextTrackInfo = new InternalTextTrackInfo(
                        internalTextTrackInfo.mPlayerTrackIndex,
                        type,
                        internalTextTrackInfo.mFormat,
                        channel);
                mInternalTextTrackInfos.set(i, replacementTextTrackInfo);
                if (mSelectedTextTrackIndex == i) {
                    mTextRenderer.select(type, channel);
                }
                populatedExistingTrack = true;
                break;
            }
        }
        if (!populatedExistingTrack) {
            InternalTextTrackInfo internalTextTrackInfo = new InternalTextTrackInfo(
                    mPlayerTextTrackIndex, type, /* format= */ null, channel);
            mInternalTextTrackInfos.add(internalTextTrackInfo);
            mTextTrackInfos.add(internalTextTrackInfo.mTrackInfo);
            mPendingMetadataUpdate = true;
        }
    }

    public boolean hasPendingMetadataUpdate() {
        boolean pendingMetadataUpdate = mPendingMetadataUpdate;
        mPendingMetadataUpdate = false;
        return pendingMetadataUpdate;
    }

    public int getSelectedTrack(int trackType) {
        // Note: This logic should be aligned with the order of track types in getTrackInfos().
        switch (trackType) {
            case MEDIA_TRACK_TYPE_VIDEO:
                return mSelectedVideoTrackIndex;
            case MEDIA_TRACK_TYPE_AUDIO:
                if (mSelectedAudioTrackIndex < 0) break;
                return mVideoTrackInfos.size() + mSelectedAudioTrackIndex;
            case MEDIA_TRACK_TYPE_METADATA:
                if (mSelectedMetadataTrackIndex < 0) break;
                return mVideoTrackInfos.size() + mAudioTrackInfos.size()
                        + mSelectedMetadataTrackIndex;
            case MEDIA_TRACK_TYPE_SUBTITLE:
                if (mSelectedTextTrackIndex < 0) break;
                return mVideoTrackInfos.size() + mAudioTrackInfos.size()
                        + mMetadataTrackInfos.size() + mSelectedTextTrackIndex;
            case MEDIA_TRACK_TYPE_UNKNOWN:
            default:
                break;
        }
        return TRACK_INDEX_UNSET;
    }

    public List<MediaPlayer2.TrackInfo> getTrackInfos() {
        // Note: This order should be aligned with getSelectedTrack() logic.
        ArrayList<MediaPlayer2.TrackInfo> trackInfos = new ArrayList<>(
                mVideoTrackInfos.size() + mAudioTrackInfos.size() + mMetadataTrackInfos.size()
                        + mInternalTextTrackInfos.size());
        trackInfos.addAll(mVideoTrackInfos);
        trackInfos.addAll(mAudioTrackInfos);
        trackInfos.addAll(mMetadataTrackInfos);
        trackInfos.addAll(mTextTrackInfos);
        // Note: the list returned by MediaPlayer2Impl is modifiable so do the same here.
        return trackInfos;
    }

    public void selectTrack(int index) {
        Preconditions.checkArgument(
                index >= mVideoTrackInfos.size(), "Video track selection is not supported");
        index -= mVideoTrackInfos.size();
        if (index < mAudioTrackInfos.size()) {
            mSelectedAudioTrackIndex = index;
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                    Preconditions.checkNotNull(mDefaultTrackSelector.getCurrentMappedTrackInfo());
            TrackGroupArray audioTrackGroups = mappedTrackInfo.getTrackGroups(AUDIO_RENDERER_INDEX);
            TrackGroup selectedTrackGroup = audioTrackGroups.get(index);
            // Selected all adaptive tracks.
            int[] trackIndices = new int[selectedTrackGroup.length];
            for (int i = 0; i < trackIndices.length; i++) {
                trackIndices[i] = i;
            }
            DefaultTrackSelector.SelectionOverride selectionOverride =
                    new DefaultTrackSelector.SelectionOverride(index, trackIndices);
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setSelectionOverride(AUDIO_RENDERER_INDEX, audioTrackGroups, selectionOverride)
                    .build());
            return;
        }
        index -= mAudioTrackInfos.size();
        if (index < mMetadataTrackInfos.size()) {
            mSelectedMetadataTrackIndex = index;
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                    Preconditions.checkNotNull(mDefaultTrackSelector.getCurrentMappedTrackInfo());
            TrackGroupArray metadataTrackGroups =
                    mappedTrackInfo.getTrackGroups(METADATA_RENDERER_INDEX);
            DefaultTrackSelector.SelectionOverride selectionOverride =
                    new DefaultTrackSelector.SelectionOverride(index, /* tracks= */ 0);
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setRendererDisabled(METADATA_RENDERER_INDEX, /* disabled= */ false)
                    .setSelectionOverride(
                            METADATA_RENDERER_INDEX, metadataTrackGroups, selectionOverride)
                    .build());
            return;
        }
        index -= mMetadataTrackInfos.size();
        Preconditions.checkArgument(index < mInternalTextTrackInfos.size());
        InternalTextTrackInfo internalTextTrackInfo = mInternalTextTrackInfos.get(index);
        if (mPlayerTextTrackIndex != internalTextTrackInfo.mPlayerTrackIndex) {
            // We need to do a player-level track selection.
            mTextRenderer.clearSelection();
            mPlayerTextTrackIndex = internalTextTrackInfo.mPlayerTrackIndex;
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                    Preconditions.checkNotNull(mDefaultTrackSelector.getCurrentMappedTrackInfo());
            TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(TEXT_RENDERER_INDEX);
            DefaultTrackSelector.SelectionOverride selectionOverride =
                    new DefaultTrackSelector.SelectionOverride(mPlayerTextTrackIndex, 0);
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setSelectionOverride(TEXT_RENDERER_INDEX, textTrackGroups, selectionOverride)
                    .build());
        }
        if (internalTextTrackInfo.mChannel != UNSET) {
            mTextRenderer.select(internalTextTrackInfo.mType, internalTextTrackInfo.mChannel);
        }
        mSelectedTextTrackIndex = index;
    }

    public void deselectTrack(int index) {
        Preconditions.checkArgument(
                index >= mVideoTrackInfos.size(), "Video track deselection is not supported");
        index -= mVideoTrackInfos.size();
        Preconditions.checkArgument(
                index >= mAudioTrackInfos.size(), "Audio track deselection is not supported");
        index -= mAudioTrackInfos.size();
        if (index < mMetadataTrackInfos.size()) {
            mSelectedMetadataTrackIndex = TRACK_INDEX_UNSET;
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setRendererDisabled(METADATA_RENDERER_INDEX, /* disabled= */ true));
            return;
        }
        index -= mMetadataTrackInfos.size();
        Preconditions.checkArgument(index == mSelectedTextTrackIndex);
        mTextRenderer.clearSelection();
        mSelectedTextTrackIndex = TRACK_INDEX_UNSET;
    }

    private static int getTextTrackType(String sampleMimeType) {
        switch (sampleMimeType) {
            case MimeTypes.APPLICATION_CEA608:
                return TRACK_TYPE_CEA608;
            case MimeTypes.APPLICATION_CEA708:
                return TRACK_TYPE_CEA708;
            case MimeTypes.TEXT_VTT:
                return TRACK_TYPE_WEBVTT;
            default:
                throw new IllegalArgumentException("Unexpected text MIME type " + sampleMimeType);
        }
    }

    public static final class InternalTextTrackInfo {

        public static final String MIMETYPE_TEXT_CEA_608 = "text/cea-608";
        public static final String MIMETYPE_TEXT_CEA_708 = "text/cea-708";

        public static final int UNSET = -1;

        public final int mPlayerTrackIndex;
        public final TrackInfoImpl mTrackInfo;
        public final int mType;
        public final int mChannel;
        @Nullable public final Format mFormat;

        InternalTextTrackInfo(
                int playerTrackIndex, int type, @Nullable Format format, int channel) {
            mPlayerTrackIndex = playerTrackIndex;
            @C.SelectionFlags int selectionFlags;
            if (type == TRACK_TYPE_CEA608 && channel == 0) {
                selectionFlags = C.SELECTION_FLAG_AUTOSELECT | C.SELECTION_FLAG_DEFAULT;
            } else if (type == TRACK_TYPE_CEA708 && channel == 1) {
                selectionFlags = C.SELECTION_FLAG_DEFAULT;
            } else {
                selectionFlags = format == null ? 0 : format.selectionFlags;
            }
            String language = format == null ? C.LANGUAGE_UNDETERMINED : format.language;
            mTrackInfo = getTrackInfo(type, language, selectionFlags);
            mType = type;
            mChannel = channel;
            mFormat = format;
        }

        static TrackInfoImpl getTrackInfo(
                int type, String language, @C.SelectionFlags int selectionFlags) {
            MediaFormat mediaFormat = new MediaFormat();
            if (type == TRACK_TYPE_CEA608) {
                mediaFormat.setString(MediaFormat.KEY_MIME, MIMETYPE_TEXT_CEA_608);
            } else if (type == TRACK_TYPE_CEA708) {
                mediaFormat.setString(MediaFormat.KEY_MIME, MIMETYPE_TEXT_CEA_708);
            } else if (type == TRACK_TYPE_WEBVTT) {
                mediaFormat.setString(MediaFormat.KEY_MIME, MimeTypes.TEXT_VTT);
            } else {
                // Unexpected.
                throw new IllegalStateException();
            }
            mediaFormat.setString(MediaFormat.KEY_LANGUAGE, language);
            mediaFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE,
                    (selectionFlags & C.SELECTION_FLAG_FORCED) != 0 ? 1 : 0);
            mediaFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT,
                    (selectionFlags & C.SELECTION_FLAG_AUTOSELECT) != 0 ? 1 : 0);
            mediaFormat.setInteger(MediaFormat.KEY_IS_DEFAULT,
                    (selectionFlags & C.SELECTION_FLAG_DEFAULT) != 0 ? 1 : 0);
            // Hide WebVTT tracks, like the NuPlayer-based implementation
            // (see [internal: b/120081663]).
            int trackInfoType =
                    type == TRACK_TYPE_WEBVTT ? MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN
                            : MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;
            return new TrackInfoImpl(trackInfoType, mediaFormat);
        }

    }

}
