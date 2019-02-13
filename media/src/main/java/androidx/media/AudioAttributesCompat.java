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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseIntArray;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class to encapsulate a collection of attributes describing information about an audio stream.
 *
 * <p><code>AudioAttributesCompat</code> supersede the notion of stream types (see for instance
 * {@link AudioManager#STREAM_MUSIC} or {@link AudioManager#STREAM_ALARM}) for defining the behavior
 * of audio playback. Attributes allow an application to specify more information than is conveyed
 * in a stream type by allowing the application to define:
 *
 * <ul>
 * <li>usage: "why" you are playing a sound, what is this sound used for. This is achieved with
 * the "usage" information. Examples of usage are {@link #USAGE_MEDIA} and {@link
 * #USAGE_ALARM}. These two examples are the closest to stream types, but more detailed use
 * cases are available. Usage information is more expressive than a stream type, and allows
 * certain platforms or routing policies to use this information for more refined volume or
 * routing decisions. Usage is the most important information to supply in <code>
 * AudioAttributesCompat</code> and it is recommended to build any instance with this
 * information supplied, see {@link AudioAttributesCompat.Builder} for exceptions.
 * <li>content type: "what" you are playing. The content type expresses the general category of
 * the content. This information is optional. But in case it is known (for instance {@link
 * #CONTENT_TYPE_MOVIE} for a movie streaming service or {@link #CONTENT_TYPE_MUSIC} for a
 * music playback application) this information might be used by the audio framework to
 * selectively configure some audio post-processing blocks.
 * <li>flags: "how" is playback to be affected, see the flag definitions for the specific playback
 * behaviors they control.
 * </ul>
 *
 * <p><code>AudioAttributesCompat</code> instance is built through its builder, {@link
 * AudioAttributesCompat.Builder}. Also see {@link android.media.AudioAttributes} for the framework
 * implementation of this class.
 */
@VersionedParcelize(jetifyAs = "android.support.v4.media.AudioAttributesCompat")
public class AudioAttributesCompat implements VersionedParcelable {
    private static final String TAG = "AudioAttributesCompat";

    /**
     * Content type value to use when the content type is unknown, or other than the ones defined.
     */
    public static final int CONTENT_TYPE_UNKNOWN = AudioAttributes.CONTENT_TYPE_UNKNOWN;
    /** Content type value to use when the content type is speech. */
    public static final int CONTENT_TYPE_SPEECH = AudioAttributes.CONTENT_TYPE_SPEECH;
    /** Content type value to use when the content type is music. */
    public static final int CONTENT_TYPE_MUSIC = AudioAttributes.CONTENT_TYPE_MUSIC;
    /**
     * Content type value to use when the content type is a soundtrack, typically accompanying a
     * movie or TV program.
     */
    public static final int CONTENT_TYPE_MOVIE = AudioAttributes.CONTENT_TYPE_MOVIE;
    /**
     * Content type value to use when the content type is a sound used to accompany a user action,
     * such as a beep or sound effect expressing a key click, or event, such as the type of a sound
     * for a bonus being received in a game. These sounds are mostly synthesized or short Foley
     * sounds.
     */
    public static final int CONTENT_TYPE_SONIFICATION = AudioAttributes.CONTENT_TYPE_SONIFICATION;

    /** Usage value to use when the usage is unknown. */
    public static final int USAGE_UNKNOWN = AudioAttributes.USAGE_UNKNOWN;
    /** Usage value to use when the usage is media, such as music, or movie soundtracks. */
    public static final int USAGE_MEDIA = AudioAttributes.USAGE_MEDIA;
    /** Usage value to use when the usage is voice communications, such as telephony or VoIP. */
    public static final int USAGE_VOICE_COMMUNICATION = AudioAttributes.USAGE_VOICE_COMMUNICATION;
    /**
     * Usage value to use when the usage is in-call signalling, such as with a "busy" beep, or DTMF
     * tones.
     */
    public static final int USAGE_VOICE_COMMUNICATION_SIGNALLING =
             AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING;
    /** Usage value to use when the usage is an alarm (e.g. wake-up alarm). */
    public static final int USAGE_ALARM = AudioAttributes.USAGE_ALARM;
    /**
     * Usage value to use when the usage is notification. See other notification usages for more
     * specialized uses.
     */
    public static final int USAGE_NOTIFICATION = AudioAttributes.USAGE_NOTIFICATION;
    /** Usage value to use when the usage is telephony ringtone. */
    public static final int USAGE_NOTIFICATION_RINGTONE =
             AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
    /**
     * Usage value to use when the usage is a request to enter/end a communication, such as a VoIP
     * communication or video-conference.
     */
    public static final int USAGE_NOTIFICATION_COMMUNICATION_REQUEST =
             AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST;
    /**
     * Usage value to use when the usage is notification for an "instant" communication such as a
     * chat, or SMS.
     */
    public static final int USAGE_NOTIFICATION_COMMUNICATION_INSTANT =
             AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT;
    /**
     * Usage value to use when the usage is notification for a non-immediate type of communication
     * such as e-mail.
     */
    public static final int USAGE_NOTIFICATION_COMMUNICATION_DELAYED =
             AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED;
    /**
     * Usage value to use when the usage is to attract the user's attention, such as a reminder or
     * low battery warning.
     */
    public static final int USAGE_NOTIFICATION_EVENT = AudioAttributes.USAGE_NOTIFICATION_EVENT;
    /** Usage value to use when the usage is for accessibility, such as with a screen reader. */
    public static final int USAGE_ASSISTANCE_ACCESSIBILITY =
             AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY;
    /** Usage value to use when the usage is driving or navigation directions. */
    public static final int USAGE_ASSISTANCE_NAVIGATION_GUIDANCE =
             AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
    /** Usage value to use when the usage is sonification, such as with user interface sounds. */
    public static final int USAGE_ASSISTANCE_SONIFICATION =
             AudioAttributes.USAGE_ASSISTANCE_SONIFICATION;
    /** Usage value to use when the usage is for game audio. */
    public static final int USAGE_GAME = AudioAttributes.USAGE_GAME;

    // usage not available to clients
    static final int USAGE_VIRTUAL_SOURCE = 15; // AudioAttributes.USAGE_VIRTUAL_SOURCE;
    /**
     * Usage value to use for audio responses to user queries, audio instructions or help
     * utterances.
     */
    public static final int USAGE_ASSISTANT = AudioAttributes.USAGE_ASSISTANT;

    /**
     * IMPORTANT: when adding new usage types, add them to SDK_USAGES and update SUPPRESSIBLE_USAGES
     * if applicable.
     */

    // private API
    private static final int SUPPRESSIBLE_NOTIFICATION = 1;

    private static final int SUPPRESSIBLE_CALL = 2;
    private static final SparseIntArray SUPPRESSIBLE_USAGES;

    // used by tests
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean sForceLegacyBehavior;

    static {
        SUPPRESSIBLE_USAGES = new SparseIntArray();
        SUPPRESSIBLE_USAGES.put(USAGE_NOTIFICATION, SUPPRESSIBLE_NOTIFICATION);
        SUPPRESSIBLE_USAGES.put(USAGE_NOTIFICATION_RINGTONE, SUPPRESSIBLE_CALL);
        SUPPRESSIBLE_USAGES.put(USAGE_NOTIFICATION_COMMUNICATION_REQUEST, SUPPRESSIBLE_CALL);
        SUPPRESSIBLE_USAGES.put(
                USAGE_NOTIFICATION_COMMUNICATION_INSTANT, SUPPRESSIBLE_NOTIFICATION);
        SUPPRESSIBLE_USAGES.put(
                USAGE_NOTIFICATION_COMMUNICATION_DELAYED, SUPPRESSIBLE_NOTIFICATION);
        SUPPRESSIBLE_USAGES.put(USAGE_NOTIFICATION_EVENT, SUPPRESSIBLE_NOTIFICATION);
    }

    private static final int[] SDK_USAGES = {
            USAGE_UNKNOWN,
            USAGE_MEDIA,
            USAGE_VOICE_COMMUNICATION,
            USAGE_VOICE_COMMUNICATION_SIGNALLING,
            USAGE_ALARM,
            USAGE_NOTIFICATION,
            USAGE_NOTIFICATION_RINGTONE,
            USAGE_NOTIFICATION_COMMUNICATION_REQUEST,
            USAGE_NOTIFICATION_COMMUNICATION_INSTANT,
            USAGE_NOTIFICATION_COMMUNICATION_DELAYED,
            USAGE_NOTIFICATION_EVENT,
            USAGE_ASSISTANCE_ACCESSIBILITY,
            USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            USAGE_ASSISTANCE_SONIFICATION,
            USAGE_GAME,
            USAGE_ASSISTANT,
    };

    /** Flag defining a behavior where the audibility of the sound will be ensured by the system. */
    public static final int FLAG_AUDIBILITY_ENFORCED = 0x1 << 0;

    // flags for @hide API so we can create a proper flags mask
    static final int FLAG_SECURE = 0x1 << 1;
    static final int FLAG_SCO = 0x1 << 2;
    static final int FLAG_BEACON = 0x1 << 3;

    /** Flag requesting the use of an output stream supporting hardware A/V synchronization. */
    public static final int FLAG_HW_AV_SYNC = 0x1 << 4;

    // more @hide flags
    static final int FLAG_HW_HOTWORD = 0x1 << 5;
    static final int FLAG_BYPASS_INTERRUPTION_POLICY = 0x1 << 6;
    static final int FLAG_BYPASS_MUTE = 0x1 << 7;
    static final int FLAG_LOW_LATENCY = 0x1 << 8;
    static final int FLAG_DEEP_BUFFER = 0x1 << 9;

    static final int FLAG_ALL =
            (FLAG_AUDIBILITY_ENFORCED
                    | FLAG_SECURE
                    | FLAG_SCO
                    | FLAG_BEACON
                    | FLAG_HW_AV_SYNC
                    | FLAG_HW_HOTWORD
                    | FLAG_BYPASS_INTERRUPTION_POLICY
                    | FLAG_BYPASS_MUTE
                    | FLAG_LOW_LATENCY
                    | FLAG_DEEP_BUFFER);
    static final int FLAG_ALL_PUBLIC =
            (FLAG_AUDIBILITY_ENFORCED | FLAG_HW_AV_SYNC | FLAG_LOW_LATENCY);

    static final int INVALID_STREAM_TYPE = -1;  // AudioSystem.STREAM_DEFAULT

    /** Keys to convert to (or create from) Bundle. */
    static final String AUDIO_ATTRIBUTES_FRAMEWORKS =
            "androidx.media.audio_attrs.FRAMEWORKS";
    static final String AUDIO_ATTRIBUTES_USAGE = "androidx.media.audio_attrs.USAGE";
    static final String AUDIO_ATTRIBUTES_CONTENT_TYPE =
            "androidx.media.audio_attrs.CONTENT_TYPE";
    static final String AUDIO_ATTRIBUTES_FLAGS = "androidx.media.audio_attrs.FLAGS";
    static final String AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE =
            "androidx.media.audio_attrs.LEGACY_STREAM_TYPE";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @ParcelField(1)
    public AudioAttributesImpl mImpl;

    AudioAttributesCompat() {
    }

    AudioAttributesCompat(AudioAttributesImpl impl) {
        mImpl = impl;
    }

    /**
     * Returns the stream type matching the given attributes for volume control. Use this method to
     * derive the stream type needed to configure the volume control slider in an {@link
     * android.app.Activity} with {@link android.app.Activity#setVolumeControlStream(int)}. <br>
     * Do not use this method to set the stream type on an audio player object (e.g. {@link
     * android.media.AudioTrack}, {@link android.media.MediaPlayer}) as this is deprecated;
     * use <code>AudioAttributes</code> instead.
     *
     * @return a valid stream type for <code>Activity</code> or stream volume control that matches
     * the attributes, or {@link AudioManager#USE_DEFAULT_STREAM_TYPE} if there isn't a direct
     * match. Note that <code>USE_DEFAULT_STREAM_TYPE</code> is not a valid value for {@link
     * AudioManager#setStreamVolume(int, int, int)}.
     */
    public int getVolumeControlStream() {
        return mImpl.getVolumeControlStream();
    }

    // public API unique to AudioAttributesCompat

    /**
     * If the current SDK level is 21 or higher, return the {@link AudioAttributes} object inside
     * this {@link AudioAttributesCompat}. Otherwise <code>null</code>.
     *
     * @return the underlying {@link AudioAttributes} object or null
     */
    @Nullable
    public Object unwrap() {
        return mImpl.getAudioAttributes();
    }

    /**
     * Returns a stream type passed to {@link Builder#setLegacyStreamType(int)}, or best guessing
     * from flags and usage, or -1 if there is no converting logic in framework side (API 21+).
     *
     * @return the stream type {@see AudioManager}
     */
    public int getLegacyStreamType() {
        return mImpl.getLegacyStreamType();
    }

    /**
     * Creates an {@link AudioAttributesCompat} given an API 21 {@link AudioAttributes} object.
     *
     * @param aa an instance of {@link AudioAttributes}.
     * @return the new <code>AudioAttributesCompat</code>, or <code>null</code> on API &lt; 21
     */
    @Nullable
    public static AudioAttributesCompat wrap(@NonNull final Object aa) {
        if (sForceLegacyBehavior) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return new AudioAttributesCompat(new AudioAttributesImplApi26((AudioAttributes) aa));
        } else if (Build.VERSION.SDK_INT >= 21) {
            return new AudioAttributesCompat(new AudioAttributesImplApi21((AudioAttributes) aa));
        }
        return null;
    }

    // The rest of this file implements an approximation to AudioAttributes using old stream types

    /**
     * Returns the content type.
     *
     * @return one of the values that can be set in {@link Builder#setContentType(int)}
     */
    public int getContentType() {
        return mImpl.getContentType();
    }

    /**
     * Returns the usage.
     *
     * @return one of the values that can be set in {@link Builder#setUsage(int)}
     */
    public @AttributeUsage int getUsage() {
        return mImpl.getUsage();
    }

    /**
     * Returns the flags.
     *
     * @return a combined mask of all flags
     */
    public int getFlags() {
        return mImpl.getFlags();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @NonNull Bundle toBundle() {
        return mImpl.toBundle();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static AudioAttributesCompat fromBundle(Bundle bundle) {
        AudioAttributesImpl impl;
        if (Build.VERSION.SDK_INT >= 21) {
            impl = AudioAttributesImplApi21.fromBundle(bundle);
        } else {
            impl = AudioAttributesImplBase.fromBundle(bundle);
        }
        return impl == null ? null : new AudioAttributesCompat(impl);
    }

    /**
     * Builder class for {@link AudioAttributesCompat} objects.
     *
     * <p>example:
     *
     * <pre class="prettyprint">
     * new AudioAttributes.Builder()
     * .setUsage(AudioAttributes.USAGE_MEDIA)
     * .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
     * .build();
     * </pre>
     *
     * <p>By default all types of information (usage, content type, flags) conveyed by an <code>
     * AudioAttributesCompat</code> instance are set to "unknown". Unknown information will be
     * interpreted as a default value that is dependent on the context of use, for instance a {@link
     * android.media.MediaPlayer} will use a default usage of
     * {@link AudioAttributesCompat#USAGE_MEDIA}. See also {@link AudioAttributes.Builder}.
     */
    public static class Builder {
        final AudioAttributesImpl.Builder mBuilderImpl;
        /**
         * Constructs a new Builder with the defaults. By default, usage and content type are
         * respectively {@link AudioAttributesCompat#USAGE_UNKNOWN} and {@link
         * AudioAttributesCompat#CONTENT_TYPE_UNKNOWN}, and flags are 0. It is recommended to
         * configure the usage (with {@link #setUsage(int)}) or deriving attributes from a legacy
         * stream type (with {@link #setLegacyStreamType(int)}) before calling {@link #build()} to
         * override any default playback behavior in terms of routing and volume management.
         */
        public Builder() {
            if (sForceLegacyBehavior) {
                mBuilderImpl = new AudioAttributesImplBase.Builder();
            } else if (Build.VERSION.SDK_INT >= 26) {
                mBuilderImpl = new AudioAttributesImplApi26.Builder();
            } else if (Build.VERSION.SDK_INT >= 21) {
                mBuilderImpl = new AudioAttributesImplApi21.Builder();
            } else {
                mBuilderImpl = new AudioAttributesImplBase.Builder();
            }
        }

        /**
         * Constructs a new Builder from a given AudioAttributes
         *
         * @param aa the AudioAttributesCompat object whose data will be reused in the new Builder.
         */
        public Builder(AudioAttributesCompat aa) {
            if (sForceLegacyBehavior) {
                mBuilderImpl = new AudioAttributesImplBase.Builder(aa);
            } else if (Build.VERSION.SDK_INT >= 26) {
                mBuilderImpl = new AudioAttributesImplApi26.Builder(aa.unwrap());
            } else if (Build.VERSION.SDK_INT >= 21) {
                mBuilderImpl = new AudioAttributesImplApi21.Builder(aa.unwrap());
            } else {
                mBuilderImpl = new AudioAttributesImplBase.Builder(aa);
            }
        }

        /**
         * Combines all of the attributes that have been set and return a new {@link
         * AudioAttributesCompat} object.
         *
         * @return a new {@link AudioAttributesCompat} object
         */
        public AudioAttributesCompat build() {
            return new AudioAttributesCompat(mBuilderImpl.build());
        }

        /**
         * Sets the attribute describing what is the intended use of the the audio signal, such as
         * alarm or ringtone.
         *
         * @param usage one of {@link AudioAttributesCompat#USAGE_UNKNOWN}, {@link
         *              AudioAttributesCompat#USAGE_MEDIA}, {@link
         *              AudioAttributesCompat#USAGE_VOICE_COMMUNICATION}, {@link
         *              AudioAttributesCompat#USAGE_VOICE_COMMUNICATION_SIGNALLING}, {@link
         *              AudioAttributesCompat#USAGE_ALARM},
         *              {@link AudioAttributesCompat#USAGE_NOTIFICATION},
         *              {@link AudioAttributesCompat#USAGE_NOTIFICATION_RINGTONE}, {@link
         *              AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_REQUEST}, {@link
         *              AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_INSTANT}, {@link
         *              AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_DELAYED}, {@link
         *              AudioAttributesCompat#USAGE_NOTIFICATION_EVENT}, {@link
         *              AudioAttributesCompat#USAGE_ASSISTANT}, {@link
         *              AudioAttributesCompat#USAGE_ASSISTANCE_ACCESSIBILITY}, {@link
         *              AudioAttributesCompat#USAGE_ASSISTANCE_NAVIGATION_GUIDANCE}, {@link
         *              AudioAttributesCompat#USAGE_ASSISTANCE_SONIFICATION}, {@link
         *              AudioAttributesCompat#USAGE_GAME}.
         * @return the same Builder instance.
         */
        public Builder setUsage(@AttributeUsage int usage) {
            mBuilderImpl.setUsage(usage);
            return this;
        }

        /**
         * Sets the attribute describing the content type of the audio signal, such as speech, or
         * music.
         *
         * @param contentType the content type values, one of {@link
         *                    AudioAttributesCompat#CONTENT_TYPE_MOVIE}, {@link
         *                    AudioAttributesCompat#CONTENT_TYPE_MUSIC}, {@link
         *                    AudioAttributesCompat#CONTENT_TYPE_SONIFICATION}, {@link
         *                    AudioAttributesCompat#CONTENT_TYPE_SPEECH}, {@link
         *                    AudioAttributesCompat#CONTENT_TYPE_UNKNOWN}.
         * @return the same Builder instance.
         */
        public Builder setContentType(@AttributeContentType int contentType) {
            mBuilderImpl.setContentType(contentType);
            return this;
        }

        /**
         * Sets the combination of flags.
         *
         * <p>This is a bitwise OR with the existing flags.
         *
         * @param flags a combination of {@link AudioAttributesCompat#FLAG_AUDIBILITY_ENFORCED},
         *              {@link AudioAttributesCompat#FLAG_HW_AV_SYNC}.
         * @return the same Builder instance.
         */
        public Builder setFlags(int flags) {
            mBuilderImpl.setFlags(flags);
            return this;
        }

        /**
         * Create an {@link AudioAttributesCompat} that best approximates the specified {@link
         * AudioManager} stream type constant.
         *
         * @param streamType one of <code>AudioManager.STREAM_*</code>
         * @return this same Builder
         */
        public Builder setLegacyStreamType(int streamType) {
            mBuilderImpl.setLegacyStreamType(streamType);
            return this;
        }
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @Override
    public String toString() {
        return mImpl.toString();
    }

    static String usageToString(int usage) {
        switch (usage) {
            case USAGE_UNKNOWN:
                return "USAGE_UNKNOWN";
            case USAGE_MEDIA:
                return "USAGE_MEDIA";
            case USAGE_VOICE_COMMUNICATION:
                return "USAGE_VOICE_COMMUNICATION";
            case USAGE_VOICE_COMMUNICATION_SIGNALLING:
                return "USAGE_VOICE_COMMUNICATION_SIGNALLING";
            case USAGE_ALARM:
                return "USAGE_ALARM";
            case USAGE_NOTIFICATION:
                return "USAGE_NOTIFICATION";
            case USAGE_NOTIFICATION_RINGTONE:
                return "USAGE_NOTIFICATION_RINGTONE";
            case USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                return "USAGE_NOTIFICATION_COMMUNICATION_REQUEST";
            case USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                return "USAGE_NOTIFICATION_COMMUNICATION_INSTANT";
            case USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                return "USAGE_NOTIFICATION_COMMUNICATION_DELAYED";
            case USAGE_NOTIFICATION_EVENT:
                return "USAGE_NOTIFICATION_EVENT";
            case USAGE_ASSISTANCE_ACCESSIBILITY:
                return "USAGE_ASSISTANCE_ACCESSIBILITY";
            case USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                return "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE";
            case USAGE_ASSISTANCE_SONIFICATION:
                return "USAGE_ASSISTANCE_SONIFICATION";
            case USAGE_GAME:
                return "USAGE_GAME";
            case USAGE_ASSISTANT:
                return "USAGE_ASSISTANT";
            default:
                return "unknown usage " + usage;
        }
    }

    abstract static class AudioManagerHidden {
        public static final int STREAM_BLUETOOTH_SCO = 6;
        public static final int STREAM_SYSTEM_ENFORCED = 7;
        public static final int STREAM_TTS = 9;
        public static final int STREAM_ACCESSIBILITY = 10;

        private AudioManagerHidden() {
        }
    }

    /**
     * Prevent AudioAttributes from being used even on platforms that support it.
     *
     * @hide For testing only.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void setForceLegacyBehavior(boolean force) {
        sForceLegacyBehavior = force;
    }

    int getRawLegacyStreamType() {
        return mImpl.getRawLegacyStreamType();
    }

    static int toVolumeStreamType(
            boolean fromGetVolumeControlStream, int flags, @AttributeUsage int usage) {
        // flags to stream type mapping
        if ((flags & FLAG_AUDIBILITY_ENFORCED) == FLAG_AUDIBILITY_ENFORCED) {
            return fromGetVolumeControlStream
                    ? AudioManager.STREAM_SYSTEM
                    : AudioManagerHidden.STREAM_SYSTEM_ENFORCED;
        }
        if ((flags & FLAG_SCO) == FLAG_SCO) {
            return fromGetVolumeControlStream
                    ? AudioManager.STREAM_VOICE_CALL
                    : AudioManagerHidden.STREAM_BLUETOOTH_SCO;
        }

        // usage to stream type mapping
        switch (usage) {
            case USAGE_MEDIA:
            case USAGE_GAME:
            case USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
            case USAGE_ASSISTANT:
                return AudioManager.STREAM_MUSIC;
            case USAGE_ASSISTANCE_SONIFICATION:
                return AudioManager.STREAM_SYSTEM;
            case USAGE_VOICE_COMMUNICATION:
                return AudioManager.STREAM_VOICE_CALL;
            case USAGE_VOICE_COMMUNICATION_SIGNALLING:
                return fromGetVolumeControlStream
                        ? AudioManager.STREAM_VOICE_CALL
                        : AudioManager.STREAM_DTMF;
            case USAGE_ALARM:
                return AudioManager.STREAM_ALARM;
            case USAGE_NOTIFICATION_RINGTONE:
                return AudioManager.STREAM_RING;
            case USAGE_NOTIFICATION:
            case USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
            case USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
            case USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
            case USAGE_NOTIFICATION_EVENT:
                return AudioManager.STREAM_NOTIFICATION;
            case USAGE_ASSISTANCE_ACCESSIBILITY:
                return AudioManagerHidden.STREAM_ACCESSIBILITY;
            case USAGE_UNKNOWN:
                return AudioManager.STREAM_MUSIC;
            default:
                if (fromGetVolumeControlStream) {
                    throw new IllegalArgumentException(
                            "Unknown usage value " + usage + " in audio attributes");
                } else {
                    return AudioManager.STREAM_MUSIC;
                }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AudioAttributesCompat)) {
            return false;
        }
        final AudioAttributesCompat that = (AudioAttributesCompat) o;
        if (this.mImpl == null) {
            return that.mImpl == null;
        }
        return this.mImpl.equals(that.mImpl);
    }

    /** @hide */
    @IntDef({
            USAGE_UNKNOWN,
            USAGE_MEDIA,
            USAGE_VOICE_COMMUNICATION,
            USAGE_VOICE_COMMUNICATION_SIGNALLING,
            USAGE_ALARM,
            USAGE_NOTIFICATION,
            USAGE_NOTIFICATION_RINGTONE,
            USAGE_NOTIFICATION_COMMUNICATION_REQUEST,
            USAGE_NOTIFICATION_COMMUNICATION_INSTANT,
            USAGE_NOTIFICATION_COMMUNICATION_DELAYED,
            USAGE_NOTIFICATION_EVENT,
            USAGE_ASSISTANCE_ACCESSIBILITY,
            USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            USAGE_ASSISTANCE_SONIFICATION,
            USAGE_GAME,
            USAGE_ASSISTANT,
    })
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(RetentionPolicy.SOURCE)
    public @interface AttributeUsage {
    }

    /** @hide */
    @IntDef({
            CONTENT_TYPE_UNKNOWN,
            CONTENT_TYPE_SPEECH,
            CONTENT_TYPE_MUSIC,
            CONTENT_TYPE_MOVIE,
            CONTENT_TYPE_SONIFICATION
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @interface AttributeContentType {
    }
}
