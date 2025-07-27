/**
 * Copyright 2019 Ken Dobson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.magictractor.webcache.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class ExpiryListenersTest {

    private static final int HOUR = 8;
    private static final int MINUTE = 15;
    private static final LocalDateTime FROM = LocalDateTime.of(2020, 8, 24, HOUR, MINUTE);

    @Test
    public void testDaily_sameDay() {
        LocalDateTime actual = ExpiryListeners.daily(FROM.minusMinutes(1), HOUR, MINUTE);

        assertThat(actual).isEqualTo(FROM);
    }

    @Test
    public void testDaily_nextDay() {
        LocalDateTime actual = ExpiryListeners.daily(FROM.plusMinutes(1), HOUR, MINUTE);

        assertThat(actual).isEqualTo(FROM.plusDays(1));
    }

    @Test
    public void testDayOfWeek_sameDayBefore() {
        LocalDateTime actual = ExpiryListeners.nextDayOfWeek(FROM.minusMinutes(1), DayOfWeek.MONDAY, HOUR, MINUTE);

        assertThat(actual).isEqualTo(FROM);
    }

    @Test
    public void testDayOfWeek_sameDayAfter() {
        LocalDateTime actual = ExpiryListeners.nextDayOfWeek(FROM.plusMinutes(1), DayOfWeek.MONDAY, HOUR, MINUTE);

        assertThat(actual).isEqualTo(FROM.plusDays(7));
    }

    @Test
    public void testDayOfWeek_nextDay() {
        LocalDateTime actual = ExpiryListeners.nextDayOfWeek(FROM.minusMinutes(1), DayOfWeek.TUESDAY, HOUR, MINUTE);

        assertThat(actual).isEqualTo(FROM.plusDays(1));
    }

}
