/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.accessibilityservice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.view.View;

/**
 * Helper for accessing features in {@link android.accessibilityservice.AccessibilityService}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class AccessibilityServiceInfoCompat {

    static interface AccessibilityServiceInfoVersionImpl {
        public String getId(AccessibilityServiceInfo info);
        public ResolveInfo getResolveInfo(AccessibilityServiceInfo info);
        public boolean getCanRetrieveWindowContent(AccessibilityServiceInfo info);
        public String getDescription(AccessibilityServiceInfo info);
        public String getSettingsActivityName(AccessibilityServiceInfo info);
        public int getCapabilities(AccessibilityServiceInfo info);
    }

    static class AccessibilityServiceInfoStubImpl implements AccessibilityServiceInfoVersionImpl {

        public boolean getCanRetrieveWindowContent(AccessibilityServiceInfo info) {
            return false;
        }

        public String getDescription(AccessibilityServiceInfo info) {
            return null;
        }

        public String getId(AccessibilityServiceInfo info) {
            return null;
        }

        public ResolveInfo getResolveInfo(AccessibilityServiceInfo info) {
            return null;
        }

        public String getSettingsActivityName(AccessibilityServiceInfo info) {
            return null;
        }

        public int getCapabilities(AccessibilityServiceInfo info) {
            return 0;
        }
    }

    static class AccessibilityServiceInfoIcsImpl extends AccessibilityServiceInfoStubImpl {

        @Override
        public boolean getCanRetrieveWindowContent(AccessibilityServiceInfo info) {
            return AccessibilityServiceInfoCompatIcs.getCanRetrieveWindowContent(info);
        }

        @Override
        public String getDescription(AccessibilityServiceInfo info) {
            return AccessibilityServiceInfoCompatIcs.getDescription(info);
        }

        @Override
        public String getId(AccessibilityServiceInfo info) {
            return AccessibilityServiceInfoCompatIcs.getId(info);
        }

        @Override
        public ResolveInfo getResolveInfo(AccessibilityServiceInfo info) {
            return AccessibilityServiceInfoCompatIcs.getResolveInfo(info);
        }

        @Override
        public String getSettingsActivityName(AccessibilityServiceInfo info) {
            return AccessibilityServiceInfoCompatIcs.getSettingsActivityName(info);
        }

        @Override
        public int getCapabilities(AccessibilityServiceInfo info) {
            if (getCanRetrieveWindowContent(info)) {
                return CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT;
            }
            return 0;
        }
    }

    static class AccessibilityServiceInfoJellyBeanMr2 extends AccessibilityServiceInfoIcsImpl {
        @Override
        public int getCapabilities(AccessibilityServiceInfo info) {
            return AccessibilityServiceInfoCompatJellyBeanMr2.getCapabilities(info);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 18) { // JellyBean MR2
            IMPL = new AccessibilityServiceInfoJellyBeanMr2();
        } else if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new AccessibilityServiceInfoIcsImpl();
        } else {
            IMPL = new AccessibilityServiceInfoStubImpl();
        }
    }

    // Capabilities

    private static final AccessibilityServiceInfoVersionImpl IMPL;

    /**
     * Capability: This accessibility service can retrieve the active window content.
     */
    public static final int CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT = 0x00000001;

    /**
     * Capability: This accessibility service can request touch exploration mode in which
     * touched items are spoken aloud and the UI can be explored via gestures.
     */
    public static final int CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION = 0x00000002;

    /**
     * Capability: This accessibility service can request enhanced web accessibility
     * enhancements. For example, installing scripts to make app content more accessible.
     */
    public static final int CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY = 0x00000004;

    /**
     * Capability: This accessibility service can filter the key event stream.
     */
    public static final int CAPABILITY_CAN_FILTER_KEY_EVENTS = 0x00000008;

    // Feedback types

    /**
     * Denotes braille feedback.
     */
    public static final int FEEDBACK_BRAILLE = 0x0000020;

    /**
     * Mask for all feedback types.
     *
     * @see AccessibilityServiceInfo#FEEDBACK_SPOKEN
     * @see AccessibilityServiceInfo#FEEDBACK_HAPTIC
     * @see AccessibilityServiceInfo#FEEDBACK_AUDIBLE
     * @see AccessibilityServiceInfo#FEEDBACK_VISUAL
     * @see AccessibilityServiceInfo#FEEDBACK_GENERIC
     * @see FEEDBACK_BRAILLE
     */
    public static final int FEEDBACK_ALL_MASK = 0xFFFFFFFF;

    // Flags

    /**
     * If an {@link AccessibilityService} is the default for a given type.
     * Default service is invoked only if no package specific one exists. In case of
     * more than one package specific service only the earlier registered is notified.
     */
    public static final int DEFAULT = 0x0000001;

    /**
     * If this flag is set the system will regard views that are not important
     * for accessibility in addition to the ones that are important for accessibility.
     * That is, views that are marked as not important for accessibility via
     * {@link View#IMPORTANT_FOR_ACCESSIBILITY_NO} or
     * {@link View#IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS} and views that are
     * marked as potentially important for accessibility via
     * {@link View#IMPORTANT_FOR_ACCESSIBILITY_AUTO} for which the system has determined
     * that are not important for accessibility, are both reported while querying the
     * window content and also the accessibility service will receive accessibility events
     * from them.
     * <p>
     * <strong>Note:</strong> For accessibility services targeting API version
     * {@link Build.VERSION_CODES#JELLY_BEAN} or higher this flag has to be explicitly
     * set for the system to regard views that are not important for accessibility. For
     * accessibility services targeting API version lower than
     * {@link Build.VERSION_CODES#JELLY_BEAN} this flag is ignored and all views are
     * regarded for accessibility purposes.
     * </p>
     * <p>
     * Usually views not important for accessibility are layout managers that do not
     * react to user actions, do not draw any content, and do not have any special
     * semantics in the context of the screen content. For example, a three by three
     * grid can be implemented as three horizontal linear layouts and one vertical,
     * or three vertical linear layouts and one horizontal, or one grid layout, etc.
     * In this context the actual layout mangers used to achieve the grid configuration
     * are not important, rather it is important that there are nine evenly distributed
     * elements.
     * </p>
     */
    public static final int FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 0x0000002;

    /**
     * This flag requests that the system gets into touch exploration mode.
     * In this mode a single finger moving on the screen behaves as a mouse
     * pointer hovering over the user interface. The system will also detect
     * certain gestures performed on the touch screen and notify this service.
     * The system will enable touch exploration mode if there is at least one
     * accessibility service that has this flag set. Hence, clearing this
     * flag does not guarantee that the device will not be in touch exploration
     * mode since there may be another enabled service that requested it.
     * <p>
     * For accessibility services targeting API version higher than
     * {@link Build.VERSION_CODES#JELLY_BEAN_MR1} that want to set
     * this flag have to declare this capability in their meta-data by setting
     * the attribute canRequestTouchExplorationMode to true, otherwise this flag
     * will be ignored. For how to declare the meta-data of a service refer to
     * {@value AccessibilityService#SERVICE_META_DATA}.
     * </p>
     * <p>
     * Services targeting API version equal to or lower than
     * {@link Build.VERSION_CODES#JELLY_BEAN_MR1} will work normally, i.e.
     * the first time they are run, if this flag is specified, a dialog is
     * shown to the user to confirm enabling explore by touch.
     * </p>
     */
    public static final int FLAG_REQUEST_TOUCH_EXPLORATION_MODE = 0x0000004;

    /**
     * This flag requests from the system to enable web accessibility enhancing
     * extensions. Such extensions aim to provide improved accessibility support
     * for content presented in a {@link android.webkit.WebView}. An example of such
     * an extension is injecting JavaScript from a secure source. The system will enable
     * enhanced web accessibility if there is at least one accessibility service
     * that has this flag set. Hence, clearing this flag does not guarantee that the
     * device will not have enhanced web accessibility enabled since there may be
     * another enabled service that requested it.
     * <p>
     * Services that want to set this flag have to declare this capability
     * in their meta-data by setting the attribute canRequestEnhancedWebAccessibility
     * to true, otherwise this flag will be ignored. For how to declare the meta-data
     * of a service refer to {@value AccessibilityService#SERVICE_META_DATA}.
     * </p>
     */
    public static final int FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY = 0x00000008;

    /**
     * This flag requests that the AccessibilityNodeInfos obtained
     * by an {@link AccessibilityService} contain the id of the source view.
     * The source view id will be a fully qualified resource name of the
     * form "package:id/name", for example "foo.bar:id/my_list", and it is
     * useful for UI test automation. This flag is not set by default.
     */
    public static final int FLAG_REPORT_VIEW_IDS = 0x00000010;

    /**
     * This flag requests from the system to filter key events. If this flag
     * is set the accessibility service will receive the key events before
     * applications allowing it implement global shortcuts. Setting this flag
     * does not guarantee that this service will filter key events since only
     * one service can do so at any given time. This avoids user confusion due
     * to behavior change in case different key filtering services are enabled.
     * If there is already another key filtering service enabled, this one will
     * not receive key events.
     * <p>
     * Services that want to set this flag have to declare this capability
     * in their meta-data by setting the attribute canRequestFilterKeyEvents
     * to true, otherwise this flag will be ignored. For how to declare the meta
     * -data of a service refer to {@value AccessibilityService#SERVICE_META_DATA}.
     * </p>
     */
    public static final int FLAG_REQUEST_FILTER_KEY_EVENTS = 0x00000020;

    /*
     * Hide constructor
     */
    private AccessibilityServiceInfoCompat() {}

    /**
     * The accessibility service id.
     * <p>
     * <strong>Generated by the system.</strong>
     * </p>
     *
     * @return The id.
     */
    public static String getId(AccessibilityServiceInfo info) {
        return IMPL.getId(info);
    }

    /**
     * The service {@link ResolveInfo}.
     * <p>
     * <strong>Generated by the system.</strong>
     * </p>
     *
     * @return The info.
     */
    public static ResolveInfo getResolveInfo(AccessibilityServiceInfo info) {
        return IMPL.getResolveInfo(info);
    }

    /**
     * The settings activity name.
     * <p>
     * <strong>Statically set from {@link AccessibilityService#SERVICE_META_DATA
     * meta-data}.</strong>
     * </p>
     *
     * @return The settings activity name.
     */
    public static String getSettingsActivityName(AccessibilityServiceInfo info) {
        return IMPL.getSettingsActivityName(info);
    }

    /**
     * Whether this service can retrieve the current window's content.
     * <p>
     * <strong>Statically set from {@link AccessibilityService#SERVICE_META_DATA
     * meta-data}.</strong>
     * </p>
     *
     * @return True window content can be retrieved.
     */
    public static boolean getCanRetrieveWindowContent(AccessibilityServiceInfo info) {
        return IMPL.getCanRetrieveWindowContent(info);
    }

    /**
     * Description of the accessibility service.
     * <p>
     * <strong>Statically set from {@link AccessibilityService#SERVICE_META_DATA
     * meta-data}.</strong>
     * </p>
     *
     * @return The description.
     */
    public static String getDescription(AccessibilityServiceInfo info) {
        return IMPL.getDescription(info);
    }

    /**
     * Returns the string representation of a feedback type. For example,
     * {@link AccessibilityServiceInfo#FEEDBACK_SPOKEN} is represented by the
     * string FEEDBACK_SPOKEN.
     *
     * @param feedbackType The feedback type.
     * @return The string representation.
     */
    public static String feedbackTypeToString(int feedbackType) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        while (feedbackType > 0) {
            final int feedbackTypeFlag = 1 << Integer.numberOfTrailingZeros(feedbackType);
            feedbackType &= ~feedbackTypeFlag;
            if (builder.length() > 1) {
                builder.append(", ");
            }
            switch (feedbackTypeFlag) {
                case AccessibilityServiceInfo.FEEDBACK_AUDIBLE:
                    builder.append("FEEDBACK_AUDIBLE");
                    break;
                case AccessibilityServiceInfo.FEEDBACK_HAPTIC:
                    builder.append("FEEDBACK_HAPTIC");
                    break;
                case AccessibilityServiceInfo.FEEDBACK_GENERIC:
                    builder.append("FEEDBACK_GENERIC");
                    break;
                case AccessibilityServiceInfo.FEEDBACK_SPOKEN:
                    builder.append("FEEDBACK_SPOKEN");
                    break;
                case AccessibilityServiceInfo.FEEDBACK_VISUAL:
                    builder.append("FEEDBACK_VISUAL");
                    break;
            }
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Returns the string representation of a flag. For example,
     * {@link AccessibilityServiceInfo#DEFAULT} is represented by the
     * string DEFAULT.
     *
     * @param flag The flag.
     * @return The string representation.
     */
    public static String flagToString(int flag) {
        switch (flag) {
            case DEFAULT:
                return "DEFAULT";
            case FLAG_INCLUDE_NOT_IMPORTANT_VIEWS:
                return "FLAG_INCLUDE_NOT_IMPORTANT_VIEWS";
            case FLAG_REQUEST_TOUCH_EXPLORATION_MODE:
                return "FLAG_REQUEST_TOUCH_EXPLORATION_MODE";
            case FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY:
                return "FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY";
            case FLAG_REPORT_VIEW_IDS:
                return "FLAG_REPORT_VIEW_IDS";
            case FLAG_REQUEST_FILTER_KEY_EVENTS:
                return "FLAG_REQUEST_FILTER_KEY_EVENTS";
            default:
                return null;
        }
    }

    /**
     * Returns the bit mask of capabilities this accessibility service has such as
     * being able to retrieve the active window content, etc.
     *
     * @param info The service info whose capabilities to get.
     * @return The capability bit mask.
     *
     * @see #CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT
     * @see #CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION
     * @see #CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY
     * @see #CAPABILITY_CAN_FILTER_KEY_EVENTS
     */
    public static int getCapabilities(AccessibilityServiceInfo info) {
        return IMPL.getCapabilities(info);
    }

    /**
     * Returns the string representation of a capability. For example,
     * {@link #CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT} is represented
     * by the string CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT.
     *
     * @param capability The capability.
     * @return The string representation.
     */
    public static String capabilityToString(int capability) {
        switch (capability) {
            case CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT:
                return "CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT";
            case CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION:
                return "CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION";
            case CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY:
                return "CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY";
            case CAPABILITY_CAN_FILTER_KEY_EVENTS:
                return "CAPABILITY_CAN_FILTER_KEY_EVENTS";
            default:
                return "UNKNOWN";
        }
    }
}
