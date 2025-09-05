package com.example.bidder.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MicroConverter {
    public static long toMicro(BigDecimal cpm) {
        return cpm.multiply(BigDecimal.valueOf(1_000_000)).longValue();
    }
    public static BigDecimal fromMirco(long micro) {
        return BigDecimal.valueOf(micro).divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
