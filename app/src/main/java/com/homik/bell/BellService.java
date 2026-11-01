package com.homik.bell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class BellService extends Service {
    public static final String ACTION_REFRESH_TEXT = "com.homik.bell.REFRESH_TEXT";
    private static final String CHANNEL = "school_schedule";
    private static final int NOTIFICATION_ID = 42;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updater = new Runnable() {
        @Override public void run() {
            if (!getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false)) {
                stopSelf();
                return;
            }
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.notify(NOTIFICATION_ID, makeNotification(notificationTitle(), statusText()));
            BellWidgetProvider.refresh(BellService.this);
            handler.postDelayed(this, 15_000);
        }
    };

    @Override public void onCreate() { super.onCreate(); createChannel(); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("enabled", false).apply();
            handler.removeCallbacks(updater);
            stopForeground(true); stopSelf(); return START_NOT_STICKY;
        }
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("enabled", true).apply();
        createChannel();
        if (intent == null || !ACTION_REFRESH_TEXT.equals(intent.getAction())) chooseCustomText();
        handler.removeCallbacks(updater);
        startForeground(NOTIFICATION_ID, makeNotification(notificationTitle(), statusText()));
        BellWidgetProvider.refresh(this);
        handler.postDelayed(updater, 15_000);
        return START_STICKY;
    }

    private String statusText() {
        LocalDateTime now = LocalDateTime.now();
        List<Schedule.Slot> slots = Schedule.forDay(now.getDayOfWeek(), this);
        if (slots.isEmpty()) return orderNotificationBlocks(AppStrings.t(this, "Сегодня занятий по расписанию нет"), "", vacationText(now));
        LocalTime time = now.toLocalTime();
        for (int i = 0; i < slots.size(); i++) {
            Schedule.Slot s = slots.get(i);
            if (!time.isBefore(s.start) && time.isBefore(s.end)) {
                String next = i + 1 < slots.size() ? SubjectCatalog.label(this, slots.get(i + 1).title) : AppStrings.t(this, "занятий больше нет");
                String lessonName = SubjectCatalog.label(this, s.title);
                String nextLine = AppStrings.t(this, "Далее: ") + next
                        + (i + 1 < slots.size() ? AppStrings.t(this, " в ") + slots.get(i + 1).start.format(AppStrings.clockFormatter(this)) : "");
                return orderNotificationBlocks(lessonName + " · " + AppStrings.t(this, "До конца урока ") + timeLeft(now, s.end), nextLine, vacationText(now));
            }
            if (time.isBefore(s.start)) {
                String nextLine = AppStrings.t(this, "Следующий урок: ") + SubjectCatalog.label(this, s.title)
                        + AppStrings.t(this, " в ") + s.start.format(AppStrings.clockFormatter(this));
                return orderNotificationBlocks(AppStrings.t(this, "Перемена · до звонка ") + timeLeft(now, s.start), nextLine, vacationText(now));
            }
        }
        return orderNotificationBlocks(AppStrings.t(this, "Уроки закончились · до завтра"), "", vacationText(now));
    }

    private String orderNotificationBlocks(String status, String next, String holiday) {
        java.util.Map<String, String> blocks = new java.util.HashMap<>();
        blocks.put("status", status); blocks.put("next", next); blocks.put("holiday", holiday);
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String order = prefs.getString("notificationBlockOrder", "status,next,holiday");
        Set<String> hidden = prefs.getStringSet("hidden_notificationBlockOrder", java.util.Collections.emptySet());
        List<String> lines = new ArrayList<>();
        for (String key : order.split(",")) {
            String value = blocks.remove(key);
            if (value != null && !value.isEmpty() && !hidden.contains(key)) lines.add(value);
        }
        for (java.util.Map.Entry<String, String> entry : blocks.entrySet())
            if (!entry.getValue().isEmpty() && !hidden.contains(entry.getKey())) lines.add(entry.getValue());
        return String.join("\n", lines);
    }

    private String notificationTitle() {
        String custom = getSharedPreferences("settings", MODE_PRIVATE).getString("activeCustomText", "").trim();
        return custom.isEmpty() ? AppStrings.t(this, "Расписание звонков") : custom;
    }

    private String vacationText(LocalDateTime now) {
        SchoolCalendar.Vacation v = SchoolCalendar.next(now.toLocalDate(), this);
        boolean active = v.contains(now.toLocalDate());
        long days = active ? v.daysLeftIncludingToday(now.toLocalDate()) : v.daysUntilStart(now.toLocalDate());
        return SchoolCalendar.countdown(now.toLocalDate(), this);
    }

    private void chooseCustomText() {
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        List<String> lines = new ArrayList<>();
        if (prefs.getBoolean("showCustomText", false)) {
            String saved = prefs.getString("customLines", "");
            for (String line : saved.split("\\R")) if (!line.trim().isEmpty()) lines.add(line.trim());
        }
        String previous = prefs.getString("activeCustomText", "");
        int previousIndex = prefs.getInt("activeCustomTextIndex", -1);
        if (lines.isEmpty()) {
            prefs.edit().putString("activeCustomText", "").putInt("activeCustomTextIndex", -1).apply();
            return;
        }
        List<Integer> choices = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) if (!lines.get(i).equals(previous)) choices.add(i);
        if (choices.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) if (i != previousIndex) choices.add(i);
        }
        if (choices.isEmpty()) choices.add(0);
        int selected = choices.get(new Random().nextInt(choices.size()));
        prefs.edit().putString("activeCustomText", lines.get(selected)).putInt("activeCustomTextIndex", selected).apply();
    }

    private String timeLeft(LocalDateTime now, LocalTime end) {
        long seconds = Math.max(0, Duration.between(now.toLocalTime(), end).getSeconds());
        if (seconds >= 3600) return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private LocalDateTime nextEventTime() {
        LocalDateTime now = LocalDateTime.now();
        List<Schedule.Slot> today = Schedule.forDay(now.getDayOfWeek(), this);
        for (Schedule.Slot slot : today) {
            if (!now.toLocalTime().isBefore(slot.start) && now.toLocalTime().isBefore(slot.end))
                return now.toLocalDate().atTime(slot.end);
            if (now.toLocalTime().isBefore(slot.start)) return now.toLocalDate().atTime(slot.start);
        }
        for (int offset = 1; offset <= 7; offset++) {
            LocalDateTime date = now.plusDays(offset);
            List<Schedule.Slot> slots = Schedule.forDay(date.getDayOfWeek(), this);
            if (!slots.isEmpty()) return date.toLocalDate().atTime(slots.get(0).start);
        }
        return null;
    }

    private Notification makeNotification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, BellService.class).setAction("STOP");
        PendingIntent stopPi = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_bell)
                .setContentTitle(title).setContentText(text).setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_STATUS)
                .addAction(android.R.drawable.ic_media_pause, AppStrings.t(this, "Остановить"), stopPi);
        LocalDateTime target = nextEventTime();
        if (target != null) {
            long millisLeft = Math.max(0, Duration.between(LocalDateTime.now(), target).toMillis());
            builder.setShowWhen(true).setWhen(System.currentTimeMillis() + millisLeft)
                    .setUsesChronometer(true).setChronometerCountDown(true);
        }
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel ch = manager.getNotificationChannel(CHANNEL);
            if (ch == null) ch = new NotificationChannel(CHANNEL, AppStrings.t(this, "Школьное расписание"), NotificationManager.IMPORTANCE_LOW);
            ch.setName(AppStrings.t(this, "Школьное расписание"));
            ch.setDescription(AppStrings.t(this, "Покажет, сколько осталось до звонка и какой урок будет дальше."));
            manager.createNotificationChannel(ch);
        }
    }

    @Override public void onDestroy() { handler.removeCallbacks(updater); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
