package foo;

import com.android.support.lifecycle.GenericLifecycleObserver;
import com.android.support.lifecycle.LifecycleOwner;

import java.lang.Object;
import java.lang.Override;

public class InheritanceOk2Derived_LifecycleAdapter implements GenericLifecycleObserver {
    final InheritanceOk2Derived mReceiver;

    InheritanceOk2Derived_LifecycleAdapter(InheritanceOk2Derived receiver) {
        this.mReceiver = receiver;
    }

    @Override
    public void onStateChanged(LifecycleOwner owner, int event) {
        if ((event & 8192) != 0) {
            mReceiver.onStop(owner, event);
            mReceiver.onStop2(owner, event);
        }
    }

    public Object getReceiver() {
        return mReceiver;
    }
}
