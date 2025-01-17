package seedu.address.model.timetable;

import seedu.address.commons.exceptions.IllegalValueException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import com.google.common.collect.Sets;

public class GenerateSlot {
    /**
     * Generate timeslot
     * @param timeTables List of timetables of all members.
     * @param numberOfHours Must be <= 23 hour
     * @param userSpecifiedTimeRange TimeRange to generate timeslot within.
     * @return List of TimeRange where meeting is possible.
     * @throws IllegalValueException When unable to generate timeslot.
     */
    public static List<TimeRange> generate(Collection<TimeTable> timeTables, int numberOfHours, TimeRange userSpecifiedTimeRange) throws IllegalValueException {
        List<TimeRange> uniqueTimeRanges = filterUniqueTimeRanges(timeTables);
        List<TimeRange> merged = mergeOverlappingTimeRanges(uniqueTimeRanges);
        List<TimeRange> inverted = getFreeTimeRanges(merged);
        List<TimeRange> truncated = truncateTimeRange(inverted, userSpecifiedTimeRange);

        return getSuitableTimeRanges(truncated, numberOfHours);
    }

    public static TimeSlotsAvailable generateWithMostPeople(List<TimeTable> timeTables, int numberOfHours, TimeRange userSpecifiedTimeRange) throws IllegalValueException {
        Set<TimeTable> set = new HashSet<>(timeTables);
        Set<Set<TimeTable>> powerSet = Sets.powerSet(set);
        List<Set<TimeTable>> powerList = new ArrayList<>(powerSet);
        powerList.sort((x, y) -> y.size() - x.size()); // Descending order of size
        for (Set<TimeTable> possibleTimeTables : powerList) {
            List<TimeRange> timeRanges = generate(possibleTimeTables, numberOfHours, userSpecifiedTimeRange);
            if (!timeRanges.isEmpty()) {
                return new TimeSlotsAvailable(possibleTimeTables, timeRanges);
            }
        }
        return new TimeSlotsAvailable(true);
    }

    private static List<TimeRange> filterUniqueTimeRanges(Collection<TimeTable> timeTables) {
        Set<TimeRange> timeRanges = new HashSet<>();
        for (TimeTable timeTable : timeTables) {
            timeRanges.addAll(timeTable.getTimeRanges());
        }
        return new ArrayList<>(timeRanges);
    }

    /**
     * Merge overlapping TimeRange into 1 TimeRange.
     * @param timeRanges List of TimeRange.
     * @return Merged list of TimeRange.
     * @throws IllegalValueException
     */
    private static List<TimeRange> mergeOverlappingTimeRanges(Collection<TimeRange> timeRanges) throws IllegalValueException {
        List<TimeRange> timeRangesList = new ArrayList<>(timeRanges);
        Collections.sort(timeRangesList);
        List<TimeRange> merged = new ArrayList<>();
        for (TimeRange timeRange : timeRangesList) {
            if (merged.isEmpty()) {
                merged.add(timeRange);
                continue;
            }
            TimeRange latest = merged.get(merged.size() - 1);
            if (latest.overlapInclusive(timeRange)) { // If last TimeRange in merged overlaps with new latest
                TimeRange tr = mergeTimeRange(timeRange, latest);
                merged.set(merged.size() - 1, tr);
            } else {
                merged.add(timeRange);
            }
        }
        return merged;
    }

    /**
     * Merge 2 TimeRange together, assuming that they overlap.
     * @param r1 TimeRange 1.
     * @param r2 TimeRange 2.
     * @return new merged TimeRange.
     * @throws IllegalValueException If error in creating new TimeRange.
     */
    private static TimeRange mergeTimeRange(TimeRange r1, TimeRange r2) throws IllegalValueException {
        return new TimeRange(r1.getStart().isBefore(r2.getStart()) ? r1.getStart() : r2.getStart(), r1.getEnd().isAfter(r2.getEnd()) ? r1.getEnd() : r2.getEnd());
    }

    private static List<TimeRange> getFreeTimeRanges(List<TimeRange> timeRanges) throws IllegalValueException {
        // Start from MONDAY 0000, to SUNDAY 2359
        List<TimeRange> inverted = new ArrayList<>();
        DayOfWeek curDay = DayOfWeek.MONDAY;
        LocalTime curTime = LocalTime.parse("00:00");
        WeekTime cur = new WeekTime(curDay, curTime);
        for (TimeRange timeRange : timeRanges) {
            TimeRange toAdd = new TimeRange(cur, timeRange.getStart());
            inverted.add(toAdd);
            cur = timeRange.getEnd();
        }
        inverted.add(new TimeRange(cur.getDay(), cur.getTime(), DayOfWeek.SUNDAY, LocalTime.parse("23:59")));
        return inverted;
    }

    private static List<TimeRange> truncateTimeRange(List<TimeRange> timeRanges, TimeRange limit) throws IllegalValueException {
        List<TimeRange> truncated = new ArrayList<>();
        for (TimeRange timeRange : timeRanges) {
            if (!timeRange.overlap(limit)) { // Start after end
                continue;
            }
            WeekTime start = timeRange.getStart().isBefore(limit.getStart()) ? limit.getStart() : timeRange.getStart();
            WeekTime end = timeRange.getEnd().isAfter(limit.getEnd()) ? limit.getEnd() : timeRange.getEnd();
            truncated.add(new TimeRange(start, end));
        }
        return truncated;
    }

    private static List<TimeRange> getSuitableTimeRanges(List<TimeRange> timeRanges, int numberOfHours) {
        List<TimeRange> possibleRanges = new ArrayList<>();
        for (TimeRange timeRange : timeRanges) {
            if (timeRange.getDuration().compareTo(new Duration(0, numberOfHours, 0)) >= 0) {
                possibleRanges.add(timeRange);
            }
        }
        return possibleRanges;
    }

}
