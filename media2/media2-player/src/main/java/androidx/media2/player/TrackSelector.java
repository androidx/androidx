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

package androidx.media2.player;

import static androidx.media2.common.SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO;
import static androidx.media2.common.SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_METADATA;
import static androidx.media2.common.SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;
import static androidx.media2.common.SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN;
import static androidx.media2.common.SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO;
import static androidx.media2.player.RenderersFactory.AUDIO_RENDERER_INDEX;
import static androidx.media2.player.RenderersFactory.METADATA_RENDERER_INDEX;
import static androidx.media2.player.RenderersFactory.TEXT_RENDERER_INDEX;
import static androidx.media2.player.RenderersFactory.VIDEO_RENDERER_INDEX;
import static androidx.media2.player.TextRenderer.TRACK_TYPE_CEA608;
import static androidx.media2.player.TextRenderer.TRACK_TYPE_CEA708;
import static androidx.media2.player.TextRenderer.TRACK_TYPE_WEBVTT;
import static androidx.media2.player.TrackSelector.InternalTextTrackInfo.UNSET;

import android.annotation.SuppressLint;
import android.media.MediaFormat;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.DefaultTrackSelector;
import androidx.media2.exoplayer.external.trackselection.MappingTrackSelector;
import androidx.media2.exoplayer.external.trackselection.TrackSelection;
import androidx.media2.exoplayer.external.trackselection.TrackSelectionArray;
import androidx.media2.exoplayer.external.util.MimeTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages track selection for {@link ExoPlayerWrapper}.
 */
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class TrackSelector {

    private static final int TRACK_INDEX_UNSET = -1;

    private int mNextTrackId;
    private MediaItem mCurrentMediaItem;
    private final TextRenderer mTextRenderer;
    private final DefaultTrackSelector mDefaultTrackSelector;
    private final SparseArray<InternalTrackInfo> mAudioTracks;
    private final SparseArray<InternalTrackInfo> mVideoTracks;
    private final SparseArray<InternalTrackInfo> mMetadataTracks;
    private final SparseArray<InternalTextTrackInfo> mTextTracks;

    private boolean mPendingTracksUpdate;
    private InternalTrackInfo mSelectedAudioTrack;
    private InternalTrackInfo mSelectedVideoTrack;
    private InternalTrackInfo mSelectedMetadataTrack;
    private InternalTextTrackInfo mSelectedTextTrack;
    private int mPlayerTextTrackIndex;

    TrackSelector(TextRenderer textRenderer) {
        mTextRenderer = textRenderer;
        mDefaultTrackSelector = new DefaultTrackSelector();
        mAudioTracks = new SparseArray<>();
        mVideoTracks = new SparseArray<>();
        mMetadataTracks = new SparseArray<>();
        mTextTracks = new SparseArray<>();
        mSelectedAudioTrack = null;
        mSelectedVideoTrack = null;
        mSelectedMetadataTrack = null;
        mSelectedTextTrack = null;
        mPlayerTextTrackIndex = TRACK_INDEX_UNSET;
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

    public void handlePlayerTracksChanged(MediaItem item, TrackSelectionArray trackSelections) {
        final boolean itemChanged = mCurrentMediaItem != item;
        mCurrentMediaItem = item;

        mPendingTracksUpdate = true;

        // Clear all selection state.
        mDefaultTrackSelector.setParameters(
                mDefaultTrackSelector.buildUponParameters().clearSelectionOverrides());
        mSelectedAudioTrack = null;
        mSelectedVideoTrack = null;
        mSelectedMetadataTrack = null;
        mSelectedTextTrack = null;
        mPlayerTextTrackIndex = TRACK_INDEX_UNSET;
        mTextRenderer.clearSelection();

        if (itemChanged) {
            mAudioTracks.clear();
            mVideoTracks.clear();
            mMetadataTracks.clear();
            mTextTracks.clear();
        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                mDefaultTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        // Get track selections to determine selected track.
        TrackSelection audioTrackSelection = trackSelections.get(AUDIO_RENDERER_INDEX);
        TrackGroup selectedAudioTrackGroup = audioTrackSelection == null ? null
                : audioTrackSelection.getTrackGroup();
        TrackSelection videoTrackSelection = trackSelections.get(VIDEO_RENDERER_INDEX);
        TrackGroup selectedVideoTrackGroup = videoTrackSelection == null ? null
                : videoTrackSelection.getTrackGroup();
        TrackSelection metadataTrackSelection = trackSelections.get(METADATA_RENDERER_INDEX);
        TrackGroup selectedMetadataTrackGroup = metadataTrackSelection == null ? null
                : metadataTrackSelection.getTrackGroup();
        TrackSelection textTrackSelection = trackSelections.get(TEXT_RENDERER_INDEX);
        TrackGroup selectedTextTrackGroup = textTrackSelection == null ? null
                : textTrackSelection.getTrackGroup();

        // Enumerate track information.
        TrackGroupArray audioTrackGroups = mappedTrackInfo.getTrackGroups(AUDIO_RENDERER_INDEX);
        for (int i = mAudioTracks.size(); i < audioTrackGroups.length; i++) {
            TrackGroup trackGroup = audioTrackGroups.get(i);
            InternalTrackInfo track = new InternalTrackInfo(
                    i,
                    MEDIA_TRACK_TYPE_AUDIO,
                    ExoPlayerUtils.getMediaFormat(trackGroup.getFormat(0)),
                    mNextTrackId++);
            mAudioTracks.put(track.mExternalTrackInfo.getId(), track);
            if (trackGroup.equals(selectedAudioTrackGroup)) {
                mSelectedAudioTrack = track;
            }
        }
        TrackGroupArray videoTrackGroups = mappedTrackInfo.getTrackGroups(VIDEO_RENDERER_INDEX);
        for (int i = mVideoTracks.size(); i < videoTrackGroups.length; i++) {
            TrackGroup trackGroup = videoTrackGroups.get(i);
            InternalTrackInfo track = new InternalTrackInfo(
                    i,
                    MEDIA_TRACK_TYPE_VIDEO,
                    ExoPlayerUtils.getMediaFormat(trackGroup.getFormat(0)),
                    mNextTrackId++);
            mVideoTracks.put(track.mExternalTrackInfo.getId(), track);
            if (trackGroup.equals(selectedVideoTrackGroup)) {
                mSelectedVideoTrack = track;
            }
        }
        TrackGroupArray metadataTrackGroups =
                mappedTrackInfo.getTrackGroups(METADATA_RENDERER_INDEX);
        for (int i = mMetadataTracks.size(); i < metadataTrackGroups.length; i++) {
            TrackGroup trackGroup = metadataTrackGroups.get(i);
            InternalTrackInfo track = new InternalTrackInfo(
                    i,
                    MEDIA_TRACK_TYPE_METADATA,
                    ExoPlayerUtils.getMediaFormat(trackGroup.getFormat(0)),
                    mNextTrackId++);
            mMetadataTracks.put(track.mExternalTrackInfo.getId(), track);
            if (trackGroup.equals(selectedMetadataTrackGroup)) {
                mSelectedMetadataTrack = track;
            }
        }

        // The text renderer exposes information about text tracks, but we may have preliminary
        // information from the player.
        TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(TEXT_RENDERER_INDEX);
        for (int i = mTextTracks.size(); i < textTrackGroups.length; i++) {
            TrackGroup trackGroup = textTrackGroups.get(i);
            Format format = Preconditions.checkNotNull(trackGroup.getFormat(0));
            int type = getTextTrackType(format.sampleMimeType);
            InternalTextTrackInfo textTrack = new InternalTextTrackInfo(
                    i, type, format, UNSET, mNextTrackId++);
            mTextTracks.put(textTrack.mExternalTrackInfo.getId(), textTrack);
            if (trackGroup.equals(selectedTextTrackGroup)) {
                mPlayerTextTrackIndex = i;
            }
        }
    }

    public void handleTextRendererChannelAvailable(int type, int channel) {
        // We may already be advertising a track for this type. If so, associate the existing text
        // track with the channel. Otherwise create a new text track info.
        boolean populatedExistingTrack = false;
        for (int i = 0; i < mTextTracks.size(); i++) {
            InternalTextTrackInfo textTrack = mTextTracks.valueAt(i);
            if (textTrack.mType == type && textTrack.mChannel == UNSET) {
                int trackId = textTrack.mExternalTrackInfo.getId();
                // Associate the existing text track with this channel.
                InternalTextTrackInfo replacementTextTrack = new InternalTextTrackInfo(
                        textTrack.mPlayerTrackIndex,
                        type,
                        textTrack.mFormat,
                        channel,
                        trackId);
                mTextTracks.put(trackId, replacementTextTrack);
                if (mSelectedTextTrack != null && mSelectedTextTrack.mPlayerTrackIndex == i) {
                    mTextRenderer.select(type, channel);
                }
                populatedExistingTrack = true;
                break;
            }
        }
        if (!populatedExistingTrack) {
            InternalTextTrackInfo textTrack = new InternalTextTrackInfo(
                    mPlayerTextTrackIndex, type, /* format= */ null, channel, mNextTrackId++);
            mTextTracks.put(textTrack.mExternalTrackInfo.getId(), textTrack);
            mPendingTracksUpdate = true;
        }
    }

    public boolean hasPendingTracksUpdate() {
        boolean pendingTracksUpdate = mPendingTracksUpdate;
        mPendingTracksUpdate = false;
        return pendingTracksUpdate;
    }

    public TrackInfo getSelectedTrack(int trackType) {
        switch (trackType) {
            case MEDIA_TRACK_TYPE_AUDIO:
                return mSelectedAudioTrack == null ? null
                        : mSelectedAudioTrack.mExternalTrackInfo;
            case MEDIA_TRACK_TYPE_VIDEO:
                return mSelectedVideoTrack == null ? null
                        : mSelectedVideoTrack.mExternalTrackInfo;
            case MEDIA_TRACK_TYPE_METADATA:
                return mSelectedMetadataTrack == null ? null
                        : mSelectedMetadataTrack.mExternalTrackInfo;
            case MEDIA_TRACK_TYPE_SUBTITLE:
                return mSelectedTextTrack == null ? null
                        : mSelectedTextTrack.mExternalTrackInfo;
            case MEDIA_TRACK_TYPE_UNKNOWN:
            default:
                return null;
        }
    }

    public List<TrackInfo> getTracks() {
        ArrayList<TrackInfo> externalTracks = new ArrayList<>();
        for (SparseArray<? extends InternalTrackInfo> tracks : Arrays.asList(
                mAudioTracks, mVideoTracks, mMetadataTracks, mTextTracks)) {
            for (int i = 0; i < tracks.size(); i++) {
                externalTracks.add(tracks.valueAt(i).mExternalTrackInfo);
            }
        }
        return externalTracks;
    }

    public void selectTrack(int trackId) {
        InternalTrackInfo videoTrack = mVideoTracks.get(trackId);
        Preconditions.checkArgument(videoTrack == null, "Video track selection is not supported");

        InternalTrackInfo audioTrack = mAudioTracks.get(trackId);
        if (audioTrack != null) {
            mSelectedAudioTrack = audioTrack;
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                    Preconditions.checkNotNull(mDefaultTrackSelector.getCurrentMappedTrackInfo());
            TrackGroupArray audioTrackGroups = mappedTrackInfo.getTrackGroups(AUDIO_RENDERER_INDEX);
            TrackGroup selectedTrackGroup = audioTrackGroups.get(audioTrack.mPlayerTrackIndex);
            // Selected all adaptive tracks.
            int[] trackIndices = new int[selectedTrackGroup.length];
            for (int i = 0; i < trackIndices.length; i++) {
                trackIndices[i] = i;
            }
            DefaultTrackSelector.SelectionOverride selectionOverride =
                    new DefaultTrackSelector.SelectionOverride(audioTrack.mPlayerTrackIndex,
                            trackIndices);
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setSelectionOverride(AUDIO_RENDERER_INDEX, audioTrackGroups, selectionOverride)
                    .build());
            return;
        }

        InternalTrackInfo metadataTrack = mMetadataTracks.get(trackId);
        if (metadataTrack != null) {
            mSelectedMetadataTrack = metadataTrack;
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                    Preconditions.checkNotNull(mDefaultTrackSelector.getCurrentMappedTrackInfo());
            TrackGroupArray metadataTrackGroups =
                    mappedTrackInfo.getTrackGroups(METADATA_RENDERER_INDEX);
            DefaultTrackSelector.SelectionOverride selectionOverride =
                    new DefaultTrackSelector.SelectionOverride(metadataTrack.mPlayerTrackIndex,
                            /* tracks...= */ 0);
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setRendererDisabled(METADATA_RENDERER_INDEX, /* disabled= */ false)
                    .setSelectionOverride(
                            METADATA_RENDERER_INDEX, metadataTrackGroups, selectionOverride)
                    .build());
            return;
        }

        InternalTextTrackInfo textTrack = mTextTracks.get(trackId);
        Preconditions.checkArgument(textTrack != null);
        if (mPlayerTextTrackIndex != textTrack.mPlayerTrackIndex) {
            // We need to do a player-level track selection.
            mTextRenderer.clearSelection();
            mPlayerTextTrackIndex = textTrack.mPlayerTrackIndex;
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                    Preconditions.checkNotNull(mDefaultTrackSelector.getCurrentMappedTrackInfo());
            TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(TEXT_RENDERER_INDEX);
            DefaultTrackSelector.SelectionOverride selectionOverride =
                    new DefaultTrackSelector.SelectionOverride(mPlayerTextTrackIndex, 0);
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setSelectionOverride(TEXT_RENDERER_INDEX, textTrackGroups, selectionOverride)
                    .build());
        }
        if (textTrack.mChannel != UNSET) {
            mTextRenderer.select(textTrack.mType, textTrack.mChannel);
        }
        mSelectedTextTrack = textTrack;
    }

    public void deselectTrack(int trackId) {
        InternalTrackInfo videoTrack = mVideoTracks.get(trackId);
        Preconditions.checkArgument(videoTrack == null, "Video track deselection is not supported");
        InternalTrackInfo audioTrack = mAudioTracks.get(trackId);
        Preconditions.checkArgument(audioTrack == null, "Audio track deselection is not supported");

        InternalTrackInfo metadataTrack = mMetadataTracks.get(trackId);
        if (metadataTrack != null) {
            mSelectedMetadataTrack = null;
            mDefaultTrackSelector.setParameters(mDefaultTrackSelector.buildUponParameters()
                    .setRendererDisabled(METADATA_RENDERER_INDEX, /* disabled= */ true));
            return;
        }

        Preconditions.checkArgument(mSelectedTextTrack != null
                && mSelectedTextTrack.mExternalTrackInfo.getId() == trackId);
        mTextRenderer.clearSelection();
        mSelectedTextTrack = null;
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

    static class InternalTrackInfo {
        final int mPlayerTrackIndex;
        final TrackInfo mExternalTrackInfo;

        InternalTrackInfo(int playerTrackIndex, int trackInfoType, @Nullable MediaFormat format,
                int trackId) {
            mPlayerTrackIndex = playerTrackIndex;
            mExternalTrackInfo = new TrackInfo(trackId, trackInfoType, format,
                    trackInfoType != MEDIA_TRACK_TYPE_VIDEO);
        }
    }

    static final class InternalTextTrackInfo extends InternalTrackInfo {

        static final String MIMETYPE_TEXT_CEA_608 = "text/cea-608";
        static final String MIMETYPE_TEXT_CEA_708 = "text/cea-708";

        static final int UNSET = -1;

        final int mType;
        final int mChannel;
        @Nullable final Format mFormat;

        InternalTextTrackInfo(int playerTrackIndex, @TextRenderer.TextTrackType int type,
                @Nullable Format format, int channel, int trackId) {
            super(playerTrackIndex, getTrackInfoType(type), getMediaFormat(type, format, channel),
                    trackId);

            mType = type;
            mChannel = channel;
            mFormat = format;
        }

        private static int getTrackInfoType(@TextRenderer.TextTrackType int type) {
            // Hide WebVTT tracks, like the NuPlayer-based implementation
            // (see [internal: b/120081663]).
            return type == TRACK_TYPE_WEBVTT ? TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN
                    : TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;
        }

        private static MediaFormat getMediaFormat(@TextRenderer.TextTrackType int type,
                @Nullable Format format, int channel) {
            @C.SelectionFlags int selectionFlags;
            if (type == TRACK_TYPE_CEA608 && channel == 0) {
                selectionFlags = C.SELECTION_FLAG_AUTOSELECT | C.SELECTION_FLAG_DEFAULT;
            } else if (type == TRACK_TYPE_CEA708 && channel == 1) {
                selectionFlags = C.SELECTION_FLAG_DEFAULT;
            } else {
                selectionFlags = format == null ? 0 : format.selectionFlags;
            }
            String language = format == null ? C.LANGUAGE_UNDETERMINED : format.language;
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
            return mediaFormat;
        }

    }

}
