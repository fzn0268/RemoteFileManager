package fzn.projects.android.remotefilemanager;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by FzN on 2015/9/21.
 */
public interface SaveFragmentStateCallbacks {
    void putState(Bundle bundle);

    Bundle getState(Class<? extends Fragment> clazz);
}
