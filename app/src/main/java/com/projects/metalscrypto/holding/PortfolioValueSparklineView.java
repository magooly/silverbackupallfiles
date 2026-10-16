package com.projects.metalscrypto.holding;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

public final class PortfolioValueSparklineView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] dailyValues = new float[0];
    private boolean[] availableDays = new boolean[0];

    public PortfolioValueSparklineView(Context context, AttributeSet attributes) {
        super(context, attributes);
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(dp(1.8f));
        pointPaint.setColor(Color.BLACK);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    void setDailyValues(float[] values, boolean[] available) {
        dailyValues = Arrays.copyOf(values, values.length);
        availableDays = Arrays.copyOf(available, available.length);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int slots = Math.min(dailyValues.length, availableDays.length);
        if (slots == 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        float minimum = Float.MAX_VALUE;
        float maximum = -Float.MAX_VALUE;
        int pointCount = 0;
        for (int index = 0; index < slots; index++) {
            if (!availableDays[index] || Float.isNaN(dailyValues[index]) || Float.isInfinite(dailyValues[index])) {
                continue;
            }
            minimum = Math.min(minimum, dailyValues[index]);
            maximum = Math.max(maximum, dailyValues[index]);
            pointCount++;
        }
        if (pointCount == 0) {
            return;
        }
        if (minimum == maximum) {
            float padding = Math.max(Math.abs(minimum) * 0.01f, 1f);
            minimum -= padding;
            maximum += padding;
        }

        float inset = dp(3f);
        float plotWidth = Math.max(0, getWidth() - inset * 2);
        float plotHeight = Math.max(0, getHeight() - inset * 2);
        int previousIndex = -2;
        float previousX = 0;
        float previousY = 0;
        float pointRadius = dp(1.7f);

        for (int index = 0; index < slots; index++) {
            if (!availableDays[index] || Float.isNaN(dailyValues[index]) || Float.isInfinite(dailyValues[index])) {
                previousIndex = -2;
                continue;
            }
            float x = inset + plotWidth * index / Math.max(1, slots - 1);
            float normalizedValue = (dailyValues[index] - minimum) / (maximum - minimum);
            float y = inset + plotHeight * (1f - normalizedValue);
            if (previousIndex == index - 1) {
                canvas.drawLine(previousX, previousY, x, y, linePaint);
            }
            canvas.drawCircle(x, y, pointRadius, pointPaint);
            previousIndex = index;
            previousX = x;
            previousY = y;
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}