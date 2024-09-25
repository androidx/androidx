/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.annotation

/**
 * Denotes that the annotated element requires (or may require) one or more permissions.
 *
 * Example of requiring a single permission:
 * ```
 * @RequiresPermission(Manifest.permission.SET_WALLPAPER)
 * public abstract void setWallpaper(Bitmap bitmap) throws IOException;
 *
 * @RequiresPermission(ACCESS_COARSE_LOCATION)
 * public abstract Location getLastKnownLocation(String provider);
 * ```
 *
 * Example of requiring at least one permission from a set:
 * ```
 * @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
 * public abstract Location getLastKnownLocation(String provider);
 * ```
 *
 * Example of requiring multiple permissions:
 * ```
 * @RequiresPermission(allOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
 * public abstract Location getLastKnownLocation(String provider);
 * ```
 *
 * Example of requiring separate read and write permissions for a content provider:
 * ```
 * @RequiresPermission.Read(RequiresPermission(READ_HISTORY_BOOKMARKS))
 * @RequiresPermission.Write(RequiresPermission(WRITE_HISTORY_BOOKMARKS))
 * public static final Uri BOOKMARKS_URI = Uri.parse("content://browser/bookmarks");
 * ```
 *
 * When specified on a parameter, the annotation indicates that the method requires a permission
 * which depends on the value of the parameter. For example, consider
 * `android.app.Activity.startActivity(android.content.Intent)`:
 * ```
 * public void startActivity(@RequiresPermission Intent intent) { ... }
 * ```
 *
 * Notice how there are no actual permission names listed in the annotation. The actual permissions
 * required will depend on the particular intent passed in. For example, the code may look like
 * this:
 * ```
 * Intent intent = new Intent(Intent.ACTION_CALL);
 * startActivity(intent);
 * ```
 *
 * and the actual permission requirement for this particular intent is described on the Intent name
 * itself:
 * ```
 * @RequiresPermission(Manifest.permission.CALL_PHONE)
 * public static final String ACTION_CALL = "android.intent.action.CALL";
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
public annotation class RequiresPermission(
    /**
     * The name of the permission that is required, if precisely one permission is required. If more
     * than one permission is required, specify either [allOf] or [anyOf] instead.
     *
     * If specified, [anyOf] and [allOf] must both be null.
     */
    val value: String = "",
    /**
     * Specifies a list of permission names that are all required.
     *
     * If specified, [anyOf] and [value] must both be null.
     */
    val allOf: Array<String> = [],
    /**
     * Specifies a list of permission names where at least one is required
     *
     * If specified, [allOf] and [value] must both be null.
     */
    val anyOf: Array<String> = [],
    /**
     * If true, the permission may not be required in all cases (e.g. it may only be enforced on
     * certain platforms, or for certain call parameters, etc.
     */
    val conditional: Boolean = false
) {
    /**
     * Specifies that the given permission is required for read operations.
     *
     * When specified on a parameter, the annotation indicates that the method requires a permission
     * which depends on the value of the parameter (and typically the corresponding field passed in
     * will be one of a set of constants which have been annotated with a `@RequiresPermission`
     * annotation.)
     */
    @Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER
    )
    public annotation class Read(val value: RequiresPermission = RequiresPermission())

    /**
     * Specifies that the given permission is required for write operations.
     *
     * When specified on a parameter, the annotation indicates that the method requires a permission
     * which depends on the value of the parameter (and typically the corresponding field passed in
     * will be one of a set of constants which have been annotated with a `@RequiresPermission`
     * annotation.)
     */
    @Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER
    )
    public annotation class Write(val value: RequiresPermission = RequiresPermission())
}
