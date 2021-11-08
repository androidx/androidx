/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.navigation.common.nav;

import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.notification.CarAppExtender;
import androidx.car.app.notification.CarPendingIntent;
import androidx.car.app.sample.navigation.common.R;
import androidx.car.app.sample.navigation.common.app.MainActivity;
import androidx.car.app.sample.navigation.common.car.NavigationCarAppService;
import androidx.car.app.sample.navigation.common.model.Instruction;
import androidx.car.app.sample.navigation.common.model.Script;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Foreground service to provide navigation directions. */
public class NavigationService extends Service {
    private static final String TAG = "NavigationService";

    public static final String DEEP_LINK_ACTION = "androidx.car.app.samples.navigation.car"
            + ".NavigationDeepLinkAction";
    public static final String CHANNEL_ID = "NavigationServiceChannel";

    /** The identifier for the navigation notification displayed for the foreground service. */
    private static final int NAV_NOTIFICATION_ID = 87654321;

    /** The identifier for the non-navigation notifications, such as a traffic accident warning. */
    private static final int NOTIFICATION_ID = 77654321;

    // Constants for location broadcast
    private static final String PACKAGE_NAME =
            "androidx.car.app.sample.navigation.common.nav.navigationservice";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION =
            PACKAGE_NAME + ".started_from_notification";
    public static final String CANCEL_ACTION = "CANCEL";

    private NotificationManager mNotificationManager;
    private final IBinder mBinder = new LocalBinder();

    @Nullable
    private CarContext mCarContext;

    @Nullable
    private Listener mListener;

    @Nullable
    private NavigationManager mNavigationManager;
    private boolean mIsNavigating;
    private int mStepsSent;

    private List<Destination> mDestinations = new ArrayList<>();
    private List<Step> mSteps = new ArrayList<>();

    @Nullable
    private Script mScript;

    /** A listener for the navigation state changes. */
    public interface Listener {
        /** Callback called when the navigation state changes. */
        void navigationStateChanged(
                boolean isNavigating,
                boolean isRerouting,
                boolean hasArrived,
                @Nullable List<Destination> destinations,
                @Nullable List<Step> steps,
                @Nullable TravelEstimate nextDestinationTravelEstimate,
                @Nullable Distance nextStepRemainingDistance,
                boolean shouldShowNextStep,
                boolean shouldShowLanes,
                @Nullable CarIcon junctionImage);
    }

    /**
     * Class used for the client Binder. Since this service runs in the same process as its clients,
     * we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        @NonNull
        public NavigationService getService() {
            return NavigationService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "In onCreate()");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        Log.i(TAG, "In onStartCommand()");
        if (CANCEL_ACTION.equals(intent.getAction())) {
            stopNavigation();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    /** Sets the {@link CarContext} to use while the service is connected. */
    public void setCarContext(@NonNull CarContext carContext, @NonNull Listener listener) {
        mCarContext = carContext;
        mNavigationManager = mCarContext.getCarService(NavigationManager.class);
        mNavigationManager.setNavigationManagerCallback(
                new NavigationManagerCallback() {
                    @Override
                    public void onStopNavigation() {
                        NavigationService.this.stopNavigation();
                    }

                    @Override
                    public void onAutoDriveEnabled() {
                        Log.d(TAG, "onAutoDriveEnabled called");
                        CarToast.makeText(carContext, "Auto drive enabled", CarToast.LENGTH_LONG)
                                .show();
                    }
                });
        mListener = listener;

        // Uncomment if navigating
        // mNavigationManager.navigationStarted();
    }

    /** Clears the currently used {@link CarContext}. */
    public void clearCarContext() {
        mCarContext = null;
        mNavigationManager = null;
    }

    /** Executes the given list of navigation instructions. */
    public void executeInstructions(@NonNull List<Instruction> instructions) {
        mScript =
                Script.execute(
                        instructions,
                        (instruction) -> {
                            switch (instruction.getType()) {
                                case START_NAVIGATION:
                                    startNavigation();
                                    break;
                                case END_NAVIGATION:
                                    endNavigationFromScript();
                                    break;
                                case ADD_DESTINATION_NAVIGATION:
                                    Destination destination = instruction.getDestination();
                                    mDestinations.add(destination);
                                    break;
                                case POP_DESTINATION_NAVIGATION:
                                    mDestinations.remove(0);
                                    break;
                                case ADD_STEP_NAVIGATION:
                                    Step step = instruction.getStep();
                                    mSteps.add(step);
                                    break;
                                case POP_STEP_NAVIGATION:
                                    mSteps.remove(0);
                                    break;
                                case SET_TRIP_POSITION_NAVIGATION:
                                    if (mIsNavigating) {
                                        TravelEstimate destinationTravelEstimate =
                                                instruction.getDestinationTravelEstimate();
                                        TravelEstimate stepTravelEstimate =
                                                instruction.getStepTravelEstimate();
                                        Trip.Builder tripBuilder = new Trip.Builder();
                                        tripBuilder
                                                .addStep(mSteps.get(0), stepTravelEstimate)
                                                .addDestination(
                                                        mDestinations.get(0),
                                                        destinationTravelEstimate)
                                                .setLoading(false);

                                        String road = instruction.getRoad();
                                        if (road != null) {
                                            tripBuilder.setCurrentRoad(road);
                                        }
                                        mNavigationManager.updateTrip(tripBuilder.build());

                                        if (++mStepsSent % 10 == 0) {
                                            // For demo purposes only play audio of next turn every
                                            // 10 steps.
                                            playNavigationDirection(R.raw.turn_right);
                                            mNotificationManager.notify(
                                                    NOTIFICATION_ID,
                                                    getTrafficAccidentWarningNotification());
                                        }

                                        update(
                                                /* isNavigating= */ true,
                                                /* isRerouting= */ false,
                                                /* hasArrived= */ false,
                                                mDestinations,
                                                mSteps,
                                                destinationTravelEstimate,
                                                instruction.getStepRemainingDistance(),
                                                instruction.getShouldNotify(),
                                                instruction.getNotificationTitle(),
                                                instruction.getNotificationContent(),
                                                instruction.getNotificationIcon(),
                                                instruction.getShouldShowNextStep(),
                                                instruction.getShouldShowLanes(),
                                                instruction.getJunctionImage());
                                    }
                                    break;
                                case SET_REROUTING:
                                    if (mIsNavigating) {
                                        TravelEstimate destinationTravelEstimate =
                                                instruction.getDestinationTravelEstimate();
                                        Trip.Builder tripBuilder = new Trip.Builder();
                                        tripBuilder
                                                .addDestination(
                                                        mDestinations.get(0),
                                                        destinationTravelEstimate)
                                                .setLoading(true);
                                        mNavigationManager.updateTrip(tripBuilder.build());
                                        update(
                                                /* isNavigating= */ true,
                                                /* isRerouting= */ true,
                                                /* hasArrived= */ false,
                                                null,
                                                null,
                                                null,
                                                null,
                                                instruction.getShouldNotify(),
                                                instruction.getNotificationTitle(),
                                                instruction.getNotificationContent(),
                                                instruction.getNotificationIcon(),
                                                instruction.getShouldShowNextStep(),
                                                instruction.getShouldShowLanes(),
                                                instruction.getJunctionImage());
                                    }
                                    break;
                                case SET_ARRIVED:
                                    if (mIsNavigating) {
                                        update(
                                                /* isNavigating= */ true,
                                                /* isRerouting= */ false,
                                                /* hasArrived= */ true,
                                                mDestinations,
                                                null,
                                                null,
                                                null,
                                                instruction.getShouldNotify(),
                                                instruction.getNotificationTitle(),
                                                instruction.getNotificationContent(),
                                                instruction.getNotificationIcon(),
                                                instruction.getShouldShowNextStep(),
                                                instruction.getShouldShowLanes(),
                                                instruction.getJunctionImage());
                                    }
                                    break;
                            }
                        });
    }

    void update(
            boolean isNavigating,
            boolean isRerouting,
            boolean hasArrived,
            List<Destination> destinations,
            List<Step> steps,
            TravelEstimate nextDestinationTravelEstimate,
            Distance nextStepRemainingDistance,
            boolean shouldNotify,
            @Nullable String notificationTitle,
            @Nullable String notificationContent,
            int notificationIcon,
            boolean shouldShowNextStep,
            boolean shouldShowLanes,
            @Nullable CarIcon junctionImage) {
        if (mListener != null) {
            mListener.navigationStateChanged(
                    isNavigating,
                    isRerouting,
                    hasArrived,
                    destinations,
                    steps,
                    nextDestinationTravelEstimate,
                    nextStepRemainingDistance,
                    shouldShowNextStep,
                    shouldShowLanes,
                    junctionImage);
        }

        if (mNotificationManager != null && !TextUtils.isEmpty(notificationTitle)) {
            mNotificationManager.notify(
                    NAV_NOTIFICATION_ID,
                    getNotification(
                            shouldNotify,
                            true,
                            notificationTitle,
                            notificationContent,
                            notificationIcon));
        }
    }

    public boolean getIsNavigating() {
        return mIsNavigating;
    }

    /** Starts navigation. */
    public void startNavigation() {
        Log.i(TAG, "Starting Navigation");
        startService(new Intent(getApplicationContext(), NavigationService.class));

        Log.i(TAG, "Starting foreground service");
        startForeground(
                NAV_NOTIFICATION_ID,
                getNotification(
                        true,
                        false,
                        getString(R.string.navigation_active),
                        null,
                        R.drawable.ic_launcher));

        if (mNavigationManager != null) {
            mNavigationManager.navigationStarted();
            mIsNavigating = true;
            mListener.navigationStateChanged(
                    mIsNavigating,
                    /* isRerouting= */ true,
                    /* hasArrived= */ false,
                    /* destinations= */ null,
                    /* steps= */ null,
                    /* nextDestinationTravelEstimate= */ null,
                    /* nextStepRemainingDistance= */ null,
                    /* shouldShowNextStep= */ false,
                    /* shouldShowLanes= */ false,
                    /* junctionImage= */ null);
        }
    }

    /** Stops navigation. */
    public void stopNavigation() {
        Log.i(TAG, "Stopping Navigation");
        if (mScript != null) {
            mScript.stop();
            mDestinations.clear();
            mSteps.clear();
            mScript = null;
        }

        if (mNavigationManager != null) {
            mNavigationManager.navigationEnded();
            mIsNavigating = false;
            mListener.navigationStateChanged(
                    mIsNavigating,
                    /* isRerouting= */ false,
                    /* hasArrived= */ false,
                    /* destinations= */ null,
                    /* steps= */ null,
                    /* nextDestinationTravelEstimate= */ null,
                    /* nextStepRemainingDistance= */ null,
                    /* shouldShowNextStep= */ false,
                    /* shouldShowLanes= */ false,
                    /* junctionImage= */ null);
        }
        stopForeground(true);
        stopSelf();
    }

    private void playNavigationDirection(@RawRes int resourceId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        CarContext carContext = mCarContext;
        if (carContext == null) {
            return;
        }

        MediaPlayer mediaPlayer = new MediaPlayer();

        // Use USAGE_ASSISTANCE_NAVIGATION_GUIDANCE as the usage type for any navigation related
        // audio, so that the audio will be played in the car speaker.
        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build();

        mediaPlayer.setAudioAttributes(audioAttributes);

        // Request audio focus with AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK so that it will duck ongoing
        // media.
        // Ducking will behave differently depending on what is playing, if it is music it will
        // lower
        // the volume, if it is speech, it will pause it.
        AudioFocusRequest request =
                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(audioAttributes)
                        .build();

        AudioManager audioManager = carContext.getSystemService(AudioManager.class);

        mediaPlayer.setOnCompletionListener(
                player -> {
                    try {
                        // When the audio finishes playing: stop and release the media player.
                        player.stop();
                        player.release();
                    } finally {
                        // Release the audio focus so that any previously playing audio can
                        // continue.
                        audioManager.abandonAudioFocusRequest(request);
                    }
                });

        // Requesting the audio focus.
        if (audioManager.requestAudioFocus(request) != AUDIOFOCUS_REQUEST_GRANTED) {
            // If audio focus is not granted ignore it.
            return;
        }

        try {
            // Load our raw resource file, in the case where you synthesize the audio for the given
            // direction, just use that audio file.
            AssetFileDescriptor afd = carContext.getResources().openRawResourceFd(resourceId);
            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Failure loading audio resource", e);
            // Release the audio focus so that any previously playing audio can continue.
            audioManager.abandonAudioFocusRequest(request);
        }

        // Start the audio playback.
        mediaPlayer.start();
    }

    private void endNavigationFromScript() {
        stopNavigation();
    }

    private void createNotificationChannel() {
        mNotificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel serviceChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(serviceChannel);
        }
    }

    /** Returns the {@link NotificationCompat} used as part of the foreground service. */
    private Notification getNotification(
            boolean shouldNotify,
            boolean showInCar,
            CharSequence navigatingDisplayTitle,
            CharSequence navigatingDisplayContent,
            int notificationIcon) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentIntent(createMainActivityPendingIntent())
                        .setContentTitle(navigatingDisplayTitle)
                        .setContentText(navigatingDisplayContent)
                        .setOngoing(true)
                        .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                        .setOnlyAlertOnce(!shouldNotify)

                        // Set the notification's background color on the car screen.
                        .setColor(
                                getResources().getColor(R.color.nav_notification_background_color,
                                        null))
                        .setColorized(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(
                                BitmapFactory.decodeResource(getResources(), notificationIcon))
                        .setTicker(navigatingDisplayTitle)
                        .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }
        if (showInCar) {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setComponent(new ComponentName(this, NavigationCarAppService.class))
                    .setData(NavigationCarAppService.createDeepLinkUri(Intent.ACTION_VIEW));
            builder.extend(
                    new CarAppExtender.Builder()
                            .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
                            .setContentIntent(
                                    CarPendingIntent.getCarApp(this, intent.hashCode(),
                                            intent,
                                            0))
                            .build());
        }
        return builder.build();
    }

    private Notification getTrafficAccidentWarningNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Traffic accident ahead")
                .setContentText("Drive slowly")
                .setSmallIcon(R.drawable.ic_settings)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_settings))
                .extend(
                        new CarAppExtender.Builder()
                                .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
                                .build())
                .build();
    }

    private PendingIntent createMainActivityPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }
}
