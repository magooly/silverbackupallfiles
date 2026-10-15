package com.projects.metalscrypto.holding;

final class PortfolioValueTrend {
    enum Direction {
        UP,
        DOWN,
        UNCHANGED,
        NO_PREVIOUS
    }

    private PortfolioValueTrend() {
    }

    static Direction compare(Double currentValue, Double previousValue) {
        if (currentValue == null || Double.isNaN(currentValue) || Double.isInfinite(currentValue)
                || previousValue == null || Double.isNaN(previousValue) || Double.isInfinite(previousValue)) {
            return Direction.NO_PREVIOUS;
        }
        int comparison = Double.compare(currentValue, previousValue);
        if (comparison > 0) {
            return Direction.UP;
        }
        if (comparison < 0) {
            return Direction.DOWN;
        }
        return Direction.UNCHANGED;
    }
}