/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes a {@link android.content.Context} that is tied to a {@link android.view.Display} and
 * can be used to obtain one via {@link android.content.Context#getDisplay}. Note: it is not
 * considered to be a UI or visual Context and <b>must not</b> be used to obtain UI-related
 * services, such as {@link android.view.WindowManager}, {@link android.view.LayoutInflater} or
 * {@link android.app.WallpaperManager} via
 * {@link android.content.Context#getSystemService(String)}. If the UI services mentioned above
 * are required, instead please use Contexts which are marked as {@link UiContext}.
 * <p>
 * {@link android.app.Activity}, Context instances created with
 * {@link android.content.Context#createWindowContext(int, Bundle)} or
 * {@link android.content.Context#createDisplayContext(Display)} can be used to get an associated
 * {@link android.view.Display} instance.
 * <p>
 * This is a marker annotation and has no specific attributes.
 *
 * @see android.content.Context#getDisplay()
 * @see android.content.Context#getSystemService(String)
 * @see android.content.Context#getSystemService(Class)
 * @see android.content.Context#createDisplayContext(Display)
 * @see android.content.Context#createWindowContext(int, Bundle)
 * @see UiContext
 */
@Retention(SOURCE)
@Target({TYPE, METHOD, PARAMETER, FIELD})
public @interface DisplayContext {
}
