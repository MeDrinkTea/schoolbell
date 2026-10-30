package com.homik.bell;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import android.content.Context;

final class SchoolCalendar {
    static final class Vacation {
        final String name;
        final LocalDate start, end;
        Vacation(String name, LocalDate start, LocalDate end) {
            this.name = name; this.start = start; this.end = end;
        }
        boolean contains(LocalDate day) { return !day.isBefore(start) && !day.isAfter(end); }
        long daysUntilStart(LocalDate day) { return Math.max(0, ChronoUnit.DAYS.between(day, start)); }
        long daysLeftIncludingToday(LocalDate day) { return Math.max(1, ChronoUnit.DAYS.between(day, end) + 1); }
    }

    static boolean quarters(Context context) {
        return "quarters".equals(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("vacationMode", "trimesters"));
    }

    static Vacation next(LocalDate today, Context context) {
        List<Vacation> periods = new ArrayList<>();
        int firstYear = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        boolean useQuarters = quarters(context);
        for (int year = firstYear - 1; year <= firstYear + 2; year++) periods.addAll(forSchoolYear(year, useQuarters));
        periods.sort(Comparator.comparing(v -> v.start));
        for (Vacation vacation : periods) if (vacation.contains(today) || vacation.start.isAfter(today)) return vacation;
        return periods.get(periods.size() - 1);
    }

    static String countdown(LocalDate today, Context context) {
        Vacation vacation = next(today, context);
        if (vacation.contains(today)) {
            long days = vacation.daysLeftIncludingToday(today);
            return AppStrings.t(context, "Каникулы идут · ещё ") + days + " " + dayWord(context, days);
        }
        long days = vacation.daysUntilStart(today);
        return days == 0 ? AppStrings.t(context, "Каникулы начинаются сегодня!") : AppStrings.t(context, "До каникул: ") + days + " " + dayWord(context, days);
    }

    static String dayWord(Context context, long days) {
        if (AppStrings.english(context)) return days == 1 ? "day" : "days";
        long mod100 = days % 100, mod10 = days % 10;
        if (AppStrings.ukrainian(context)) return mod100 >= 11 && mod100 <= 14 ? "днів" : mod10 == 1 ? "день" : mod10 >= 2 && mod10 <= 4 ? "дні" : "днів";
        if (AppStrings.belarusian(context)) return mod100 >= 11 && mod100 <= 14 ? "дзён" : mod10 == 1 ? "дзень" : mod10 >= 2 && mod10 <= 4 ? "дні" : "дзён";
        if (mod100 >= 11 && mod100 <= 14) return "дней";
        switch ((int)(days % 10)) {
            case 1: return "день";
            case 2: case 3: case 4: return "дня";
            default: return "дней";
        }
    }

    private static List<Vacation> forSchoolYear(int year, boolean quarters) {
        List<Vacation> result = new ArrayList<>();
        LocalDate septemberFirst = LocalDate.of(year, Month.SEPTEMBER, 1);
        LocalDate firstWeek = septemberFirst.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (quarters) {
            LocalDate firstSchoolMonday = septemberFirst.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            LocalDate autumn = firstSchoolMonday.plusDays(49);
            result.add(new Vacation("Осенние каникулы", autumn, autumn.plusDays(6)));
        } else {
            LocalDate autumnOne = firstWeek.plusDays(35);
            LocalDate autumnTwo = autumnOne.plusDays(42);
            result.add(new Vacation("Осенние каникулы", autumnOne, autumnOne.plusDays(6)));
            result.add(new Vacation("Осенние каникулы", autumnTwo, autumnTwo.plusDays(6)));
        }

        LocalDate winterStart = LocalDate.of(year, Month.DECEMBER, 31);
        LocalDate winterEnd = LocalDate.of(year + 1, Month.JANUARY, 10);
        result.add(new Vacation("Новогодние каникулы", winterStart, winterEnd));

        LocalDate afterNewYearStudy = LocalDate.of(year + 1, Month.JANUARY, 11)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (quarters) {
            LocalDate spring = afterNewYearStudy.plusDays(70);
            result.add(new Vacation("Весенние каникулы", spring, spring.plusDays(6)));
        } else {
            LocalDate lateWinter = afterNewYearStudy.plusDays(42);
            result.add(new Vacation("Зимние каникулы", lateWinter, lateWinter.plusDays(6)));
            LocalDate springStudy = lateWinter.plusDays(7);
            LocalDate springOne = springStudy.plusDays(35);
            result.add(new Vacation("Весенние каникулы", springOne, springOne.plusDays(6)));
            LocalDate springStudyAgain = springOne.plusDays(7);
            LocalDate springTwo = springStudyAgain.plusDays(35);
            result.add(new Vacation("Весенние каникулы", springTwo, springTwo.plusDays(6)));
        }

        LocalDate summerStart = LocalDate.of(year + 1, Month.MAY, 27);
        LocalDate summerEnd = LocalDate.of(year + 1, Month.AUGUST, 31);
        result.add(new Vacation("Летние каникулы", summerStart, summerEnd));
        return result;
    }

    private SchoolCalendar() {}
}
