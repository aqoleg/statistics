package space.aqoleg.statistics.detailed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.Data;

class ViewDetailed4 extends ViewDetailed {
    // canvas objects
    private Paint[] paintChart = new Paint[6];
    private Paint[] paintChartText = new Paint[6];
    private Path[] path = new Path[6];
    // grid values
    private long xStep; // horizontal step of the grid
    // current state
    private boolean[] chart = new boolean[6]; // charts visibility
    private boolean drawPath = true; // false when path did not change
    // animation values
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private float frame; // current frame of the animation
    private boolean[] animate = new boolean[6]; // is animate
    private boolean[] stopChart = new boolean[6]; // target value of the charts visibility

    // default View constructor
    ViewDetailed4(Context context) {
        super(context);
    }

    @Override
    void initialize(int state) {
        for (int chartN = 0; chartN < 6; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set paints
            for (int chartN = 0; chartN < 6; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.FILL);
                paintChart[chartN].setPathEffect(new CornerPathEffect(density)); // smooth
                paintChartText[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChartText[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChartText[chartN].setFakeBoldText(true);
                paintChartText[chartN].setTextSize(density * 10);
            }
        }
    }

    // invokes after onLayout() or on invalidate()
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!chart[0] && !chart[1] && !chart[2] && !chart[3] && !chart[4] && !chart[5]) {
            return;
        }
        int chartN;
        float xPx, yPx;
        if (drawPath) {
            xPx = chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval;
            for (chartN = 0; chartN < 6; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(
                        chartLeftPx + chartWidthPx * (Data.getX(iStop) - xMin) / xInterval,
                        chartBottomPx
                );
                path[chartN].lineTo(xPx, chartBottomPx);
            }
            float animateIn = frame / TOTAL_FRAMES;
            float animateOut = 1 - animateIn;
            float[] y = {0, 0, 0, 0, 0, 0};
            float ySum;
            for (int i = iStart; i <= iStop; i++) {
                // calculate y
                ySum = 0;
                for (chartN = 0; chartN < 6; chartN++) {
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
                xPx = chartLeftPx + chartWidthPx * (Data.getX(i) - xMin) / xInterval;
                for (chartN = 0; chartN < 6; chartN++) {
                    if (chart[chartN]) {
                        path[chartN].lineTo(xPx, chartBottomPx - chartHeightPx * y[chartN] / ySum);
                    }
                }
            }
            for (chartN = 0; chartN < 6; chartN++) {
                path[chartN].close();
            }
            // set grid values
            xStep = (((long) (xInterval / vLines) / xMinStep) + 1) * xMinStep; // xStep > (xInterval / vLines), xStep % xMinStep = 0
            drawPath = false;
        }
        // draw charts
        for (chartN = 5; chartN >= 0; chartN--) {
            if (chart[chartN]) {
                canvas.drawPath(path[chartN], paintChart[chartN]);
            }
        }
        // draw horizontal grid
        for (int i = 0; i < 4; i++) {
            yPx = chartBottomPx - chartHeightPx * i / 4;
            canvas.drawLine(chartLeftPx, yPx, chartRightPx, yPx, paintGrid);
            canvas.drawText(i * 25 + " %", chartLeftPx, yPx - density * 4, paintGridText);
        }
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
            int[] y = {0, 0, 0, 0, 0, 0};
            int sum = 0;
            for (chartN = 0; chartN < 6; chartN++) {
                if (chart[chartN]) {
                    charts++;
                    y[chartN] = Data.getY(chartN, iSelected);
                    sum += y[chartN];
                }
            }
            xPx = chartLeftPx + chartWidthPx * (Data.getX(iSelected) - xMin) / xInterval;
            canvas.drawLine(xPx, density, xPx, chartBottomPx, paintGrid);
            // box padding 8dp, width 128dp
            if (xPx > chartCenterPx) {
                // box to the left
                boxLeftPx = xPx - density * 136;
            } else {
                // box to the right
                boxLeftPx = xPx + density * 8;
            }
            boxTopPx = density * 8;
            boxRightPx = boxLeftPx + density * 128;
            boxBottomPx = density * (32 + charts * 16);
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
            int percentSum = 0;
            for (chartN = 0; chartN < 6; chartN++) {
                if (chart[chartN]) {
                    yPx = density * (24 + lineN * 16);
                    int percent = lineN == charts ? 100 - percentSum : Math.round(100f * y[chartN] / sum);
                    percentSum += percent;
                    canvas.drawText(
                            String.valueOf(percent).concat("%"),
                            boxLeftPx + density * 8,
                            yPx,
                            paintGridText
                    );
                    canvas.drawText(
                            Data.getName(chartN),
                            boxLeftPx + density * 32,
                            yPx,
                            paintGridText
                    );
                    canvas.drawText(
                            String.valueOf(y[chartN]),
                            boxLeftPx + density * 90,
                            yPx,
                            paintChartText[chartN]
                    );
                    lineN++;
                }
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return super.onTouch(view, event);
    }

    @Override
    public boolean move(float leftPos, float rightPos) {
        super.move(leftPos, rightPos);
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
        for (int chartN = 0; chartN < 6; chartN++) {
            stopChart[chartN] = (state & (1 << chartN)) != 0;
            animate[chartN] = stopChart[chartN] != chart[chartN];
            if (animate[chartN]) {
                chart[chartN] = true;
            }
        }
        // create animation
        int delay = 0;
        for (int frame = 1; frame <= TOTAL_FRAMES; frame++) {
            Animation animation = new Animation(frame);
            animations[frame - 1] = animation;
            delay += DELAY;
            handler.postDelayed(animation, delay);
        }
    }

    private class Animation implements Runnable {
        private int animationFrame;

        Animation(int frame) {
            animationFrame = frame;
        }

        @Override
        public void run() {
            frame = animationFrame;
            if (frame == TOTAL_FRAMES) {
                // end of the animation, set stop values
                for (int chartN = 0; chartN < 6; chartN++) {
                    chart[chartN] = stopChart[chartN];
                    animate[chartN] = false;
                }
            }
            drawPath = true;
            invalidate();
        }
    }
}