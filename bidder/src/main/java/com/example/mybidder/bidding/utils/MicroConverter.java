package com.example.mybidder.bidding.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MicroConverter {
    public static long convertCpmToMicro(BigDecimal cpm) {
        return cpm.multiply(BigDecimal.valueOf(1_000_000)).longValue();
    }
    public static BigDecimal convertMicroToCpm(long micro) {
        return BigDecimal.valueOf(micro).divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
