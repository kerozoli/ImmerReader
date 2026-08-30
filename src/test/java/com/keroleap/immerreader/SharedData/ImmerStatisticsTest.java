package com.keroleap.immerreader.SharedData;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.keroleap.immerreader.ImmerRest;

import static org.junit.jupiter.api.Assertions.*;

class ImmerStatisticsTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private static ImmerStatistics newWithClock(long millis) {
        return new ImmerStatistics(Clock.fixed(Instant.ofEpochMilli(millis), ZONE));
    }

    private static ImmerStatistics newWithClock(Clock clock) {
        return new ImmerStatistics(clock);
    }

    private static ImmerRest rest(int temperature, int throttle, boolean heating) {
        ImmerRest r = new ImmerRest();
        r.setTemperaute(temperature);
        r.setThrottle(throttle);
        r.setHeating(heating);
        return r;
    }

    @Test
    void record_averagesSamplesInSameMinute() {
        long base = 1_000_000L * 60_000L; // minute boundary
        ImmerStatistics stats = newWithClock(base + 10_000L);

        stats.record(rest(20, 1, false));
        stats.record(rest(22, 2, false));
        stats.record(rest(24, 3, false));

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        List<Double> temps = snapshot.getTemperatures();
        List<Double> throttles = snapshot.getThrottleLevels();

        int lastIndex = temps.size() - 1;
        assertEquals(22.0, temps.get(lastIndex), 0.001);
        assertEquals(2.0, throttles.get(lastIndex), 0.001);
    }

    @Test
    void record_zeroThrottle_isIgnored() {
        long base = 1_000_000L * 60_000L;
        ImmerStatistics stats = newWithClock(base + 10_000L);

        stats.record(rest(20, 0, false));
        stats.record(rest(22, 2, false));
        stats.record(rest(24, 0, false));

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        List<Double> throttles = snapshot.getThrottleLevels();
        int lastIndex = throttles.size() - 1;

        assertEquals(2.0, throttles.get(lastIndex), 0.001);
    }

    @Test
    void record_accumulatesHeatingMsInSingleMinute() {
        long minute = 1_000_000L;
        long start = minute * 60_000L + 10_000L;
        long end = minute * 60_000L + 40_000L;

        AdvanceableClock clock = new AdvanceableClock(start, ZONE);
        ImmerStatistics stats = newWithClock(clock);

        stats.record(rest(20, 1, true));
        clock.advance(end - start);
        stats.record(rest(20, 1, false));

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        int index = snapshot.getHeatingMsPerMinute().size() - 1;
        assertEquals(30_000L, snapshot.getHeatingMsPerMinute().get(index));
        assertEquals(30_000L, snapshot.getTotalHeatingMs());
    }

    @Test
    void record_splitsHeatingIntervalAcrossTwoMinutes() {
        long minute = 1_000_000L;
        long start = minute * 60_000L + 45_000L;       // 45s into minute
        long end = (minute + 1) * 60_000L + 15_000L;   // 15s into next minute

        AdvanceableClock clock = new AdvanceableClock(start, ZONE);
        ImmerStatistics stats = newWithClock(clock);

        stats.record(rest(20, 1, true));
        clock.advance(end - start);
        stats.record(rest(20, 1, false));

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        int lastIndex = snapshot.getHeatingMsPerMinute().size() - 1;

        assertEquals(15_000L, snapshot.getHeatingMsPerMinute().get(lastIndex));
        assertEquals(15_000L, snapshot.getHeatingMsPerMinute().get(lastIndex - 1));
        assertEquals(30_000L, snapshot.getTotalHeatingMs());
    }

    @Test
    void getLast24Hours_contains1440Entries() {
        long now = 1_000_000L * 60_000L;
        ImmerStatistics stats = newWithClock(now);

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        assertEquals(24 * 60, snapshot.getLabels().size());
        assertEquals(24 * 60, snapshot.getTemperatures().size());
        assertEquals(24 * 60, snapshot.getThrottleLevels().size());
        assertEquals(24 * 60, snapshot.getHeatingMsPerMinute().size());
    }

    @Test
    void cleanup_removesBucketsOlderThan24Hours() {
        AdvanceableClock clock = new AdvanceableClock(0, ZONE);
        ImmerStatistics stats = newWithClock(clock);

        stats.record(rest(20, 1, false));
        clock.advance(24L * 60L * 60_000L);
        stats.record(rest(22, 2, false));

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        assertEquals(24 * 60, snapshot.getLabels().size());
        // Minute 0 should have been removed; the window now starts at minute 1.
        assertEquals("00:01", snapshot.getLabels().get(0));
        assertNull(snapshot.getTemperatures().get(0));
    }

    @Test
    void record_withNullDataAccumulatesHeatingOnly() {
        long minute = 1_000_000L;
        long start = minute * 60_000L;

        AdvanceableClock clock = new AdvanceableClock(start, ZONE);
        ImmerStatistics stats = newWithClock(clock);

        stats.record(rest(20, 1, true));
        clock.advance(10_000L);
        stats.record(null);

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        int lastIndex = snapshot.getHeatingMsPerMinute().size() - 1;
        assertEquals(10_000L, snapshot.getHeatingMsPerMinute().get(lastIndex));
        assertEquals(10_000L, snapshot.getTotalHeatingMs());
    }

    @Test
    void getLast24Hours_labelFormatIsHourMinute() {
        long now = 12L * 60L * 60_000L + 34L * 60_000L; // 12:34 at epoch
        ImmerStatistics stats = newWithClock(now);

        ImmerStatisticsSnapshot snapshot = stats.getLast24Hours();
        String lastLabel = snapshot.getLabels().get(snapshot.getLabels().size() - 1);
        assertEquals("12:34", lastLabel);
    }

    private static class AdvanceableClock extends Clock {
        private long millis;
        private final ZoneId zone;

        AdvanceableClock(long millis, ZoneId zone) {
            this.millis = millis;
            this.zone = zone;
        }

        void advance(long deltaMs) {
            this.millis += deltaMs;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new AdvanceableClock(millis, zone);
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }
}
