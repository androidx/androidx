package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_ANY;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;

public class InvalidSecondArg {
    @OnLifecycleEvent(ON_ANY)
    public void onStop(LifecycleOwner provider, Object lastEvent) {
    }
}
