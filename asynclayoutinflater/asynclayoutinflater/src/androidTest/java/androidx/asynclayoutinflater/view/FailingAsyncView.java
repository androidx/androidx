/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.asynclayoutinflater.view;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.TextView;
/**
 * A view that cannot be inflated on the bg thread because it creates a {@link Handler}.
 */
public class FailingAsyncView extends TextView {
    private final Handler mHandler = new Handler(Looper.myLooper());
    public FailingAsyncView(Context context) {
        super(context);
    }
    public FailingAsyncView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FailingAsyncView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
