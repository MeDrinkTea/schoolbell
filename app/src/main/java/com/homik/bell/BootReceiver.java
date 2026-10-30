package com.homik.bell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        ReminderScheduler.refresh(context);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("enabled", false)) {
            Intent service = new Intent(context, BellService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service); else context.startService(service);
        }
    }
}
