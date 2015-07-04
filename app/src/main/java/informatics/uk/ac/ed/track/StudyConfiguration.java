package informatics.uk.ac.ed.track;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class StudyConfiguration extends AppCompatActivity
        implements DatePickerFragment.DatePickerDialogListener,
            TimePickerFragment.TimePickerDialogListener {

    private final static int START_DATEPICKER_ID = 0;
    private final static int START_TIMEPICKER_ID = 1;
    private final static int END_TIMEPICKER_ID = 2;

    private Calendar minimumStartDate; // will be set to the following day

    private Calendar startDate;
    private int duration, samplesPerDay, notificationWindow;
    private int startTime_hour, startTime_minute;
    private int endTime_hour, endTime_minute;

    private EditText txtDuration, txtSamplesPerDay, txtNotificationWindow;
    private TextView txtVwStartDate, txtVwStartTime, txtVwEndTime;
    private TextView txtStartDate_errorMsg, txtDuration_errorMsg,
            txtSamplesPerDay_errorMsg, txtNotificationWindow_errorMsg,
            txtStarTime_errorMsg, txtEndTime_errorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_configuration);

        /* initialise UI controls */
        txtDuration = (EditText) findViewById(R.id.txtDuration);
        txtSamplesPerDay = (EditText) findViewById(R.id.txtSamplesPerDay);
        txtNotificationWindow = (EditText) findViewById(R.id.txtNotificationWindow);

        txtVwStartDate = (TextView) findViewById(R.id.txtStartDate);
        txtVwStartTime = (TextView) findViewById(R.id.txtStartTime);
        txtVwEndTime = (TextView) findViewById(R.id.txtEndTime);

        txtStartDate_errorMsg = (TextView) findViewById(R.id.txtStartDate_errorMsg);
        txtDuration_errorMsg = (TextView) findViewById(R.id.txtDuration_errorMsg);
        txtSamplesPerDay_errorMsg = (TextView) findViewById(R.id.txtSamplesPerDay_errorMsg);
        txtNotificationWindow_errorMsg = (TextView) findViewById(R.id.txtNotificationWindow_errorMsg);
        txtStarTime_errorMsg = (TextView) findViewById(R.id.txtStartTime_errorMsg);
        txtEndTime_errorMsg = (TextView) findViewById(R.id.txtEndTime_errorMsg);

        /* set start date & minimum start date to the following day */
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        this.minimumStartDate = calendar;
        this.setStartDate(calendar);

        /* set start & end times using default values */
        Resources res = getResources();
        setStartTime(res.getInteger(R.integer.defaultStartTime_Hour),
                res.getInteger(R.integer.defaultStartTime_Minute));
        setEndTime(res.getInteger(R.integer.defaultEndTime_Hour),
                res.getInteger(R.integer.defaultEndTime_Minute));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_study_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void btnNext_onClick(View view) {
        boolean valid = this.setAndValidate();

        if (valid) {
            // save settings
            this.savePreferences();
            // set up notifications
            SurveyNotificationManager notificationManager =
                    new SurveyNotificationManager(getApplicationContext());
            notificationManager.SetupNotifications(true); // TODO handle random notifications
            // proceed to next activity
            Intent intent = new Intent(this, UserAccountSetup.class);
            startActivity(intent);
        }
    }

    /**
     * Set instance variables and validate form input.
     * @return true if validation succeeds
     */
    private boolean setAndValidate() {
        boolean hasErrors = false;

        String duration_str = Utils.getTrimmedText(this.txtDuration);
        String samplePerDay_str = Utils.getTrimmedText(this.txtSamplesPerDay);
        String notificationWindow_str = Utils.getTrimmedText(this.txtNotificationWindow);

        // start date
        if (this.startDate.before(this.minimumStartDate)) {
            this.showError(this.txtStartDate_errorMsg, getString(R.string.error_startDate));
            hasErrors = true;
        } else {
            this.hideError(this.txtStartDate_errorMsg);
        }

        // duration
        if (validateNumber(duration_str, this.txtDuration_errorMsg,
                getString(R.string.error_missingDuration))) {
            this.duration = Integer.parseInt(duration_str);
        } else {
            hasErrors = true;
        }

        // #samples/day
        if (validateNumber(samplePerDay_str, this.txtSamplesPerDay_errorMsg,
                getString(R.string.error_missingSamplesPerDay))) {
            this.samplesPerDay = Integer.parseInt(samplePerDay_str);
        } else {
            hasErrors = true;
        }

        // notification window
        if (validateNumber(notificationWindow_str, this.txtNotificationWindow_errorMsg,
                getString(R.string.error_missingNotificationWindow))) {
            this.notificationWindow = Integer.parseInt(notificationWindow_str);
        } else {
            hasErrors = true;
        }

        // end time (must be after start time)
        if (endTime_hour < startTime_hour) {
            showError(this.txtEndTime_errorMsg, getString(R.string.error_dailyEndTime));
            hasErrors = true;
        } else {
            this.hideError(this.txtEndTime_errorMsg);
        }

        return !hasErrors;
    }

    private void savePreferences(){
        SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(Constants.DURATION_DAYS, this.duration);
        editor.putInt(Constants.SAMPLES_PER_DAY, this.samplesPerDay);
        editor.putInt(Constants.NOTIFICATION_WINDOW_MINUTES, this.notificationWindow);
        editor.putInt(Constants.START_TIME_HOUR, this.startTime_hour);
        editor.putInt(Constants.START_TIME_MINUTE, this.startTime_minute);
        editor.putInt(Constants.END_TIME_HOUR, this.endTime_hour);
        editor.putInt(Constants.END_TIME_MINUTE, this.endTime_minute);

        // calculate exact date time of study start (start date @ start time)
        Calendar studyStartDateTime = GregorianCalendar.getInstance();
        studyStartDateTime.set(this.startDate.get(Calendar.YEAR),
                this.startDate.get(Calendar.MONTH), this.startDate.get(Calendar.DAY_OF_MONTH),
                this.startTime_hour, this.startTime_minute, 0);
        studyStartDateTime.set(Calendar.MILLISECOND, 0);

        // calculate exact date time of study end (start date + duration @ end time)
        Calendar studyEndDateTime = GregorianCalendar.getInstance();
        studyEndDateTime .set(this.startDate.get(Calendar.YEAR),
                this.startDate.get(Calendar.MONTH), this.startDate.get(Calendar.DAY_OF_MONTH),
                this.endTime_hour, this.endTime_minute, 0);
        studyEndDateTime.set(Calendar.MILLISECOND, 0);

        // first calculate interval between notifications in milliseconds
        long timeSpanMillis =
                (studyEndDateTime.getTimeInMillis() - studyStartDateTime.getTimeInMillis());
        long intervalMillis = timeSpanMillis / (this.samplesPerDay + 1);

        // add duration to get actual end date
        studyEndDateTime.add(Calendar.DATE, (this.duration - 1));

        // save to shared preferences
        editor.putLong(Constants.STUDY_START_DATE_TIME_MILLIS, studyStartDateTime.getTimeInMillis());
        editor.putLong(Constants.STUDY_END_DATE_TIME_MILLIS, studyEndDateTime.getTimeInMillis());
        editor.putLong(Constants.NOTIFICATION_INTERVAL_MILLIS, intervalMillis);

        editor.apply();
    }

    private boolean validateNumber(String numberString, TextView txtVwError, String emptyErrorMsg) {
        boolean valid = true;

        if (numberString.isEmpty()) {
            this.showError(txtVwError, emptyErrorMsg);
            valid = false;
        } else if (!TextUtils.isDigitsOnly(numberString)) {
            this.showError(txtVwError, getString(R.string.error_enterValidNumber));
            valid = false;
        } else {
            this.hideError(txtVwError);
        }

        return valid;
    }

    private void showError(TextView txtVwError, String msg){
        txtVwError.setText(msg);
        txtVwError.setVisibility(View.VISIBLE);
    }

    private void hideError(TextView txtVwError) {
        txtVwError.setVisibility(View.GONE);
    }

    private void setStartDate(Calendar date) {
        this.startDate = date;
        SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.defaultDateFormat));
        txtVwStartDate.setText(sdf.format(this.startDate.getTime()));
    }

    private void setStartDate(int year, int month, int date) {
        Calendar calendar = new GregorianCalendar();
        calendar.set(year, month, date);
        this.setStartDate(calendar);
    }

    private void setStartTime(int hour, int minute) {
        this.startTime_hour = hour;
        this.startTime_minute = minute;
        this.setTimeTextViewText(this.txtVwStartTime, this.startTime_hour, this.startTime_minute);
    }

    private void setEndTime(int hour, int minute) {
        this.endTime_hour = hour;
        this.endTime_minute = minute;
        this.setTimeTextViewText(this.txtVwEndTime, this.endTime_hour, this.endTime_minute);
    }

    private void setTimeTextViewText(TextView txtView, int hour, int minute) {
        txtView.setText(String.format("%02d", hour) + ":" + String.format("%02d", minute));
    }

    public void lytStartDate_onClick(View view) {
        this.showDatePicker(this.startDate, START_DATEPICKER_ID);
    }

    public void lytStartTime_OnClick(View view) {
        this.showTimePicker(this.startTime_hour, this.startTime_minute, START_TIMEPICKER_ID);
    }

    public void lytEndTime_OnClick(View view) {
        this.showTimePicker(this.endTime_hour, this.endTime_minute, END_TIMEPICKER_ID);
    }

    public void showDatePicker(Calendar dateCalendar, int pickerId) {
        Bundle args = new Bundle();
        args.putInt(DatePickerFragment.ARG_PICKER_ID, pickerId);
        args.putInt(DatePickerFragment.ARG_YEAR, dateCalendar.get(Calendar.YEAR));
        args.putInt(DatePickerFragment.ARG_MONTH, dateCalendar.get(Calendar.MONTH));
        args.putInt(DatePickerFragment.ARG_DAY, dateCalendar.get(Calendar.DAY_OF_MONTH));
        args.putLong(DatePickerFragment.ARG_MIN_DATE, this.minimumStartDate.getTimeInMillis());

        DialogFragment datePicker = new DatePickerFragment();
        datePicker.setArguments(args);
        datePicker.show(getSupportFragmentManager(), "datePicker");
    }

    public void showTimePicker(int hour, int minute, int pickerId) {
        Bundle args = new Bundle();
        args.putInt(TimePickerFragment.ARG_HOUR, hour);
        args.putInt(TimePickerFragment.ARG_MINUTE, minute);
        args.putInt(TimePickerFragment.ARG_PICKER_ID, pickerId);

        DialogFragment timePicker = new TimePickerFragment();
        timePicker.setArguments(args);
        timePicker.show(getSupportFragmentManager(), "timePicker");
    }

    @Override
    public void OnDateSet(int pickerId, DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        this.setStartDate(year, monthOfYear, dayOfMonth);
    }

    @Override
    public void onTimeSet(int pickerId, TimePicker view, int hourOfDay, int minute) {
        TextView timeTxtView;

        switch (pickerId) {
            case START_TIMEPICKER_ID:
                this.setStartTime(hourOfDay, minute);
                break;
            case END_TIMEPICKER_ID:
                this.setEndTime(hourOfDay, minute);
                break;
        }
    }
}