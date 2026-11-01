package com.homik.bell;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private int INK, MUTED, PURPLE, BG, SURFACE, DIVIDER, BADGE_BG, BADGE_FG;
    private static final String[] DAYS = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
    private TextView statusLabel, actionButton;
    private TextView countdownTitle, countdownValue, countdownNext;
    private LinearLayout page;
    private boolean darkTheme;
    private String lastProfileId;
    private String lastLanguage;
    private String lastSubjectLanguage;
    private String lastVacationMode;
    private String lastBlockVisibility;
    private int lastScheduleRevision;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTicker = new Runnable() {
        @Override public void run() {
            updateCountdown();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        AppStrings.syncSystemLocale(this);
        ReminderScheduler.refresh(this);
        lastProfileId = Schedule.selectedProfileId(this);
        lastLanguage = getSharedPreferences("settings", MODE_PRIVATE).getString("language", "system");
        lastSubjectLanguage = getSharedPreferences("settings", MODE_PRIVATE).getString("subjectLanguage", "app");
        lastVacationMode = getSharedPreferences("settings", MODE_PRIVATE).getString("vacationMode", "trimesters");
        lastScheduleRevision = Schedule.revision(this);
        lastBlockVisibility = blockVisibilitySignature();
        applyPalette();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8);
        buildUi();
        if (state == null) {
            android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            boolean firstScheduleTip = !prefs.getBoolean("firstScheduleTipShown", false);
            boolean firstBatteryTip = !prefs.getBoolean("firstBatteryTipShown", false);
            if (firstScheduleTip) {
                prefs.edit().putBoolean("firstScheduleTipShown", true).apply();
                new Handler(Looper.getMainLooper()).post(() -> showFirstScheduleTip(firstBatteryTip));
            } else if (firstBatteryTip) {
                new Handler(Looper.getMainLooper()).post(this::showFirstBatteryTip);
            }
        }
        if (state == null && getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false))
            refreshNotificationText();
    }

    private void showFirstScheduleTip(boolean includeBatteryTip) {
        if (isFinishing() || isDestroyed()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(AppStrings.t(this, "Добавь своё расписание"))
                .setMessage(AppStrings.t(this, includeBatteryTip
                        ? "Заполни расписание в настройках: открой «Мои расписания» и добавь свой профиль. Чтобы уведомление обновлялось в фоне, разреши работу без ограничений батареи."
                        : "Заполни расписание в настройках: открой «Мои расписания» и добавь свой профиль."))
                .setPositiveButton(AppStrings.t(this, "Заполнить расписание"), (dialog, which) -> {
                    Intent intent = new Intent(this, SettingsActivity.class).putExtra("openSchedules", true);
                    startActivity(intent);
                })
                .setNegativeButton(AppStrings.t(this, "Позже"), null);
        if (includeBatteryTip) {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("firstBatteryTipShown", true).apply();
            builder.setNeutralButton(AppStrings.t(this, "Настройки батареи"), (dialog, which) -> openBatterySettings());
        }
        builder.show();
    }

    private void showFirstBatteryTip() {
        if (isFinishing() || isDestroyed()) return;
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("firstBatteryTipShown", true).apply();
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
            if (power != null && power.isIgnoringBatteryOptimizations(getPackageName())) return;
        }
        new AlertDialog.Builder(this)
                .setTitle(AppStrings.t(this, "Фоновая работа уведомления"))
                .setMessage(AppStrings.t(this, "Чтобы уведомление обновлялось в фоне, разреши приложению работу без ограничений батареи."))
                .setPositiveButton(AppStrings.t(this, "Настройки батареи"), (dialog, which) -> openBatterySettings())
                .setNegativeButton(AppStrings.t(this, "Позже"), null)
                .show();
    }

    private void openBatterySettings() {
        if (Build.VERSION.SDK_INT < 23) return;
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        AppPalette palette = AppPalette.from(this);
        String profileId = Schedule.selectedProfileId(this);
        int revision = Schedule.revision(this);
        boolean scheduleChanged = !profileId.equals(lastProfileId) || revision != lastScheduleRevision;
        String language = getSharedPreferences("settings", MODE_PRIVATE).getString("language", "system");
        String subjectLanguage = getSharedPreferences("settings", MODE_PRIVATE).getString("subjectLanguage", "app");
        String vacationMode = getSharedPreferences("settings", MODE_PRIVATE).getString("vacationMode", "trimesters");
        boolean languageChanged = !language.equals(lastLanguage);
        boolean subjectLanguageChanged = !subjectLanguage.equals(lastSubjectLanguage);
        boolean vacationModeChanged = !vacationMode.equals(lastVacationMode);
        boolean blockVisibilityChanged = !blockVisibilitySignature().equals(lastBlockVisibility);
        if (darkTheme != palette.dark || scheduleChanged || languageChanged || vacationModeChanged || subjectLanguageChanged || blockVisibilityChanged) {
            lastProfileId = profileId; lastScheduleRevision = revision;
            lastLanguage = language; lastVacationMode = vacationMode;
            lastSubjectLanguage = subjectLanguage;
            lastBlockVisibility = blockVisibilitySignature();
            applyPalette(); buildUi();
        }
        if (scheduleChanged || vacationModeChanged || subjectLanguageChanged || blockVisibilityChanged) refreshNotificationText();
        if (blockVisibilityChanged) BellWidgetProvider.refresh(this);
        refreshStatus(); clockHandler.removeCallbacks(clockTicker); clockTicker.run();
    }
    @Override protected void onPause() { clockHandler.removeCallbacks(clockTicker); super.onPause(); }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(30));
        page.setBackgroundColor(BG);

        LinearLayout brand = row();
        ImageView mark = new ImageView(this); mark.setImageResource(R.drawable.ic_brand_bell); mark.setPadding(dp(14), dp(14), dp(14), dp(14));
        mark.setBackground(round(PURPLE, 18));
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(54), dp(54)); brand.addView(mark, markLp);
        LinearLayout brandCopy = column(); brandCopy.setPadding(dp(13), dp(2), 0, 0);
        brandCopy.addView(text("Звонки", 25, true));
        brandCopy.addView(text("Школьное расписание", 14, false, MUTED));
        brand.addView(brandCopy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settingsButton = text("Настройки  ⚙", 13, true, PURPLE);
        settingsButton.setGravity(Gravity.CENTER); settingsButton.setPadding(dp(12), dp(10), dp(12), dp(10));
        settingsButton.setBackground(round(BADGE_BG, 15));
        brand.addView(settingsButton, new LinearLayout.LayoutParams(-2, -2));
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        page.addView(brand);

        Locale locale = AppStrings.locale(this);
        TextView date = text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), 14, true, PURPLE);
        date.setPadding(0, dp(20), 0, dp(10)); page.addView(date);

        addCountdownCard();

        addVacationCard();

        LinearLayout control = column(); control.setPadding(dp(18), dp(17), dp(18), dp(17));
        control.setBackground(round(SURFACE, 20)); control.setElevation(dp(2));
        LinearLayout controlTop = row();
        TextView bell = text("●", 12, true, 0xFFFFFFFF); bell.setGravity(Gravity.CENTER); bell.setBackground(round(BADGE_BG, 12)); bell.setTextColor(BADGE_FG);
        controlTop.addView(bell, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView label = text("Уведомление", 18, true); LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, -2, 1); labelLp.leftMargin = dp(10); controlTop.addView(label, labelLp);
        statusLabel = text("Проверяю состояние…", 12, true, MUTED); statusLabel.setGravity(Gravity.CENTER_VERTICAL); controlTop.addView(statusLabel);
        control.addView(controlTop);
        actionButton = text("Включить уведомление", 15, true, 0xFFFFFFFF); actionButton.setGravity(Gravity.CENTER);
        actionButton.setPadding(dp(16), 0, dp(16), 0); actionButton.setBackground(round(PURPLE, 14)); actionButton.setMinHeight(dp(48));
        actionButton.setOnClickListener(v -> toggleClock());
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(-1, dp(48)); actionLp.topMargin = dp(15); control.addView(actionButton, actionLp);
        TextView hint = text("Покажет, сколько осталось до звонка и какой урок будет дальше.", 13, false, MUTED);
        hint.setPadding(0, dp(11), 0, 0); control.addView(hint);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2); cardLp.bottomMargin = dp(22); page.addView(control, cardLp);

        LinearLayout sectionTitle = row();
        TextView scheduleHeading = text("Расписание", 23, true); sectionTitle.addView(scheduleHeading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView activeProfile = text(AppStrings.t(this, Schedule.selectedProfileName(this)) + "  ▾", 12, true, BADGE_FG);
        activeProfile.setGravity(Gravity.CENTER); activeProfile.setPadding(dp(10), dp(8), dp(10), dp(8));
        activeProfile.setBackground(round(BADGE_BG, 15));
        sectionTitle.addView(activeProfile);
        activeProfile.setOnClickListener(v -> showSchedulePicker());
        page.addView(sectionTitle);
        TextView sub = text("Звонки и уроки на неделю", 14, false, MUTED); sub.setPadding(0, dp(5), 0, dp(14)); page.addView(sub);

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(i == 6 ? 7 : i + 1);
            addDayCard(day, DAYS[i], day == today.getDayOfWeek());
        }
        applyHomeBlockOrder();
        scroll.addView(page);
        setContentView(scroll);
    }

    private void applyHomeBlockOrder() {
        if (page.getChildCount() < 7) return;
        View brand = page.getChildAt(0), date = page.getChildAt(1);
        View countdown = page.getChildAt(2), vacation = page.getChildAt(3), notification = page.getChildAt(4);
        LinearLayout schedule = column();
        while (page.getChildCount() > 5) {
            View child = page.getChildAt(5); page.removeViewAt(5); schedule.addView(child);
        }
        java.util.Map<String, View> blocks = new java.util.HashMap<>();
        blocks.put("countdown", countdown); blocks.put("vacation", vacation);
        blocks.put("notification", notification); blocks.put("schedule", schedule);
        String saved = getSharedPreferences("settings", MODE_PRIVATE).getString("homeBlockOrder", "countdown,vacation,notification,schedule");
        page.removeAllViews(); page.addView(brand); page.addView(date);
        for (String key : saved.split(",")) {
            View block = blocks.remove(key);
            if (block != null && !isBlockHidden("homeBlockOrder", key)) page.addView(block);
        }
        for (java.util.Map.Entry<String, View> entry : blocks.entrySet())
            if (!isBlockHidden("homeBlockOrder", entry.getKey())) page.addView(entry.getValue());
    }

    private boolean isBlockHidden(String orderKey, String blockId) {
        Set<String> hidden = getSharedPreferences("settings", MODE_PRIVATE)
                .getStringSet("hidden_" + orderKey, java.util.Collections.emptySet());
        return hidden.contains(blockId);
    }

    private String blockVisibilitySignature() {
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        return new java.util.TreeSet<>(prefs.getStringSet("hidden_homeBlockOrder", java.util.Collections.emptySet()))
                + "|" + new java.util.TreeSet<>(prefs.getStringSet("hidden_notificationBlockOrder", java.util.Collections.emptySet()));
    }

    private void addDayCard(DayOfWeek day, String title, boolean today) {
        LinearLayout card = column(); card.setPadding(dp(15), dp(14), dp(15), dp(10)); card.setBackground(round(SURFACE, 18)); card.setElevation(dp(1));
        LinearLayout heading = row();
        TextView name = text(title, 17, true); heading.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        if (today) {
            TextView badge = text("СЕГОДНЯ", 10, true, BADGE_FG); badge.setPadding(dp(8), dp(5), dp(8), dp(5)); badge.setBackground(round(BADGE_BG, 10)); heading.addView(badge);
        }
        card.addView(heading);
        java.util.List<Schedule.Slot> slots = Schedule.forDay(day, this);
        if (slots.isEmpty()) {
            TextView empty = text("Выходной", 13, false, MUTED); empty.setPadding(0, dp(10), 0, dp(7)); card.addView(empty);
        } else {
            for (Schedule.Slot slot : slots) {
                LinearLayout item = row(); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(0, dp(9), 0, dp(9));
                TextView time = text(slot.start.format(AppStrings.clockFormatter(this)) + "–" + slot.end.format(AppStrings.clockFormatter(this)), 12, true, MUTED);
                time.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                item.addView(time, new LinearLayout.LayoutParams(dp(112), -2));
                TextView subject = text(SubjectCatalog.label(this, slot.title), 14, false, INK);
                item.addView(subject, new LinearLayout.LayoutParams(0, -2, 1));
                card.addView(item);
                if (slot != slots.get(slots.size() - 1)) {
                    View divider = new View(this); divider.setBackgroundColor(DIVIDER);
                    LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, dp(1)); line.leftMargin = dp(112); card.addView(divider, line);
                }
            }
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); page.addView(card, lp);
    }

    private void addVacationCard() {
        LocalDate today = LocalDate.now();
        SchoolCalendar.Vacation vacation = SchoolCalendar.next(today, this);
        boolean alreadyOnBreak = vacation.contains(today);
        long days = alreadyOnBreak ? vacation.daysLeftIncludingToday(today) : vacation.daysUntilStart(today);
        LinearLayout card = column(); card.setPadding(dp(19), dp(17), dp(19), dp(17)); card.setBackground(round(BADGE_BG, 21));
        card.addView(text(alreadyOnBreak ? "СЕЙЧАС ОТДЫХАЕШЬ" : "ДО БЛИЖАЙШИХ КАНИКУЛ", 11, true, BADGE_FG));
        String holidayCount;
        if (AppStrings.english(this)) {
            String units = days == 1 ? "day" : "days";
            holidayCount = alreadyOnBreak ? "" + days + " " + units + " left" : days == 0 ? "Today!" : days + " " + units;
        } else {
            holidayCount = alreadyOnBreak ? AppStrings.t(this, "Ещё ") + days + " " + SchoolCalendar.dayWord(this, days) : days == 0 ? AppStrings.t(this, "Уже сегодня!") : days + " " + SchoolCalendar.dayWord(this, days);
        }
        TextView count = text(holidayCount, 30, true, PURPLE);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-1, -2); countLp.topMargin = dp(6); card.addView(count, countLp);
        Locale locale = AppStrings.locale(this);
        String range = vacation.start.format(DateTimeFormatter.ofPattern("d MMMM", locale)) + " – " + vacation.end.format(DateTimeFormatter.ofPattern("d MMMM", locale));
        TextView detail = text(AppStrings.t(this, vacation.name) + " · " + range, 14, false, INK);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2); detailLp.topMargin = dp(4); card.addView(detail, detailLp);
        TextView cycle = text(SchoolCalendar.quarters(this) ? "Расчёт по четвертям" : "Расчёт по циклу 5/1 · после Нового года 6/1", 11, false, MUTED);
        LinearLayout.LayoutParams cycleLp = new LinearLayout.LayoutParams(-1, -2); cycleLp.topMargin = dp(12); card.addView(cycle, cycleLp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(15); page.addView(card, lp);
    }

    private void addCountdownCard() {
        LinearLayout card = column();
        card.setPadding(dp(19), dp(17), dp(19), dp(17));
        card.setBackground(round(SURFACE, 20));
        card.setElevation(dp(2));
        countdownTitle = text("До ближайшего звонка", 14, true, MUTED);
        card.addView(countdownTitle);
        countdownValue = text("00:00", 38, true, PURPLE);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(-1, -2);
        valueLp.topMargin = dp(5);
        card.addView(countdownValue, valueLp);
        countdownNext = text("", 14, false, INK);
        LinearLayout.LayoutParams nextLp = new LinearLayout.LayoutParams(-1, -2);
        nextLp.topMargin = dp(4);
        card.addView(countdownNext, nextLp);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(15);
        page.addView(card, cardLp);
    }

    private void refreshNotificationText() {
        if (!getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false)) return;
        Intent intent = new Intent(this, BellService.class).setAction(BellService.ACTION_REFRESH_TEXT);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void showSchedulePicker() {
        java.util.List<Schedule.Profile> profiles = Schedule.profiles(this);
        String selectedId = Schedule.selectedProfileId(this);
        LinearLayout choices = column();
        choices.setPadding(dp(20), dp(8), dp(20), dp(4));
        for (Schedule.Profile profile : profiles) {
            boolean selected = profile.id.equals(selectedId);
            TextView option = text(AppStrings.t(this, profile.name), 15, true, selected ? BADGE_FG : INK);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(dp(16), dp(14), dp(16), dp(14));
            option.setBackground(round(selected ? BADGE_BG : BG, 15));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(6); choices.addView(option, lp);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(AppStrings.t(this, "Выбрать расписание"))
                .setView(choices)
                .setNegativeButton(AppStrings.t(this, "Закрыть"), null)
                .create();
        for (int i = 0; i < choices.getChildCount(); i++) {
            View option = choices.getChildAt(i);
            option.setOnClickListener(v -> {
                Schedule.Profile profile = profiles.get(choices.indexOfChild(v));
                Schedule.selectProfile(this, profile.id);
                lastProfileId = Schedule.selectedProfileId(this);
                dialog.dismiss(); buildUi(); refreshStatus(); refreshNotificationText();
            });
        }
        dialog.show();
    }

    private void updateCountdown() {
        if (countdownTitle == null || countdownValue == null || countdownNext == null) return;
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        java.util.List<Schedule.Slot> todaySlots = Schedule.forDay(today, this);
        for (int i = 0; i < todaySlots.size(); i++) {
            Schedule.Slot slot = todaySlots.get(i);
            if (!now.toLocalTime().isBefore(slot.start) && now.toLocalTime().isBefore(slot.end)) {
                setCountdown("До конца урока", now, now.toLocalDate().atTime(slot.end),
                        SubjectCatalog.label(this, slot.title) + (i + 1 < todaySlots.size() ? " · следующий: " + SubjectCatalog.label(this, todaySlots.get(i + 1).title) : ""));
                return;
            }
            if (now.toLocalTime().isBefore(slot.start)) {
                boolean breakTime = i > 0;
                setCountdown(breakTime ? "Перемена · до звонка" : "До первого звонка", now,
                        now.toLocalDate().atTime(slot.start), "Следующий урок: " + SubjectCatalog.label(this, slot.title));
                return;
            }
        }

        for (int offset = 1; offset <= 7; offset++) {
            LocalDate date = now.toLocalDate().plusDays(offset);
            java.util.List<Schedule.Slot> slots = Schedule.forDay(date.getDayOfWeek(), this);
            if (!slots.isEmpty()) {
                Schedule.Slot next = slots.get(0);
                String dayName = date.format(DateTimeFormatter.ofPattern("EEEE", AppStrings.locale(this)));
                setCountdown(todaySlots.isEmpty() ? "Сегодня занятий нет" : "Уроки на сегодня закончились",
                        now, date.atTime(next.start), "Следующий урок: " + SubjectCatalog.label(this, next.title) + " · " + dayName);
                return;
            }
        }
    }

    private void setCountdown(String title, LocalDateTime now, LocalDateTime target, String next) {
        long seconds = Math.max(0, Duration.between(now, target).getSeconds());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        String value;
        if (days > 0 && AppStrings.english(this)) {
            value = String.format(Locale.ENGLISH, "%d %s %02d:%02d:%02d", days, days == 1 ? "day" : "days", hours, minutes, remainingSeconds);
        } else if (days > 0) {
            value = String.format(Locale.getDefault(), "%d %s %02d:%02d:%02d", days, AppStrings.t(this, "дн"), hours, minutes, remainingSeconds);
        } else {
            value = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds);
        }
        countdownTitle.setText(AppStrings.t(this, title));
        countdownValue.setText(value);
        countdownNext.setText(next.replace("Следующий урок: ", AppStrings.t(this, "Следующий урок: "))
                .replace(" · следующий: ", AppStrings.t(this, " · следующий: ")));
    }

    private void toggleClock() {
        boolean enabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false);
        if (enabled) {
            stopService(new Intent(this, BellService.class));
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("enabled", false).apply();
        } else {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8); return;
            }
            Intent intent = new Intent(this, BellService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("enabled", true).apply();
        }
        refreshStatus();
    }

    private void refreshStatus() {
        if (statusLabel == null || actionButton == null) return;
        boolean enabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false);
        statusLabel.setText(AppStrings.t(this, enabled ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО"));
        statusLabel.setTextColor(enabled ? (darkTheme ? 0xFF52CAA1 : 0xFF258A65) : MUTED);
        actionButton.setText(AppStrings.t(this, enabled ? "Остановить уведомление" : "Включить уведомление"));
        actionButton.setBackground(round(enabled ? 0xFF30334A : PURPLE, 14));
    }

    private void applyPalette() {
        AppPalette palette = AppPalette.from(this);
        darkTheme = palette.dark;
        setTheme(darkTheme ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        BG = palette.background; SURFACE = palette.surface; INK = palette.text; MUTED = palette.muted;
        PURPLE = palette.accent; DIVIDER = palette.divider; BADGE_BG = palette.accentContainer; BADGE_FG = palette.onAccentContainer;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = darkTheme ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26 && !darkTheme) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private TextView text(String value, int size, boolean bold) { return text(value, size, bold, INK); }
    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this); view.setText(AppStrings.t(this, value)); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private LinearLayout row() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.HORIZONTAL); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
}


