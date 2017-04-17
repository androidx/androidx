package foo;

import static android.arch.lifecycle.Lifecycle.ON_STOP;

import android.arch.lifecycle.OnLifecycleEvent;

public class InvalidFirstArg {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(int lastEvent) {
    }
}
