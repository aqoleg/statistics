package space.aqoleg.statistics.detailed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.Data;

class ViewDetailed3Zoom extends ViewDetailed {
    // canvas objects
    private Paint[] paintChart = new Paint[3];
    private Paint[] paintChartText = new Paint[3];
    private Path[] path = new Path[3];
    // data y min, max
    private int[] yDataMin = new int[3];
    private int[] yDataMax = new int[3];
    private int yMin;
    private int yMax;
    private int yInterval; // =(yMax - yMin)
    // grid values
    private long xStep; // horizontal step of the grid
    private int yStep; // vertical step of the grid
    // current state
    private boolean[] chart = new boolean[3]; // charts visibility
    private boolean drawPath = true; // false when path did not change
    // animation values
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private boolean[] stopChart = new boolean[3]; // target value of the charts visibility
    private int[] startAlpha = new int[3];
    private float[] deltaAlpha = new float[3];
    private int startYMin; // yMin on the beginning of the animation
    private int startYInterval; // yInterval on the beginning of the animation
    private float deltaYMin; // change of the yMin on each frame
    private float deltaYInterval; // change of the yInterval on each frame
    private int stopYMin; // yMin at the end of the animation
    private int stopYInterval; // yInterval at the end of the animation

    // default View constructor
    ViewDetailed3Zoom(Context context) {
        super(context);
    }

    @Override
    void initialize(int state) {
        for (int chartN = 0; chartN < 3; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        findYMinMax();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set paints
            for (int chartN = 0; chartN < 3; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.STROKE);
                paintChart[chartN].setStrokeWidth(density);
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
        if (!chart[0] && !chart[1] && !chart[2]) {
            return;
        }
        int chartN;
        float xPx, yPx;
        if (drawPath) {
            for (chartN = 0; chartN < 3; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(
                        chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval,
                        chartBottomPx - chartHeightPx * (Data.getY(chartN, iStart) - yMin) / yInterval
                );
            }
            for (int i = iStart + 1; i <= iStop; i++) {
                xPx = chartLeftPx + chartWidthPx * (Data.getX(i) - xMin) / xInterval;
                for (chartN = 0; chartN < 3; chartN++) {
                    path[chartN].lineTo(xPx, chartBottomPx - chartHeightPx * (Data.getY(chartN, i) - yMin) / yInterval);
                }
            }
            // set grid values
            xStep = (((long) (xInterval / vLines) / xMinStep) + 1) * xMinStep; // xStep > (xInterval / vLines), xStep % xMinStep = 0
            yStep = round(yInterval / hLines); // yStep > (yInterval / hLines), yStep is the nice number
            drawPath = false;
        }
        // draw horizontal grid
        int yLine = (yMin / yStep) * yStep + yStep; // y of the first line
        while (yLine < yMax) {
            yPx = chartBottomPx - chartHeightPx * (yLine - yMin) / yInterval;
            canvas.drawLine(chartLeftPx, yPx, chartRightPx, yPx, paintGrid);
            canvas.drawText(stripZeros(yLine), chartLeftPx, yPx - density * 4, paintGridText);
            yLine += yStep;
        }
        // draw charts
        for (chartN = 0; chartN < 3; chartN++) {
            if (chart[chartN]) {
                canvas.drawPath(path[chartN], paintChart[chartN]);
            }
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
            for (chartN = 0; chartN < 3; chartN++) {
                if (chart[chartN]) {
                    charts++;
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
            // draw dots and values
            int lineN = 1;
            for (chartN = 0; chartN < 3; chartN++) {
                if (chart[chartN]) {
                    yPx = density * (24 + lineN * 16);
                    canvas.drawText(
                            Data.getName(chartN),
                            boxLeftPx + density * 8,
                            yPx,
                            paintGridText
                    );
                    canvas.drawText(
                            String.valueOf(Data.getY(chartN, iSelected)),
                            boxLeftPx + density * 64,
                            yPx,
                            paintChartText[chartN]
                    );
                    yPx = chartBottomPx - chartHeightPx * (Data.getY(chartN, iSelected) - yMin) / yInterval;
                    canvas.drawCircle(xPx, yPx, density * 4, paintBox);
                    canvas.drawCircle(xPx, yPx, density * 4, paintChart[chartN]);
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
        if (super.move(leftPos, rightPos)) {
            findYMinMax();
        }
        drawPath = true;
        invalidate();
        return true;
    }

    @Override
    public void changeState(int state) {
        // remove any animation
        for (Animation animation : animations) {
            handler.removeCallbacks(animation);
        }
        iSelected = 0;
        // set animation values
        for (int chartN = 0; chartN < 3; chartN++) {
            stopChart[chartN] = (state & (1 << chartN)) != 0;
            startAlpha[chartN] = chart[chartN] ? 255 : 0;
            deltaAlpha[chartN] = (float) ((stopChart[chartN] ? 255 : 0) - startAlpha[chartN]) / TOTAL_FRAMES;
            // both line are visible during animation
            chart[chartN] = true;
        }
        startYMin = yMin;
        startYInterval = yInterval;
        stopYMin = Integer.MAX_VALUE;
        if (stopChart[0] && yDataMin[0] < stopYMin) {
            stopYMin = yDataMin[0];
        }
        if (stopChart[1] && yDataMin[1] < stopYMin) {
            stopYMin = yDataMin[1];
        }
        if (stopChart[2] && yDataMin[2] < stopYMin) {
            stopYMin = yDataMin[2];
        }
        int max = 0;
        if (stopChart[0] && yDataMax[0] > max) {
            max = yDataMax[0];
        }
        if (stopChart[1] && yDataMax[1] > max) {
            max = yDataMax[1];
        }
        if (stopChart[2] && yDataMax[2] > max) {
            max = yDataMax[2];
        }
        stopYInterval = max - stopYMin;
        deltaYMin = (float) (stopYMin - startYMin) / TOTAL_FRAMES;
        deltaYInterval = (float) (stopYInterval - startYInterval) / TOTAL_FRAMES;
        // create animation
        int delay = 0;
        for (int frame = 1; frame <= TOTAL_FRAMES; frame++) {
            Animation animation = new Animation(frame);
            animations[frame - 1] = animation;
            delay += DELAY;
            handler.postDelayed(animation, delay);
        }
    }

    // find y values
    private void findYMinMax() {
        for (int chartN = 0; chartN < 3; chartN++) {
            int yMax = Data.getY(chartN, iStart);
            int yMin = yMax;
            int value;
            for (int i = iStart + 1; i <= iStop; i++) {
                value = Data.getY(chartN, i);
                if (value < yMin) {
                    yMin = value;
                } else if (value > yMax) {
                    yMax = value;
                }
            }
            yDataMin[chartN] = yMin;
            yDataMax[chartN] = yMax;
        }
        // yMin, yInterval
        yMin = Integer.MAX_VALUE;
        if (chart[0]) {
            yMin = yDataMin[0];
        }
        if (chart[1] && yDataMin[1] < yMin) {
            yMin = yDataMin[1];
        }
        if (chart[2] && yDataMin[2] < yMin) {
            yMin = yDataMin[2];
        }
        yMax = 0;
        if (chart[0]) {
            yMax = yDataMax[0];
        }
        if (chart[1] && yDataMax[1] > yMax) {
            yMax = yDataMax[1];
        }
        if (chart[2] && yDataMax[2] > yMax) {
            yMax = yDataMax[2];
        }
        yInterval = yMax - yMin;
    }

    private class Animation implements Runnable {
        private int frame;

        Animation(int frame) {
            this.frame = frame;
        }

        @Override
        public void run() {
            if (frame == TOTAL_FRAMES) {
                // end of the animation, set stop values
                for (int chartN = 0; chartN < 3; chartN++) {
                    chart[chartN] = stopChart[chartN];
                    paintChart[chartN].setAlpha(255);
                }
                yMin = stopYMin;
                yInterval = stopYInterval;
                yMax = yMin + yInterval;
            } else {
                for (int chartN = 0; chartN < 3; chartN++) {
                    paintChart[chartN].setAlpha(startAlpha[chartN] + (int) (deltaAlpha[chartN] * frame));
                }
                yMin = startYMin + (int) (frame * deltaYMin);
                yInterval = startYInterval + (int) (frame * deltaYInterval);
                yMax = yMin + yInterval;
            }
            drawPath = true;
            invalidate();
        }
    }
}