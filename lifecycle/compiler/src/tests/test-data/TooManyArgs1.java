package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_ANY;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class TooManyArgs1 implements LifecycleObserver {
    @OnLifecycleEvent(ON_ANY)
    public void onAny(LifecycleOwner provider, Event event, int x) {
    }
}
