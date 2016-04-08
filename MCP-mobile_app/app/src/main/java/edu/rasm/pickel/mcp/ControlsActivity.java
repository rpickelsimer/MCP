package edu.rasm.pickel.mcp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;

public class ControlsActivity extends ActionBarActivity {

    private static final int REQ_CODE_SPEECH = 1234;
    private final String[] spinFunctions = {"pinMode", "analogWrite", "digitalWrite", "delay"};

    // Global BT connection manager
    BTManager manager;

    // send data vie Sketch objects to Arduino - still needs to be implemented better
    Sketch sketch;

    NumberPicker numberPicker;
    Spinner spinner;
    Button btnReadSD, btnSaveSD, btnEraseSD, btnMic;
    SeekBar seekBar;
    Button btnSendData, btnReadData;
    TextView tvReadData;
    EditText etSendData;
    ScrollView scrollView;
    TableLayout tableLayout;
    String sbprint;

    boolean route_toggle;
    MyDBHandler dbHandler = new MyDBHandler(this, null, null, 1);

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case BTManager.MESSAGE_READ:

                    StringBuilder sb = new StringBuilder();
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line

                    if (endOfLineIndex > 0) {                                            // if end-of-line,
                        sbprint += sb.substring(0, endOfLineIndex);               // extract string
                        sb.delete(0, sb.length());                                      // and clear
                        // update TextView
                        btnSendData.setEnabled(true);
                        btnReadData.setEnabled(true);

                    }/**/

                    //not working

                        /** Save data from SD card to database
                        if (route_toggle == true) { //also if string.contains("log.csv")

                            String delims = ","; // use + to treat consecutive delims as one;
                            // omit to treat consecutive delims separately
                            String[] tokens = sbprint.split(delims);

                            String name = tokens[0];
                            for (int i = 1; i < tokens.length; i++) {

                                long lat = 0;
                                long lng = 0;

                                if (i % 2 == 1) {

                                    lat = Long.parseLong(tokens[i]);
                                }
                                else if (i % 2 == 0){
                                    lng = Long.parseLong(tokens[i]);
                                    GPSLocation location =
                                            new GPSLocation(name, lat, lng);

                                    dbHandler.addLocation(location);

                                    TableRow tableRow=new TableRow(getApplicationContext());
                                    TextView text=new TextView(getApplicationContext());
                                    //text.setId(i);
                                    text.setText(String.valueOf(lat) + ", " + String.valueOf(lng));
                                    tableRow.addView(text);
                                    tableLayout.addView(tableRow);

                                }
                            }
                            scrollView.addView(tableLayout);
                            route_toggle = !route_toggle;
                        }
                        else {

                        }
                   // }/**/
                    tvReadData.setText(sbprint);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);

        // get global BTManager and set new Handler
        manager = (BTManager)getApplication();
        manager.setmHandler(mHandler);

        // get layout elements
        initializeUI();

        /** NumberPicker for pin #s */
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(13);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                sketch.setIntVal(newVal);
            }
        });

        /** Spinner with Arduino function names */
        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item, spinFunctions);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(aa);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {
                switch (index) {
                    case 0:
                        sketch.setFunction(Sketch.PIN_MODE);
                        break;
                    case 1:
                        sketch.setFunction(Sketch.ANALOG_WRITE);
                        break;
                    case 2:
                        sketch.setFunction(Sketch.DIGITAL_WRITE);
                        break;
                    case 3:
                        sketch.setFunction(Sketch.DELAY);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /** SeekBar registers a value of 0 - 255 for pulse width modulation (PWM) */
        // should go to its own activity and make this activity for building sketch first
        // that way you can set what the seek bar controls, or it just reads pin and
        // assumes analogwrite. I could probably use it for read though if not something better
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                manager.write("a9," + progress);  // send a value 0 - 255 for PWM
                sketch.setIntVal(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // grab pin #s and functions from UI/sketch
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // any other state changes
            }
        });
    }

    /** Receiving voice recognition input */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    // display speech text to UI
                    String res = result.get(0);
                    tvReadData.setText(res);
                    if (res.contains("light")){
                        if (res.contains("on"))
                            manager.write("w7,1");
                        else if (res.contains("off"))
                            manager.write("w7,0");
                    }
                    else if (res.contains("save")) {
                        route_toggle = true;
                        manager.write(Sketch.FILE_TO_UI);
                    }
                }
                break;
            }
        }
    }

    /** Read file button onClick() */
    public void onReadClick(View v) {
        //route_toggle = true;
        manager.write(Sketch.FILE_TO_UI);
    }

    /** Toggle SD card save onClick() */
    boolean sd_toggle = true;
    public void onSaveClick(View v) {
        /*if (etSendData.getText().toString().equals(""))*/
        if (sd_toggle) {
            manager.write("s1");
            Toast.makeText(getApplicationContext(), "GPS data logger is on", Toast.LENGTH_SHORT).show();
        }
        else {
            manager.write("s0");
            Toast.makeText(getApplicationContext(), "GPS data logger is off", Toast.LENGTH_SHORT).show();
        }
        sd_toggle = !sd_toggle;
    }

    /** Toggle LED onClick() */
    boolean led_toggle = true;
    public void onLEDClick(View v) {
        if (led_toggle)
            manager.write("w9,1");
        else
            manager.write("w9,0");
        led_toggle = !led_toggle;
    }

    /** Microphone button onClick() */
    public void onMicClick(View v) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                R.string.voice_rec);
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Send UI created command to device */
    public void onSendDataClick(View v) {
        if (etSendData.getText().toString().equals(""))
            Toast.makeText(getApplicationContext(), "You must enter some data.", Toast.LENGTH_SHORT).show();
        else if (numberPicker.getValue() < 2 )
            Toast.makeText(getApplicationContext(), "Pins 0 and 1 are for Bluetooth", Toast.LENGTH_SHORT).show();
        else if (sketch.getFunction() == ' ')
            Toast.makeText(getApplicationContext(), "You must select a function.", Toast.LENGTH_SHORT).show();
        else {
            sketch.setIntVal(Integer.parseInt(etSendData.getText().toString()));
            sketch.setPinVal(numberPicker.getValue());
            manager.write(sketch.toCsvString());
        }
    }

    /** Read Data streams gps to serial */
    boolean toggle_data = true;
    public void onReadDataClick(View v) {
        if (toggle_data)
            manager.write("g1");
        else
            manager.write("g0");
        toggle_data = !toggle_data;
    }

    public void onOn(View v) {
        btnSendData.setEnabled(false);
        manager.write("a1");
    }

    public void onOff(View v) {
        btnReadData.setEnabled(false);
        manager.write("a0");
    }

    /** Clear all fields and reset widgets */
    public void onClearClick(View v) {
        sketch = new Sketch();
        numberPicker.setValue(0);
        spinner.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_item, spinFunctions));
        etSendData.setText("");
        tvReadData.setText("");
    }

    /** Move to MapsActivity */
    public void onMapsActivityClick(View v) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent,2);
    }

    /** Move to MainActivity */
    public void onMainActivityClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivityForResult(intent, 2);
    }

    private void initializeUI() {

        sketch = new Sketch();
        numberPicker = (NumberPicker)findViewById(R.id.numPicker);
        spinner = (Spinner)findViewById(R.id.spinner);
        btnReadSD = (Button)findViewById(R.id.btn_read_sd);
        btnSaveSD = (Button)findViewById(R.id.btn_save_sd);
        btnEraseSD = (Button)findViewById(R.id.btn_erase_sd);
        btnMic = (Button)findViewById(R.id.btn_mic);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        btnSendData = (Button)findViewById(R.id.btn_send_data);
        btnReadData = (Button)findViewById(R.id.btn_read_data);
        etSendData = (EditText)findViewById(R.id.et_send_data);
        tvReadData = (TextView)findViewById(R.id.tv_read_data);
        scrollView = (ScrollView)findViewById(R.id.myscroll1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controls, menu);
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
}
