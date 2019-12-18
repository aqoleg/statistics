package space.aqoleg.statistics;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import space.aqoleg.statistics.detailed.ViewDetailed;
import space.aqoleg.statistics.summary.ViewSummary;

public class ActivityMain extends Activity implements View.OnClickListener {
    // default selection
    private static final float LEFT = 0.75f;
    private static final float RIGHT = 1;
    private static final float LEFT_ZOOM = 3f / 7;
    private static final float RIGHT_ZOOM = 4f / 7;
    // sharedPreferences
    private static final String PREFS = "SP";
    private static final String PREFS_THEME_DAY = "SPt"; // theme, bool
    // savedInstanceState keys
    private static final String KEY_DATA_SET_N = "Kd"; // number of the data set, int
    private static final String KEY_TIME = "Kt"; // zoom-in time, long
    private static final String KEY_LEFT = "Kl"; // left boundary of the selected interval, float
    private static final String KEY_RIGHT = "Kr"; // right boundary of the selected interval, float
    private static final String KEY_LEFT_ZOOM = "Klz"; // left boundary of the zoom-in selected interval, float
    private static final String KEY_RIGHT_ZOOM = "Krz"; // right boundary of the zoom-in selected interval, float
    private static final String KEY_STATE = "Ks"; // checkboxes states, int
    // views
    private ViewDetailed viewDetailed;
    private ViewSummary viewSummary;
    private Buttons buttons;
    // current state
    private boolean themeDay; // theme, true if day
    private int dataSetN = 0; // 0 <= dataSetN < 5
    private long time = 0; // unix time, non-zero if zoom
    private float left = LEFT; // left boundary, 0 <= left < 1
    private float right = RIGHT; // right boundary, 0 < right <= 1, left < right
    private float leftZoom; // left zoom-in boundary, 0 <= leftZoom < 1
    private float rightZoom; // right zoom-in boundary, 0 < rightZoom <= 1, leftZoom < rightZoom
    private int state = ~0; // visibility, bit mask at (1 << n)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set theme before onCreate
        themeDay = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREFS_THEME_DAY, true);
        setTheme(themeDay ? R.style.day : R.style.night);
        // create activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        if (savedInstanceState != null) {
            dataSetN = savedInstanceState.getInt(KEY_DATA_SET_N);
            time = savedInstanceState.getLong(KEY_TIME);
            left = savedInstanceState.getFloat(KEY_LEFT);
            right = savedInstanceState.getFloat(KEY_RIGHT);
            leftZoom = savedInstanceState.getFloat(KEY_LEFT_ZOOM);
            rightZoom = savedInstanceState.getFloat(KEY_RIGHT_ZOOM);
            state = savedInstanceState.getInt(KEY_STATE);
        }
        findViewById(R.id.subtitle).setOnClickListener(this);
        findViewById(R.id.about).setOnClickListener(this);
        load(false, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_DATA_SET_N, dataSetN);
        outState.putLong(KEY_TIME, time);
        outState.putFloat(KEY_LEFT, time == 0 ? viewSummary.getLeftPos() : left);
        outState.putFloat(KEY_RIGHT, time == 0 ? viewSummary.getRightPos() : right);
        outState.putFloat(KEY_LEFT_ZOOM, time == 0 ? leftZoom : viewSummary.getLeftPos());
        outState.putFloat(KEY_RIGHT_ZOOM, time == 0 ? rightZoom : viewSummary.getRightPos());
        outState.putInt(KEY_STATE, buttons.getState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (time == 0) {
            super.onBackPressed();
        } else {
            time = 0;
            leftZoom = viewSummary.getLeftPos();
            rightZoom = viewSummary.getRightPos();
            if (dataSetN == 4) {
                left = leftZoom;
                right = rightZoom;
            }
            state = buttons.getState();
            load(true, AnimationUtils.loadAnimation(this, R.anim.in_left));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.next: // load next data set
                dataSetN++;
                if (dataSetN == 5) {
                    dataSetN = 0;
                }
                if (time == 0) {
                    left = viewSummary.getLeftPos();
                    right = viewSummary.getRightPos();
                } else {
                    leftZoom = viewSummary.getLeftPos();
                    rightZoom = viewSummary.getRightPos();
                }
                time = 0;
                state = ~0;
                load(true, null);
                break;
            case R.id.theme: // switch theme
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(PREFS_THEME_DAY, !themeDay)
                        .apply();
                recreate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.subtitle:
                if (time == 0) { // zoom-in
                    time = viewDetailed.getTime();
                    if (time != 0) {
                        zoomIn(time);
                    }
                } else { // zoom-out
                    onBackPressed();
                }
                break;
            case R.id.about:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aqoleg.space"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                break;
        }
    }

    public void zoomIn(long time) {
        this.time = time;
        left = viewSummary.getLeftPos();
        right = viewSummary.getRightPos();
        leftZoom = dataSetN == 4 ? left : LEFT_ZOOM;
        rightZoom = dataSetN == 4 ? right : RIGHT_ZOOM;
        state = buttons.getState();
        load(true, AnimationUtils.loadAnimation(this, R.anim.in_right));
    }

    public void zoomButton(boolean visible) {
        if (time == 0) {
            String button = visible ? getString(R.string.zoomIn) : "";
            ((TextView) findViewById(R.id.subtitle)).setText(button);
        }
    }

    private void load(boolean changeView, Animation animation) {
        if (!Data.load(this, dataSetN, time)) {
            onBackPressed();
            Toast.makeText(this, getString(R.string.noData), Toast.LENGTH_SHORT).show();
            return;
        }
        String title = "";
        switch (dataSetN) {
            case 0:
                title = getString(R.string.title0);
                break;
            case 1:
                title = getString(R.string.title1);
                break;
            case 2:
                title = getString(R.string.title2);
                break;
            case 3:
                title = getString(R.string.title3);
                break;
            case 4:
                title = getString(R.string.title4);
                break;
        }
        ((TextView) findViewById(R.id.title)).setText(title);
        title = time == 0 ? "" : getString(R.string.zoomOut);
        ((TextView) findViewById(R.id.subtitle)).setText(title);
        viewDetailed = ViewDetailed.create(
                this,
                themeDay,
                dataSetN,
                time != 0,
                time == 0 ? left : leftZoom,
                time == 0 ? right : rightZoom,
                state
        );
        if (changeView) {
            ((FrameLayout) findViewById(R.id.detailed)).removeAllViews();
            ((FrameLayout) findViewById(R.id.detailed)).addView(viewDetailed);
            if (animation != null) {
                viewDetailed.startAnimation(animation);
            }
        } else {
            ((FrameLayout) findViewById(R.id.detailed)).addView(viewDetailed);
        }
        viewSummary = ViewSummary.create(
                this,
                viewDetailed,
                themeDay,
                dataSetN,
                time != 0,
                time == 0 ? left : leftZoom,
                time == 0 ? right : rightZoom,
                state
        );
        if (changeView) {
            ((FrameLayout) findViewById(R.id.summary)).removeAllViews();
        }
        ((FrameLayout) findViewById(R.id.summary)).addView(viewSummary);
        buttons = Buttons.create(this, viewDetailed, viewSummary, themeDay, dataSetN, time != 0, state);
        if (changeView) {
            ((FrameLayout) findViewById(R.id.buttons)).removeAllViews();
        }
        ((FrameLayout) findViewById(R.id.buttons)).addView(buttons);
    }
}