package space.aqoleg.statistics.detailed;

import android.content.Context;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import space.aqoleg.statistics.ActivityMain;
import space.aqoleg.statistics.Data;
import space.aqoleg.statistics.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ViewDetailed extends View implements View.OnTouchListener {
    // animation constants
    static final int TOTAL_FRAMES = 10;
    static final int DELAY = 20; // ms
    // grid constants
    static long X_START; // 00:00 at the current timezone
    long xMinStep; // day or hour
    float vLines; // number of the vertical lines
    float hLines; // number of the horizontal lines
    // other objects
    SimpleDateFormat gridSdf;
    SimpleDateFormat pointSdf;
    Date date = new Date();
    private ActivityMain activity;
    // canvas objects
    Paint paintBox = new Paint();
    Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint paintGridText = new Paint(Paint.ANTI_ALIAS_FLAG);
    // data x min, max
    long xMin;
    long xMax;
    long xInterval; // =(xMax - xMin)
    private long x0; // xMin of the whole data
    private long xWholeInterval; // =(xMax - xMin) of the whole data
    // layout constants, side padding 16dp, bottom padding 16dp, top padding 1dp
    float density; // =(px / dp)
    float chartWidthPx; // width of the chart without padding, =(width - 2*16dp)
    float chartHeightPx; // height of the chart without padding, =(height - 1dp - 16dp)
    float chartLeftPx; // left side of the chart, =16dp
    float chartCenterPx; // center of the chart, =(width / 2)
    float chartRightPx; // right side of the chart, =(width - 16dp)
    float chartBottomPx; // bottom of the chart, =(height - 1dp)
    // box coordinates
    float boxLeftPx;
    float boxRightPx;
    float boxTopPx;
    float boxBottomPx;
    // current state
    boolean day; // day theme
    boolean zoom;
    int iStart; // index of the left point
    int iStop; // index of the right point
    int iSelected = 0; // index of the selected point, if 0 there is none
    private float leftPos; // left side of the selector, 0 <= leftPos < 1
    private float rightPos; // right side of the selector, 0 < rightPos <= 1, leftPos < rightPos
    private boolean stop = false; // do not handle onTouch move event
    private float prevPx; // previous onTouch() value

    static {
        X_START = -TimeZone.getDefault().getRawOffset();
    }

    // default View constructor
    ViewDetailed(Context context) {
        super(context);
    }

    public static ViewDetailed create(
            ActivityMain activity,
            boolean day,
            int dataSetN,
            boolean zoom,
            float left,
            float right,
            int state
    ) {
        ViewDetailed view;
        switch (dataSetN) {
            case 0:
                view = new ViewDetailed0(activity);
                break;
            case 1:
                view = new ViewDetailed1(activity);
                break;
            case 2:
                view = new ViewDetailed2(activity);
                break;
            case 3:
                if (zoom) {
                    view = new ViewDetailed3Zoom(activity);
                } else {
                    view = new ViewDetailed3(activity);
                }
                break;
            case 4:
                if (zoom) {
                    view = new ViewDetailed4Zoom(activity);
                } else {
                    view = new ViewDetailed4(activity);
                }
                break;
            default:
                view = new ViewDetailed(activity);
                break;
        }
        view.activity = activity;
        view.day = day;
        view.zoom = zoom;
        view.leftPos = left;
        view.rightPos = right;
        if (zoom) {
            view.xMinStep = 3600000; // hour
            view.gridSdf = new SimpleDateFormat(activity.getString(R.string.detailedGridSdf), Locale.getDefault());
            if (dataSetN == 3) {
                view.pointSdf = new SimpleDateFormat(activity.getString(R.string.detailedGridSdf), Locale.getDefault());
            } else {
                view.pointSdf = new SimpleDateFormat(activity.getString(R.string.detailedPointSdf), Locale.getDefault());
            }
        } else {
            view.xMinStep = 24 * 3600000; // day
            view.gridSdf = new SimpleDateFormat(activity.getString(R.string.gridSdf), Locale.getDefault());
            view.pointSdf = new SimpleDateFormat(activity.getString(R.string.pointSdf), Locale.getDefault());
        }
        view.findMinMax();
        view.initialize(state);
        view.setOnTouchListener(view);
        return view;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // set layout constants
            density = activity.getResources().getDisplayMetrics().density;
            chartWidthPx = getWidth() - density * 32;
            chartHeightPx = getHeight() - density * 17;
            chartLeftPx = density * 16;
            chartCenterPx = getWidth() / 2;
            chartRightPx = getWidth() - density * 16;
            chartBottomPx = getHeight() - density * 16;
            // set grid constants
            vLines = chartWidthPx / (density * 60);
            hLines = chartHeightPx / (density * 50);
            // set paints
            paintBox.setStyle(Paint.Style.FILL);
            paintGrid.setStyle(Paint.Style.STROKE);
            paintGrid.setStrokeWidth(density * 0.5f);
            paintGridText.setTextSize(density * 10);
            if (day) {
                paintBox.setARGB(230, 255, 255, 255);
                paintGrid.setARGB(126, 24, 45, 59);
                paintGridText.setARGB(255, 24, 45, 59);
            } else {
                paintBox.setARGB(230, 0, 0, 0);
                paintGrid.setARGB(126, 255, 255, 255);
                paintGridText.setARGB(255, 255, 255, 255);
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        float xPx = event.getX();
        // indexes and px values of the nearest points
        int iRight, iLeft;
        float rightPx, leftPx;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // if touch box, zoom in
                stop = !zoom && iSelected != 0 &&
                        xPx > boxLeftPx && xPx < boxRightPx && event.getY() > boxTopPx && event.getY() < boxBottomPx;
                if (stop) {
                    activity.zoomIn(Data.getX(iSelected));
                    return true;
                }
                activity.zoomButton(true);
                prevPx = xPx;
                // find nearest right point
                iRight = iStart + 1;
                do {
                    rightPx = chartLeftPx + chartWidthPx * (Data.getX(iRight) - xMin) / xInterval;
                    if (rightPx > xPx) {
                        break;
                    }
                    iRight++;
                } while (iRight <= iStop);
                if (iRight > iStop) { // if only two dots
                    iRight = iStop;
                }
                // find nearest left point
                iLeft = iRight - 1;
                leftPx = chartLeftPx + chartWidthPx * (Data.getX(iLeft) - xMin) / xInterval;
                // find the nearest from two points and reload
                if ((rightPx + leftPx) / 2 < xPx) {
                    iSelected = iRight;
                } else {
                    iSelected = iLeft;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (stop) {
                    return true;
                }
                // search new point
                if (xPx >= prevPx) {
                    // move to the right
                    iRight = iSelected;
                    do {
                        rightPx = chartLeftPx + chartWidthPx * (Data.getX(iRight) - xMin) / xInterval;
                        if (rightPx > xPx) {
                            break;
                        }
                        iRight++;
                    } while (iRight <= iStop);
                    if (iRight > iStop) { // if iSelected == iStop
                        iRight = iStop;
                    }
                    iLeft = iRight == iStart ? iRight : iRight - 1;
                    leftPx = chartLeftPx + chartWidthPx * (Data.getX(iLeft) - xMin) / xInterval;
                } else {
                    // move to the left
                    iLeft = iSelected;
                    do {
                        leftPx = chartLeftPx + chartWidthPx * (Data.getX(iLeft) - xMin) / xInterval;
                        if (leftPx < xPx) {
                            break;
                        }
                        iLeft--;
                    } while (iLeft >= iStart);
                    if (iLeft < iStart) { // if iSelected == iStop
                        iLeft = iStart;
                    }
                    iRight = iLeft == iStop ? iLeft : iLeft + 1;
                    rightPx = chartLeftPx + chartWidthPx * (Data.getX(iRight) - xMin) / xInterval;
                }
                prevPx = xPx;
                // find the nearest from two points and reload if it was changed
                if ((rightPx + leftPx) / 2 < xPx) {
                    if (iSelected != iRight) {
                        iSelected = iRight;
                        invalidate();
                    }
                } else if (iSelected != iLeft) {
                    iSelected = iLeft;
                    invalidate();
                }
                break;
        }
        return true;
    }

    public boolean move(float leftPos, float rightPos) {
        activity.zoomButton(false);
        iSelected = 0;
        // find new iStart
        int newIStart = iStart;
        if (leftPos != this.leftPos) {
            xMin = x0 + (long) (leftPos * xWholeInterval);
            if (leftPos > this.leftPos) {
                // move to the right till find new value
                while (true) {
                    if (Data.getX(newIStart) > xMin) {
                        newIStart--;
                        break;
                    }
                    newIStart++;
                }
            } else {
                // move to the left till find new value
                while (newIStart != 1) {
                    if (Data.getX(newIStart) < xMin) {
                        break;
                    }
                    newIStart--;
                }
            }
            this.leftPos = leftPos;
        }
        // find new iStop
        int newIStop = iStop;
        if (rightPos != this.rightPos) {
            xMax = x0 + (long) (rightPos * xWholeInterval);
            int stop = Data.getLength() - 1;
            if (rightPos > this.rightPos) {
                // move to the right till find new value
                while (newIStop != stop) {
                    if (Data.getX(newIStop) > xMax) {
                        break;
                    }
                    newIStop++;
                }
            } else {
                // move to the left till find new value
                while (true) {
                    if (Data.getX(newIStop) < xMax) {
                        newIStop++;
                        break;
                    }
                    newIStop--;
                }
            }
            this.rightPos = rightPos;
        }
        // set interval
        xInterval = xMax - xMin;
        // if data was changed, return true
        if (iStart != newIStart || iStop != newIStop) {
            iStart = newIStart;
            iStop = newIStop;
            return true;
        }
        return false;
    }

    // getter for zoom-in
    public long getTime() {
        return iSelected == 0 ? 0 : Data.getX(iSelected);
    }

    public void changeState(int state) {
        activity.zoomButton(false);
    }

    void initialize(int state) {
    }

    // set data x min and max, iStart and iStop
    private void findMinMax() {
        // x whole interval
        x0 = Data.getX(1);
        xWholeInterval = Data.getX(Data.getLength() - 1) - x0;
        // xMin, iStart
        xMin = x0 + (long) (leftPos * xWholeInterval);
        iStart = 1;
        while (true) {
            if (Data.getX(iStart) > xMin) {
                if (iStart != 1) {
                    iStart--;
                }
                break;
            }
            iStart++;
        }
        // xMax, iStop
        xMax = x0 + (long) (rightPos * xWholeInterval);
        iStop = iStart + 1;
        int stop = Data.getLength() - 1;
        while (iStop < stop) {
            if (Data.getX(iStop) > xMax) {
                break;
            }
            iStop++;
        }
        // set interval and y values
        xInterval = xMax - xMin;
    }

    // return closest number, starts with 1, or 2, or 5 and ends with zeros
    static int round(float input) {
        // find n >= input, n = 10**n
        int n = 1;
        while (input > n) {
            n *= 10;
        }
        // 0.1 <= normalized < 1
        float normalized = input / n;
        if (normalized < 0.15) {
            n = n / 10; // 1..
        } else if (normalized < 0.35) {
            n = n / 5; // 2...
        } else if (normalized < 0.75) {
            n = n / 2; // 5...
        }
        if (n == 0) {
            return 1;
        }
        return n;
    }

    static String stripZeros(int value) {
        if (value < 10000) {
            return String.valueOf(value);
        } else if (value < 10000000) {
            return String.valueOf(value / 1000).concat("K");
        } else {
            return String.valueOf(value / 1000000).concat("M");
        }
    }
}