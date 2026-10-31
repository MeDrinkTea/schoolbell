package com.homik.bell;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class BellWidgetProvider extends AppWidgetProvider {
    static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateComponent(context, manager, new ComponentName(context, CompactBellWidgetProvider.class), false);
        updateComponent(context, manager, new ComponentName(context, DetailedBellWidgetProvider.class), true);
    }

    private static void updateComponent(Context c, AppWidgetManager manager, ComponentName component, boolean detailed) {
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        for (int id : ids) manager.updateAppWidget(id, views(c, detailed));
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        boolean detailed = getClass() == DetailedBellWidgetProvider.class;
        for (int id : ids) manager.updateAppWidget(id, views(context, detailed));
    }

    static RemoteViews views(Context c, boolean detailed) {
        int layout = detailed ? R.layout.widget_bell_detailed : R.layout.widget_bell_compact;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        LocalDateTime now = LocalDateTime.now();
        Target target = findTarget(c, now);
        String title = target == null ? AppStrings.t(c, "Добавьте расписание") : AppStrings.t(c, target.label);
        v.setTextViewText(R.id.widget_title, title);
        if (target == null) {
            v.setTextViewText(R.id.widget_countdown, "—");
        } else {
            long left = Math.max(0, Duration.between(now, target.at).toMillis());
            v.setTextViewText(R.id.widget_countdown, formatDuration(left));
            if (detailed) v.setTextViewText(R.id.widget_next, target.next);
        }
        if (detailed && target == null) v.setTextViewText(R.id.widget_next, AppStrings.t(c, "Нажмите, чтобы создать профиль расписания"));
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, detailed ? 702 : 701, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_root, pi);
        return v;
    }

    private static String formatDuration(long millis) {
        long total = Math.max(0, millis / 1000);
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static Target findTarget(Context c, LocalDateTime now) {
        List<Schedule.Slot> today = Schedule.forDay(now.getDayOfWeek(), c);
        LocalTime time = now.toLocalTime();
        for (int i = 0; i < today.size(); i++) {
            Schedule.Slot s = today.get(i);
            if (!time.isBefore(s.start) && time.isBefore(s.end)) {
                String next = i + 1 < today.size() ? AppStrings.t(c, "Следующий урок: ") + SubjectCatalog.label(c, today.get(i + 1).title)
                        : AppStrings.t(c, "Уроки на сегодня закончились");
                return new Target(now.toLocalDate().atTime(s.end), AppStrings.t(c, "До конца урока"), next);
            }
            if (time.isBefore(s.start)) {
                boolean breakTime = i > 0;
                String next = AppStrings.t(c, "Следующий урок: ") + SubjectCatalog.label(c, s.title);
                return new Target(now.toLocalDate().atTime(s.start), AppStrings.t(c, breakTime ? "Перемена · до звонка" : "До первого звонка"), next);
            }
        }
        for (int d = 1; d <= 7; d++) {
            LocalDate date = now.toLocalDate().plusDays(d);
            List<Schedule.Slot> slots = Schedule.forDay(date.getDayOfWeek(), c);
            if (!slots.isEmpty()) {
                String day = date.format(DateTimeFormatter.ofPattern("EEEE", AppStrings.locale(c)));
                return new Target(date.atTime(slots.get(0).start), AppStrings.t(c, "До следующего урока"),
                        AppStrings.t(c, "Следующий урок: ") + SubjectCatalog.label(c, slots.get(0).title) + " · " + day);
            }
        }
        return null;
    }

    private static final class Target {
        final LocalDateTime at; final String label, next;
        Target(LocalDateTime at, String label, String next) { this.at = at; this.label = label; this.next = next; }
    }
}
