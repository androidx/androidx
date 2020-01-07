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

package androidx.message.browser;

import android.os.Bundle;

import androidx.message.browser.IMessageBrowser;

/**
 * Interface from MessageBrowser to MessageLibraryService.
 * <p>
 * Keep this interface oneway to avoid infinite waiting.
 * @hide
 */
oneway interface IMessageLibraryService {
    void connect(IMessageBrowser browser, int seq, in Bundle connectionRequest) = 0;
    void disconnect(IMessageBrowser browser, int seq) = 1;
    void sendCustomCommand(IMessageBrowser browser, int seq, in Bundle command, in Bundle args) = 2;
}
