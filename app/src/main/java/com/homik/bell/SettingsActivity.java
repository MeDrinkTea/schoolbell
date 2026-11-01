package com.homik.bell;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SettingsActivity extends Activity {
    private static final int REQUEST_IMPORT_TEXT = 31;
    private int BG, SURFACE, TEXT, MUTED, ACCENT, DIVIDER, TONAL, ON_TONAL;
    private LinearLayout page;
    private ScrollView settingsScroll;
    private int restoredScrollY;
    private EditText customText;
    private TextView customTextSwitch;
    private TextView customTextCount;
    private int themeMode;
    private LinearLayout lessonReminderRows, breakReminderRows;
    private final List<EditText> lessonReminderInputs = new ArrayList<>(), breakReminderInputs = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) restoredScrollY = state.getInt("settingsScrollY", 0);
        else if (getIntent().getBooleanExtra("openSchedules", false)) restoredScrollY = Integer.MAX_VALUE;
        themeMode = AppPalette.themeMode(this);
        applyPalette();
        buildUi();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (settingsScroll != null) outState.putInt("settingsScrollY", settingsScroll.getScrollY());
        super.onSaveInstanceState(outState);
    }

    private void buildUi() {
        int scrollY = settingsScroll == null ? restoredScrollY : settingsScroll.getScrollY();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(32));
        page.setBackgroundColor(BG);

        LinearLayout top = row();
        TextView back = text("‹", 32, false, ACCENT);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        back.setOnClickListener(v -> finish());
        TextView heading = text("Настройки", 26, true);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-2, -2);
        headingLp.leftMargin = dp(10);
        top.addView(heading, headingLp);
        page.addView(top);

        addThemeCard();
        addTimeFormatCard();
        addLanguageCard();
        addSubjectLanguageCard();
        addBlockOrderCard();
        addCustomTextCard();
        addVacationModeCard();
        addReminderCard();
        addSchedulesCard();

        scroll.addView(page);
        settingsScroll = scroll;
        setContentView(scroll);
        final int targetScrollY = scrollY;
        scroll.post(() -> {
            if (targetScrollY == Integer.MAX_VALUE && page.getChildCount() > 0)
                scroll.scrollTo(0, page.getChildAt(page.getChildCount() - 1).getTop());
            else scroll.scrollTo(0, targetScrollY);
        });
    }

    private void addVacationModeCard() {
        LinearLayout card = card();
        card.addView(text("Календарь каникул", 19, true));
        TextView description = text("Выбери, по какому учебному календарю считать ближайшие каникулы.", 13, false, MUTED);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2); descLp.topMargin = dp(5); card.addView(description, descLp);
        String selected = getSharedPreferences("settings", MODE_PRIVATE).getString("vacationMode", "trimesters");
        LinearLayout options = row();
        addVacationModeOption(options, "Триместры", "trimesters", selected);
        addVacationModeOption(options, "Четверти", "quarters", selected);
        LinearLayout.LayoutParams optionsLp = new LinearLayout.LayoutParams(-1, dp(46)); optionsLp.topMargin = dp(12); card.addView(options, optionsLp);
        TextView note = text("Даты четвертных каникул рассчитаны по типовым неделям учебного года.", 12, false, MUTED);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2); noteLp.topMargin = dp(8); card.addView(note, noteLp);
        addCardToPage(card, 14);
    }

    private void addBlockOrderCard() {
        LinearLayout card = card();
        card.addView(text("Порядок блоков", 19, true));
        card.addView(text("Меняй порядок стрелками. Он применяется к уведомлению и главному экрану.", 13, false, MUTED));
        card.addView(text("В уведомлении", 15, true));
        addOrderList(card, "notificationBlockOrder", new String[]{"status", "next", "holiday"},
                new String[]{"Текущий урок или перемена", "Следующий урок", "Каникулы"});
        card.addView(text("На главном экране", 15, true));
        addOrderList(card, "homeBlockOrder", new String[]{"countdown", "vacation", "notification", "schedule"},
                new String[]{"Таймер", "Каникулы", "Уведомление", "Расписание"});
        addCardToPage(card, 14);
    }

    private void addOrderList(LinearLayout card, String key, String[] defaults, String[] labels) {
        String saved = getSharedPreferences("settings", MODE_PRIVATE).getString(key, String.join(",", defaults));
        List<String> parsed = new ArrayList<>(java.util.Arrays.asList(saved.split(",")));
        if (parsed.size() != defaults.length || !parsed.containsAll(java.util.Arrays.asList(defaults))) parsed = new ArrayList<>(java.util.Arrays.asList(defaults));
        final List<String> order = parsed;
        for (int i = 0; i < order.size(); i++) {
            final int index = i;
            LinearLayout line = row();
            TextView name = text(labels[java.util.Arrays.asList(defaults).indexOf(order.get(i))], 13, true);
            line.addView(name, new LinearLayout.LayoutParams(0, dp(42), 1));
            addOrderArrow(line, "↑", index > 0, () -> swapOrder(key, order, index, index - 1));
            addOrderArrow(line, "↓", index + 1 < order.size(), () -> swapOrder(key, order, index, index + 1));
            String blockId = order.get(i);
            Set<String> hidden = getSharedPreferences("settings", MODE_PRIVATE)
                    .getStringSet("hidden_" + key, java.util.Collections.emptySet());
            boolean isHidden = hidden.contains(blockId);
            ImageView visibility = new ImageView(this);
            visibility.setImageResource(isHidden ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            visibility.setColorFilter(isHidden ? MUTED : ACCENT);
            visibility.setPadding(dp(9), dp(9), dp(9), dp(9));
            visibility.setBackground(round(BG, 10));
            visibility.setContentDescription(AppStrings.t(this, isHidden ? "Показать блок" : "Скрыть блок"));
            LinearLayout.LayoutParams visibilityLp = new LinearLayout.LayoutParams(dp(38), dp(38));
            visibilityLp.leftMargin = dp(5); line.addView(visibility, visibilityLp);
            visibility.setOnClickListener(v -> toggleBlockVisibility(key, blockId));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44)); lp.topMargin = dp(4); card.addView(line, lp);
        }
    }

    private void toggleBlockVisibility(String key, String blockId) {
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Set<String> hidden = new HashSet<>(prefs.getStringSet("hidden_" + key, java.util.Collections.emptySet()));
        if (!hidden.add(blockId)) hidden.remove(blockId);
        prefs.edit().putStringSet("hidden_" + key, hidden).apply();
        buildUi(); refreshNotificationText(); BellWidgetProvider.refresh(this);
    }

    private void addOrderArrow(LinearLayout row, String glyph, boolean enabled, Runnable action) {
        TextView button = text(glyph, 18, true, enabled ? ACCENT : MUTED);
        button.setGravity(Gravity.CENTER); button.setBackground(round(BG, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(38)); lp.leftMargin = dp(5); row.addView(button, lp);
        button.setEnabled(enabled); button.setOnClickListener(v -> action.run());
    }

    private void swapOrder(String key, List<String> order, int from, int to) {
        java.util.Collections.swap(order, from, to);
        getSharedPreferences("settings", MODE_PRIVATE).edit().putString(key, String.join(",", order)).apply();
        buildUi(); refreshNotificationText(); BellWidgetProvider.refresh(this);
    }

    private void addTimeFormatCard() {
        LinearLayout card = card();
        card.addView(text("Формат времени", 19, true));
        LinearLayout options = row();
        String selected = getSharedPreferences("settings", MODE_PRIVATE).getString("timeFormat", "system");
        if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("twelveHourTime", false)) selected = "12";
        addTimeFormatOption(options, "Как в системе", "system", selected);
        addTimeFormatOption(options, "24 часа", "24", selected);
        addTimeFormatOption(options, "12 часов", "12", selected);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
        lp.topMargin = dp(12); card.addView(options, lp);
        addCardToPage(card, 14);
    }

    private void addTimeFormatOption(LinearLayout row, String label, String value, String selected) {
        boolean active = selected.equals(value);
        TextView option = text(label, 12, true, active ? ON_TONAL : MUTED);
        option.setGravity(Gravity.CENTER); option.setBackground(round(active ? TONAL : BG, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) lp.leftMargin = dp(7);
        row.addView(option, lp);
        option.setOnClickListener(v -> {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("timeFormat", value).remove("twelveHourTime").apply();
            buildUi(); refreshNotificationText(); BellWidgetProvider.refresh(this);
        });
    }

    private void addVacationModeOption(LinearLayout options, String label, String value, String selected) {
        boolean active = value.equals(selected);
        TextView button = text(label, 13, true, active ? ON_TONAL : MUTED);
        button.setGravity(Gravity.CENTER); button.setBackground(round(active ? TONAL : BG, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        if (options.getChildCount() > 0) lp.leftMargin = dp(7);
        options.addView(button, lp);
        button.setOnClickListener(v -> {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("vacationMode", value).apply();
            buildUi(); refreshNotificationText();
        });
    }

    private void addLanguageCard() {
        LinearLayout card = card();
        card.addView(text("Язык / Language", 19, true));
        card.addView(text("Выбери язык интерфейса приложения.", 13, false, MUTED));
        String[] labels = {"Как в системе", "Русский", "English", "Українська", "Беларуская"};
        String[] codes = {"system", "ru", "en", "uk", "be"};
        String current = getSharedPreferences("settings", MODE_PRIVATE).getString("language", "system");
        for (int i = 0; i < codes.length; i++) {
            final String code = codes[i];
            TextView option = text(labels[i], 14, true, code.equals(current) ? ON_TONAL : TEXT);
            option.setPadding(dp(12), dp(11), dp(12), dp(11));
            option.setBackground(round(code.equals(current) ? TONAL : BG, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(7); card.addView(option, lp);
            option.setOnClickListener(v -> {
                getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", code).apply();
                AppStrings.syncSystemLocale(this);
                buildUi(); refreshNotificationText(); BellWidgetProvider.refresh(this);
            });
        }
        addCardToPage(card, 14);
    }

    private void addSubjectLanguageCard() {
        LinearLayout card = card();
        card.addView(text("Язык названий предметов", 19, true));
        TextView description = text("Меняет язык только для известных названий предметов. Свои названия остаются как введены.", 13, false, MUTED);
        LinearLayout.LayoutParams descriptionLp = new LinearLayout.LayoutParams(-1, -2);
        descriptionLp.topMargin = dp(5);
        card.addView(description, descriptionLp);

        String[] codes = {"app", "ru", "en", "uk", "be"};
        String[] labels = {"Как в приложении", "Русский", "English", "Українська", "Беларуская"};
        String selected = getSharedPreferences("settings", MODE_PRIVATE).getString("subjectLanguage", "app");
        for (int i = 0; i < codes.length; i++) {
            final String code = codes[i];
            boolean active = code.equals(selected);
            TextView option = text(labels[i], 14, true, active ? ON_TONAL : TEXT);
            option.setPadding(dp(12), dp(11), dp(12), dp(11));
            option.setBackground(round(active ? TONAL : BG, 12));
            LinearLayout.LayoutParams optionLp = new LinearLayout.LayoutParams(-1, -2);
            optionLp.topMargin = dp(7); card.addView(option, optionLp);
            option.setOnClickListener(v -> {
                getSharedPreferences("settings", MODE_PRIVATE).edit().putString("subjectLanguage", code).apply();
                BellWidgetProvider.refresh(this);
                refreshNotificationText();
                buildUi();
            });
        }
        addCardToPage(card, 14);
    }

    private String subjectLanguageLabel(String code) {
        switch (code) {
            case "ru": return AppStrings.t(this, "Русский");
            case "uk": return AppStrings.t(this, "Українська");
            case "be": return AppStrings.t(this, "Беларуская");
            case "en": return "English";
            default: return AppStrings.t(this, "Как в приложении");
        }
    }

    private void addReminderCard() {
        LinearLayout card = card();
        card.addView(text("Звуковые напоминания", 19, true));
        TextView desc = text("Добавляй сколько нужно сигналов отдельно для уроков и перемен. Формат — минуты:секунды (например, 7:30); можно указать целое число минут. Пустая строка не используется.", 13, false, MUTED);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2); descLp.topMargin = dp(5); card.addView(desc, descLp);
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        lessonReminderInputs.clear(); breakReminderInputs.clear();
        lessonReminderRows = column(); breakReminderRows = column();
        addReminderGroup(card, "Напоминания до конца урока", lessonReminderRows, lessonReminderInputs,
                readReminderOffsets(prefs, "lessonReminderTimesSec", "lessonReminderMinutes"));
        addReminderGroup(card, "Напоминания до конца перемены", breakReminderRows, breakReminderInputs,
                readReminderOffsets(prefs, "breakReminderTimesSec", "breakReminderMinutes"));
        TextView save = text("Сохранить напоминания", 14, true, ON_TONAL); save.setGravity(Gravity.CENTER); save.setBackground(round(TONAL, 13));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(46)); saveLp.topMargin = dp(12); card.addView(save, saveLp);
        save.setOnClickListener(v -> {
            List<Integer> lessons = collectReminderOffsets(lessonReminderInputs);
            List<Integer> breaks = collectReminderOffsets(breakReminderInputs);
            if (lessons == null || breaks == null) { Toast.makeText(this, AppStrings.t(this, "Формат: минуты:секунды, например 7:30"), Toast.LENGTH_SHORT).show(); return; }
            prefs.edit().putString("lessonReminderTimesSec", joinOffsets(lessons)).putString("breakReminderTimesSec", joinOffsets(breaks))
                    .remove("lessonReminderMinutes1").remove("lessonReminderMinutes2").remove("breakReminderMinutes1").remove("breakReminderMinutes2")
                    .remove("lessonReminderMinutes").remove("breakReminderMinutes").apply();
            ReminderScheduler.refresh(this);
            Toast.makeText(this, "Напоминания сохранены", Toast.LENGTH_SHORT).show();
        });
        if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
            TextView permission = text("Разрешить точные напоминания", 14, true, ACCENT); permission.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams permissionLp = new LinearLayout.LayoutParams(-1, dp(46)); permissionLp.topMargin = dp(8); card.addView(permission, permissionLp);
            permission.setOnClickListener(v -> {
                try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + getPackageName()))); }
                catch (Exception e) { Toast.makeText(this, "Открой доступ «Будильники и напоминания» в настройках приложения", Toast.LENGTH_LONG).show(); }
            });
        }
        addCardToPage(card, 14);
    }

    private void addReminderGroup(LinearLayout card, String title, LinearLayout rows, List<EditText> inputs, List<Integer> saved) {
        TextView heading = text(title, 15, true);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-1, -2); headingLp.topMargin = dp(13); card.addView(heading, headingLp);
        LinearLayout.LayoutParams rowsLp = new LinearLayout.LayoutParams(-1, -2); rowsLp.topMargin = dp(5); card.addView(rows, rowsLp);
        for (int seconds : saved) addReminderRow(rows, inputs, formatOffset(seconds));
        if (saved.isEmpty()) addReminderRow(rows, inputs, "");
        TextView add = text("＋  Добавить время", 13, true, ACCENT); add.setGravity(Gravity.CENTER); add.setBackground(round(BG, 12));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(40)); addLp.topMargin = dp(5); card.addView(add, addLp);
        add.setOnClickListener(v -> addReminderRow(rows, inputs, ""));
    }

    private void addReminderRow(LinearLayout rows, List<EditText> inputs, String value) {
        LinearLayout item = row();
        EditText input = new EditText(this); input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint(AppStrings.t(this, "Например, 15:00 или 7:30")); input.setText(value); input.setTextColor(TEXT); input.setHintTextColor(MUTED);
        input.setPadding(dp(12), dp(6), dp(12), dp(6)); input.setBackground(round(BG, 12));
        item.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView remove = text("×", 24, false, ACCENT); remove.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams removeLp = new LinearLayout.LayoutParams(dp(44), dp(48)); removeLp.leftMargin = dp(7); item.addView(remove, removeLp);
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(-1, dp(48)); itemLp.topMargin = dp(6); rows.addView(item, itemLp);
        inputs.add(input); remove.setOnClickListener(v -> { rows.removeView(item); inputs.remove(input); });
    }

    private List<Integer> readReminderOffsets(android.content.SharedPreferences prefs, String key, String oldKey) {
        List<Integer> result = new ArrayList<>();
        if (prefs.contains(key)) {
            for (String token : prefs.getString(key, "").split(",")) try {
                int seconds = Integer.parseInt(token.trim()); if (seconds > 0 && !result.contains(seconds)) result.add(seconds);
            } catch (NumberFormatException ignored) {}
        } else {
            int first = prefs.getInt(oldKey + "1", prefs.getInt(oldKey, 0)), second = prefs.getInt(oldKey + "2", 0);
            if (first > 0) result.add(first * 60); if (second > 0 && second != first) result.add(second * 60);
        }
        return result;
    }

    private String formatOffset(int seconds) { return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60); }

    private List<Integer> collectReminderOffsets(List<EditText> inputs) {
        java.util.LinkedHashSet<Integer> result = new java.util.LinkedHashSet<>();
        for (EditText input : inputs) {
            String value = input.getText().toString().trim(); if (value.isEmpty()) continue;
            try {
                int seconds;
                if (value.matches("\\d{1,4}:\\d{2}")) {
                    String[] parts = value.split(":"); int sec = Integer.parseInt(parts[1]); if (sec > 59) return null;
                    seconds = Integer.parseInt(parts[0]) * 60 + sec;
                } else if (value.matches("\\d{1,4}")) seconds = Integer.parseInt(value) * 60;
                else return null;
                if (seconds <= 0 || seconds > 86400) return null;
                result.add(seconds);
            } catch (NumberFormatException e) { return null; }
        }
        return new ArrayList<>(result);
    }

    private String joinOffsets(List<Integer> offsets) {
        StringBuilder out = new StringBuilder();
        for (int value : offsets) { if (out.length() > 0) out.append(','); out.append(value); }
        return out.toString();
    }

    @Override protected void onResume() { super.onResume(); ReminderScheduler.refresh(this); }

    private void addThemeCard() {
        LinearLayout card = card();
        card.addView(text("Оформление", 19, true));
        TextView description = text("Палитра Material You подстраивается под обои на Android 12 и новее.", 13, false, MUTED);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2);
        descLp.topMargin = dp(5);
        card.addView(description, descLp);
        LinearLayout options = row();
        addThemeOption(options, "Система", AppPalette.THEME_SYSTEM);
        addThemeOption(options, "Светлая", AppPalette.THEME_LIGHT);
        addThemeOption(options, "Тёмная", AppPalette.THEME_DARK);
        LinearLayout.LayoutParams optionsLp = new LinearLayout.LayoutParams(-1, dp(44));
        optionsLp.topMargin = dp(14);
        card.addView(options, optionsLp);
        addCardToPage(card, 14);
    }

    private void addThemeOption(LinearLayout row, String label, int mode) {
        TextView option = text(label, 12, true, themeMode == mode ? ON_TONAL : MUTED);
        option.setGravity(Gravity.CENTER);
        option.setBackground(round(themeMode == mode ? TONAL : BG, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) lp.leftMargin = dp(6);
        row.addView(option, lp);
        option.setOnClickListener(v -> {
            themeMode = mode;
            AppPalette.setThemeMode(this, mode);
            applyPalette();
            buildUi();
        });
    }

    private void addCustomTextCard() {
        LinearLayout card = card();
        card.addView(text("Текст в уведомлении", 19, true));
        customTextCount = text("", 13, false, MUTED);
        card.addView(customTextCount);
        TextView edit = text("Изменить строки", 14, true, ON_TONAL);
        edit.setGravity(Gravity.CENTER); edit.setBackground(round(TONAL, 14));
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(-1, dp(44));
        editLp.topMargin = dp(10); card.addView(edit, editLp);
        edit.setOnClickListener(v -> openCustomTextEditor());

        boolean enabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("showCustomText", false);
        customTextSwitch = text(AppStrings.t(this, "Добавлять строку к уведомлению: ") + AppStrings.t(this, enabled ? "ВКЛ" : "ВЫКЛ"), 13, true,
                enabled ? ON_TONAL : MUTED);
        customTextSwitch.setGravity(Gravity.CENTER);
        customTextSwitch.setPadding(dp(10), dp(11), dp(10), dp(11));
        customTextSwitch.setBackground(round(enabled ? TONAL : BG, 14));
        LinearLayout.LayoutParams switchLp = new LinearLayout.LayoutParams(-1, -2);
        switchLp.topMargin = dp(10);
        card.addView(customTextSwitch, switchLp);
        customTextSwitch.setOnClickListener(v -> toggleCustomText());
        updateCustomTextCount();
        addCardToPage(card, 14);
    }

    private void openCustomTextEditor() {
        customText = new EditText(this);
        customText.setTextSize(14); customText.setTextColor(TEXT); customText.setHintTextColor(MUTED);
        customText.setHint(AppStrings.t(this, "Каждая новая строка — отдельный вариант"));
        customText.setGravity(Gravity.TOP | Gravity.START); customText.setMinLines(5); customText.setMaxLines(12);
        customText.setPadding(dp(13), dp(11), dp(13), dp(11)); customText.setBackground(round(BG, 15));
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        customText.setText(prefs.getString("customDraft", prefs.getString("customLines", "")));
        customText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { prefs.edit().putString("customDraft", s.toString()).apply(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        LinearLayout editor = column(); editor.setPadding(dp(20), dp(8), dp(20), 0); editor.addView(customText);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(AppStrings.t(this, "Текст в уведомлении"))
                .setView(editor).setNegativeButton(AppStrings.t(this, "Закрыть"), null)
                .setNeutralButton(AppStrings.t(this, "Импорт .txt"), (d, w) -> pickTextFile())
                .setPositiveButton(AppStrings.t(this, "Сохранить"), (d, w) -> { saveCustomText(); updateCustomTextCount(); }).create();
        dialog.show();
    }

    private void updateCustomTextCount() {
        int count = savedLines().size();
        customTextCount.setText(AppStrings.t(this, "Сохранено строк: ") + count);
    }

    private void toggleCustomText() {
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean enabled = !prefs.getBoolean("showCustomText", false);
        if (enabled && savedLines().isEmpty()) {
            Toast.makeText(this, "Сначала добавь и сохрани строки", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit().putBoolean("showCustomText", enabled).apply();
        updateCustomToggle(enabled);
        refreshNotificationText();
    }

    private void updateCustomToggle(boolean enabled) {
        customTextSwitch.setText(AppStrings.t(this, "Добавлять строку к уведомлению: ") + AppStrings.t(this, enabled ? "ВКЛ" : "ВЫКЛ"));
        customTextSwitch.setTextColor(enabled ? ON_TONAL : MUTED);
        customTextSwitch.setBackground(round(enabled ? TONAL : BG, 14));
    }

    private List<String> savedLines() {
        String source = getSharedPreferences("settings", MODE_PRIVATE).getString("customLines", "");
        List<String> lines = new ArrayList<>();
        for (String line : source.split("\\R")) if (!line.trim().isEmpty()) lines.add(line.trim());
        return lines;
    }

    private void saveCustomText() {
        String raw = customText.getText().toString();
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\\R")) if (!line.trim().isEmpty()) lines.add(line.trim());
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putString("customLines", String.join("\n", lines)).putString("customDraft", raw).apply();
        if (lines.isEmpty()) {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("showCustomText", false).apply();
            updateCustomToggle(false);
        }
        refreshNotificationText();
        Toast.makeText(this, "Сохранено строк: " + lines.size(), Toast.LENGTH_SHORT).show();
    }

    private void pickTextFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        try { startActivityForResult(intent, REQUEST_IMPORT_TEXT); }
        catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Не найден файловый менеджер", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_TEXT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getContentResolver().openInputStream(data.getData()), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append('\n');
            customText.setText(content.toString().replace("\uFEFF", ""));
            saveCustomText();
            updateCustomTextCount();
        } catch (IOException | NullPointerException e) {
            Toast.makeText(this, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show();
        }
    }

    private void addSchedulesCard() {
        LinearLayout card = card();
        card.addView(text("Мои расписания", 19, true));
        TextView description = text("Выбери расписание для главного экрана или создай своё.", 13, false, MUTED);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2);
        descLp.topMargin = dp(5);
        card.addView(description, descLp);

        String selected = Schedule.selectedProfileId(this);
        for (Schedule.Profile profile : Schedule.profiles(this)) {
            LinearLayout profileCard = column();
            profileCard.setPadding(dp(13), dp(12), dp(13), dp(12));
            profileCard.setBackground(round(BG, 15));
            LinearLayout.LayoutParams profileLp = new LinearLayout.LayoutParams(-1, -2);
            profileLp.topMargin = dp(10);
            card.addView(profileCard, profileLp);

            LinearLayout titleRow = row();
            TextView name = text(profile.name, 15, true);
            titleRow.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            boolean active = profile.id.equals(selected);
            if (active) {
                TextView badge = text("ВЫБРАНО", 10, true, ON_TONAL);
                badge.setPadding(dp(8), dp(5), dp(8), dp(5));
                badge.setBackground(round(TONAL, 10));
                titleRow.addView(badge);
            }
            profileCard.addView(titleRow);
            LinearLayout actions = row();
            if (!active) addAction(actions, "Выбрать", true, () -> {
                Schedule.selectProfile(this, profile.id);
                buildUi();
            });
            if (!Schedule.DEFAULT_PROFILE_ID.equals(profile.id)) {
                addAction(actions, "Изменить", false, () -> showProfileEditor(profile));
                addAction(actions, "Удалить", false, () -> confirmDelete(profile));
            }
            if (actions.getChildCount() > 0) {
                LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, dp(40));
                actionsLp.topMargin = dp(8);
                profileCard.addView(actions, actionsLp);
            }
        }

        TextView add = text("＋  Добавить расписание", 14, true, ON_TONAL);
        add.setGravity(Gravity.CENTER);
        add.setBackground(round(TONAL, 15));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(48));
        addLp.topMargin = dp(12);
        card.addView(add, addLp);
        add.setOnClickListener(v -> showProfileEditor(null));
        TextView timed = text("Создать по дням и времени", 14, true, ACCENT);
        timed.setGravity(Gravity.CENTER); timed.setBackground(round(BG, 15));
        LinearLayout.LayoutParams timedLp = new LinearLayout.LayoutParams(-1, dp(46)); timedLp.topMargin = dp(7); card.addView(timed, timedLp);
        timed.setOnClickListener(v -> showScheduleBuilder(null, "", ""));
        addCardToPage(card, 14);
    }

    private void showProfileEditor(Schedule.Profile profile) {
        boolean editing = profile != null;
        LinearLayout form = column();
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setTextSize(15); name.setTextColor(TEXT); name.setHintTextColor(MUTED);
        name.setSingleLine(true); name.setHint(AppStrings.t(this, "Название расписания")); name.setText(editing ? profile.name : "");
        name.setBackground(round(BG, 12)); name.setPadding(dp(12), dp(8), dp(12), dp(8));
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView hint = text("Формат каждой строки: день время предмет\nМожно писать полное название или код. Например: en, worldlit, ukrlit, bellit, worldhistory, dnkr. Неизвестные предметы можно вписывать как обычно.\nДни: Пн, Вт, Ср, Чт, Пт, Сб, Вс.", 12, false, MUTED);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.topMargin = dp(10);
        form.addView(hint, hintLp);

        EditText source = new EditText(this);
        source.setTextSize(13); source.setTextColor(TEXT); source.setHintTextColor(MUTED);
        source.setGravity(Gravity.TOP | Gravity.START); source.setMinLines(7); source.setMaxLines(12);
        source.setHint("Пн 08:00-08:40 Предмет\nПн 08:50-09:30 Другой предмет");
        source.setPadding(dp(12), dp(10), dp(12), dp(10)); source.setBackground(round(BG, 12));
        android.content.SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        source.setText(editing ? prefs.getString("scheduleProfile_" + profile.id + "_data", "") : "");
        TextView builder = text("Открыть конструктор с таймпикером", 13, true, ACCENT);
        builder.setGravity(Gravity.CENTER); builder.setBackground(round(BG, 12));
        LinearLayout.LayoutParams builderLp = new LinearLayout.LayoutParams(-1, dp(42)); builderLp.topMargin = dp(8); form.addView(builder, builderLp);
        LinearLayout.LayoutParams sourceLp = new LinearLayout.LayoutParams(-1, -2);
        sourceLp.topMargin = dp(8);
        form.addView(source, sourceLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(AppStrings.t(this, editing ? "Изменить расписание" : "Новое расписание"))
                .setView(form).setNegativeButton(AppStrings.t(this, "Отмена"), null)
                .setPositiveButton(AppStrings.t(this, "Сохранить"), null).create();
        builder.setOnClickListener(v -> { dialog.dismiss(); showScheduleBuilder(profile, name.getText().toString(), source.getText().toString()); });
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String profileName = name.getText().toString().trim();
            if (profileName.isEmpty()) {
                name.setError(AppStrings.t(this, "Укажи название"));
                return;
            }
            try {
                Schedule.saveProfile(this, editing ? profile.id : null, profileName, source.getText().toString());
                dialog.dismiss();
                buildUi();
            } catch (IllegalArgumentException e) {
                String message = e.getMessage() == null ? "" : e.getMessage();
                int separator = message.indexOf(": ");
                if (message.startsWith("Строка ") && separator > 0) {
                    message = AppStrings.t(this, "Строка") + message.substring("Строка".length(), separator + 1)
                            + AppStrings.t(this, message.substring(separator + 2));
                } else message = AppStrings.t(this, message);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private void confirmDelete(Schedule.Profile profile) {
        new AlertDialog.Builder(this).setTitle(AppStrings.t(this, "Удалить расписание?"))
                .setMessage("«" + profile.name + "»" + AppStrings.t(this, " будет удалено."))
                .setNegativeButton(AppStrings.t(this, "Отмена"), null)
                .setPositiveButton(AppStrings.t(this, "Удалить"), (dialog, which) -> {
                    Schedule.deleteProfile(this, profile.id);
                    buildUi();
                }).show();
    }

    private static final class TimedLesson {
        int sh=8, sm=0, eh=8, em=40; String subject="";
    }

    private void showScheduleBuilder(Schedule.Profile profile, String initialName, String initialSource) {
        String[] dayCodes={"Пн","Вт","Ср","Чт","Пт","Сб","Вс"};
        java.util.List<TimedLesson>[] days=new java.util.ArrayList[7];
        for(int i=0;i<7;i++) days[i]=new java.util.ArrayList<>();
        try {
            for(java.util.Map.Entry<java.time.DayOfWeek, java.util.List<Schedule.Slot>> e:Schedule.parseCustomSchedule(initialSource).entrySet()) {
                int d=e.getKey().getValue()-1;
                for(Schedule.Slot slot:e.getValue()) { TimedLesson x=new TimedLesson(); x.sh=slot.start.getHour();x.sm=slot.start.getMinute();x.eh=slot.end.getHour();x.em=slot.end.getMinute();x.subject=slot.title;days[d].add(x); }
            }
        } catch(Exception ignored) {}
        EditText name=new EditText(this); name.setSingleLine(true);name.setTextColor(TEXT);name.setHintTextColor(MUTED);name.setHint(AppStrings.t(this,"Название расписания"));name.setText(initialName);name.setPadding(dp(12),dp(8),dp(12),dp(8));name.setBackground(round(BG,12));
        Spinner dayPicker=new Spinner(this); dayPicker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dayCodes));
        LinearLayout rows=column(); TextView add=text("＋  Добавить урок",14,true,ACCENT);add.setGravity(Gravity.CENTER);add.setBackground(round(BG,12));
        java.util.function.IntConsumer renderDay=new java.util.function.IntConsumer(){ public void accept(int index){ rows.removeAllViews(); for(TimedLesson x:days[index]) addTimedLessonRow(rows,x); } };
        dayPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){renderDay.accept(pos);}});
        add.setOnClickListener(v->{TimedLesson x=new TimedLesson();days[dayPicker.getSelectedItemPosition()].add(x);renderDay.accept(dayPicker.getSelectedItemPosition());});
        TextView copy=text("Скопировать этот день на все",13,true,ACCENT);copy.setGravity(Gravity.CENTER);copy.setBackground(round(BG,12));
        copy.setOnClickListener(v->{int source=dayPicker.getSelectedItemPosition();java.util.List<TimedLesson> template=days[source];for(int d=0;d<7;d++){if(d==source)continue;int count=Math.min(days[d].size(),template.size());for(int i=0;i<count;i++){TimedLesson from=template.get(i),to=days[d].get(i);to.sh=from.sh;to.sm=from.sm;to.eh=from.eh;to.em=from.em;}}renderDay.accept(source);Toast.makeText(this,AppStrings.t(this,"Время звонков скопировано; предметы не изменены"),Toast.LENGTH_SHORT).show();});
        LinearLayout body=column();body.setPadding(dp(18),dp(8),dp(18),0);body.addView(name);body.addView(dayPicker);body.addView(rows);LinearLayout.LayoutParams addLp=new LinearLayout.LayoutParams(-1,dp(44));addLp.topMargin=dp(8);body.addView(add,addLp);LinearLayout.LayoutParams copyLp=new LinearLayout.LayoutParams(-1,dp(42));copyLp.topMargin=dp(6);body.addView(copy,copyLp);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(AppStrings.t(this,"Создать по дням и времени")).setView(body).setNegativeButton(AppStrings.t(this,"Отмена"),null).setPositiveButton(AppStrings.t(this,"Сохранить"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String profileName=name.getText().toString().trim();if(profileName.isEmpty()){name.setError(AppStrings.t(this,"Укажи название"));return;}StringBuilder src=new StringBuilder();for(int i=0;i<7;i++)for(TimedLesson x:days[i])if(!x.subject.trim().isEmpty())src.append(dayCodes[i]).append(' ').append(String.format(java.util.Locale.ROOT,"%02d:%02d-%02d:%02d",x.sh,x.sm,x.eh,x.em)).append(' ').append(x.subject.trim()).append('\n');try{Schedule.saveProfile(this,profile==null?null:profile.id,profileName,src.toString());dialog.dismiss();buildUi();}catch(Exception ex){Toast.makeText(this,AppStrings.t(this,"Проверь время урока"),Toast.LENGTH_LONG).show();}}));
        dialog.show();
    }

    private void addTimedLessonRow(LinearLayout rows, TimedLesson lesson) {
        LinearLayout line=column();line.setPadding(dp(8),dp(6),dp(8),dp(6));line.setBackground(round(BG,12));
        LinearLayout times=row(); TextView start=text(String.format(java.util.Locale.ROOT,"%02d:%02d",lesson.sh,lesson.sm),14,true,ACCENT);TextView end=text(String.format(java.util.Locale.ROOT,"%02d:%02d",lesson.eh,lesson.em),14,true,ACCENT);
        times.addView(start,new LinearLayout.LayoutParams(0,dp(38),1));TextView dash=text("–",14,true);times.addView(dash);times.addView(end,new LinearLayout.LayoutParams(0,dp(38),1));TextView del=text("×",20,true,ACCENT);times.addView(del,new LinearLayout.LayoutParams(dp(36),dp(38)));
        start.setOnClickListener(v->pickTime(lesson,true,start));end.setOnClickListener(v->pickTime(lesson,false,end));del.setOnClickListener(v->rows.removeView(line));
        EditText subject=new EditText(this);subject.setSingleLine(true);subject.setTextSize(14);subject.setTextColor(TEXT);subject.setHintTextColor(MUTED);subject.setHint(AppStrings.t(this,"Предмет"));subject.setText(lesson.subject);subject.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){lesson.subject=s.toString();}public void afterTextChanged(Editable e){}});
        line.addView(times);line.addView(subject);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(5);rows.addView(line,lp);
    }

    private void pickTime(TimedLesson lesson, boolean start, TextView label) {
        int h=start?lesson.sh:lesson.eh,m=start?lesson.sm:lesson.em;
        new TimePickerDialog(this,(picker,hour,minute)->{if(start){lesson.sh=hour;lesson.sm=minute;}else{lesson.eh=hour;lesson.em=minute;}label.setText(String.format(java.util.Locale.ROOT,"%02d:%02d",hour,minute));},h,m,true).show();
    }

    private void addAction(LinearLayout row, String label, boolean primary, Runnable action) {
        TextView button = text(label, 12, true, primary ? ON_TONAL : ACCENT);
        button.setGravity(Gravity.CENTER);
        button.setBackground(round(primary ? TONAL : SURFACE, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) lp.leftMargin = dp(6);
        row.addView(button, lp);
        button.setOnClickListener(v -> action.run());
    }

    private void refreshNotificationText() {
        if (!getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false)) return;
        Intent intent = new Intent(this, BellService.class).setAction(BellService.ACTION_REFRESH_TEXT);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void applyPalette() {
        AppPalette colors = AppPalette.from(this);
        setTheme(colors.dark ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        BG = colors.background; SURFACE = colors.surface; TEXT = colors.text; MUTED = colors.muted;
        ACCENT = colors.accent; DIVIDER = colors.divider; TONAL = colors.accentContainer; ON_TONAL = colors.onAccentContainer;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = colors.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26 && !colors.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void addCardToPage(LinearLayout card, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16); lp.bottomMargin = dp(bottom);
        page.addView(card, lp);
    }

    private LinearLayout card() {
        LinearLayout view = column();
        view.setPadding(dp(17), dp(16), dp(17), dp(16));
        view.setBackground(round(SURFACE, 22));
        view.setElevation(dp(1));
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private TextView text(String value, int size, boolean bold) { return text(value, size, bold, TEXT); }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(AppStrings.t(this, value)); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
}
