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

package androidx.remotecallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies parameters of an {@link RemoteCallable} that will be pulled from
 * the caller of the callback. The key to pull the value from will be specified
 * in {@link #value()}. Any value passed to this parameter during creation of
 * the callback will be ignored.
 *
 * <pre>.
 * createRemoteCallback(context, "setSliderValue", R.id.slider_1, 0 /* ingored *\/);
 * createRemoteCallback(context, "setSliderValue", R.id.slider_2, 0 /* ingored *\/);
 * createRemoteCallback(context, "setSliderValue", R.id.slider_3, 0 /* ingored *\/);
 *
 * \@RemoteCallable
 * public MyClass setSliderValue(int slideId, @ExternalInput int newValue) {
 *   ...
 *   return this;
 * }</pre>
 *
 * @deprecated Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
@Deprecated
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface ExternalInput {
    /**
     * The key to pull the actual value of this parameter from.
     */
    String value();
}
