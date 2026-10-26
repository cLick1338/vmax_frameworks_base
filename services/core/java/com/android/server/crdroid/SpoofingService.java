//Vmax
package com.android.server.crdroid;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;

import com.android.server.SystemService;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public final class SpoofingService extends SystemService {
    private static final String TAG = "SpoofingService";
    private static final File BOOT_STATE_DIR = new File("/cache/spoofing");
    private Boolean mPersistedRomHide;
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
    public synchronized void onUserSwitching(TargetUser from, TargetUser to) {
        mUserId = to.getUserIdentifier();
        updateRomHide();
    }

    private synchronized void updateRomHide() {
        boolean enabled = Settings.Secure.getIntForUser(getContext().getContentResolver(),
                Settings.Secure.SPOOFING_ROM_HIDE, 1, mUserId) == 1;
        SystemProperties.set("sys.spoof.rom_hide", enabled ? "1" : "0");
        persistRomHide(enabled);
    }

    private void persistRomHide(boolean enabled) {
        if (mPersistedRomHide != null && mPersistedRomHide == enabled) return;

        File pending = new File(BOOT_STATE_DIR, "rom_hide.new");
        File committed = new File(BOOT_STATE_DIR, "rom_hide");
        try {
            // Init creates and labels the private directory after mounting cache.
            if (!BOOT_STATE_DIR.isDirectory()) {
                throw new IOException("Boot state directory is unavailable");
            }
            try (FileOutputStream stream = new FileOutputStream(pending)) {
                Os.fchmod(stream.getFD(), 0600);
                stream.write(enabled ? '1' : '0');
                stream.getFD().sync();
            }
            Os.rename(pending.getPath(), committed.getPath());
            FileDescriptor directory = Os.open(BOOT_STATE_DIR.getPath(),
                    OsConstants.O_RDONLY | OsConstants.O_CLOEXEC, 0);
            try {
                Os.fsync(directory);
            } finally {
                Os.close(directory);
            }
            mPersistedRomHide = enabled;
        } catch (IOException | ErrnoException e) {
            Slog.e(TAG, "Failed to persist ROM hiding choice for next boot", e);
        }
    }
}
