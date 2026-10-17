//Vmax
package com.android.internal.util;

import android.content.ContentResolver;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

/** @hide */
public final class HideDeveloperStatusUtils {
    private HideDeveloperStatusUtils() {}

    public static boolean shouldHideDevStatus(ContentResolver resolver, String name, int userId) {
        if (!Settings.Global.ADB_ENABLED.equals(name)
                && !Settings.Global.ADB_WIFI_ENABLED.equals(name)
                && !Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
            return false;
        }
        if (resolver == null || !SystemProperties.getBoolean("sys.boot_completed", false)) {
            return false;
        }
        String packageName = resolver.getPackageName();
        if (packageName == null || "org.lineageos.updater".equals(packageName)) return false;
        if (userId == UserHandle.USER_CURRENT || userId == UserHandle.USER_CURRENT_OR_SELF) {
            userId = resolver.getUserId();
        }
        String apps = Settings.Secure.getStringForUser(resolver,
                Settings.Secure.HIDE_DEVELOPER_STATUS, userId);
        if (apps == null || apps.isEmpty()) return false;
        for (String app : apps.split(",")) {
            if (packageName.equals(app)) return true;
        }
        return false;
    }
}
