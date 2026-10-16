//Vmax
package com.android.internal.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;

/** Reports Android 9 to UAPP, whose LG DoP path only runs on SDK 28 or lower. @hide */
public final class UappDopSpoofUtils {
    private static final String TAG = "UappDopSpoof";
    private static final int SPOOF_SDK = 28;
    private static final String SPOOF_RELEASE = "9";

    private UappDopSpoofUtils() {}

    public static void maybeSpoof(Context context) {
        if (context == null) return;
        String packageName = context.getPackageName();
        if (!"com.extreamsd.usbaudioplayerpro".equals(packageName)
                && !"com.extreamsd.usbaudioplayerprotrial".equals(packageName)) {
            return;
        }
        try {
            if (Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.SPOOFING_UAPP_DOP, 0) != 1) {
                return;
            }
            setField("SDK_INT", SPOOF_SDK);
            setField("RELEASE", SPOOF_RELEASE);
            setField("RELEASE_OR_CODENAME", SPOOF_RELEASE);
            setField("RELEASE_OR_PREVIEW_DISPLAY", SPOOF_RELEASE);
            Log.i(TAG, "Reporting SDK " + SPOOF_SDK + " to " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to spoof " + packageName, e);
        }
    }

    private static void setField(String name, Object value) throws Exception {
        Field field = Build.VERSION.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
