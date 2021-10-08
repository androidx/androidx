# This is necessary so that RemoteWorkManager can be initialized (also marked with @Keep)
-keep class androidx.work.multiprocess.RemoteWorkManagerClient {
    public <init>(...);
}
