package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.junit.Test;

public class WholeMoneyFormatTest {
    @Test
    public void roundsAmountsOnBothSidesOfTheHalfUnitBoundary() {
        DecimalFormat format = WholeMoneyFormat.newFormatter(true);
        assertEquals(format.format(12), format.format(12.49));
        assertEquals(format.format(13), format.format(12.50));
        assertEquals(format.format(-13), format.format(-12.50));
        assertEquals(format.format(1000), format.format(new BigDecimal("999.50")));
        assertEquals(0, format.getMaximumFractionDigits());
    }

    @Test
    public void showsTwoDecimalPlacesWhenWholeDollarsAreOff() {
        DecimalFormat format = WholeMoneyFormat.newFormatter(false);
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(2, format.getMaximumFractionDigits());
        assertEquals(format.format(new BigDecimal("12.49")), format.format(new BigDecimal("12.494")));
        assertEquals(format.format(new BigDecimal("12.50")), format.format(new BigDecimal("12.495")));
    }
}
