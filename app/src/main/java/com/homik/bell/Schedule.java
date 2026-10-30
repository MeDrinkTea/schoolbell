package com.homik.bell;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import android.content.Context;
import android.content.SharedPreferences;

final class Schedule {
    static final String DEFAULT_PROFILE_ID = "default";
    static final class Profile {
        final String id, name;
        Profile(String id, String name) { this.id = id; this.name = name; }
    }

    static final class Slot {
        final String title;
        final LocalTime start, end;
        Slot(String title, String start, String end) {
            this.title = title; this.start = LocalTime.parse(start); this.end = LocalTime.parse(end);
        }
    }

    static List<Slot> forDay(DayOfWeek day) {
        // The public app contains no school-specific timetable. Users add/import
        // their own timetable through Settings so the GitHub build is reusable.
        return new ArrayList<>();
    }

    static List<Profile> profiles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Set<String> ids = new HashSet<>(prefs.getStringSet("scheduleProfileIds", Collections.emptySet()));
        List<Profile> result = new ArrayList<>();
        result.add(new Profile(DEFAULT_PROFILE_ID, "Без расписания"));
        for (String id : ids) {
            String name = prefs.getString(profileKey(id, "name"), null);
            if (name != null && !name.trim().isEmpty()) result.add(new Profile(id, name));
        }
        result.subList(1, result.size()).sort(Comparator.comparing(p -> p.name.toLowerCase(java.util.Locale.ROOT)));
        return result;
    }

    static String selectedProfileId(Context context) {
        String id = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("selectedScheduleProfile", DEFAULT_PROFILE_ID);
        for (Profile profile : profiles(context)) if (profile.id.equals(id)) return id;
        return DEFAULT_PROFILE_ID;
    }

    static String selectedProfileName(Context context) {
        String selected = selectedProfileId(context);
        for (Profile profile : profiles(context)) if (profile.id.equals(selected)) return profile.name;
        return "Без расписания";
    }

    static int revision(Context context) {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("scheduleRevision", 0);
    }

    static void selectProfile(Context context, String id) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putString("selectedScheduleProfile", id).apply();
        ReminderScheduler.refresh(context);
        BellWidgetProvider.refresh(context);
    }

    static String saveProfile(Context context, String id, String name, String source) {
        parseCustomSchedule(source); // Validate before storing the profile.
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean create = id == null || id.trim().isEmpty();
        String profileId = create ? UUID.randomUUID().toString() : id;
        Set<String> ids = new HashSet<>(prefs.getStringSet("scheduleProfileIds", Collections.emptySet()));
        ids.add(profileId);
        prefs.edit().putStringSet("scheduleProfileIds", ids)
                .putString(profileKey(profileId, "name"), name.trim())
                .putString(profileKey(profileId, "data"), source.trim())
                .putString("selectedScheduleProfile", profileId)
                .putInt("scheduleRevision", revision(context) + 1).apply();
        ReminderScheduler.refresh(context);
        BellWidgetProvider.refresh(context);
        return profileId;
    }

    static void deleteProfile(Context context, String id) {
        if (DEFAULT_PROFILE_ID.equals(id)) return;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Set<String> ids = new HashSet<>(prefs.getStringSet("scheduleProfileIds", Collections.emptySet()));
        ids.remove(id);
        SharedPreferences.Editor edit = prefs.edit().putStringSet("scheduleProfileIds", ids)
                .remove(profileKey(id, "name")).remove(profileKey(id, "data"))
                .putInt("scheduleRevision", revision(context) + 1);
        if (id.equals(prefs.getString("selectedScheduleProfile", DEFAULT_PROFILE_ID)))
            edit.putString("selectedScheduleProfile", DEFAULT_PROFILE_ID);
        edit.apply();
        ReminderScheduler.refresh(context);
        BellWidgetProvider.refresh(context);
    }

    static List<Slot> forDay(DayOfWeek day, Context context) {
        String id = selectedProfileId(context);
        if (DEFAULT_PROFILE_ID.equals(id)) return forDay(day);
        String source = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString(profileKey(id, "data"), "");
        return parseCustomSchedule(source).getOrDefault(day, new ArrayList<>());
    }

    static Map<DayOfWeek, List<Slot>> parseCustomSchedule(String source) {
        Map<DayOfWeek, List<Slot>> result = new EnumMap<>(DayOfWeek.class);
        String[] lines = source.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 3 || !parts[1].matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}"))
                throw new IllegalArgumentException("Строка " + (i + 1) + ": укажи день, время и предмет");
            DayOfWeek day = parseDay(parts[0]);
            if (day == null) throw new IllegalArgumentException("Строка " + (i + 1) + ": неизвестный день недели");
            String[] times = parts[1].split("-");
            try {
                Slot slot = new Slot(parts[2].trim(), normalizeTime(times[0]), normalizeTime(times[1]));
                if (slot.title.isEmpty() || !slot.start.isBefore(slot.end)) throw new IllegalArgumentException();
                result.computeIfAbsent(day, ignored -> new ArrayList<>()).add(slot);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Строка " + (i + 1) + ": проверь время урока", e);
            }
        }
        for (List<Slot> slots : result.values()) slots.sort(Comparator.comparing(slot -> slot.start));
        if (result.isEmpty()) throw new IllegalArgumentException("Добавь хотя бы один урок");
        return result;
    }

    private static String normalizeTime(String value) {
        String[] parts = value.split(":");
        return String.format(java.util.Locale.ROOT, "%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private static DayOfWeek parseDay(String value) {
        String day = value.toLowerCase(java.util.Locale.ROOT);
        switch (day) {
            case "пн": case "пон": case "понедельник": return DayOfWeek.MONDAY;
            case "вт": case "вторник": return DayOfWeek.TUESDAY;
            case "ср": case "среда": return DayOfWeek.WEDNESDAY;
            case "чт": case "четверг": return DayOfWeek.THURSDAY;
            case "пт": case "пятница": return DayOfWeek.FRIDAY;
            case "сб": case "суббота": return DayOfWeek.SATURDAY;
            case "вс": case "воскресенье": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private static String profileKey(String id, String field) { return "scheduleProfile_" + id + "_" + field; }

    private Schedule() {}
}
