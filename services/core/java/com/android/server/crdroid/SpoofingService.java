//Vmax
package com.android.server.crdroid;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.server.SystemService;

public final class SpoofingService extends SystemService {
    private int mUserId = UserHandle.USER_SYSTEM;

    public SpoofingService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateRomHide();
            }
        };
        getContext().getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.SPOOFING_ROM_HIDE), false, observer,
                UserHandle.USER_ALL);
        updateRomHide();
    }

    @Override
    public void onUserSwitching(TargetUser from, TargetUser to) {
        mUserId = to.getUserIdentifier();
        updateRomHide();
    }

    private void updateRomHide() {
        boolean enabled = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                Settings.Secure.SPOOFING_ROM_HIDE, 1, mUserId) == 1;
        SystemProperties.set("sys.spoof.rom_hide", enabled ? "1" : "0");
    }
}
