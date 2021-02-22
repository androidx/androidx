package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class TooManyArgs2 implements LifecycleObserver {
    @OnLifecycleEvent(ON_STOP)
    public void onStop(LifecycleOwner provider, Event event) {
    }
}
