package space.aqoleg.statistics.detailed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.Data;

class ViewDetailed2 extends ViewDetailed {
    // canvas objects
    private Paint[] paintChart = new Paint[7];
    private Paint[] paintChartText = new Paint[7];
    private Paint paintMask = new Paint();
    private Path[] path = new Path[7];
    private Path pathMask = new Path();
    // data y min, max
    private int yMax;
    // grid values
    private long xStep; // horizontal step of the grid
    private int yStep; // vertical step of the grid
    // bar width
    private float barWidthPx;
    // current state
    private boolean[] chart = new boolean[7]; // charts visibility
    private boolean drawPath = true; // false when path did not change
    // animation values
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private float frame; // current frame of the animation
    private boolean[] animate = new boolean[7]; // is animate
    private boolean[] stopChart = new boolean[7]; // target value of the charts visibility
    private int startYMax; // yMax on the beginning of the animation
    private float deltaYMax; // change of the yMax on each frame
    private int stopYMax; // yMax at the end of the animation

    // default View constructor
    ViewDetailed2(Context context) {
        super(context);
    }

    @Override
    void initialize(int state) {
        for (int chartN = 0; chartN < 7; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        yMax = findYMax(chart);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set paints
            for (int chartN = 0; chartN < 7; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint();
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.FILL);
                paintChartText[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChartText[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChartText[chartN].setFakeBoldText(true);
                paintChartText[chartN].setTextSize(density * 10);
            }
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
        if (!chart[0] && !chart[1] && !chart[2] && !chart[3] && !chart[4] && !chart[5] && !chart[6]) {
            return;
        }
        int chartN;
        float xPx, yPx;
        if (drawPath) {
            barWidthPx = chartWidthPx / xInterval * (Data.getX(iStop) - Data.getX(iStart)) / (iStop - iStart);
            xPx = chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval - barWidthPx / 2;
            for (chartN = 0; chartN < 7; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(
                        chartLeftPx + chartWidthPx * (Data.getX(iStop) - xMin) / xInterval + barWidthPx / 2,
                        chartBottomPx
                );
                path[chartN].lineTo(xPx, chartBottomPx);
            }
            float animateIn = frame / TOTAL_FRAMES;
            float animateOut = 1 - animateIn;
            float[] y = {0, 0, 0, 0, 0, 0, 0};
            float ySum;
            for (int i = iStart; i <= iStop; i++) {
                // calculate y
                ySum = 0;
                for (chartN = 0; chartN < 7; chartN++) {
                    if (chart[chartN]) {
                        if (!animate[chartN]) {
                            ySum += Data.getY(chartN, i);
                        } else {
                            ySum += Data.getY(chartN, i) * (stopChart[chartN] ? animateIn : animateOut);
                        }
                        y[chartN] = ySum;
                    }
                }
                // move path
                for (chartN = 0; chartN < 7; chartN++) {
                    if (chart[chartN]) {
                        yPx = chartBottomPx - chartHeightPx * y[chartN] / yMax;
                        path[chartN].lineTo(xPx, yPx);
                        path[chartN].lineTo(xPx + barWidthPx, yPx);
                    }
                }
                xPx += barWidthPx;
            }
            for (chartN = 0; chartN < 7; chartN++) {
                path[chartN].close();
            }
            // set grid values
            xStep = (((long) (xInterval / vLines) / xMinStep) + 1) * xMinStep; // xStep > (xInterval / vLines), xStep % xMinStep = 0
            yStep = round(yMax / hLines); // yStep > (yInterval / hLines), yStep is the nice number
            drawPath = false;
        }
        // draw charts
        for (chartN = 6; chartN >= 0; chartN--) {
            if (chart[chartN]) {
                canvas.drawPath(path[chartN], paintChart[chartN]);
            }
        }
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
            int charts = 0;
            int[] y = {0, 0, 0, 0, 0, 0, 0};
            int sum = 0;
            for (chartN = 0; chartN < 7; chartN++) {
                if (chart[chartN]) {
                    charts++;
                    y[chartN] = Data.getY(chartN, iSelected);
                    sum += y[chartN];
                }
            }
            xPx = chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval + barWidthPx * (iSelected - iStart - 0.5f);
            yPx = chartBottomPx - chartHeightPx * sum / yMax;
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
            boxBottomPx = density * (48 + charts * 16);
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
            int lineN = 1;
            for (chartN = 0; chartN < 7; chartN++) {
                if (chart[chartN]) {
                    yPx = density * (24 + lineN * 16);
                    canvas.drawText(
                            Data.getName(chartN),
                            boxLeftPx + density * 8,
                            yPx,
                            paintGridText
                    );
                    canvas.drawText(
                            String.valueOf(y[chartN]),
                            boxLeftPx + density * 64,
                            yPx,
                            paintChartText[chartN]
                    );
                    lineN++;
                }
            }
            yPx = density * (24 + lineN * 16);
            canvas.drawText(
                    "All",
                    boxLeftPx + density * 8,
                    yPx,
                    paintGridText
            );
            canvas.drawText(
                    String.valueOf(sum),
                    boxLeftPx + density * 64,
                    yPx,
                    paintGridText
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
            yMax = findYMax(chart);
        }
        drawPath = true;
        invalidate();
        return true;
    }

    @Override
    public void changeState(int state) {
        super.changeState(state);
        // remove any animation
        for (Animation animation : animations) {
            handler.removeCallbacks(animation);
        }
        iSelected = 0;
        // set animation values
        for (int chartN = 0; chartN < 7; chartN++) {
            stopChart[chartN] = (state & (1 << chartN)) != 0;
            animate[chartN] = stopChart[chartN] != chart[chartN];
            if (animate[chartN]) {
                chart[chartN] = true;
            }
        }
        startYMax = yMax;
        stopYMax = findYMax(stopChart);
        deltaYMax = (float) (stopYMax - startYMax) / TOTAL_FRAMES;
        // create animation
        int delay = 0;
        for (int frame = 1; frame <= TOTAL_FRAMES; frame++) {
            Animation animation = new Animation(frame);
            animations[frame - 1] = animation;
            delay += DELAY;
            handler.postDelayed(animation, delay);
        }
    }

    // set data max
    private int findYMax(boolean[] chart) {
        int max = 0;
        int value;
        for (int i = iStart; i <= iStop; i++) {
            value = 0;
            for (int chartN = 0; chartN < 7; chartN++) {
                if (chart[chartN]) {
                    value += Data.getY(chartN, i);
                }
            }
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private class Animation implements Runnable {
        private int animationFrame;

        Animation(int frame) {
            animationFrame = frame;
        }

        @Override
        public void run() {
            frame = animationFrame;
            if (frame != TOTAL_FRAMES) {
                yMax = startYMax + (int) (frame * deltaYMax);
            } else {
                // end of the animation, set stop values
                yMax = stopYMax;
                for (int chartN = 0; chartN < 7; chartN++) {
                    chart[chartN] = stopChart[chartN];
                    animate[chartN] = false;
                }
            }
            drawPath = true;
            invalidate();
        }
    }
}