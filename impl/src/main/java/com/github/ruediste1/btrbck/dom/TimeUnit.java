package com.github.ruediste1.btrbck.dom;

import org.joda.time.DateTime;
import org.joda.time.Period;

public enum TimeUnit {

    SECOND(Period.seconds(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.secondOfMinute().roundFloorCopy();
        }
    },
    MINUTE(Period.minutes(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.minuteOfHour().roundFloorCopy();
        }
    },
    HOUR(Period.hours(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.hourOfDay().roundFloorCopy();
        }
    },
    DAY(Period.days(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.dayOfWeek().roundFloorCopy();
        }
    },
    WEEK(Period.weeks(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.weekOfWeekyear().roundFloorCopy();
        }
    },
    MONTH(Period.months(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.monthOfYear().roundFloorCopy();
        }
    },
    YEAR(Period.years(1)) {
        @Override
        public DateTime truncate(DateTime time) {
            return time.year().roundFloorCopy();
        }
    },
    DECADE(Period.years(10)) {
        @Override
        public DateTime truncate(DateTime time) {
            int year = (int) Math.round(Math.floor(time.year().get() / 10.0)) * 10;
            return time.year().roundFloorCopy().withYear(year);
        }
    };

    final private Period period;

    private TimeUnit(Period period) {
        this.period = period;

    }

    public Period getPeriod() {
        return period;
    }

    public abstract DateTime truncate(DateTime time);
}
