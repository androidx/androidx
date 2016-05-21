/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package android.support.v17.leanback.supportleanbackshowcase.app.media;

import android.content.Context;
import android.graphics.Color;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.supportleanbackshowcase.app.media.MediaPlayerGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;

public abstract class VideoMediaPlayerGlue extends MediaPlayerGlue {

    private final PlaybackControlsRow.ClosedCaptioningAction mClosedCaptioningAction;

    public VideoMediaPlayerGlue(Context context, PlaybackOverlayFragment fragment) {
        super(context, fragment);

        // Instantiate secondary actions
        mClosedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(context);
        setFadingEnabled(true);
    }

    @Override protected void addSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        secondaryActionsAdapter.add(mClosedCaptioningAction);
        secondaryActionsAdapter.add(mThumbsDownAction);
        secondaryActionsAdapter.add(mThumbsUpAction);
    }

    @Override public void onActionClicked(Action action) {
        super.onActionClicked(action);
        if (action == mClosedCaptioningAction) {
            mClosedCaptioningAction.nextIndex();
        }
    }

    public void setupControlsRowPresenter(PlaybackControlsRowPresenter presenter) {
        // TODO: hahnr@ move into resources
        presenter.setProgressColor(Color.parseColor("#EEFF41"));
        presenter.setBackgroundColor(Color.parseColor("#007236"));
    }
}
