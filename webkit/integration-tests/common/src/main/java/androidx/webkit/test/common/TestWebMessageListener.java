/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.webkit.test.common;

import android.net.Uri;
import android.webkit.WebView;

import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestWebMessageListener implements WebViewCompat.WebMessageListener {
    private final BlockingQueue<Data> mQueue = new LinkedBlockingQueue<>();

    public static class Data {
        public @NonNull WebMessageCompat mMessage;
        Uri mSourceOrigin;
        boolean mIsMainFrame;
        public @Nullable JavaScriptReplyProxy mReplyProxy;

        Data(WebMessageCompat message, Uri sourceOrigin, boolean isMainFrame,
                JavaScriptReplyProxy replyProxy) {
            mMessage = message;
            mSourceOrigin = sourceOrigin;
            mIsMainFrame = isMainFrame;
            mReplyProxy = replyProxy;
        }
    }

    @Override
    public void onPostMessage(@NonNull WebView webView, @NonNull WebMessageCompat message,
            @NonNull Uri sourceOrigin,
            boolean isMainFrame, @NonNull JavaScriptReplyProxy replyProxy) {
        mQueue.add(new Data(message, sourceOrigin, isMainFrame, replyProxy));
    }

    /**
     * Blocks and waits for onPostMessage to queue up data from JavaScript.
     */
    public @NonNull Data waitForOnPostMessage() throws Exception {
        return WebkitUtils.waitForNextQueueElement(mQueue);
    }

    /**
     * Indicates whether the queue has any elements enqueued.
     */
    public boolean hasNoMoreOnPostMessage() {
        return mQueue.isEmpty();
    }
}
