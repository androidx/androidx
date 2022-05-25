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

/**
 * Android Studio ships a
 * <a href="https://developer.android.com/studio/debug/layout-inspector">Layout Inspector</a>
 * tool that enables developers to examine their Views or Compose based UI while it's running on
 * a device. It works by using JVMTI over ADB to inject code into a debuggable application which
 * collects information about the view hierarchy and screen captures and pipes them back to Studio.
 * <p>
 * Getting this working in a library with custom views takes some work to connect all the dots
 * for the inspector:
 * <ol>
 *     <li>
 *         Views with custom attributes need to call
 *         {@link androidx.core.view.ViewCompat.saveAttributeDataForStyleable(android.view.View,
 *             android.content.Context,
 *             int[],
 *             android.util.AttributeSet,
 *             android.content.res.TypedArray,
 *             int,
 *             int)}
 *         in their constructor. This adds custom attributes to the attribute resolution stack. The
 *         inspector consumes this information to show developers where in XML an attribute value
 *         came from, which can help debug configuration-specific resources.
 *     </li>
 *     <li>
 *         Add the resource inspection annotation processor to your build and add a dependency on
 *         the annotation package. For Gradle, this is as simple as an {@code annotationProcessor}
 *         dependency. For Kotlin projects, use
 *         <a href="https://kotlinlang.org/docs/kapt.html">KAPT</a> instead.
 *     </li>
 *     <li>
 *         Annotate getters for custom attributes with @Attribute. The annotation processor uses
 *         these to generate an InspectionCompanion for each view. The inspector finds these
 *         reflectively and uses them to quickly read all of the attributes of the view.
 *     </li>
 * </ol>
 *
 * A minimal example looks something like this:
 * <pre>
 * public class CustomView extends View {
 *     public CustomView(
 *         &#64;NonNull Context context,
 *         &#64;Nullable AttributeSet attrs,
 *         int defStyleAttr,
 *         int defStyleRes
 *     ) {
 *         super(context, attrs, defStyleAttr, defStyleRes);
 *         TypedArray attributesArray = context.obtainStyledAttributes
 *             attrs, R.styleable.CustomView, defstyleAttr, defStyleRes);
 *         ViewCompat.saveAttributeDataForStyleable(
 *             this, context, R.styleable.CustomView, attrs, attributeArray,
 *             defStyleAttr, defStyleRes);
 *     }
 *
 *     &#64;Attribute("com.example:customAttribute")
 *     public int getCustomAttribute() { /* ... &#42;/ }
 * }
 * </pre>
 */
package androidx.resourceinspection;
