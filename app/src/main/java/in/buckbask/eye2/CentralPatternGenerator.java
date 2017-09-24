package in.buckbask.eye2;

import android.graphics.Color;
import android.os.VibrationEffect;

/**
 * Created by buck on 9/23/17.
 */

public class CentralPatternGenerator {
    public enum COLOR {
        WHITE,
        RED,
        GREEN,
        BLUE,
        BLACK
    }

    long[] pattern(COLOR color, float urgency) {
        int index = 0;
        if (color == COLOR.WHITE) {
            index = 0;
        } else if (color == COLOR.RED) {
            index = 1;
        } else if (color == COLOR.GREEN) {
            index = 2;
        } else if (color == COLOR.BLUE) {
            index = 3;
        } else {
            index = 4;
        }
        long[] result = new long[TIMINGS[index].length];
        for (int i = 0; i < TIMINGS[index].length; i++) {
            result[i] = (long) (TIMINGS[index][i]);
        }
        return result;

    }

//    VibrationEffect pattern(COLOR color, float urgency) {
//        int index = 0;
//        if (color == COLOR.WHITE) {
//            index = 0;
//        } else if (color == COLOR.RED) {
//            index = 1;
//        } else if (color == COLOR.GREEN) {
//            index = 2;
//        } else if (color == COLOR.BLUE) {
//            index = 3;
//        } else {
//            index = 4;
//        }
//        if (urgency >= 1.0) {
//            return VibrationEffect.createWaveform(RAW_AMPLITUDES, TIMINGS[index], 0);
//        } else if (urgency < 0.0) {
//            return VibrationEffect.createWaveform(MIN_AMPLITUDES, TIMINGS[index], 0);
//        }
//        long[] amplitudes = new long[RAW_AMPLITUDES.length];
//        for (int i = 0; i < amplitudes.length; i++) {
//            amplitudes[i] = (long) (RAW_AMPLITUDES[i] * urgency);
//        }
//        return VibrationEffect.createWaveform(amplitudes, TIMINGS[index], 0);
//    }

    private final long[] MIN_AMPLITUDES = {0, 0, 0, 0, 0, 0, 0, 0};

    private final long[] RAW_AMPLITUDES = {
            255, 0, 255, 0, 255, 0, 255, 0,
    };
    private final int[][] TIMINGS = {
            {100, 50, 100, 50, 100, 50, 100, 50}, // white 1111
            {100, 50, 100, 50, 100, 50, 0, 150}, // red 1110
            {100, 50, 0, 150, 100, 50, 0, 150}, // green 1010
            {100, 50, 100, 50, 0, 150, 0, 150,}, // blue 1100
            {100, 50, 0, 150, 0, 150, 0, 150}, // black 1000
    };
}
