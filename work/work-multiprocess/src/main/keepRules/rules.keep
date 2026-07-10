# This is necessary so that RemoteWorkManager can be initialized
-keep class androidx.work.multiprocess.RemoteWorkManagerClient {
    public <init>(...);
}
# This is necessary since RemoteListenableDelegatingWorker is enqueued by FQN from work-runtime
-keep class androidx.work.multiprocess.RemoteListenableDelegatingWorker {
    public <init>(...);
}
