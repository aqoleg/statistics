package space.aqoleg.statistics;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import space.aqoleg.statistics.detailed.ViewDetailed;
import space.aqoleg.statistics.summary.ViewSummary;

class Buttons extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {
    private ViewDetailed viewDetailed;
    private ViewSummary viewSummary;
    private boolean day;
    private int state;
    private int buttonsN; // total amount of the buttons

    // default Layout constructor
    private Buttons(Context context) {
        super(context);
    }

    static Buttons create(
            ActivityMain activity,
            ViewDetailed viewDetailed,
            ViewSummary viewSummary,
            boolean day,
            int dataSetN,
            boolean zoom,
            int state
    ) {
        Buttons buttons = new Buttons(activity);
        buttons.viewDetailed = viewDetailed;
        buttons.viewSummary = viewSummary;
        buttons.day = day;
        buttons.state = state;
        switch (dataSetN) {
            case 0:
            case 1:
                buttons.buttonsN = 2;
                activity.getLayoutInflater().inflate(R.layout.buttons_2, buttons);
                break;
            case 2:
                buttons.buttonsN = 7;
                activity.getLayoutInflater().inflate(R.layout.buttons_7, buttons);
                break;
            case 3:
                if (zoom) {
                    buttons.buttonsN = 3;
                    activity.getLayoutInflater().inflate(R.layout.buttons_3, buttons);
                }
                break;
            case 4:
                buttons.buttonsN = 6;
                activity.getLayoutInflater().inflate(R.layout.buttons_6, buttons);
                break;
        }
        if (buttons.buttonsN >= 2) {
            buttons.initialize(buttons.findViewById(R.id.button0), 0);
            buttons.initialize(buttons.findViewById(R.id.button1), 1);
        }
        if (buttons.buttonsN >= 3) {
            buttons.initialize(buttons.findViewById(R.id.button2), 2);
        }
        if (buttons.buttonsN >= 6) {
            buttons.initialize(buttons.findViewById(R.id.button3), 3);
            buttons.initialize(buttons.findViewById(R.id.button4), 4);
            buttons.initialize(buttons.findViewById(R.id.button5), 5);
        }
        if (buttons.buttonsN >= 7) {
            buttons.initialize(buttons.findViewById(R.id.button6), 6);
        }
        return buttons;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button0:
                state ^= 0b1;
                setChecked(view, 0, (state & 0b1) != 0);
                break;
            case R.id.button1:
                state ^= 0b10;
                setChecked(view, 1, (state & 0b10) != 0);
                break;
            case R.id.button2:
                state ^= 0b100;
                setChecked(view, 2, (state & 0b100) != 0);
                break;
            case R.id.button3:
                state ^= 0b1000;
                setChecked(view, 3, (state & 0b1000) != 0);
                break;
            case R.id.button4:
                state ^= 0b10000;
                setChecked(view, 4, (state & 0b10000) != 0);
                break;
            case R.id.button5:
                state ^= 0b100000;
                setChecked(view, 5, (state & 0b100000) != 0);
                break;
            case R.id.button6:
                state ^= 0b1000000;
                setChecked(view, 6, (state & 0b1000000) != 0);
                break;
        }
        viewSummary.changeState(state);
        viewDetailed.changeState(state);
    }

    // check this, uncheck others
    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.button0:
                state = 0b1;
                break;
            case R.id.button1:
                state = 0b10;
                break;
            case R.id.button2:
                state = 0b100;
                break;
            case R.id.button3:
                state = 0b1000;
                break;
            case R.id.button4:
                state = 0b10000;
                break;
            case R.id.button5:
                state = 0b100000;
                break;
            case R.id.button6:
                state = 0b1000000;
                break;
        }
        setChecked(findViewById(R.id.button0), 0, (state & 0b1) != 0);
        setChecked(findViewById(R.id.button1), 1, (state & 0b10) != 0);
        if (buttonsN >= 3) {
            setChecked(findViewById(R.id.button2), 2, (state & 0b100) != 0);
        }
        if (buttonsN >= 6) {
            setChecked(findViewById(R.id.button3), 3, (state & 0b1000) != 0);
            setChecked(findViewById(R.id.button4), 4, (state & 0b10000) != 0);
            setChecked(findViewById(R.id.button5), 5, (state & 0b100000) != 0);
        }
        if (buttonsN >= 7) {
            setChecked(findViewById(R.id.button6), 6, (state & 0b1000000) != 0);
        }
        viewSummary.changeState(state);
        viewDetailed.changeState(state);
        return true;
    }

    // getter for onSaveInstanceState
    int getState() {
        return state;
    }

    private void initialize(View view, int buttonN) {
        ((TextView) view).setText(Data.getName(buttonN));
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        setChecked(view, buttonN, (state & (1 << buttonN)) != 0);
    }

    private void setChecked(View view, int buttonN, boolean checked) {
        if (checked) {
            view.setBackgroundColor(Color.parseColor(Data.getColor(buttonN)));
            if (!day) {
                ((TextView) view).setTextColor(Color.BLACK);
            }
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
            if (!day) {
                ((TextView) view).setTextColor(Color.WHITE);
            }
        }
    }
}