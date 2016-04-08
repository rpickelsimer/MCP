package edu.rasm.pickel.mcp;
import android.app.Dialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GetDirections.OnGetDirectionsListener{

    // for returning address results
    final static int MAX_RESULT = 10;

    // list of points from lat/lng in xml string
    List<LatLng> points;

    // google map
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    // addresses/ coordinates
    Geocoder myGeocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        myGeocoder = new Geocoder(this);
        points = new ArrayList<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     *
     * googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
     * googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
     * googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
     * googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            // set map type to hybrid
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
    }

    @Override
    public void onDirectionsSelected(String origin, String dest) {

        // use input to get a url string for directions
        String url = getDirectionsUrl(origin, dest);

        // AsyncTask for downloading content
        DownloadTask downloadTask = new DownloadTask();

        // Start downloading xml data from Google Directions API
        downloadTask.execute(url);

        // Get addresses from user input
        Address addressOrigin = searchFromLocationName(origin);
        Address addressDest = searchFromLocationName(dest);

        // Get coordinates for markers
        double latOrigin = addressOrigin.getLatitude();
        double lonOrigin = addressOrigin.getLongitude();
        double latDest = addressDest.getLatitude();
        double lonDest = addressDest.getLongitude();

        // create markers
        MarkerOptions markerOrigin = new MarkerOptions().position(new LatLng(latOrigin, lonOrigin)).title("Hello Maps ");
        MarkerOptions markerDest = new MarkerOptions().position(new LatLng(latDest, lonDest)).title("Hello Maps ");

        // add markers
        mMap.addMarker(markerOrigin);
        mMap.addMarker(markerDest);
    }

    /** Returns address from a string */
    private Address searchFromLocationName(String name){
        Address address = null;
        try {
            List<Address> result
                    = myGeocoder.getFromLocationName(name, MAX_RESULT);

            if ((result == null)||(result.isEmpty())){
                Toast.makeText(MapsActivity.this,
                        "No matches were found or there is no backend service!",
                        Toast.LENGTH_LONG).show();
            }else{

                // just taking the first address for now
                address = result.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MapsActivity.this,
                    "The network is unavailable or another I/O problem occurred!",
                    Toast.LENGTH_LONG).show();
        }
        return address;
    }

    /**********************************************************************************/
    /****************************     Downloading data     ****************************/
    /**********************************************************************************/


    private String getDirectionsUrl(String origin, String dest){

        // Origin of route
        String str_origin = "origin="+origin;
        // Destination of route
        String str_dest = "destination="+dest;
        // Sensor enabled
        String sensor = "sensor=false";
        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;
        // Output format
        String output = "xml";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;
    }

    /** A method to download xml data from url */
    private String downloadUrl(String strUrl) throws IOException {

        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;

        try{
            // Get URL
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();
            // Data to string
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();

        }catch(Exception e){
            Log.d("My Log Entry", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        // Return xml string of directions - start tag <DirectionsResponse>
        return data;
    }

    /** AsyncTask for downloading xml data. **/
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // UI elements
        ProgressBar pb;

        @Override
        protected void onPreExecute() {
            // update the UI immediately after the task is executed
            super.onPreExecute();

            /** Getting Google Play availability status  **/
            int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
            if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

                int requestCode = 10;
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, MapsActivity.this, requestCode);
                dialog.show();

            }
            else { /** Google Play Services are available **/

                pb = (ProgressBar)findViewById(R.id.progressBar);
                // show the ProgressBar
                pb.setVisibility(View.VISIBLE);
            }
        }

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            String s  = null;
            try {
                // Download directions with url
                s = downloadUrl(url[0]);
            }
            catch (IOException e) {
                Log.d("Background Task",e.toString());
            }
            // Return a string with xml data
            return s;
        }

        // Executes in UI thread, after the execution of doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Start background processing for xml parsing
            XMLTask xmlTask = new XMLTask();
            xmlTask.execute(result);
        }
    }


    /********************************************************************************/
    /****************************     XML Processing     ****************************/
    /********************************************************************************/


    /** Parse xml string to a displayable string **/
    private String parseXML(String is) throws
            XmlPullParserException, IOException, URISyntaxException {

        // xml classes used to read xml file
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();

        // Set xml string to input
        parser.setInput(new StringReader(is));

        // Start and end tags and end document
        int eventType = parser.getEventType();

        // current tag
        String currentTag;

        // current value of tag/element
        String currentElement;

        int counter = 0;
        StringBuilder sb = new StringBuilder();

        // parse the entire xml file until done
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // look for start tags
            if (eventType == XmlPullParser.START_TAG) {
                // get the name of the start tag
                currentTag = parser.getName();

                if (currentTag.equals("step")) {
                    sb.append("Step " + ++counter + ":\n");
                    sb.append("-----------------------------------------\n");
                }
                // Driving directions
                else if (currentTag.equals("html_instructions")) {
                    currentElement = parser.nextText();

                    // Replace html bold tags
                    currentElement = currentElement.replace("<b>", "");
                    currentElement = currentElement.replace("</b>", "");

                    // Get rid of div tags
                    String s;
                    while (currentElement.contains("<")) {
                        if (currentElement.indexOf(">") == currentElement.lastIndexOf(">")){
                            s = "";
                        } else {
                            s = " ";
                        }
                        currentElement = currentElement.replace(currentElement.substring(currentElement.indexOf("<"),
                                currentElement.indexOf(">") + 1), s);
                    }

                    // Append html parsed string to directions string
                    currentElement += ".\n\n";
                    sb.append(currentElement);
                }
/*******************************************************************************************/
                /** LatLng points */

                /*else if (currentTag.equals("overview_polylines")) {

                    eventType = parser.next();
                    currentTag = parser.getName();
                    if (currentTag.equals("points")) {
                        String polyline = parser.nextText();
                        points = decodePoly(polyline);
                    }
                }*/

                // get geo-points from steps for markers
                else if (currentTag.contains("_location")) {
                    double lat = 0, lng = 0;
                    eventType = parser.next();
                    eventType = parser.next();
                    currentTag = parser.getName();
                    if (currentTag.equals("lat")) {
                        lat = Double.parseDouble(parser.nextText());
                        eventType = parser.next();
                        eventType = parser.next();
                        currentTag = parser.getName();
                    }
                    if (currentTag.equals("lng")) {
                        lng = Double.parseDouble(parser.nextText());
                        LatLng position = new LatLng(lat, lng);
                        points.add(position);
                    }
                }
            }

            // If the end of the string is reached
            else if (eventType == XmlPullParser.END_TAG) {
                currentTag = parser.getName();

                if (currentTag.equals("DirectionsResponse")) {

                    // return displayable string
                    sb.append("------------------------------------\n\n");
                    return sb.toString();
                }
            }
            // get next tag
            eventType = parser.next();
        }
        // return null if something went wrong
        return null;
    }

    /** AsyncTask for xml parsing **/
    private class XMLTask extends AsyncTask<String, Void, String> {

        ProgressBar pb;


        // Parse xml string in a separate thread
        @Override
        protected String doInBackground(String... is) {

            // Make sure string is not empty
            if (is[0] != null) {
                try {
                    // return a displayable string
                    return parseXML(is[0]);
                } catch (Exception e) {
                    Log.e("XMLExample", e.getMessage());
                }
            }
            // Return null if something went wrong
            return null;
        }

        /** update map with and display written directions */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Button btn = (Button)findViewById(R.id.button);
            btn.setEnabled(true);

            // If string is not empty, display result directions
            if (result != null) {
                // update left_fragment
                TextView tvResult = (TextView)findViewById(R.id.tvResult);
                tvResult.setText(result);
            }

            // Close the progress bar
            pb = (ProgressBar)findViewById(R.id.progressBar);
            pb.setVisibility(View.GONE);

            // Instantiates a new Polyline object and adds points to define a rectangle
            PolylineOptions rectOptions = new PolylineOptions();
            for (int i = 0; i < points.size(); i++){
                rectOptions.add(points.get(i));
            }
            Polyline polyline = mMap.addPolyline(rectOptions);

            // refocus on route
            List<LatLng> points = rectOptions.getPoints();
            LatLngBounds.Builder bc = new LatLngBounds.Builder();
            for (LatLng item : points) {
                bc.include(item);
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50));
        }
    }
}