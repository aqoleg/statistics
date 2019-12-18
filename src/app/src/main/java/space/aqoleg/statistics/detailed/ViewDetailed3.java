package space.aqoleg.statistics.detailed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.Data;

class ViewDetailed3 extends ViewDetailed {
    // canvas objects
    private Paint paintChart = new Paint();
    private Paint paintChartText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintMask = new Paint();
    private Path path = new Path();
    private Path pathMask = new Path();
    // data y min, max
    private int yMax;
    // grid values
    private long xStep; // horizontal step of the grid
    private int yStep; // vertical step of the grid
    // bar width
    private float barWidthPx;
    // current state
    private boolean drawPath = true; // false when path did not change

    // default View constructor
    ViewDetailed3(Context context) {
        super(context);
    }

    @Override
    void initialize(int state) {
        findYMax();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set paints
            paintChart.setColor(Color.parseColor(Data.getColor(0)));
            paintChart.setStyle(Paint.Style.FILL);
            paintChartText.setColor(Color.parseColor(Data.getColor(0)));
            paintChartText.setFakeBoldText(true);
            paintChartText.setTextSize(density * 10);
            paintMask.setStyle(Paint.Style.FILL);
            if (day) {
                paintMask.setARGB(127, 0, 0, 0);
            } else {
                paintMask.setARGB(127, 255, 255, 255);
            }
        }
    }

    // invokes after onLayout() or on invalidate()
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float xPx, yPx;
        if (drawPath) {
            barWidthPx = chartWidthPx / xInterval * (Data.getX(iStop) - Data.getX(iStart)) / (iStop - iStart);
            path.reset();
            path.moveTo(
                    chartLeftPx + chartWidthPx * (Data.getX(iStop) - xMin) / xInterval + barWidthPx / 2,
                    chartBottomPx
            );
            xPx = chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval - barWidthPx / 2;
            path.lineTo(xPx, chartBottomPx);
            for (int i = iStart; i <= iStop; i++) {
                // move path
                yPx = chartBottomPx - chartHeightPx * Data.getY(0, i) / yMax;
                path.lineTo(xPx, yPx);
                xPx += barWidthPx;
                path.lineTo(xPx, yPx);
            }
            path.close();
            // set grid values
            xStep = (((long) (xInterval / vLines) / xMinStep) + 1) * xMinStep; // xStep > (xInterval / vLines), xStep % xMinStep = 0
            yStep = round(yMax / hLines); // yStep > (yInterval / hLines), yStep is the nice number
            drawPath = false;
        }
        // draw chart
        canvas.drawPath(path, paintChart);
        // draw horizontal grid
        int yLine = yStep; // y of the first line
        while (yLine < yMax) {
            yPx = chartBottomPx - chartHeightPx * yLine / yMax;
            canvas.drawLine(chartLeftPx, yPx, chartRightPx, yPx, paintGrid);
            canvas.drawText(
                    stripZeros(yLine),
                    chartLeftPx,
                    yPx - density * 4,
                    paintGridText
            );
            yLine += yStep;
        }
        // draw bottom line
        canvas.drawLine(chartLeftPx, chartBottomPx, chartRightPx, chartBottomPx, paintGrid);
        // draw time grid
        long xLine = X_START + (xMin / xStep) * xStep + xStep; // x of the first line
        while (xLine < xMax) {
            xPx = chartLeftPx + chartWidthPx * (xLine - xMin) / xInterval;
            date.setTime(xLine);
            canvas.drawText(gridSdf.format(date), xPx, chartBottomPx + density * 12, paintGridText);
            canvas.drawLine(xPx, chartBottomPx, xPx, chartBottomPx + density * 2, paintGrid);
            xLine += xStep;
        }
        // draw selection
        if (iSelected != 0) {
            xPx = chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval + barWidthPx * (iSelected - iStart - 0.5f);
            yPx = chartBottomPx - chartHeightPx * Data.getY(0, iSelected) / yMax;
            pathMask.reset();
            pathMask.moveTo(xPx + barWidthPx, chartBottomPx);
            pathMask.lineTo(xPx, chartBottomPx);
            pathMask.lineTo(xPx, yPx);
            pathMask.lineTo(xPx + barWidthPx, yPx);
            canvas.drawPath(pathMask, paintMask);
            // box padding 8dp, width 128dp
            if (xPx > chartCenterPx) {
                // box to the left
                boxLeftPx = xPx - density * 136;
            } else {
                // box to the right
                boxLeftPx = xPx + barWidthPx + density * 8;
            }
            boxTopPx = density * 8;
            boxRightPx = boxLeftPx + density * 128;
            boxBottomPx = density * 48;
            // draw background of the box
            canvas.drawRect(boxLeftPx, boxTopPx, boxRightPx, boxBottomPx, paintBox);
            canvas.drawRect(boxLeftPx, boxTopPx, boxRightPx, boxBottomPx, paintGrid);
            // draw date
            date.setTime(Data.getX(iSelected));
            canvas.drawText(pointSdf.format(date), boxLeftPx + density * 8, density * 22, paintGridText);
            // draw array
            if (!zoom) {
                canvas.drawLine(
                        boxLeftPx + density * 115,
                        boxTopPx + density * 5,
                        boxLeftPx + density * 120,
                        boxTopPx + density * 10,
                        paintGrid
                );
                canvas.drawLine(
                        boxLeftPx + density * 120,
                        boxTopPx + density * 10,
                        boxLeftPx + density * 115,
                        boxTopPx + density * 15,
                        paintGrid
                );
            }
            // draw values
            yPx = density * 40;
            canvas.drawText(
                    Data.getName(0),
                    boxLeftPx + density * 8,
                    yPx,
                    paintGridText
            );
            canvas.drawText(
                    String.valueOf(Data.getY(0, iSelected)),
                    boxLeftPx + density * 64,
                    yPx,
                    paintChartText
            );
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return super.onTouch(view, event);
    }

    @Override
    public boolean move(float leftPos, float rightPos) {
        if (super.move(leftPos, rightPos)) {
            findYMax();
        }
        drawPath = true;
        invalidate();
        return true;
    }

    // find y values
    private void findYMax() {
        yMax = 0;
        int value;
        for (int i = iStart; i < iStop; i++) {
            value = Data.getY(0, i);
            if (value > yMax) {
                yMax = value;
            }
        }
    }
}