package com.homik.bell;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "school_schedule_reminders";
    @Override public void onReceive(Context context, Intent intent) {
        int kind = intent.getIntExtra("kind", 0), seconds = intent.getIntExtra("seconds", 0);
        String subject = SubjectCatalog.label(context, intent.getStringExtra("subject") == null ? "" : intent.getStringExtra("subject"));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
            if (channel == null) channel = new NotificationChannel(CHANNEL, AppStrings.t(context, "Напоминания о звонках"), NotificationManager.IMPORTANCE_HIGH);
            channel.setName(AppStrings.t(context, "Напоминания о звонках"));
            channel.setDescription(AppStrings.t(context, "Звуковые напоминания до конца урока или перемены"));
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
        String title = kind == 0 ? AppStrings.t(context, "Скоро конец урока") : AppStrings.t(context, "Скоро конец перемены");
        String remaining = formatRemaining(context, seconds);
        String body = AppStrings.t(context, kind == 0 ? "До конца урока " : "До следующего урока ") + remaining + " · " + subject;
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        manager.notify(id, new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_bell).setContentTitle(title).setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true).setOnlyAlertOnce(false).build());
        ReminderScheduler.refresh(context);
    }

    private String formatRemaining(Context context, int totalSeconds) {
        int minutes = totalSeconds / 60, seconds = totalSeconds % 60;
        if (AppStrings.english(context)) return seconds == 0 ? minutes + (minutes == 1 ? " min" : " min") : minutes + " min " + seconds + " sec";
        if (seconds == 0) return minutes + " " + AppStrings.t(context, "мин");
        return minutes + " " + AppStrings.t(context, "мин") + " " + seconds + " " + AppStrings.t(context, "сек");
    }
}
