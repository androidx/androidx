package androidx.health.services.client.impl

/**
 * Collection of constants used for IPC.
 *
 * @hide
 */
public object IpcConstants {
    public const val SERVICE_PACKAGE_NAME: String = "com.google.android.wearable.healthservices"

    public const val EXERCISE_API_BIND_ACTION: String = "hs.exerciseclient.BIND"
    public const val HEALTH_SERVICES_BIND_ACTION: String = "hs.healthservicesclient.BIND"
    public const val MEASURE_API_BIND_ACTION: String = "hs.measureclient.BIND"
    public const val PASSIVE_API_BIND_ACTION: String = "hs.passiveclient.BIND"
}
