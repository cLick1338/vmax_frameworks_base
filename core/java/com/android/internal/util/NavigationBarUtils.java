//Vmax
package com.android.internal.util;

import android.content.ContentResolver;
import android.graphics.Color;
import android.provider.Settings;

/** @hide */
public final class NavigationBarUtils {
    public static final int HEIGHT_DEFAULT = 100;
    public static final int HEIGHT_MIN = 50;
    public static final int HEIGHT_MAX = 150;

    private NavigationBarUtils() {}

    public static int getHeightScale(ContentResolver resolver, int userId) {
        final int value = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.BUTTON_NAVIGATION_HEIGHT, HEIGHT_DEFAULT, userId);
        return value >= HEIGHT_MIN && value <= HEIGHT_MAX ? value : HEIGHT_DEFAULT;
    }

    public static int scaleSize(int size, int scale) {
        return Math.round(size * (scale / 100f));
    }

    public static int getIconColor(ContentResolver resolver, int userId) {
        final int color = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.BUTTON_NAVIGATION_ICON_COLOR, 0, userId);
        return Color.alpha(color) == 255 ? color : 0;
    }
}
