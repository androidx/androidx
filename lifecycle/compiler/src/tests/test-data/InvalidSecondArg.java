package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;

public class InvalidSecondArg {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(LifecycleOwner provider, Object lastEvent) {
    }
}
