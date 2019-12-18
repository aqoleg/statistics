package space.aqoleg.statistics.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import space.aqoleg.statistics.Data;

class ViewSummary4 extends ViewSummary {
    // canvas objects
    private Paint[] paintChart = new Paint[6];
    private Path[] path = new Path[6];
    // data values
    private long xMin;
    private long xInterval; // =(xMax - xMin)
    // current state
    private boolean drawPath = true; // false when path did not changes
    private boolean[] chart = new boolean[6]; // charts visibility
    // animation
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private float frame; // current frame of the animation
    private boolean[] animate = new boolean[6]; // is animate
    private boolean[] stopChart = new boolean[6]; // target value of the charts visibility

    // default View constructor
    ViewSummary4(Context context) {
        super(context);
    }

    ViewSummary4 initialize(int state) {
        for (int chartN = 0; chartN < 6; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        xMin = Data.getX(1);
        xInterval = Data.getX(Data.getLength() - 1) - xMin;
        return this;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            for (int chartN = 0; chartN < 6; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.FILL);
                paintChart[chartN].setPathEffect(new CornerPathEffect(density)); // smooth
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
        float xPx;
        if (drawPath) {
            for (chartN = 0; chartN < 6; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(chartRightPx, chartBottomPx);
                path[chartN].lineTo(chartLeftPx, chartBottomPx);
            }
            float animateIn = frame / TOTAL_FRAMES;
            float animateOut = 1 - animateIn;
            float[] y = {0, 0, 0, 0, 0, 0};
            float ySum;
            int length = Data.getLength();
            for (int i = 1; i < length; i++) {
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
            drawPath = false;
        }
        // draw lines
        for (chartN = 5; chartN >= 0; chartN--) {
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