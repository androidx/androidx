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

package androidx.media2;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.net.Uri;

import androidx.media2.IMediaController;
import androidx.versionedparcelable.ParcelImpl;

/**
 * Interface from MediaController to MediaSessionService.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaSessionService {
    void connect(IMediaController caller, String callingPackage) = 0;
    // Next Id : 1
}
