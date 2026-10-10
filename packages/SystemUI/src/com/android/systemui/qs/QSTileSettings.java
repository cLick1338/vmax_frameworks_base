//Vmax
package com.android.systemui.qs;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.android.systemui.FontSizeUtils;
import com.android.systemui.res.R;

import java.util.ArrayList;

/** User overrides for the quick settings tile grid and tile appearance. */
public final class QSTileSettings {

    /** Notified on the main thread after any tile setting changes. */
    public interface Listener {
        void onQSTileSettingsChanged();
    }

    public static final int ANIMATION_NONE = 0;
    public static final int ANIMATION_FLIP = 1;
    public static final int ANIMATION_ROTATE = 2;

    private static final int UNSET = 0;
    private static final int MAX_COLUMNS = 8;
    private static final int MAX_ROWS = 8;
    private static final int MIN_LABEL_SIZE_SP = 8;
    private static final int MAX_LABEL_SIZE_SP = 24;
    private static final int DEFAULT_ANIMATION_DURATION = 800;
    private static final int MAX_ANIMATION_DURATION = 3000;

    private static final String[] KEYS = {
            Settings.System.QS_LAYOUT_COLUMNS,
            Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE,
            Settings.System.QS_LAYOUT_ROWS,
            Settings.System.QS_LAYOUT_ROWS_LANDSCAPE,
            Settings.System.QQS_LAYOUT_ROWS,
            Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE,
            Settings.System.QS_TILE_LABEL_HIDE,
            Settings.System.QS_TILE_LABEL_SIZE,
            Settings.System.QS_TILE_VERTICAL_LAYOUT,
            Settings.System.QS_TILE_ANIMATION_STYLE,
            Settings.System.QS_TILE_ANIMATION_DURATION,
            Settings.System.QS_TILE_ANIMATION_INTERPOLATOR,
            Settings.System.QUICK_SETTINGS_VIBRATE,
    };

    private static final ArrayMap<String, Integer> sCache = new ArrayMap<>();
    private static final ArrayList<Listener> sListeners = new ArrayList<>();
    private static int sCacheUser = UserHandle.USER_NULL;
    private static ContentObserver sObserver;
    private static Handler sHandler;
    private static final Runnable sNotifyListeners = () -> {
        for (Listener l : new ArrayList<>(sListeners)) {
            l.onQSTileSettingsChanged();
        }
    };

    private QSTileSettings() {}

    /** Starts observing the tile settings; must be called on the main thread. */
    public static void addListener(Context context, Listener listener) {
        if (sObserver == null) {
            sHandler = new Handler(Looper.getMainLooper());
            sObserver = new ContentObserver(sHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (sCache) {
                        sCache.clear();
                    }
                    // Coalesce bursts, e.g. a reset touching every key.
                    sHandler.removeCallbacks(sNotifyListeners);
                    sHandler.post(sNotifyListeners);
                }
            };
            for (String key : KEYS) {
                context.getApplicationContext().getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(key), false, sObserver, UserHandle.USER_ALL);
            }
        }
        if (!sListeners.contains(listener)) sListeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        sListeners.remove(listener);
    }

    private static int getInt(Context context, String key) {
        final int user = ActivityManager.getCurrentUser();
        synchronized (sCache) {
            if (user != sCacheUser) {
                sCache.clear();
                sCacheUser = user;
            }
            Integer value = sCache.get(key);
            if (value == null) {
                value = Settings.System.getIntForUser(context.getContentResolver(), key, UNSET,
                        user);
                sCache.put(key, value);
            }
            return value;
        }
    }

    private static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static int getOverride(Context context, String portrait, String landscape, int max,
            int fallback) {
        final int value = getInt(context, isLandscape(context) ? landscape : portrait);
        return value > UNSET ? Math.min(value, max) : fallback;
    }

    /** Columns for the current orientation. */
    public static int getColumns(Context context, int fallback) {
        return getOverride(context, Settings.System.QS_LAYOUT_COLUMNS,
                Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE, MAX_COLUMNS, fallback);
    }

    public static int getColumns(Context context) {
        return getColumns(context,
                context.getResources().getInteger(R.integer.quick_settings_num_columns));
    }

    /** Rows per page of the expanded panel for the current orientation. */
    public static int getRows(Context context, int fallback) {
        return getOverride(context, Settings.System.QS_LAYOUT_ROWS,
                Settings.System.QS_LAYOUT_ROWS_LANDSCAPE, MAX_ROWS, fallback);
    }

    /** Rows of the collapsed panel; defaults to the rows the stock tile count fills. */
    public static int getQqsRows(Context context) {
        final Resources res = context.getResources();
        final int tiles = res.getInteger(R.integer.quick_qs_panel_max_tiles);
        final int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        return getOverride(context, Settings.System.QQS_LAYOUT_ROWS,
                Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE, MAX_ROWS,
                Math.max(1, (tiles + columns - 1) / columns));
    }

    /** Tiles shown in the collapsed panel. */
    public static int getQqsMaxTiles(Context context) {
        return getColumns(context) * getQqsRows(context);
    }

    /** Columns used next to the media player in the horizontal layout. */
    public static int getMediaColumns(Context context) {
        return Math.max(2, getColumns(context) / 2);
    }

    public static boolean isLabelHidden(Context context) {
        return getInt(context, Settings.System.QS_TILE_LABEL_HIDE) != 0;
    }

    public static boolean isVerticalLayout(Context context) {
        return getInt(context, Settings.System.QS_TILE_VERTICAL_LAYOUT) != 0;
    }

    /** Applies the configured tile label size, or the default one. */
    public static void updateLabelSize(Context context, TextView view) {
        final int size = getInt(context, Settings.System.QS_TILE_LABEL_SIZE);
        if (size > UNSET) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    Math.max(MIN_LABEL_SIZE_SP, Math.min(size, MAX_LABEL_SIZE_SP)));
        } else {
            FontSizeUtils.updateFontSize(view, R.dimen.qs_tile_text_size);
        }
    }

    /** Estimated tile height for the given number of label lines. */
    public static int estimateCellHeight(Context context, TextView temp, int lines) {
        final Resources res = context.getResources();
        updateLabelSize(context, temp);
        final int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        temp.measure(unspecified, unspecified);
        final int labels = isLabelHidden(context) ? 0 : temp.getMeasuredHeight() * lines;
        if (!isVerticalLayout(context)) {
            return labels + res.getDimensionPixelSize(R.dimen.qs_tile_padding) * 2;
        }
        return res.getDimensionPixelSize(R.dimen.qs_tile_vertical_padding) * 2
                + res.getDimensionPixelSize(R.dimen.qs_icon_size)
                + (labels > 0
                        ? labels + res.getDimensionPixelSize(R.dimen.qs_tile_vertical_label_margin)
                        : 0);
    }

    public static int getAnimationStyle(Context context) {
        return getInt(context, Settings.System.QS_TILE_ANIMATION_STYLE);
    }

    public static long getAnimationDuration(Context context) {
        final int duration = getInt(context, Settings.System.QS_TILE_ANIMATION_DURATION);
        return duration > UNSET
                ? Math.min(duration, MAX_ANIMATION_DURATION) : DEFAULT_ANIMATION_DURATION;
    }

    public static Interpolator getAnimationInterpolator(Context context) {
        switch (getInt(context, Settings.System.QS_TILE_ANIMATION_INTERPOLATOR)) {
            case 1: return new AccelerateInterpolator();
            case 2: return new DecelerateInterpolator();
            case 3: return new AccelerateDecelerateInterpolator();
            case 4: return new BounceInterpolator();
            case 5: return new OvershootInterpolator();
            case 6: return new AnticipateInterpolator();
            default: return new LinearInterpolator();
        }
    }

    public static boolean isVibrateEnabled(Context context) {
        return getInt(context, Settings.System.QUICK_SETTINGS_VIBRATE) != 0;
    }
}
