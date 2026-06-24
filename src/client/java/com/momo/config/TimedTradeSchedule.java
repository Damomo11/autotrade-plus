package com.momo.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TimedTradeSchedule {
    public static final int DAY_TICKS = 24000;
    public static final String DEFAULT_TIMES = "3000,8000,13000";

    private TimedTradeSchedule() {
    }

    public static List<Integer> parse(String value) {
        Set<Integer> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        for (String part : value.split("[,，\\s]+")) {
            try {
                int tick = Integer.parseInt(part.trim());
                if (tick >= 0 && tick < DAY_TICKS) {
                    result.add(tick);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        List<Integer> sorted = new ArrayList<>(result);
        sorted.sort(Integer::compareTo);
        return sorted;
    }

    public static String normalize(String value) {
        return join(parse(value));
    }

    public static String join(Iterable<Integer> ticks) {
        StringBuilder builder = new StringBuilder();
        for (int tick : ticks) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(tick);
        }
        return builder.toString();
    }
}
