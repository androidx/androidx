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

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.IMediaBrowserServiceCompatCallbacks;
import android.support.v4.os.ResultReceiver;

/**
 * Media API allows clients to browse through hierarchy of a user’s media collection,
 * playback a specific media entry and interact with the now playing queue.
 * @hide
 */
oneway interface IMediaBrowserServiceCompat {
    void connect(String pkg, in Bundle rootHints, IMediaBrowserServiceCompatCallbacks callbacks);
    void disconnect(IMediaBrowserServiceCompatCallbacks callbacks);

    void addSubscription(String uri, IMediaBrowserServiceCompatCallbacks callbacks);
    void removeSubscription(String uri, IMediaBrowserServiceCompatCallbacks callbacks);
    void getMediaItem(String uri, in ResultReceiver cb);
}