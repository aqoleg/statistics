package space.aqoleg.statistics.summary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import space.aqoleg.statistics.Data;

class ViewSummary3 extends ViewSummary {
    // canvas objects
    private Paint paintChart = new Paint();
    private Path path = new Path();
    // data values
    private int yMax;
    // bar constant
    private float barWidthPx;
    // current state
    private boolean drawPath = true; // false when path did not changes

    // default View constructor
    ViewSummary3(Context context) {
        super(context);
    }

    ViewSummary3 initialize() {
        yMax = 0;
        int value;
        int length = Data.getLength();
        for (int i = 1; i < length; i++) {
            value = Data.getY(0, i);
            if (value > yMax) {
                yMax = value;
            }
        }
        return this;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            barWidthPx = chartWidthPx / (Data.getLength() - 2);
            paintChart.setColor(Color.parseColor(Data.getColor(0)));
            paintChart.setStyle(Paint.Style.FILL);
        }
    }

    // invokes after onLayout() or on invalidate()
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawPath) {
            float xPx, yPx;
            path.reset();
            path.moveTo(chartRightPx + barWidthPx / 2, chartBottomPx);
            xPx = chartLeftPx - barWidthPx / 2;
            path.lineTo(xPx, chartBottomPx);
            int length = Data.getLength();
            for (int i = 1; i < length; i++) {
                // move path
                yPx = chartBottomPx - chartHeightPx * Data.getY(0, i) / yMax;
                path.lineTo(xPx, yPx);
                xPx += barWidthPx;
                path.lineTo(xPx, yPx);
            }
            path.close();
            drawPath = false;
        }
        canvas.drawPath(path, paintChart);
        drawSelector(canvas);
    }
}