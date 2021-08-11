/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.core.role;

/**
 * This class contains the name and documentation for roles that might be available in the system.
 * <p>
 * The list of available roles might change with a system app update, so apps should not make
 * assumption about the availability of roles. Instead, they should always check if the role is
 * available using {@link android.app.role.RoleManager#isRoleAvailable(String)} before trying to do
 * anything with it.
 *
 * @see android.app.role.RoleManager
 */
public final class RoleManagerCompat {

    /**
     * The name of the assistant role.
     * <p>
     * To qualify for this role, an application needs to either implement
     * {@link android.service.voice.VoiceInteractionService} or handle
     * {@link android.content.Intent#ACTION_ASSIST}. The application will be able to access call log
     * and SMS for its functionality.
     *
     * @see android.service.voice.VoiceInteractionService
     * @see android.content.Intent#ACTION_ASSIST
     */
    public static final String ROLE_ASSISTANT = "android.app.role.ASSISTANT";

    /**
     * The name of the browser role.
     * <p>
     * To qualify for this role, an application needs to handle the intent to browse the Internet:
     * <pre class="prettyprint">{@code
     * <activity>
     *     <intent-filter>
     *         <action android:name="android.intent.action.VIEW" />
     *         <category android:name="android.intent.category.BROWSABLE" />
     *         <category android:name="android.intent.category.DEFAULT" />
     *         <data android:scheme="http" />
     *     </intent-filter>
     * </activity>
     * }</pre>
     * The application will be able to handle that intent by default.
     * <p>
     * Apps that hold this role are allowed to start activities in response to notification clicks
     * or notification action clicks when targeting {@link android.os.Build.VERSION_CODES#S} to give
     * browsers time to adapt. This is temporary and browsers will be subjected to the same
     * trampoline restrictions at some point in future releases. For more details on those
     * restrictions see {@link android.app.Notification.Builder#setContentIntent(PendingIntent)} and
     * {@link android.app.Notification.Action.Builder#Builder(android.graphics.drawable.Icon,
     * java.lang.CharSequence, android.app.PendingIntent)}.
     *
     * @see android.content.Intent#CATEGORY_APP_BROWSER
     */
    public static final String ROLE_BROWSER = "android.app.role.BROWSER";

    /**
     * The name of the dialer role.
     * <p>
     * To qualify for this role, an application needs to handle the intent to dial:
     * <pre class="prettyprint">{@code
     * <activity>
     *     <intent-filter>
     *         <action android:name="android.intent.action.DIAL" />
     *         <category android:name="android.intent.category.DEFAULT"/>
     *     </intent-filter>
     *     <intent-filter>
     *         <action android:name="android.intent.action.DIAL" />
     *         <category android:name="android.intent.category.DEFAULT"/>
     *         <data android:scheme="tel" />
     *     </intent-filter>
     * </activity>
     * }</pre>
     * The application will be able to handle those intents by default, and gain access to phone,
     * contacts, SMS, microphone and camera.
     *
     * @see android.content.Intent#ACTION_DIAL
     */
    public static final String ROLE_DIALER = "android.app.role.DIALER";

    /**
     * The name of the SMS role.
     * <p>
     * To qualify for this role, an application needs to declare the following components:
     * <pre class="prettyprint">{@code
     * <activity>
     *     <intent-filter>
     *         <action android:name="android.intent.action.SENDTO" />
     *         <category android:name="android.intent.category.DEFAULT" />
     *         <data android:scheme="smsto" />
     *     </intent-filter>
     * </activity>
     * <service android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE">
     *     <intent-filter>
     *         <action android:name="android.intent.action.RESPOND_VIA_MESSAGE" />
     *         <category android:name="android.intent.category.DEFAULT" />
     *         <data android:scheme="smsto" />
     *     </intent-filter>
     * </service>
     * <receiver android:permission="android.permission.BROADCAST_SMS">
     *     <intent-filter>
     *         <action android:name="android.provider.Telephony.SMS_DELIVER" />
     *     </intent-filter>
     * </receiver>
     * <receiver android:permission="android.permission.BROADCAST_WAP_PUSH">
     *     <intent-filter>
     *         <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER" />
     *         <data android:mimeType="application/vnd.wap.mms-message" />
     *     </intent-filter>
     * </receiver>
     * }</pre>
     * The application will be able to handle the intent to send SMS by default, and gain access to
     * phone, contacts, SMS, storage, microphone and camera.
     *
     * @see android.content.Intent#CATEGORY_APP_MESSAGING
     */
    public static final String ROLE_SMS = "android.app.role.SMS";

    /**
     * The name of the emergency role.
     * <p>
     * You may not be able to request for this role on most devices as it's hidden by default and
     * only for system apps.
     * <p>
     * To qualify for this role, an application needs to handle the intent for emergency assitance:
     * <pre class="prettyprint">{@code
     * <activity>
     *     <intent-filter>
     *         <action android:name="android.telephony.action.EMERGENCY_ASSISTANCE" />
     *         <category android:name="android.intent.category.DEFAULT" />
     *     </intent-filter>
     * </activity>
     * }</pre>
     * The application will be used for emergency assistance.
     */
    public static final String ROLE_EMERGENCY = "android.app.role.EMERGENCY";

    /**
     * The name of the home role.
     * <p>
     * To qualify for this role, an application needs to handle the intent for home:
     * <pre class="prettyprint">{@code
     * <activity>
     *     <intent-filter>
     *         <action android:name="android.intent.action.MAIN" />
     *         <category android:name="android.intent.category.DEFAULT" />
     *         <category android:name="android.intent.category.HOME" />
     *     </intent-filter>
     * </activity>
     * }</pre>
     * The application will be able to handle that intent by default, and used as the default home
     * app.
     *
     * @see android.content.Intent#CATEGORY_HOME
     */
    public static final String ROLE_HOME = "android.app.role.HOME";

    /**
     * The name of the call redirection role.
     * <p>
     * To qualify for this role, an application needs to implement
     * {@link android.telecom.CallRedirectionService}. The application will be able to re-write the
     * phone number for an outgoing call to place the call through a call redirection service.
     *
     * @see android.telecom.CallRedirectionService
     */
    public static final String ROLE_CALL_REDIRECTION = "android.app.role.CALL_REDIRECTION";

    /**
     * The name of the call screening and caller id role.
     * <p>
     * To qualify for this role, an application needs to implement
     * {@link android.telecom.CallScreeningService}. The application will be able to screen calls
     * and provide call identification. The application will also be able to display over other apps
     * on Android 11 or above.
     *
     * @see android.telecom.CallScreeningService
     */
    public static final String ROLE_CALL_SCREENING = "android.app.role.CALL_SCREENING";

    /**
     * The name of the system gallery role.
     * <p>
     * You can not request for this role because it's hidden and only for system apps. It's meant to
     * be granted out-of-the-box to a gallery app that shipped with the device.
     * <p>
     * The application will gain full read and write access to all image and video files on external
     * storage, including access to location metadata.
     *
     * @see android.provider.MediaStore.Images
     * @see android.provider.MediaStore.Video
     * @see android.Manifest.permission#ACCESS_MEDIA_LOCATION
     */
    public static final String ROLE_SYSTEM_GALLERY = "android.app.role.SYSTEM_GALLERY";

    private RoleManagerCompat() {}
}
