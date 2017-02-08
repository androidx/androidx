package foo;

import com.android.support.lifecycle.GenericLifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;

import java.lang.Object;
import java.lang.Override;

public class InheritanceOk2Base_LifecycleAdapter implements GenericLifecycleObserver {
    final InheritanceOk2Base mReceiver;

    InheritanceOk2Base_LifecycleAdapter(InheritanceOk2Base receiver) {
        this.mReceiver = receiver;
    }

    @Override
    public void onStateChanged(LifecycleProvider provider, int event) {
        if ((event & 8192) != 0) {
            mReceiver.onStop(provider, event);
        }
    }

    public Object getReceiver() {
        return mReceiver;
    }
}
