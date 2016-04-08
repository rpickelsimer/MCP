package edu.rasm.pickel.mcp;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by ryan on 6/7/15.
 */
public class GetDirections extends Fragment{

    private OnGetDirectionsListener listener;

    // UI components
    EditText etFrom;
    EditText etTo;
    Button btn;
    TextView tvResult;

    // Global BT connection manager
    BTManager manager;

    // incoming from Arduino
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
                    if (endOfLineIndex > 0) {                                           // if end-of-line,
                        String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                        sb.delete(0, sb.length());                                      // and clear

                        etTo.setText(sbprint);            // update TextView

                    }
                    break;
            }
        }
    };



    // interface for sending data back to Activity
    public interface OnGetDirectionsListener {
        public void onDirectionsSelected(String origin, String dest);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnGetDirectionsListener) {
            listener = (OnGetDirectionsListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement MyListFragment.OnGetDirectionsListener");
        }
        // get global BTManager instance
        manager = (BTManager)activity.getApplication();
        manager.setmHandler(mHandler);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.get_directions_fragment,
                container, false);

        // initialize UI components
        etFrom = (EditText)view.findViewById(R.id.etFrom);
        etTo = (EditText)view.findViewById(R.id.etTo);
        btn = (Button)view.findViewById(R.id.button);
        tvResult = (TextView)view.findViewById(R.id.tvResult);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDirections(view);
            }
        });

        return view;
    }

    /** onClick() method for get directions button **/
    public void getDirections(View v) {

        // User input locations
        String origin = etFrom.getText().toString();
        String dest = etTo.getText().toString();

        // send data back to Activity
        listener.onDirectionsSelected(origin, dest);
    }

    /** Get gps coordinates from device onClick() */
    public void getDeviceGPSClick(View v) {

        manager.write("c");
    }
}
