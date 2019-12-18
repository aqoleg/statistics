package space.aqoleg.statistics.summary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.ActivityMain;
import space.aqoleg.statistics.detailed.ViewDetailed;

public class ViewSummary extends View implements View.OnTouchListener {
    // animation constants
    static final int TOTAL_FRAMES = 10;
    static final int DELAY = 20; // ms
    // states of the selector
    private static final int MOVE_LEFT = 1;
    private static final int MOVE_RIGHT = 2;
    private static final int MOVE_BOTH = 3;
    // other objects
    private ActivityMain activity;
    private ViewDetailed viewDetailed;
    // canvas objects
    private Paint paintSelector = new Paint();
    private Paint paintBackground = new Paint();
    // layout constants, in px, side padding of the background 16dp, selector 6dp * 1dp
    float density; // =(px / dp)
    float chartWidthPx; // width of the chart without padding, =(width - 2*16dp - 2*6dp)
    float chartHeightPx; // height of the chart without padding, =(height - 2*1dp)
    float chartLeftPx; // left side of the chart, =(16dp + 6dp)
    float chartRightPx; // right side of the chart, =(width - 16dp - 6dp)
    float chartBottomPx; // bottom of the chart, =(height - 1dp)
    // selector constants, in px, count from the inner side of the selector
    private float thresholdPx; // distance into the selector between move both sides and one side, 8dp
    private float minIntervalPx; // min width of the selector, 32dp
    // selector values, px
    private float leftPx; // left side of the selector
    private float rightPx; // right side of the selector
    private float offsetPx; // =(x - left), or =(x - right) for MOVE_RIGHT
    private float intervalPx; // =(right - left)
    // current state
    private boolean day; // day theme
    private float leftPos; // left side of the selector, 0 <= leftPos < 1
    private float rightPos; // right side of the selector, 0 < rightPos <= 1, leftPos < rightPos
    private int moveMode; // current state of the selector

    // default View constructor
    ViewSummary(Context context) {
        super(context);
    }

    public static ViewSummary create(
            ActivityMain activity,
            ViewDetailed viewDetailed,
            boolean day,
            int dataSetN,
            boolean zoom,
            float left,
            float right,
            int state
    ) {
        ViewSummary view;
        switch (dataSetN) {
            case 0:
                view = new ViewSummary0(activity).initialize(state);
                break;
            case 1:
                view = new ViewSummary1(activity).initialize(state);
                break;
            case 2:
                view = new ViewSummary2(activity).initialize(state);
                break;
            case 3:
                if (zoom) {
                    view = new ViewSummary3Zoom(activity).initialize(state);
                } else {
                    view = new ViewSummary3(activity).initialize();
                }
                break;
            case 4:
                view = new ViewSummary4(activity).initialize(state);
                break;
            default:
                view = new ViewSummary(activity);
                break;
        }
        view.activity = activity;
        view.viewDetailed = viewDetailed;
        view.day = day;
        view.leftPos = left;
        view.rightPos = right;
        view.setOnTouchListener(view);
        return view;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set layout constants
            density = activity.getResources().getDisplayMetrics().density;
            chartWidthPx = getWidth() - density * 44;
            chartHeightPx = getHeight() - density * 2;
            chartLeftPx = density * 22;
            chartRightPx = getWidth() - density * 22;
            chartBottomPx = getHeight() - density;
            thresholdPx = density * 8;
            minIntervalPx = density * 32;
            // set selection
            leftPx = chartLeftPx + leftPos * chartWidthPx;
            rightPx = chartLeftPx + rightPos * chartWidthPx;
            // set paints
            paintSelector.setStyle(Paint.Style.STROKE);
            paintSelector.setStrokeWidth(density * 6); // 6dp
            paintBackground.setStyle(Paint.Style.FILL);
            if (day) {
                paintSelector.setARGB(127, 134, 169, 196);
                paintBackground.setARGB(153, 226, 238, 249);
            } else {
                paintSelector.setARGB(127, 111, 137, 158);
                paintBackground.setARGB(153, 48, 66, 89);
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        float xPx = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // determines move mode
                if (xPx < leftPx + thresholdPx) {
                    moveMode = MOVE_LEFT;
                    offsetPx = xPx - leftPx;
                } else if (xPx < rightPx - thresholdPx) {
                    moveMode = MOVE_BOTH;
                    offsetPx = xPx - leftPx;
                    intervalPx = rightPx - leftPx;
                } else {
                    moveMode = MOVE_RIGHT;
                    offsetPx = xPx - rightPx;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // moving selector
                switch (moveMode) {
                    case MOVE_BOTH:
                        leftPx = xPx - offsetPx;
                        if (leftPx < chartLeftPx) {
                            leftPx = chartLeftPx;
                        }
                        rightPx = leftPx + intervalPx;
                        if (rightPx > chartRightPx) {
                            rightPx = chartRightPx;
                            leftPx = rightPx - intervalPx;
                        }
                        leftPos = (leftPx - chartLeftPx) / chartWidthPx;
                        rightPos = (rightPx - chartLeftPx) / chartWidthPx;
                        break;
                    case MOVE_LEFT:
                        leftPx = xPx - offsetPx;
                        if (leftPx < chartLeftPx) {
                            leftPx = chartLeftPx;
                        } else if (leftPx > rightPx - minIntervalPx) {
                            leftPx = rightPx - minIntervalPx;
                        }
                        leftPos = (leftPx - chartLeftPx) / chartWidthPx;
                        break;
                    case MOVE_RIGHT:
                        rightPx = xPx - offsetPx;
                        if (rightPx > chartRightPx) {
                            rightPx = chartRightPx;
                        } else if (rightPx < leftPx + minIntervalPx) {
                            rightPx = leftPx + minIntervalPx;
                        }
                        rightPos = (rightPx - chartLeftPx) / chartWidthPx;
                        break;
                }
                // refresh charts
                invalidate();
                viewDetailed.move(leftPos, rightPos);
        }
        return true;
    }

    // getter for onSaveInstanceState
    public float getLeftPos() {
        return leftPos;
    }

    // getter for onSaveInstanceState
    public float getRightPos() {
        return rightPos;
    }

    // changes line visibility
    public void changeState(int state) {
    }

    void drawSelector(Canvas canvas) {
        // draw background, side padding 16dp
        canvas.drawRect(
                density * 16,
                0,
                leftPx,
                getHeight(),
                paintBackground
        );
        canvas.drawRect(
                rightPx,
                0,
                getWidth() - density * 16,
                getHeight(),
                paintBackground
        );
        // draw selector, 6dp * 1dp
        canvas.drawRect(
                leftPx - density * 3,
                -density * 2,
                rightPx + density * 3,
                getHeight() + density * 2,
                paintSelector
        );
    }
}