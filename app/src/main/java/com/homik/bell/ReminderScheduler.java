package com.homik.bell;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

final class ReminderScheduler {
    static void refresh(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        android.content.SharedPreferences prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE);
        List<String> old = new ArrayList<>(prefs.getStringSet("reminderAlarmIds", java.util.Collections.emptySet()));
        for (String id : old) {
            Intent oldIntent = new Intent(app, ReminderReceiver.class).setAction("com.homik.bell.REMINDER");
            int requestCode = 0;
            if (id.startsWith("schoolbell://")) oldIntent.setData(android.net.Uri.parse(id));
            else try { requestCode = Integer.parseInt(id); } catch (NumberFormatException ignored) { continue; }
            PendingIntent pi = PendingIntent.getBroadcast(app, requestCode, oldIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) { manager.cancel(pi); pi.cancel(); }
        }
        prefs.edit().remove("reminderAlarmIds").apply();
        List<Integer> lessonLeads = offsets(prefs, "lessonReminderTimesSec", "lessonReminderMinutes");
        List<Integer> breakLeads = offsets(prefs, "breakReminderTimesSec", "breakReminderMinutes");
        if (lessonLeads.isEmpty() && breakLeads.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) return;

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<String> ids = new ArrayList<>();
        for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            List<Schedule.Slot> slots = Schedule.forDay(date.getDayOfWeek(), app);
            for (int i = 0; i < slots.size(); i++) {
                Schedule.Slot slot = slots.get(i);
                for (int reminder = 0; reminder < lessonLeads.size(); reminder++) {
                    int lessonLeadSeconds = lessonLeads.get(reminder);
                    if (java.time.Duration.between(slot.start, slot.end).toSeconds() > lessonLeadSeconds) {
                        LocalDateTime trigger = date.atTime(slot.end).minusSeconds(lessonLeadSeconds);
                        add(app, manager, ids, trigger, now, date, i, 0, reminder, lessonLeadSeconds, slot.title);
                    }
                }
                if (i > 0) {
                    Schedule.Slot previous = slots.get(i - 1);
                    for (int reminder = 0; reminder < breakLeads.size(); reminder++) {
                        int breakLeadSeconds = breakLeads.get(reminder);
                        LocalDateTime trigger = date.atTime(slot.start).minusSeconds(breakLeadSeconds);
                        if (!trigger.isBefore(date.atTime(previous.end)))
                            add(app, manager, ids, trigger, now, date, i, 1, reminder, breakLeadSeconds, slot.title);
                    }
                }
            }
        }
        prefs.edit().putStringSet("reminderAlarmIds", new java.util.HashSet<>(ids)).apply();
    }

    private static void add(Context context, AlarmManager manager, List<String> ids, LocalDateTime when,
                            LocalDateTime now, LocalDate date, int slotIndex, int kind, int reminderIndex, int seconds, String subject) {
        if (!when.isAfter(now)) return;
        String id = "schoolbell://reminder/" + date.toEpochDay() + "/" + slotIndex + "/" + kind + "/" + reminderIndex;
        Intent intent = new Intent(context, ReminderReceiver.class).setAction("com.homik.bell.REMINDER").setData(android.net.Uri.parse(id))
                .putExtra("kind", kind).putExtra("seconds", seconds).putExtra("subject", subject);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long millis = when.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        ids.add(id);
    }

    private static List<Integer> offsets(android.content.SharedPreferences prefs, String key, String oldKey) {
        java.util.LinkedHashSet<Integer> result = new java.util.LinkedHashSet<>();
        if (prefs.contains(key)) {
            for (String part : prefs.getString(key, "").split(",")) try {
                int seconds = Integer.parseInt(part.trim()); if (seconds > 0 && seconds <= 86400) result.add(seconds);
            } catch (NumberFormatException ignored) {}
        } else {
            int first = prefs.getInt(oldKey + "1", prefs.getInt(oldKey, 0)), second = prefs.getInt(oldKey + "2", 0);
            if (first > 0) result.add(first * 60); if (second > 0) result.add(second * 60);
        }
        return new ArrayList<>(result);
    }

    private ReminderScheduler() {}
}
