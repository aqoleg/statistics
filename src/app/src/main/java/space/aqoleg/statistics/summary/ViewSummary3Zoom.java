package space.aqoleg.statistics.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import space.aqoleg.statistics.Data;

class ViewSummary3Zoom extends ViewSummary {
    // canvas objects
    private Paint[] paintChart = new Paint[3];
    private Path[] path = new Path[3];
    // data values
    private long xMin;
    private long xInterval; // =(xMax - xMin)
    private int[] yDataMin = new int[3];
    private int[] yDataMax = new int[3];
    private int yMin;
    private int yInterval; // =(yMax - yMin)
    // current state
    private boolean drawPath = true; // false when path did not changes
    private boolean[] chart = new boolean[3]; // charts visibility
    // animation
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
    ViewSummary3Zoom(Context context) {
        super(context);
    }

    ViewSummary3Zoom initialize(int state) {
        for (int chartN = 0; chartN < 3; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        findMinMax();
        return this;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            for (int chartN = 0; chartN < 3; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.STROKE);
                paintChart[chartN].setStrokeWidth(density); // 1dp
                paintChart[chartN].setPathEffect(new CornerPathEffect(density)); // smooth
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
        if (drawPath) {
            for (chartN = 0; chartN < 3; chartN++) {
                path[chartN].reset();
                path[chartN].moveTo(
                        chartLeftPx,
                        chartBottomPx - chartHeightPx * (Data.getY(chartN, 1) - yMin) / yInterval
                );
            }
            float xPx;
            int length = Data.getLength();
            for (int i = 2; i < length; i++) {
                xPx = chartLeftPx + chartWidthPx * (Data.getX(i) - xMin) / xInterval;
                for (chartN = 0; chartN < 3; chartN++) {
                    path[chartN].lineTo(
                            xPx,
                            chartBottomPx - chartHeightPx * (Data.getY(chartN, i) - yMin) / yInterval
                    );
                }
            }
            drawPath = false;
        }
        // draw lines
        for (chartN = 0; chartN < 3; chartN++) {
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
        if (stopChart[0]) {
            stopYMin = yDataMin[0];
        }
        if (stopChart[1] && yDataMin[1] < stopYMin) {
            stopYMin = yDataMin[1];
        }
        if (stopChart[2] && yDataMin[2] < stopYMin) {
            stopYMin = yDataMin[2];
        }
        int max = 0;
        if (stopChart[0]) {
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

    private void findMinMax() {
        // x
        xMin = Data.getX(1);
        xInterval = Data.getX(Data.getLength() - 1) - xMin;
        // y
        for (int chartN = 0; chartN < 3; chartN++) {
            int yMax = Data.getY(chartN, 1);
            int yMin = yMax;
            int value;
            int length = Data.getLength();
            for (int i = 2; i < length; i++) {
                value = Data.getY(chartN, i);
                if (value < yMin) {
                    yMin = value;
                } else if (value > yMax) {
                    yMax = value;
                }
            }
            this.yDataMin[chartN] = yMin;
            this.yDataMax[chartN] = yMax;
        }
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
        int max = 0;
        if (chart[0]) {
            max = yDataMax[0];
        }
        if (chart[1] && yDataMax[1] > max) {
            max = yDataMax[1];
        }
        if (chart[2] && yDataMax[2] > max) {
            max = yDataMax[2];
        }
        yInterval = max - yMin;
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
            } else {
                for (int chartN = 0; chartN < 3; chartN++) {
                    paintChart[chartN].setAlpha(startAlpha[chartN] + (int) (deltaAlpha[chartN] * frame));
                }
                yMin = startYMin + (int) (frame * deltaYMin);
                yInterval = startYInterval + (int) (frame * deltaYInterval);
            }
            drawPath = true;
            invalidate();
        }
    }
}