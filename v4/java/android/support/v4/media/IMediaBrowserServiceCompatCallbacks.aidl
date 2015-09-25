/*
** Copyright 2015, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.support.v4.media;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

/**
 * Media API allows clients to browse through hierarchy of a user’s media collection,
 * playback a specific media entry and interact with the now playing queue.
 * @hide
 */
oneway interface IMediaBrowserServiceCompatCallbacks {
    /**
     * Invoked when the connected has been established.
     * @param root The root media id for browsing.
     * @param session The {@link MediaSessionCompat.Token media session token} that can be used to
     *         control the playback of the media app.
     * @param extra Extras returned by the media service.
     */
    void onConnect(String root, in MediaSessionCompat.Token session, in Bundle extras);
    void onConnectFailed();
    void onLoadChildren(String mediaId, in List list);
}
