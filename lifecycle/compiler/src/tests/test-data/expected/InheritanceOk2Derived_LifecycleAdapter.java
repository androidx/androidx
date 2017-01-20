package foo;

import com.android.support.lifecycle.GenericLifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;

import java.lang.Object;
import java.lang.Override;

public class InheritanceOk2Derived_LifecycleAdapter implements GenericLifecycleObserver {
    final InheritanceOk2Derived mReceiver;

    InheritanceOk2Derived_LifecycleAdapter(InheritanceOk2Derived receiver) {
        this.mReceiver = receiver;
    }

    @Override
    public void onStateChanged(LifecycleProvider provider, int previousState) {
        final int curState = provider.getLifecycle().getCurrentState();
        if ((curState & 8192) != 0) {
            mReceiver.onStop(provider, previousState);
            mReceiver.onStop2(provider, previousState);
        }
    }

    public Object getReceiver() {
        return mReceiver;
    }
}
