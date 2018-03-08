package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_ANY;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class InvalidSecondArg implements LifecycleObserver {
    @OnLifecycleEvent(ON_ANY)
    public void onStop(LifecycleOwner provider, Object lastEvent) {
    }
}
