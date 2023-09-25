package utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LamportClockTest {
    LamportClock clock;
    @BeforeEach
    void SetUp(){
        clock = new LamportClock();
    }

    @ParameterizedTest
    @ValueSource(ints={5,6,7,8,9})
    void getTimestamp(int value) {
        // Simulate sending
        clock.getTimestamp();
        clock.setTimestamp(value);
        int sendTS = clock.getTimestamp();
        assertEquals(value+2,sendTS);
    }

}