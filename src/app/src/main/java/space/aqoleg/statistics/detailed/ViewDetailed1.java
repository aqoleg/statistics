package space.aqoleg.statistics.detailed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.Data;

class ViewDetailed1 extends ViewDetailed {
    // canvas objects
    private Paint[] paintChart = new Paint[2];
    private Paint[] paintChartGridText = new Paint[2];
    private Paint[] paintChartText = new Paint[2];
    private Path[] path = new Path[2];
    // data y min, max
    private int[] yDataMin = new int[2];
    private int[] yDataMax = new int[2];
    private float[] yMin = new float[2];
    private float[] yInterval = new float[2]; // =(yMax - yMin)
    // grid values
    private long xStep; // horizontal step of the grid
    private int y0Step; // vertical step of the grid
    private int y1Step;
    private int y0Base; // base line - the lowest visible grid line
    private int y1Base;
    // current state
    private boolean[] chart = new boolean[2]; // charts visibility
    private boolean drawPath = true; // false when path did not change
    // animation values
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private boolean[] stopChart = new boolean[2]; // target value of the charts visibility
    private int[] startAlpha = new int[2];
    private float[] deltaAlpha = new float[2];

    // default View constructor
    ViewDetailed1(Context context) {
        super(context);
    }

    @Override
    void initialize(int state) {
        for (int chartN = 0; chartN < 2; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        findYDataMinMax();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set paints
            for (int chartN = 0; chartN < 2; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.STROKE);
                paintChart[chartN].setStrokeWidth(density);
                paintChart[chartN].setPathEffect(new CornerPathEffect(density)); // smooth
                paintChartGridText[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChartGridText[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChartGridText[chartN].setTextSize(density * 10);
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
        if (!chart[0] && !chart[1]) {
            return;
        }
        int chartN;
        float xPx, yPx;
        if (drawPath) {
            setGridValues();
            // create path
            for (chartN = 0; chartN < 2; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(
                        chartLeftPx + chartWidthPx * (Data.getX(iStart) - xMin) / xInterval,
                        chartBottomPx - chartHeightPx * (Data.getY(chartN, iStart) - yMin[chartN]) / yInterval[chartN]
                );
            }
            for (int i = iStart + 1; i <= iStop; i++) {
                xPx = chartLeftPx + chartWidthPx * (Data.getX(i) - xMin) / xInterval;
                for (chartN = 0; chartN < 2; chartN++) {
                    path[chartN].lineTo(
                            xPx,
                            chartBottomPx - chartHeightPx * (Data.getY(chartN, i) - yMin[chartN]) / yInterval[chartN]
                    );
                }
            }
            // set grid values
            xStep = (((long) (xInterval / vLines) / xMinStep) + 1) * xMinStep; // xStep > (xInterval / vLines), xStep % xMinStep = 0
            drawPath = false;
        }
        // draw horizontal grid
        int y0Line = y0Base; // y of the first line
        int y1Line = y1Base;
        while (y0Line < yDataMax[0] || y1Line < yDataMax[1]) {
            yPx = chartBottomPx - chartHeightPx * (y0Line - yMin[0]) / yInterval[0];
            canvas.drawLine(chartLeftPx, yPx, chartRightPx, yPx, paintGrid); // line
            if (chart[0]) {
                canvas.drawText(
                        stripZeros(y0Line),
                        chartLeftPx,
                        yPx - density * 4,
                        paintChartGridText[0]
                );
            }
            if (chart[1]) {
                canvas.drawText(
                        stripZeros(y1Line),
                        chartRightPx - density * 20,
                        yPx - density * 4,
                        paintChartGridText[1]
                );
            }
            y0Line += y0Step;
            y1Line += y1Step;
        }
        // draw charts
        for (chartN = 0; chartN < 2; chartN++) {
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
            int charts = chart[0] && chart[1] ? 2 : 1;
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
            for (chartN = 0; chartN < 2; chartN++) {
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
                    yPx = chartBottomPx - chartHeightPx * (Data.getY(chartN, iSelected) - yMin[chartN]) / yInterval[chartN];
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
            findYDataMinMax();
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
        for (int chartN = 0; chartN < 2; chartN++) {
            stopChart[chartN] = (state & (1 << chartN)) != 0;
            startAlpha[chartN] = chart[chartN] ? 255 : 0;
            deltaAlpha[chartN] = (float) ((stopChart[chartN] ? 255 : 0) - startAlpha[chartN]) / TOTAL_FRAMES;
            // both line are visible during animation
            chart[chartN] = true;
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

    // find y values
    private void findYDataMinMax() {
        for (int chartN = 0; chartN < 2; chartN++) {
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
    }

    private void setGridValues() {
        // find yStep, yStep > (yInterval / hLines), yStep is the nice number
        float step0 = (yDataMax[0] - yDataMin[0]) / hLines;
        float step1 = (yDataMax[1] - yDataMin[1]) / hLines;
        // find base >= input, base = 10**n
        int base0 = 1;
        while (step0 > base0) {
            base0 *= 10;
        }
        int base1 = 1;
        while (step1 > base1) {
            base1 *= 10;
        }
        // 0.1 <= step < 1
        step0 /= base0;
        step1 /= base1;
        // find rounded/1 up and down values
        float down0, up0, down1, up1;
        if (step0 < 0.2) {
            down0 = 0.1f / step0;
            up0 = 0.2f / step0;
        } else if (step0 < 0.5) {
            down0 = 0.2f / step0;
            up0 = 0.5f / step0;
        } else {
            down0 = 0.5f / step0;
            up0 = 1 / step0;
        }
        if (step1 < 0.2) {
            down1 = 0.1f / step1;
            up1 = 0.2f / step1;
        } else if (step1 < 0.5) {
            down1 = 0.2f / step1;
            up1 = 0.5f / step1;
        } else {
            down1 = 0.5f / step1;
            up1 = 1 / step1;
        }
        // find min difference
        float value = Math.abs(down0 - down1);
        float min = value;
        int n = 0; // 0 - down0-down1, 1 - down0-up1, 2 - up0-down1, 3 - up0-up1
        value = Math.abs(down0 - up1);
        if (value < min) {
            min = value;
            n = 1;
        }
        value = Math.abs(up0 - down1);
        if (value < min) {
            min = value;
            n = 2;
        }
        value = Math.abs(up0 - up1);
        if (value < min) {
            n = 3;
        }
        // calculate yStep
        if (step0 < 0.2) {
            y0Step = n < 2 ? (base0 / 10) : (base0 / 5);
        } else if (step0 < 0.5) {
            y0Step = n < 2 ? (base0 / 5) : (base0 / 2);
        } else {
            y0Step = n < 2 ? (base0 / 2) : base0;
        }
        if (step1 < 0.2) {
            y1Step = n == 0 || n == 2 ? (base1 / 10) : (base1 / 5);
        } else if (step1 < 0.5) {
            y1Step = n == 0 || n == 2 ? (base1 / 5) : (base1 / 2);
        } else {
            y1Step = n == 0 || n == 2 ? (base1 / 2) : base1;
        }
        // base line - the lowest visible grid line
        y0Base = (yDataMin[0] / y0Step) * y0Step + y0Step;
        y1Base = (yDataMin[1] / y1Step) * y1Step + y1Step;
        // find distance from base line to the lowest point, in 1/step
        float dist0 = (float) (y0Base - yDataMin[0]) / y0Step;
        float dist1 = (float) (y1Base - yDataMin[1]) / y1Step;
        // find lowest chart, set data minimum
        if (dist0 > dist1) {
            yMin[0] = yDataMin[0];
            yMin[1] = y1Base - dist0 * y1Step;
        } else {
            yMin[0] = y0Base - dist1 * y0Step;
            yMin[1] = yDataMin[1];
        }
        // find distance from base line to the highest point, in 1/step
        dist0 = (float) (yDataMax[0] - y0Base) / y0Step;
        dist1 = (float) (yDataMax[1] - y1Base) / y1Step;
        // find highest chart, set data interval
        if (dist0 > dist1) {
            yInterval[0] = (float) yDataMax[0] - yMin[0];
            yInterval[1] = y1Base + dist0 * y1Step - yMin[1];
        } else {
            yInterval[0] = y0Base + dist1 * y0Step - yMin[0];
            yInterval[1] = (float) yDataMax[1] - yMin[1];
        }
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
                for (int chartN = 0; chartN < 2; chartN++) {
                    chart[chartN] = stopChart[chartN];
                    paintChart[chartN].setAlpha(255);
                    paintChartGridText[chartN].setAlpha(255);
                }
            } else {
                for (int chartN = 0; chartN < 2; chartN++) {
                    paintChart[chartN].setAlpha(startAlpha[chartN] + (int) (deltaAlpha[chartN] * frame));
                    paintChartGridText[chartN].setAlpha(startAlpha[chartN] + (int) (deltaAlpha[chartN] * frame));
                }
            }
            invalidate();
        }
    }
}