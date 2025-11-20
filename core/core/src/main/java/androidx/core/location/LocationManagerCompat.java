/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.location;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import static androidx.core.location.LocationCompat.getElapsedRealtimeMillis;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.SuppressLint;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.collection.SimpleArrayMap;
import androidx.core.os.ExecutorCompat;
import androidx.core.util.Consumer;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Helper for accessing features in {@link LocationManager}.
 */
@SuppressWarnings({"deprecation", "unused"})
public final class LocationManagerCompat {

    private static final long GET_CURRENT_LOCATION_TIMEOUT_MS = 30 * 1000;
    private static final long MAX_CURRENT_LOCATION_AGE_MS = 10 * 1000;
    private static final long PRE_N_LOOPER_TIMEOUT_S = 5;

    private static Field sContextField;
    private static Class<?> sGnssRequestBuilderClass;
    private static Method sGnssRequestBuilderBuildMethod;
    private static Method sRegisterGnssMeasurementsCallbackMethod;

    /**
     * Returns the current enabled/disabled state of location.
     *
     * @return {@code true} if location is enabled or {@code false} if location is disabled
     */
    public static boolean isLocationEnabled(@NonNull LocationManager locationManager) {
        if (VERSION.SDK_INT >= 28) {
            return Api28Impl.isLocationEnabled(locationManager);
        }

        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Returns true if the given location provider exists on this device, irrespective of whether
     * it is currently enabled or not. If called on Android Q and below for the
     * {@link LocationManager#FUSED_PROVIDER}, this method may return incorrect results if the
     * client does not hold at least the {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * permission.
     */
    public static boolean hasProvider(@NonNull LocationManager locationManager,
            @NonNull String provider) {
        if (VERSION.SDK_INT >= 31) {
            return Api31Impl.hasProvider(locationManager, provider);
        }

        // will not work for the FUSED provider by default
        if (locationManager.getAllProviders().contains(provider)) {
            return true;
        }

        try {
            // Q and below have pointless location permission requirements when using getProvider()
            return locationManager.getProvider(provider) != null;
        } catch (SecurityException ignored) {
        }

        return false;
    }

    /**
     * Asynchronously returns a single current location fix from the given provider. This may
     * activate sensors in order to compute a new location. The given callback will be invoked once
     * and only once, either with a valid location or with a null location if the provider was
     * unable to generate a valid location.
     *
     * <p>A client may supply an optional {@link CancellationSignal}. If this is used to cancel the
     * operation, no callback should be expected after the cancellation.
     *
     * <p>This method may return locations from the very recent past (on the order of several
     * seconds), but will never return older locations (for example, several minutes old or older).
     * Clients may rely upon the guarantee that if this method returns a location, it will represent
     * the best estimation of the location of the device in the present moment.
     *
     * <p>Clients calling this method from the background may notice that the method fails to
     * determine a valid location fix more often than while in the foreground. Background
     * applications may be throttled in their location accesses to some degree.
     *
     * @deprecated Use
     * {@link #getCurrentLocation(LocationManager, String, CancellationSignal, Executor, Consumer)}
     */
    @Deprecated
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public static void getCurrentLocation(@NonNull LocationManager locationManager,
            @NonNull String provider,
            androidx.core.os.@Nullable CancellationSignal cancellationSignal,
            @NonNull Executor executor, final @NonNull Consumer<Location> consumer) {
        getCurrentLocation(locationManager, provider, cancellationSignal != null
                        ? (CancellationSignal) cancellationSignal.getCancellationSignalObject() :
                        null,
                executor, consumer);
    }

    /**
     * Asynchronously returns a single current location fix from the given provider. This may
     * activate sensors in order to compute a new location. The given callback will be invoked once
     * and only once, either with a valid location or with a null location if the provider was
     * unable to generate a valid location.
     *
     * <p>A client may supply an optional {@link CancellationSignal}. If this is used to cancel the
     * operation, no callback should be expected after the cancellation.
     *
     * <p>This method may return locations from the very recent past (on the order of several
     * seconds), but will never return older locations (for example, several minutes old or older).
     * Clients may rely upon the guarantee that if this method returns a location, it will represent
     * the best estimation of the location of the device in the present moment.
     *
     * <p>Clients calling this method from the background may notice that the method fails to
     * determine a valid location fix more often than while in the foreground. Background
     * applications may be throttled in their location accesses to some degree.
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public static void getCurrentLocation(@NonNull LocationManager locationManager,
            @NonNull String provider, @Nullable CancellationSignal cancellationSignal,
            @NonNull Executor executor, final @NonNull Consumer<Location> consumer) {
        if (VERSION.SDK_INT >= 30) {
            Api30Impl.getCurrentLocation(locationManager, provider, cancellationSignal, executor,
                    consumer);
            return;
        }

        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }

        final Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            long locationAgeMs =
                    SystemClock.elapsedRealtime() - getElapsedRealtimeMillis(location);
            if (locationAgeMs < MAX_CURRENT_LOCATION_AGE_MS) {
                executor.execute(() -> consumer.accept(location));
                return;
            }
        }

        final CancellableLocationListener listener =
                new CancellableLocationListener(locationManager, executor, consumer);
        locationManager.requestLocationUpdates(provider, 0, 0, listener,
                Looper.getMainLooper());

        if (cancellationSignal != null) {
            cancellationSignal.setOnCancelListener(listener::cancel);
        }

        listener.startTimeout(GET_CURRENT_LOCATION_TIMEOUT_MS);
    }

    @GuardedBy("sLocationListeners")
    static final WeakHashMap<LocationListenerKey, WeakReference<LocationListenerTransport>>
            sLocationListeners = new WeakHashMap<>();

    /**
     * Register for location updates from the specified provider, using a
     * {@link LocationRequestCompat}, and a callback on the specified {@link Executor}.
     *
     * <p>See
     * {@link LocationManager#requestLocationUpdates(String, LocationRequest, Executor,
     * LocationListener)} for more information.
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public static void requestLocationUpdates(@NonNull LocationManager locationManager,
            @NonNull String provider,
            @NonNull LocationRequestCompat locationRequest,
            @NonNull Executor executor,
            @NonNull LocationListenerCompat listener) {
        if (VERSION.SDK_INT >= 31) {
            Api31Impl.requestLocationUpdates(locationManager, provider,
                    locationRequest.toLocationRequest(), executor, listener);
            return;
        }

        if (VERSION.SDK_INT >= 30 && Api30Impl.tryRequestLocationUpdates(
                locationManager, provider, locationRequest, executor, listener)) {
            return;
        }

        LocationListenerTransport transport = new LocationListenerTransport(
                new LocationListenerKey(provider, listener), executor);

        if (Api19Impl.tryRequestLocationUpdates(
                locationManager, provider, locationRequest, transport)) {
            return;
        }

        synchronized (sLocationListeners) {
            locationManager.requestLocationUpdates(provider, locationRequest.getIntervalMillis(),
                    locationRequest.getMinUpdateDistanceMeters(), transport,
                    Looper.getMainLooper());
            registerLocationListenerTransport(locationManager, transport);
        }
    }

    @GuardedBy("sLocationListeners")
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    static void registerLocationListenerTransport(LocationManager locationManager,
            LocationListenerTransport transport) {
        WeakReference<LocationListenerTransport> oldRef =
                sLocationListeners.put(transport.getKey(), new WeakReference<>(transport));
        LocationListenerTransport oldTransport = oldRef != null ? oldRef.get() : null;
        if (oldTransport != null) {
            oldTransport.unregister();
            locationManager.removeUpdates(oldTransport);
        }
    }

    /**
     * Register for location updates from the specified provider, using a
     * {@link LocationRequestCompat}, and a callback on the specified {@link Looper}.
     *
     * <p>See
     * {@link LocationManager#requestLocationUpdates(String, LocationRequest, Executor,
     * LocationListener)} for more information.
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public static void requestLocationUpdates(@NonNull LocationManager locationManager,
            @NonNull String provider,
            @NonNull LocationRequestCompat locationRequest,
            @NonNull LocationListenerCompat listener,
            @NonNull Looper looper) {
        if (VERSION.SDK_INT >= 31) {
            Api31Impl.requestLocationUpdates(locationManager, provider,
                    locationRequest.toLocationRequest(),
                    ExecutorCompat.create(new Handler(looper)), listener);
            return;
        }

        if (Api19Impl.tryRequestLocationUpdates(
                    locationManager, provider, locationRequest, listener, looper)) {
            return;
        }

        locationManager.requestLocationUpdates(provider, locationRequest.getIntervalMillis(),
                locationRequest.getMinUpdateDistanceMeters(), listener, looper);
    }

    /**
     * Removes all location updates for the specified {@link LocationListener}.
     *
     * <p>See {@link LocationManager#removeUpdates(LocationListener)} for more information.
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public static void removeUpdates(@NonNull LocationManager locationManager,
            @NonNull LocationListenerCompat listener) {
        synchronized (sLocationListeners) {
            ArrayList<LocationListenerKey> cleanup = null;
            for (WeakReference<LocationListenerTransport> transportRef :
                    sLocationListeners.values()) {
                LocationListenerTransport transport = transportRef.get();
                if (transport == null) {
                    continue;
                }
                LocationListenerKey key = transport.getKey();
                if (key.mListener == listener) {
                    if (cleanup == null) {
                        cleanup = new ArrayList<>();
                    }
                    cleanup.add(key);
                    transport.unregister();
                    locationManager.removeUpdates(transport);
                }
            }
            if (cleanup != null) {
                for (LocationListenerKey key : cleanup) {
                    sLocationListeners.remove(key);
                }
            }
        }

        // a given listener could have been registered both with an executor and a looper, so we
        // need to remove all possible cases
        locationManager.removeUpdates(listener);
    }

    /**
     * Returns the model name (including vendor and hardware/software version) of the GNSS
     * hardware driver, or null if this information is not available.
     *
     * <p>No device-specific serial number or ID is returned from this API.
     */
    public static @Nullable String getGnssHardwareModelName(
            @NonNull LocationManager locationManager) {
        if (VERSION.SDK_INT >= 28) {
            return Api28Impl.getGnssHardwareModelName(locationManager);
        } else {
            return null;
        }
    }

    /**
     * Returns the model year of the GNSS hardware and software build, or 0 if the model year is
     * before 2016.
     */
    public static int getGnssYearOfHardware(@NonNull LocationManager locationManager) {
        if (VERSION.SDK_INT >= 28) {
            return Api28Impl.getGnssYearOfHardware(locationManager);
        } else {
            return 0;
        }
    }

    // allows lazy instantiation since most processes do not use GNSS APIs
    private static class GnssListenersHolder {
        @GuardedBy("sGnssStatusListeners")
        static final SimpleArrayMap<Object, Object> sGnssStatusListeners =
                new SimpleArrayMap<>();
        @GuardedBy("sGnssMeasurementListeners")
        static final SimpleArrayMap<GnssMeasurementsEvent.Callback, GnssMeasurementsEvent.Callback>
                sGnssMeasurementListeners = new SimpleArrayMap<>();
    }

    /**
     * Registers a GNSS measurement callback. See
     * {@link LocationManager#registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback, Handler)}.
     *
     * <p>The primary purpose for this compatibility method is to help avoid crashes when delivering
     * GNSS measurements to client on Android R. This bug was fixed in Android R QPR1, but since
     * it's possible not all Android R devices have upgraded to QPR1, this compatibility method is
     * provided to ensure GNSS measurements can be delivered successfully on all platforms.
     */
    @RequiresApi(VERSION_CODES.N)
    @RequiresPermission(ACCESS_FINE_LOCATION)
    public static boolean registerGnssMeasurementsCallback(@NonNull LocationManager locationManager,
            GnssMeasurementsEvent.@NonNull Callback callback, @NonNull Handler handler) {
        if (VERSION.SDK_INT > VERSION_CODES.R) {
            return Api24Impl.registerGnssMeasurementsCallback(locationManager, callback, handler);
        } else if (VERSION.SDK_INT == VERSION_CODES.R) {
            return registerGnssMeasurementsCallbackOnR(locationManager,
                    ExecutorCompat.create(handler),
                    callback);
        } else {
            synchronized (GnssListenersHolder.sGnssMeasurementListeners) {
                unregisterGnssMeasurementsCallback(locationManager, callback);
                if (Api24Impl.registerGnssMeasurementsCallback(locationManager, callback,
                        handler)) {
                    GnssListenersHolder.sGnssMeasurementListeners.put(callback, callback);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Registers a GNSS measurement callback. See
     * {@link LocationManager#registerGnssMeasurementsCallback(Executor, GnssMeasurementsEvent.Callback)}.
     *
     * <p>In addition to allowing the use of Executors on older platforms, this compatibility method
     * also helps avoid crashes when delivering GNSS measurements to clients on Android R. This
     * bug was fixed in Android R QPR1, but since it's possible not all Android R devices have
     * upgraded to QPR1, this compatibility method is provided to ensure GNSS measurements can be
     * delivered successfully on all platforms.
     */
    @RequiresApi(VERSION_CODES.N)
    @RequiresPermission(ACCESS_FINE_LOCATION)
    public static boolean registerGnssMeasurementsCallback(@NonNull LocationManager locationManager,
            @NonNull Executor executor, GnssMeasurementsEvent.@NonNull Callback callback) {
        if (VERSION.SDK_INT > VERSION_CODES.R) {
            return Api31Impl.registerGnssMeasurementsCallback(locationManager, executor, callback);
        } else if (VERSION.SDK_INT == VERSION_CODES.R) {
            return registerGnssMeasurementsCallbackOnR(locationManager,
                    executor,
                    callback);
        } else {
            synchronized (GnssListenersHolder.sGnssMeasurementListeners) {
                GnssMeasurementsTransport newTransport = new GnssMeasurementsTransport(callback,
                        executor);
                unregisterGnssMeasurementsCallback(locationManager, callback);
                if (Api24Impl.registerGnssMeasurementsCallback(locationManager, newTransport)) {
                    GnssListenersHolder.sGnssMeasurementListeners.put(callback, newTransport);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Unregisters a GNSS measurement callback. See
     * {@link LocationManager#unregisterGnssMeasurementsCallback(GnssMeasurementsEvent.Callback)}.
     */
    @RequiresApi(VERSION_CODES.N)
    public static void unregisterGnssMeasurementsCallback(@NonNull LocationManager locationManager,
            GnssMeasurementsEvent.@NonNull Callback callback) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            Api24Impl.unregisterGnssMeasurementsCallback(locationManager, callback);
        } else {
            synchronized (GnssListenersHolder.sGnssMeasurementListeners) {
                GnssMeasurementsEvent.Callback transport =
                        GnssListenersHolder.sGnssMeasurementListeners.remove(callback);
                if (transport != null) {
                    if (transport instanceof GnssMeasurementsTransport) {
                        ((GnssMeasurementsTransport) transport).unregister();
                    }
                    Api24Impl.unregisterGnssMeasurementsCallback(locationManager, transport);
                }
            }
        }
    }

    // Android R without QPR1 has a bug where the default version of this method will always
    // cause crashes. Reflect to invoke the SystemApi version instead, which avoids this.
    @RequiresApi(VERSION_CODES.R)
    private static boolean registerGnssMeasurementsCallbackOnR(
            @NonNull LocationManager locationManager, @NonNull Executor executor,
            GnssMeasurementsEvent.@NonNull Callback callback) {
        if (VERSION.SDK_INT == VERSION_CODES.R) {
            try {
                if (sGnssRequestBuilderClass == null) {
                    sGnssRequestBuilderClass = Class.forName(
                            "android.location.GnssRequest$Builder");
                }
                if (sGnssRequestBuilderBuildMethod == null) {
                    sGnssRequestBuilderBuildMethod = sGnssRequestBuilderClass.getDeclaredMethod(
                            "build");
                    sGnssRequestBuilderBuildMethod.setAccessible(true);
                }
                if (sRegisterGnssMeasurementsCallbackMethod == null) {
                    sRegisterGnssMeasurementsCallbackMethod =
                            LocationManager.class.getDeclaredMethod(
                                    "registerGnssMeasurementsCallback",
                                    Class.forName("android.location.GnssRequest"), Executor.class,
                                    GnssMeasurementsEvent.Callback.class);
                    sRegisterGnssMeasurementsCallbackMethod.setAccessible(true);
                }

                Object success = sRegisterGnssMeasurementsCallbackMethod.invoke(locationManager,
                        sGnssRequestBuilderBuildMethod.invoke(
                                sGnssRequestBuilderClass.getDeclaredConstructor().newInstance()),
                        executor, callback);
                return success != null && (boolean) success;
            } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException
                    | IllegalAccessException | InstantiationException e) {
                return false;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Registers a platform agnostic {@link GnssStatusCompat.Callback}. See
     * {@link LocationManager#addGpsStatusListener(GpsStatus.Listener)} and
     * {@link LocationManager#registerGnssStatusCallback(GnssStatus.Callback, Handler)}.
     *
     * @see #registerGnssStatusCallback(LocationManager, Executor, GnssStatusCompat.Callback)
     */
    @RequiresPermission(ACCESS_FINE_LOCATION)
    public static boolean registerGnssStatusCallback(@NonNull LocationManager locationManager,
            GnssStatusCompat.@NonNull Callback callback, @NonNull Handler handler) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            return registerGnssStatusCallback(locationManager, ExecutorCompat.create(handler),
                callback);
        } else {
            return registerGnssStatusCallback(locationManager, new InlineHandlerExecutor(handler),
                    callback);
        }
    }

    /**
     * Registers a platform agnostic {@link GnssStatusCompat.Callback}. See
     * {@link LocationManager#addGpsStatusListener(GpsStatus.Listener)} and
     * {@link LocationManager#registerGnssStatusCallback(Executor, GnssStatus.Callback)}.
     *
     * <p>Internally, this API will always utilize GnssStatus APIs and instances on Android N and
     * above, and will always utilize GpsStatus APIs and instances below Android N. Callbacks will
     * always occur on the given executor.
     *
     * <p>If invoked on Android M or below, this will result in GpsStatus registration being run on
     * either the current Looper or main Looper. If the thread this function is invoked on is
     * different from that Looper, the caller must ensure that the Looper thread cannot be blocked
     * by the thread this function is invoked on. The easiest way to avoid this is to ensure this
     * function is invoked on a Looper thread.
     *
     * @throws IllegalStateException on Android M or below, if the current Looper or main Looper
     *                               is blocked by the thread this function is invoked on
     */
    @RequiresPermission(ACCESS_FINE_LOCATION)
    public static boolean registerGnssStatusCallback(@NonNull LocationManager locationManager,
            @NonNull Executor executor, GnssStatusCompat.@NonNull Callback callback) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            return registerGnssStatusCallback(locationManager, null, executor, callback);
        } else {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                looper = Looper.getMainLooper();
            }
            return registerGnssStatusCallback(locationManager, new Handler(looper), executor,
                    callback);
        }
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    private static boolean registerGnssStatusCallback(final LocationManager locationManager,
            Handler baseHandler, Executor executor, GnssStatusCompat.Callback callback) {
        if (VERSION.SDK_INT >= 30) {
            return Api30Impl.registerGnssStatusCallback(
                    locationManager, baseHandler, executor, callback);
        } else if (VERSION.SDK_INT >= 24) {
            return Api24Impl.registerGnssStatusCallback(
                    locationManager, baseHandler, executor, callback);
        } else {
            Preconditions.checkArgument(baseHandler != null);
            synchronized (GnssListenersHolder.sGnssStatusListeners) {
                GpsStatusTransport transport =
                        (GpsStatusTransport) GnssListenersHolder.sGnssStatusListeners.get(callback);
                if (transport == null) {
                    transport = new GpsStatusTransport(locationManager, callback);
                } else {
                    transport.unregister();
                }
                transport.register(executor);

                final GpsStatusTransport myTransport = transport;
                FutureTask<Boolean> task = new FutureTask<>(
                        () -> locationManager.addGpsStatusListener(myTransport));

                if (Looper.myLooper() == baseHandler.getLooper()) {
                    task.run();
                } else if (!baseHandler.post(task)) {
                    throw new IllegalStateException(baseHandler + " is shutting down");
                }

                boolean interrupted = false;
                try {
                    long remainingNanos = SECONDS.toNanos(PRE_N_LOOPER_TIMEOUT_S);
                    long end = System.nanoTime() + remainingNanos;
                    while (true) {
                        try {
                            if (task.get(remainingNanos, NANOSECONDS)) {
                                GnssListenersHolder.sGnssStatusListeners.put(callback, myTransport);
                                return true;
                            } else {
                                return false;
                            }
                        } catch (InterruptedException e) {
                            // this is conceptually not an interruptible operation
                            interrupted = true;
                            remainingNanos = end - System.nanoTime();
                        }
                    }
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    } else if (e.getCause() instanceof Error) {
                        throw (Error) e.getCause();
                    } else {
                        throw new IllegalStateException(e);
                    }
                } catch (TimeoutException e) {
                    throw new IllegalStateException(baseHandler + " appears to be blocked, please"
                            + " run registerGnssStatusCallback() directly on a Looper thread or "
                            + "ensure the main Looper is not blocked by this thread", e);
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Unregisters a platform agnostic {@link GnssStatusCompat.Callback}. See
     * {@link LocationManager#removeGpsStatusListener(GpsStatus.Listener)}
     * and {@link LocationManager#unregisterGnssStatusCallback(GnssStatus.Callback)}.
     */
    public static void unregisterGnssStatusCallback(@NonNull LocationManager locationManager,
            GnssStatusCompat.@NonNull Callback callback) {
        if (VERSION.SDK_INT >= 24) {
            synchronized (GnssListenersHolder.sGnssStatusListeners) {
                Object transport = GnssListenersHolder.sGnssStatusListeners.remove(callback);
                if (transport != null) {
                    Api24Impl.unregisterGnssStatusCallback(locationManager, transport);
                }
            }
        } else {
            synchronized (GnssListenersHolder.sGnssStatusListeners) {
                GpsStatusTransport transport =
                        (GpsStatusTransport) GnssListenersHolder.sGnssStatusListeners.remove(
                                callback);
                if (transport != null) {
                    transport.unregister();
                    locationManager.removeGpsStatusListener(transport);
                }
            }
        }
    }

    private LocationManagerCompat() {}

    private static class LocationListenerKey {
        final String mProvider;
        final LocationListenerCompat mListener;

        LocationListenerKey(String provider,
                LocationListenerCompat listener) {
            mProvider = ObjectsCompat.requireNonNull(provider, "invalid null provider");
            mListener = ObjectsCompat.requireNonNull(listener, "invalid null listener");
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LocationListenerKey)) {
                return false;
            }

            LocationListenerKey that = (LocationListenerKey) o;
            return mProvider.equals(that.mProvider) && mListener.equals(that.mListener);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mProvider, mListener);
        }
    }

    private static class LocationListenerTransport implements LocationListener {

        volatile @Nullable LocationListenerKey mKey;
        final Executor mExecutor;

        LocationListenerTransport(@Nullable LocationListenerKey key, Executor executor) {
            mKey = key;
            mExecutor = executor;
        }

        public LocationListenerKey getKey() {
            return ObjectsCompat.requireNonNull(mKey);
        }

        public void unregister() {
            mKey = null;
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (mKey == null) {
                return;
            }

            mExecutor.execute(() -> {
                LocationListenerKey key = mKey;
                if (key == null) {
                    return;
                }
                key.mListener.onLocationChanged(location);
            });
        }

        @Override
        public void onLocationChanged(@NonNull List<Location> locations) {
            if (mKey == null) {
                return;
            }

            mExecutor.execute(() -> {
                LocationListenerKey key = mKey;
                if (key == null) {
                    return;
                }
                key.mListener.onLocationChanged(locations);
            });
        }

        @Override
        public void onFlushComplete(int requestCode) {
            if (mKey == null) {
                return;
            }

            mExecutor.execute(() -> {
                LocationListenerKey key = mKey;
                if (key == null) {
                    return;
                }
                key.mListener.onFlushComplete(requestCode);
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (mKey == null) {
                return;
            }

            mExecutor.execute(() -> {
                LocationListenerKey key = mKey;
                if (key == null) {
                    return;
                }
                key.mListener.onStatusChanged(provider, status, extras);
            });
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            if (mKey == null) {
                return;
            }

            mExecutor.execute(() -> {
                LocationListenerKey key = mKey;
                if (key == null) {
                    return;
                }
                key.mListener.onProviderEnabled(provider);
            });
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            if (mKey == null) {
                return;
            }

            mExecutor.execute(() -> {
                LocationListenerKey key = mKey;
                if (key == null) {
                    return;
                }
                key.mListener.onProviderDisabled(provider);
            });
        }
    }

    @RequiresApi(24)
    private static class GnssMeasurementsTransport extends GnssMeasurementsEvent.Callback {

        final GnssMeasurementsEvent.Callback mCallback;
        volatile @Nullable Executor mExecutor;

        GnssMeasurementsTransport(GnssMeasurementsEvent.@NonNull Callback callback,
                @NonNull Executor executor) {
            mCallback = callback;
            mExecutor = executor;
        }

        public void unregister() {
            mExecutor = null;
        }

        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent gnssMeasurementsEvent) {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            executor.execute(() -> {
                if (mExecutor != executor) {
                    return;
                }
                mCallback.onGnssMeasurementsReceived(gnssMeasurementsEvent);
            });
        }

        @Override
        public void onStatusChanged(int status) {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            executor.execute(() -> {
                if (mExecutor != executor) {
                    return;
                }
                mCallback.onStatusChanged(status);
            });
        }
    }

    @RequiresApi(30)
    private static class GnssStatusTransport extends GnssStatus.Callback {

        final GnssStatusCompat.Callback mCallback;

        GnssStatusTransport(GnssStatusCompat.Callback callback) {
            Preconditions.checkArgument(callback != null, "invalid null callback");
            mCallback = callback;
        }

        @Override
        public void onStarted() {
            mCallback.onStarted();
        }

        @Override
        public void onStopped() {
            mCallback.onStopped();
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            mCallback.onFirstFix(ttffMillis);
        }

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            mCallback.onSatelliteStatusChanged(GnssStatusCompat.wrap(status));
        }
    }

    @RequiresApi(24)
    private static class PreRGnssStatusTransport extends GnssStatus.Callback {

        final GnssStatusCompat.Callback mCallback;

        volatile @Nullable Executor mExecutor;

        PreRGnssStatusTransport(GnssStatusCompat.Callback callback) {
            Preconditions.checkArgument(callback != null, "invalid null callback");
            mCallback = callback;
        }

        public void register(Executor executor) {
            Preconditions.checkArgument(executor != null, "invalid null executor");
            Preconditions.checkState(mExecutor == null);
            mExecutor = executor;
        }

        public void unregister() {
            mExecutor = null;
        }

        @Override
        public void onStarted() {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            executor.execute(() -> {
                if (mExecutor != executor) {
                    return;
                }
                mCallback.onStarted();
            });
        }

        @Override
        public void onStopped() {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            executor.execute(() -> {
                if (mExecutor != executor) {
                    return;
                }
                mCallback.onStopped();
            });
        }

        @Override
        public void onFirstFix(final int ttffMillis) {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            executor.execute(() -> {
                if (mExecutor != executor) {
                    return;
                }
                mCallback.onFirstFix(ttffMillis);
            });
        }

        @Override
        public void onSatelliteStatusChanged(final GnssStatus status) {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            executor.execute(() -> {
                if (mExecutor != executor) {
                    return;
                }
                mCallback.onSatelliteStatusChanged(GnssStatusCompat.wrap(status));
            });
        }
    }

    private static class GpsStatusTransport implements GpsStatus.Listener {

        private final LocationManager mLocationManager;
        final GnssStatusCompat.Callback mCallback;

        volatile @Nullable Executor mExecutor;

        GpsStatusTransport(LocationManager locationManager,
                GnssStatusCompat.Callback callback) {
            Preconditions.checkArgument(callback != null, "invalid null callback");
            mLocationManager = locationManager;
            mCallback = callback;
        }

        public void register(Executor executor) {
            Preconditions.checkState(mExecutor == null);
            mExecutor = executor;
        }

        public void unregister() {
            mExecutor = null;
        }

        @RequiresPermission(ACCESS_FINE_LOCATION)
        @Override
        public void onGpsStatusChanged(int event) {
            final Executor executor = mExecutor;
            if (executor == null) {
                return;
            }

            GpsStatus gpsStatus;

            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    executor.execute(() -> {
                        if (mExecutor != executor) {
                            return;
                        }
                        mCallback.onStarted();
                    });
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    executor.execute(() -> {
                        if (mExecutor != executor) {
                            return;
                        }
                        mCallback.onStopped();
                    });
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    gpsStatus = mLocationManager.getGpsStatus(null);
                    if (gpsStatus != null) {
                        final int ttff = gpsStatus.getTimeToFirstFix();
                        executor.execute(() -> {
                            if (mExecutor != executor) {
                                return;
                            }
                            mCallback.onFirstFix(ttff);
                        });
                    }
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    gpsStatus = mLocationManager.getGpsStatus(null);
                    if (gpsStatus != null) {
                        final GnssStatusCompat gnssStatus = GnssStatusCompat.wrap(gpsStatus);
                        executor.execute(() -> {
                            if (mExecutor != executor) {
                                return;
                            }
                            mCallback.onSatelliteStatusChanged(gnssStatus);
                        });
                    }
                    break;
            }
        }
    }

    private static final class CancellableLocationListener implements LocationListener {

        private final LocationManager mLocationManager;
        private final Executor mExecutor;
        private final Handler mTimeoutHandler;

        private Consumer<Location> mConsumer;

        @GuardedBy("this")
        private boolean mTriggered;

        @Nullable Runnable mTimeoutRunnable;

        CancellableLocationListener(LocationManager locationManager,
                Executor executor, Consumer<Location> consumer) {
            mLocationManager = locationManager;
            mExecutor = executor;
            mTimeoutHandler = new Handler(Looper.getMainLooper());

            mConsumer = consumer;
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        public void cancel() {
            synchronized (this) {
                if (mTriggered) {
                    return;
                }
                mTriggered = true;
            }

            cleanup();
        }

        @SuppressLint("MissingPermission") // Can't annotate a lambda
        public void startTimeout(long timeoutMs) {
            synchronized (this) {
                if (mTriggered) {
                    return;
                }

                // ideally this would be a wakeup alarm, but that would require another compat layer
                // to deal with translating pending intent alarms into listeners which doesn't exist
                // at the moment, so this should be sufficient to prevent extreme battery drain
                mTimeoutRunnable = () -> {
                    mTimeoutRunnable = null;
                    onLocationChanged((Location) null);
                };
                mTimeoutHandler.postDelayed(mTimeoutRunnable, timeoutMs);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        @Override
        public void onProviderDisabled(@NonNull String p) {
            onLocationChanged((Location) null);
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        @Override
        public void onLocationChanged(final @Nullable Location location) {
            synchronized (this) {
                if (mTriggered) {
                    return;
                }
                mTriggered = true;
            }

            final Consumer<Location> consumer = mConsumer;
            mExecutor.execute(() -> consumer.accept(location));

            cleanup();
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        private void cleanup() {
            mConsumer = null;
            mLocationManager.removeUpdates(this);
            if (mTimeoutRunnable != null) {
                mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
                mTimeoutRunnable = null;
            }
        }
    }


    /**
     * An {@link Executor} that posts all executed tasks onto the given {@link Handler}. This
     * version differs from {@link ExecutorCompat#create(Handler)} in that if the execute call is
     * already occurring on the Looper of the given Handler, the Runnable will simply be executed
     * directly. This avoids the cost of an additional thread trampoline when not necessary, but
     * can introduce out-of-order execution violations as it is possible a given Runnable may
     * execute before some other Runnable that was submitted to the executor earlier. Because of
     * this limitation, use this Executor only when you are sure that all Runnables will always
     * be submitted to this Executor from the same logical thread, and only if it is acceptable to
     * bypass the given Handler completely.
     */
    private static final class InlineHandlerExecutor implements Executor {
        private final Handler mHandler;

        InlineHandlerExecutor(@NonNull Handler handler) {
            mHandler = Preconditions.checkNotNull(handler);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (Looper.myLooper() == mHandler.getLooper()) {
                command.run();
            } else if (!mHandler.post(Preconditions.checkNotNull(command))) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }
    }

    @RequiresApi(31)
    private static class Api31Impl {
        private Api31Impl() {
            // This class is not instantiable.
        }

        static boolean hasProvider(LocationManager locationManager, @NonNull String provider) {
            return locationManager.hasProvider(provider);
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        static void requestLocationUpdates(LocationManager locationManager,
                @NonNull String provider, @NonNull LocationRequest locationRequest,
                @NonNull Executor executor, @NonNull LocationListener listener) {
            locationManager.requestLocationUpdates(provider, locationRequest, executor, listener);
        }

        @RequiresPermission(ACCESS_FINE_LOCATION)
        static boolean registerGnssMeasurementsCallback(@NonNull LocationManager locationManager,
                @NonNull Executor executor, GnssMeasurementsEvent.@NonNull Callback callback) {
            return locationManager.registerGnssMeasurementsCallback(executor, callback);
        }
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private static Class<?> sLocationRequestClass;
        private static Method sRequestLocationUpdatesExecutorMethod;

        private Api30Impl() {
            // This class is not instantiable.
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        static void getCurrentLocation(LocationManager locationManager, @NonNull String provider,
                @Nullable CancellationSignal cancellationSignal,
                @NonNull Executor executor, final @NonNull Consumer<Location> consumer) {
            locationManager.getCurrentLocation(provider,
                    cancellationSignal,
                    executor,
                    consumer::accept);
        }

        @SuppressWarnings("JavaReflectionMemberAccess")
        public static boolean tryRequestLocationUpdates(LocationManager locationManager,
                String provider, LocationRequestCompat locationRequest, Executor executor,
                LocationListenerCompat listener) {
            if (VERSION.SDK_INT >= 30) { // Satisfy reflection lint check
                try {
                    if (sLocationRequestClass == null) {
                        sLocationRequestClass = Class.forName("android.location.LocationRequest");
                    }
                    if (sRequestLocationUpdatesExecutorMethod == null) {
                        sRequestLocationUpdatesExecutorMethod =
                                LocationManager.class.getDeclaredMethod(
                                        "requestLocationUpdates",
                                        sLocationRequestClass, Executor.class,
                                        LocationListener.class);
                        sRequestLocationUpdatesExecutorMethod.setAccessible(true);
                    }

                    Object request = locationRequest.toLocationRequest(provider);
                    if (request != null) {
                        sRequestLocationUpdatesExecutorMethod.invoke(locationManager, request,
                                executor,
                                listener);
                        return true;
                    }
                } catch (NoSuchMethodException
                        | InvocationTargetException
                        | IllegalAccessException
                        | ClassNotFoundException
                        | UnsupportedOperationException e) {
                    // ignored
                }
            }
            return false;
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        public static boolean registerGnssStatusCallback(LocationManager locationManager,
                Handler baseHandler, Executor executor, GnssStatusCompat.Callback callback) {
            synchronized (GnssListenersHolder.sGnssStatusListeners) {
                GnssStatusTransport transport =
                        (GnssStatusTransport) GnssListenersHolder.sGnssStatusListeners.get(
                                callback);
                if (transport == null) {
                    transport = new GnssStatusTransport(callback);
                }
                if (locationManager.registerGnssStatusCallback(executor, transport)) {
                    GnssListenersHolder.sGnssStatusListeners.put(callback, transport);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    @RequiresApi(28)
    private static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static boolean isLocationEnabled(LocationManager locationManager) {
            return locationManager.isLocationEnabled();
        }

        static String getGnssHardwareModelName(LocationManager locationManager) {
            return locationManager.getGnssHardwareModelName();
        }

        static int getGnssYearOfHardware(LocationManager locationManager) {
            return locationManager.getGnssYearOfHardware();
        }
    }

    static class Api19Impl {
        private static Class<?> sLocationRequestClass;
        private static Method sRequestLocationUpdatesLooperMethod;

        private Api19Impl() {
            // This class is not instantiable.
        }

        @SuppressLint("BanUncheckedReflection")
        @SuppressWarnings("JavaReflectionMemberAccess")
        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        static boolean tryRequestLocationUpdates(LocationManager locationManager,
                String provider, LocationRequestCompat locationRequest,
                LocationListenerTransport transport) {
            // Satisfy reflection lint check
            try {
                if (sLocationRequestClass == null) {
                    sLocationRequestClass = Class.forName("android.location.LocationRequest");
                }
                if (sRequestLocationUpdatesLooperMethod == null) {
                    sRequestLocationUpdatesLooperMethod =
                            LocationManager.class.getDeclaredMethod(
                                    "requestLocationUpdates",
                                    sLocationRequestClass, LocationListener.class,
                                    Looper.class);
                    sRequestLocationUpdatesLooperMethod.setAccessible(true);
                }

                LocationRequest request = locationRequest.toLocationRequest(provider);
                if (request != null) {
                    synchronized (sLocationListeners) {
                        sRequestLocationUpdatesLooperMethod.invoke(locationManager, request,
                                transport, Looper.getMainLooper());
                        registerLocationListenerTransport(locationManager, transport);
                        return true;
                    }
                }
            } catch (NoSuchMethodException
                    | InvocationTargetException
                    | IllegalAccessException
                    | ClassNotFoundException
                    | UnsupportedOperationException e) {
                // ignored
            }
            return false;
        }

        @SuppressLint("BanUncheckedReflection")
        @SuppressWarnings("JavaReflectionMemberAccess")
        static boolean tryRequestLocationUpdates(LocationManager locationManager, String provider,
                LocationRequestCompat locationRequest, LocationListenerCompat listener,
                Looper looper) {
            // Satisfy reflection lint check
            try {
                if (sLocationRequestClass == null) {
                    sLocationRequestClass = Class.forName("android.location.LocationRequest");
                }

                if (sRequestLocationUpdatesLooperMethod == null) {
                    sRequestLocationUpdatesLooperMethod =
                            LocationManager.class.getDeclaredMethod(
                                    "requestLocationUpdates",
                                    sLocationRequestClass, LocationListener.class,
                                    Looper.class);
                    sRequestLocationUpdatesLooperMethod.setAccessible(true);
                }

                LocationRequest request = locationRequest.toLocationRequest(provider);
                if (request != null) {
                    sRequestLocationUpdatesLooperMethod.invoke(
                            locationManager, request, listener, looper);
                    return true;
                }
            } catch (NoSuchMethodException
                    | InvocationTargetException
                    | IllegalAccessException
                    | ClassNotFoundException
                    | UnsupportedOperationException e) {
                // ignored
            }
            return false;
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @RequiresPermission(ACCESS_FINE_LOCATION)
        static boolean registerGnssMeasurementsCallback(@NonNull LocationManager locationManager,
                GnssMeasurementsEvent.@NonNull Callback callback) {
            return locationManager.registerGnssMeasurementsCallback(callback);
        }

        @RequiresPermission(ACCESS_FINE_LOCATION)
        static boolean registerGnssMeasurementsCallback(@NonNull LocationManager locationManager,
                GnssMeasurementsEvent.@NonNull Callback callback, @NonNull Handler handler) {
            return locationManager.registerGnssMeasurementsCallback(callback, handler);
        }

        static void unregisterGnssMeasurementsCallback(@NonNull LocationManager locationManager,
                GnssMeasurementsEvent.@NonNull Callback callback) {
            locationManager.unregisterGnssMeasurementsCallback(callback);
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        static boolean registerGnssStatusCallback(LocationManager locationManager,
                Handler baseHandler, Executor executor, GnssStatusCompat.Callback callback) {
            Preconditions.checkArgument(baseHandler != null);

            synchronized (GnssListenersHolder.sGnssStatusListeners) {
                PreRGnssStatusTransport transport =
                        (PreRGnssStatusTransport) GnssListenersHolder.sGnssStatusListeners.get(
                                callback);
                if (transport == null) {
                    transport = new PreRGnssStatusTransport(callback);
                } else {
                    transport.unregister();
                }
                transport.register(executor);

                if (locationManager.registerGnssStatusCallback(transport, baseHandler)) {
                    GnssListenersHolder.sGnssStatusListeners.put(callback, transport);
                    return true;
                } else {
                    return false;
                }
            }
        }

        static void unregisterGnssStatusCallback(LocationManager locationManager, Object callback) {
            if (callback instanceof PreRGnssStatusTransport) {
                ((PreRGnssStatusTransport) callback).unregister();
            }
            locationManager.unregisterGnssStatusCallback((GnssStatus.Callback) callback);
        }
    }
}
