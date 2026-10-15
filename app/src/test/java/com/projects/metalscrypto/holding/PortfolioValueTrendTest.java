package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PortfolioValueTrendTest {
    @Test
    public void comparesCurrentAndPreviousAudTotals() {
        assertEquals(PortfolioValueTrend.Direction.UP, PortfolioValueTrend.compare(101.25, 100.5));
        assertEquals(PortfolioValueTrend.Direction.DOWN, PortfolioValueTrend.compare(99.5, 100.5));
        assertEquals(PortfolioValueTrend.Direction.UNCHANGED, PortfolioValueTrend.compare(100.5, 100.5));
    }

    @Test
    public void missingOrNonFiniteValuesHaveNoTrend() {
        assertEquals(PortfolioValueTrend.Direction.NO_PREVIOUS, PortfolioValueTrend.compare(101.0, null));
        assertEquals(PortfolioValueTrend.Direction.NO_PREVIOUS, PortfolioValueTrend.compare(null, 100.0));
        assertEquals(PortfolioValueTrend.Direction.NO_PREVIOUS,
                PortfolioValueTrend.compare(Double.NaN, 100.0));
    }
}