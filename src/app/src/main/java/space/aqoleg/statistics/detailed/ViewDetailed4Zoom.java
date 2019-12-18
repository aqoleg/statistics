package space.aqoleg.statistics.detailed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.Data;

class ViewDetailed4Zoom extends ViewDetailed {
    // canvas objects
    private RectF circle;
    private Paint[] paintChart = new Paint[6];
    private Paint[] paintChartText = new Paint[6];
    private Path[] path = new Path[6];
    // data values
    private long[] yData = new long[6];
    private long ySum;
    // layout values
    private float radiusPx;
    private float chartCenterYPx;
    private float[] angles = new float[6];
    // current state
    private boolean[] chart = new boolean[6]; // charts visibility
    private boolean drawPath = true; // false when path did not change
    private int selectedChart = -1; // -1 if there is no selection
    // animation values
    private Handler handler = new Handler();
    private Animation[] animations = new Animation[TOTAL_FRAMES];
    private float frame; // current frame of the animation
    private boolean[] animate = new boolean[6]; // is animate
    private boolean[] stopChart = new boolean[6]; // target value of the charts visibility

    // default View constructor
    ViewDetailed4Zoom(Context context) {
        super(context);
    }

    @Override
    void initialize(int state) {
        for (int chartN = 0; chartN < 6; chartN++) {
            chart[chartN] = (state & (1 << chartN)) != 0;
        }
        findData();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set circle
            radiusPx = (chartHeightPx < chartWidthPx ? chartHeightPx : chartWidthPx - density * 16) / 2;
            chartCenterYPx = density + chartHeightPx / 2;
            circle = new RectF(
                    chartCenterPx - radiusPx,
                    chartCenterYPx - radiusPx,
                    chartCenterPx + radiusPx,
                    chartCenterYPx + radiusPx
            );
            // set paints
            for (int chartN = 0; chartN < 6; chartN++) {
                path[chartN] = new Path();
                paintChart[chartN] = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintChart[chartN].setColor(Color.parseColor(Data.getColor(chartN)));
                paintChart[chartN].setStyle(Paint.Style.FILL);
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
        if (drawPath) {
            float animateIn = frame / TOTAL_FRAMES;
            float animateOut = 1 - animateIn;
            ySum = 0;
            int charts = 0;
            for (chartN = 0; chartN < 6; chartN++) {
                if (chart[chartN]) {
                    charts++;
                    if (!animate[chartN]) {
                        ySum += yData[chartN];
                    } else {
                        ySum += yData[chartN] * (stopChart[chartN] ? animateIn : animateOut);
                    }
                }
            }
            float angle = -90;
            for (chartN = 0; chartN < 6; chartN++) {
                if (chart[chartN]) {
                    charts--;
                    if (!animate[chartN]) {
                        angles[chartN] = 360f * yData[chartN] / ySum;
                    } else {
                        angles[chartN] = 360f * yData[chartN] * (stopChart[chartN] ? animateIn : animateOut) / ySum;
                    }
                    path[chartN].reset();
                    path[chartN].addArc(
                            circle,
                            angle,
                            charts == 0 ? 270f - angle : angles[chartN]
                    );
                    path[chartN].lineTo(chartCenterPx, chartCenterYPx);
                    path[chartN].close();
                    angle += angles[chartN];
                }
            }
            drawPath = false;
        }
        // draw charts
        for (chartN = 0; chartN < 6; chartN++) {
            if (chart[chartN]) {
                canvas.drawPath(path[chartN], paintChart[chartN]);
            }
        }
        // draw selection
        if (selectedChart != -1) {
            // box padding 8dp, width 64dp
            boxLeftPx = chartCenterPx - density * 32;
            boxTopPx = chartCenterYPx - density * 32;
            boxRightPx = chartCenterPx + density * 32;
            boxBottomPx = boxTopPx + density * 56;
            // draw background of the box
            canvas.drawRect(boxLeftPx, boxTopPx, boxRightPx, boxBottomPx, paintBox);
            canvas.drawRect(boxLeftPx, boxTopPx, boxRightPx, boxBottomPx, paintGrid);
            // draw values
            canvas.drawText(
                    Data.getName(selectedChart),
                    boxLeftPx + density * 8,
                    boxTopPx + density * 14,
                    paintGridText
            );
            canvas.drawText(
                    String.valueOf(yData[selectedChart]),
                    boxLeftPx + density * 8,
                    boxTopPx + density * 32,
                    paintChartText[selectedChart]
            );
            canvas.drawText(
                    String.valueOf(Math.round(100f / ySum * yData[selectedChart])).concat(" %"),
                    boxLeftPx + density * 8,
                    boxTopPx + density * 48,
                    paintChartText[selectedChart]
            );
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        float r = (float) Math.sqrt(Math.pow(event.getX() - chartCenterPx, 2) + Math.pow(event.getY() - chartCenterYPx, 2));
        if (r > radiusPx) {
            if (selectedChart != -1) {
                selectedChart = -1;
                invalidate();
            }
            return true;
        }
        float angle = (float) (180 * Math.acos((event.getX() - chartCenterPx) / r) / Math.PI);
        angle = (event.getY() > chartCenterYPx) ? 90 + angle : 90 - angle;
        if (angle < 0) {
            angle += 360;
        }
        for (int chartN = 0; chartN < 6; chartN++) {
            if (chart[chartN]) {
                angle -= angles[chartN];
                if (angle < 0) {
                    selectedChart = chartN;
                    break;
                }
            }
        }
        invalidate();
        return true;
    }

    @Override
    public boolean move(float leftPos, float rightPos) {
        if (super.move(leftPos, rightPos)) {
            findData();
            drawPath = true;
            invalidate();
        }
        return true;
    }

    @Override
    public void changeState(int state) {
        // remove any animation
        for (Animation animation : animations) {
            handler.removeCallbacks(animation);
        }
        selectedChart = -1;
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

    private void findData() {
        long sum;
        for (int chartN = 0; chartN < 6; chartN++) {
            sum = 0;
            for (int i = iStart + 1; i < iStop; i++) {
                sum += Data.getY(chartN, i);
            }
            yData[chartN] = sum;
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