package space.aqoleg.statistics.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import space.aqoleg.statistics.Data;

class ViewSummary2 extends ViewSummary {
    // canvas objects
    private Paint[] paintChart = new Paint[7];
    private Path[] path = new Path[7];
    // data values
    private int yMax;
    // bar constant
    private float barWidthPx;
    // current state
    private boolean drawPath = true; // false when path did not changes
    private boolean[] chart = new boolean[7]; // charts visibility
    // animation
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private float frame; // current frame of the animation
    private boolean[] animate = new boolean[7]; // is animate
    private boolean[] stopChart = new boolean[7]; // target value of the charts visibility
    private int startYMax; // yMax on the beginning of the animation
    private float deltaYMax; // change of the yMax on each frame
    private int stopYMax; // yMax at the end of the animation

    // default View constructor
    ViewSummary2(Context context) {
        super(context);
    }

    ViewSummary2 initialize(int state) {
        for (int chartN = 0; chartN < 7; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        yMax = findMax(chart);
        return this;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            barWidthPx = chartWidthPx / (Data.getLength() - 2);
            for (int chartN = 0; chartN < 7; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint();
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.FILL);
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
        if (drawPath) {
            float xPx = chartLeftPx - barWidthPx / 2;
            for (chartN = 0; chartN < 7; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(chartRightPx + barWidthPx / 2, chartBottomPx);
                path[chartN].lineTo(xPx, chartBottomPx);
            }
            float animateIn = frame / TOTAL_FRAMES;
            float animateOut = 1 - animateIn;
            float[] y = {0, 0, 0, 0, 0, 0, 0};
            float yPx, ySum;
            int length = Data.getLength();
            for (int i = 1; i < length; i++) {
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
            drawPath = false;
        }
        // draw lines
        for (chartN = 6; chartN >= 0; chartN--) {
            if (chart[chartN]) {
                canvas.drawPath(path[chartN], paintChart[chartN]);
            }
        }
        drawSelector(canvas);
    }

    @Override
    public void changeState(int state) {
        // remove any animation
        for (Animation animation : animations) {
            handler.removeCallbacks(animation);
        }
        // set animation values
        for (int chartN = 0; chartN < 7; chartN++) {
            stopChart[chartN] = (state & (1 << chartN)) != 0;
            animate[chartN] = stopChart[chartN] != chart[chartN];
            if (animate[chartN]) {
                chart[chartN] = true;
            }
        }
        startYMax = yMax;
        stopYMax = findMax(stopChart);
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
    private static int findMax(boolean[] chart) {
        int max = 0;
        int value;
        int length = Data.getLength();
        for (int i = 1; i < length; i++) {
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
            if (frame == TOTAL_FRAMES) {
                // end of the animation, set stop values
                yMax = stopYMax;
                for (int chartN = 0; chartN < 7; chartN++) {
                    chart[chartN] = stopChart[chartN];
                    animate[chartN] = false;
                }
            } else {
                yMax = startYMax + (int) (frame * deltaYMax);
            }
            drawPath = true;
            invalidate();
        }
    }
}