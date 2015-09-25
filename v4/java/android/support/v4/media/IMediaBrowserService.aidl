// Copyright 2015 Google Inc. All Rights Reserved.

package android.support.v4.media;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.IMediaBrowserServiceCallbacks;
import android.support.v4.os.ResultReceiver;

/**
 * Media API allows clients to browse through hierarchy of a user’s media collection,
 * playback a specific media entry and interact with the now playing queue.
 * @hide
 */
oneway interface IMediaBrowserService {
    void connect(String pkg, in Bundle rootHints, IMediaBrowserServiceCallbacks callbacks);
    void disconnect(IMediaBrowserServiceCallbacks callbacks);

    void addSubscription(String uri, IMediaBrowserServiceCallbacks callbacks);
    void removeSubscription(String uri, IMediaBrowserServiceCallbacks callbacks);
    void getMediaItem(String uri, in ResultReceiver cb);
}