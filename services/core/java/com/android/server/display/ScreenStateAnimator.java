//Vmax

package com.android.server.display;

import android.content.Context;

import java.io.PrintWriter;

/** @hide */
interface ScreenStateAnimator {
    int MODE_WARM_UP = 0;
    int MODE_COOL_DOWN = 1;
    int MODE_FADE = 2;
    int MODE_SCALE_DOWN = 3;

    boolean prepare(Context context, int mode);

    default void dismissResources() {}

    void dismiss();

    boolean draw(float level);

    void dump(PrintWriter pw);

    void destroy();
}
