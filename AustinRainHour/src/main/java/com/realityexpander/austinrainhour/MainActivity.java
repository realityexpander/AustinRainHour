package com.realityexpander.austinrainhour;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements FragmentManager.OnBackStackChangedListener {

    /* API variables */
    private final String API_KEY = "e432b91f50911786e5653ab22eb3073a";
    private final String API_GEONAMES_USERNAME = "realityexpander";
    private static final String TAG = "0xD3ADB33F MainActivity"; // debug helper

    private final String CLOUDY = "CLOUDY";
    private final String CLEAR_DAY = "CLEAR-DAY";
    private final String CLEAR_NIGHT = "CLEAR-NIGHT";
    private final String RAIN = "RAIN";
    private final String SNOW = "SNOW";
    private final String SLEET = "SLEET";
    private final String WIND = "WIND";
    private final String FOG = "FOG";
    private final String PARTLY_CLOUDY_DAY = "PARTLY-CLOUDY-DAY";
    private final String PARTLY_CLOUDY_NIGHT = "PARTLY-CLOUDY-NIGHT";
    private final String HAIL = "HAIL";
    private final String THUNDERSTORMS = "THUNDERSTORMS";
    private final String TORNADO = "TORNADO";

    /* MainActivity variables */
    protected Location location;
    private static long updateForecastDefaultDelay = 5000;
    private static long updateDisplayDefaultDelay = 1000;
    public static Location currentLocation;

    // Bundle Save/Restore Data
    private String mLocationName;
    private String[] mForecastStrings;

    // Graphview stuff
    private GraphView graphView;
    private GraphView.GraphViewData[] data;
    private GraphViewSeries seriesRainChance;
    private GraphViewSeries seriesRainIntensity;
    private GraphView.GraphViewData[] dataRainSeries;
    private GraphView.GraphViewData[] dataRainIntensity;
    final int NUM_MINUTES = 60;

    /* loading layout variables */
    private boolean contentLoaded = false;

    //private UserLocationManager mWeatherListener;
    public WeatherInfoFragment weatherInfoFragment = new WeatherInfoFragment();

    // Card flip stuff
    private static boolean mShowingBack = false;

    private LatLongBroadcastReceiver latLongBroadcastReceiver;

    /**
     * A handler object, used for deferring UI operations.
     */
    private Handler mHandler = new Handler();

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("place_name", mLocationName);
        outState.putStringArray("forecast", mForecastStrings);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init the weather data
        dataRainSeries = new GraphView.GraphViewData[NUM_MINUTES];
        dataRainIntensity = new GraphView.GraphViewData[NUM_MINUTES];
        for(int i=0; i<NUM_MINUTES; i++) {
            dataRainSeries[i] = new GraphView.GraphViewData(i, 0);
            dataRainIntensity[i] = new GraphView.GraphViewData(i, 0);
        }

        // Prepare the animated view frame
        if (savedInstanceState == null) {

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.graph1, new BarGraphFragment())
                    .commit();
        } else {
            mShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);
        }
        getFragmentManager().addOnBackStackChangedListener(this);

        // Start the listener from GPS service
        latLongBroadcastReceiver = new LatLongBroadcastReceiver();

        // Start the GPS listener service
        startService(new Intent(MainActivity.this, GPSService.class));

    }

//    @Override
//    public void onOrientationChange()

    public class LatLongBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Double lat;
            Double lng;

            lat = intent.getDoubleExtra("lat", 0);
            lng = intent.getDoubleExtra("lng", 0);

            if(location == null)
                location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(lat);
            location.setLongitude(lng);
//            setLocation(location);
            updateForecast(0);
            updateGeoLocation(0);
        }

    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        this.unregisterReceiver(latLongBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.registerReceiver(latLongBroadcastReceiver, new IntentFilter("LatLong"));
    }

    public void flipCard() {
        if (mShowingBack) {
            getFragmentManager().popBackStack();
            return;
        }

        // Flip to the back.
        mShowingBack = true;

        // Create and commit a new fragment transaction that adds the fragment for the back of
        // the card, uses custom animations, and is part of the fragment manager's back stack.
        getFragmentManager()
                .beginTransaction()

                .setCustomAnimations(
                        R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                        R.animator.card_flip_left_in, R.animator.card_flip_left_out)
                .replace(R.id.graph1, weatherInfoFragment)
                .addToBackStack(null)
                .commit();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onBackStackChanged() {
        mShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);

        // When the back stack changes, invalidate the options menu (action bar).
        invalidateOptionsMenu();
    }

    /**
     * Created by Kin on 9/3/13.
     */
    public class BarGraphFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_bargraph, container, false);

            // draw current graph
            graphView = new LineGraphView(
                    getActivity(),
                    "60 minute Rain Chance & Intensity"
            );
            seriesRainChance = new GraphViewSeries("% Chance", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(20, 20, 230), 3), dataRainSeries);
            seriesRainIntensity = new GraphViewSeries("Intensity", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(230, 230, 30), 3), dataRainIntensity);

            ((LineGraphView) graphView).setBackgroundColor(Color.rgb(40, 40, 150));
            ((LineGraphView) graphView).setDrawBackground(true);
            graphView.addSeries(seriesRainChance);

            graphView.addSeries(seriesRainIntensity);

            graphView.setViewPort(0, 59);
            graphView.setScrollable(false);
            graphView.setShowLegend(true);
            graphView.getGraphViewStyle().setTextSize(12);
            graphView.setManualYAxisBounds(100,0);
            graphView.setManualYAxis(true);
            LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.linearLayout); // Use the fragment layout
            layout.addView(graphView);

            return rootView;
        }
    }

    /**
     * Created by Kin on 9/3/13.
     */
    public class WeatherInfoFragment extends Fragment {

        int precip[];
        int intensity[];

        TextView textView;

        public WeatherInfoFragment(){
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_time_prob_intense, container, false);

            // text_time, text_precip, text_intense
            textView = (TextView) rootView.findViewById(R.id.text_time);
            textView.setText("Time\n+10 min\n+20 min\n+30 min\n+40 min\n+50 min\n+60 min");
            textView = (TextView) rootView.findViewById(R.id.text_precip);
            textView.setText("Precip Chance\n" + precip[0]+"%\n"+ precip[1]+"%\n"+ precip[2]+"%\n" + precip[3]+"%\n" +
                    precip[4]+"%\n"+ precip[5]+"%\n" );
            textView = (TextView) rootView.findViewById(R.id.text_intense);
            textView.setText("Intensity\n" + intensity[0]+"%\n"+ intensity[1]+"%\n"+ intensity[2]+"%\n" + intensity[3]+"%\n" +
                    + intensity[4]+"%\n"+ intensity[5]+"%\n");

            return rootView;
        }

        public void SetPrecipAndIntensity(int[] curPrecip, int[] curIntensity){
            precip = curPrecip;
            intensity = curIntensity;
        }
    }

    // set the location from the GPS (this is a call-back)
    public void setLocation( Location loc){
        location = loc;
        updateForecast(0);
        updateGeoLocation(0);
    }

    private String formatTemp(String input) {
        return input.substring(0, input.indexOf('.')+2) + "\u00B0";
    }

    /**
     * waits for location data to be received at some specified interval
     */
    private void updateForecast(long interval) {
        final Handler h = new Handler();
        final Location location = this.location;
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (location != null) {

                    ProgressBar spinner = (ProgressBar) findViewById(R.id.loading_spinner);
                    spinner.setVisibility(View.VISIBLE);

                    updateForecastDisplay(
                            new Forecast(                                       // instantiate a forecast.io forecast
                                    MainActivity.this.location.getLatitude(),   // the Location.getLatitude()
                                    MainActivity.this.location.getLongitude(),  // the Location.getLongitude()
                                    API_KEY                                     // your unique forecast.io api_key
                            )
                    );
                    h.removeCallbacks(this);
                } else {
                    updateForecast();
                }
            }
        }, interval); /* todo:simulate a slow network */
    }

    /**
     * waits for location data to be received at some specified interval
     */
    private void updateGeoLocation(long interval) {
        final Handler h = new Handler();
        final Location location = this.location;
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (location != null) {
                    updateGeoLocationDisplay(
                            // Get the new GeoLocation name from location
                            new GeoLocation(
                                    MainActivity.this.location.getLatitude(),
                                    MainActivity.this.location.getLongitude(),
                                    API_GEONAMES_USERNAME
                            )
                    );
                    h.removeCallbacks(this);
                } else {
                    updateGeoLocation();
                }
            }
        }, interval); /* todo:simulate a slow network */
    }


    private void updateForecast() {
        updateForecast(updateForecastDefaultDelay);
    }
    private void updateGeoLocation() {
        updateGeoLocation(updateForecastDefaultDelay);
    }

    //update gelocationname
    private void updateGeoLocationDisplay(final GeoLocation geoLocation, long interval) {
        final Handler h = new Handler();
        final GeoLocation g = geoLocation;

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (g.getGeoNamesStatus() == HttpStatus.SC_OK) {
                    //h is the handler, removing the callback causes it to stop being called
                    h.removeCallbacks(this);
                    // Set the geolocationName
                    TextView textView = (TextView) findViewById(R.id.text_view);
                    textView.setText("");

                    try {
                        ListView listView = (ListView)findViewById(R.id.list_view);
                        JSONObject currentLocation = geoLocation.getGeoNamesData();


                        int     numItems = currentLocation.length();
                        //if (currentLocation.length() <= 2)
                        //    locationName = "Location error.";
                        //else
                        mLocationName =
                                currentLocation.getJSONArray("geonames").getJSONObject(0).getString("name") + ", " +
                                        currentLocation.getJSONArray("geonames").getJSONObject(0).getString("adminCode1");
                        // JSONRawData->JSONNamedObject(geonames)->JSONArray[0]->JSONObject->Key=adminCode1
                        textView.setText(mLocationName);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    updateGeoLocationDisplay(g);
                }

            }
        }, interval);
    }

    private void updateForecastDisplay(final Forecast forecast, long interval) {
        final Handler h = new Handler();
        final Forecast f = forecast;

        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                // Get the weather data
                if (f.getStatus() == HttpStatus.SC_OK) {
                    h.removeCallbacks(this);

                    ProgressBar spinner = (ProgressBar) findViewById(R.id.loading_spinner);
                    spinner.setVisibility(View.INVISIBLE);

                    try {
                        ListView listView = (ListView)findViewById(R.id.list_view);

                        JSONObject currentForecast = forecast.getData().getJSONObject("currently");
                        Long time = currentForecast.getLong("time");
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        Date formattedTime = new Date();

                        try {
                            formattedTime = format.parse(time.toString());
                        } catch(ParseException e) {
                            e.printStackTrace();
                        }

                        // Get the minute-by-minute
                        JSONObject currentForecastMinutely = forecast.getData().getJSONObject("minutely");
                        String[] minutelySummaryString = new String[]{
                                currentForecastMinutely.getString("summary"),
                                currentForecastMinutely.getString("icon")
                        };
                        JSONArray minutelyBlockArray = currentForecastMinutely.getJSONArray("data");
//                        String[] minutelyBlockString = new String[]{
//                                minutelyBlockArray.getJSONObject(0).getString("time"),
//                                minutelyBlockArray.getJSONObject(0).getString("precipIntensity"),
//                                minutelyBlockArray.getJSONObject(0).getString("precipProbability")
//                        };


                        // Set the icon to the weather type for the hour
                        Drawable theIcon;
                        try {
                            if (currentForecastMinutely.getString("icon").equalsIgnoreCase(CLOUDY) ) {
                                theIcon = getResources().getDrawable(R.drawable.cloudy);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(CLEAR_DAY)) {
                                theIcon = getResources().getDrawable(R.drawable.sunny);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(CLEAR_NIGHT)) {
                                theIcon = getResources().getDrawable(R.drawable.moon);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(RAIN)) {
                                theIcon = getResources().getDrawable(R.drawable.drizzle);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(SNOW)) {
                                theIcon = getResources().getDrawable(R.drawable.snow);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(SLEET)) {
                                theIcon = getResources().getDrawable(R.drawable.drizzle_snow);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(WIND)) {
                                theIcon = getResources().getDrawable(R.drawable.windy);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(FOG)) {
                                theIcon = getResources().getDrawable(R.drawable.haze);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(PARTLY_CLOUDY_DAY)) {
                                theIcon = getResources().getDrawable(R.drawable.cloudy);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(PARTLY_CLOUDY_NIGHT)) {
                                theIcon = getResources().getDrawable(R.drawable.cloudy_night);
                            } else if (currentForecastMinutely.getString("icon").equalsIgnoreCase(THUNDERSTORMS)) {
                                theIcon = getResources().getDrawable(R.drawable.thunderstorms);
                            } else theIcon = getResources().getDrawable(R.drawable.sunny);  // Default sunny

                            ImageView weatherIconView = (ImageView) findViewById(R.id.weather_icon);
                            weatherIconView.setImageDrawable(theIcon);

                        } catch (Exception e) {
                            Log.e(TAG, "Problem parsing weather type" + minutelySummaryString[0]);
                        }

                        // TODO if the weather is inclement for the next hour, put up a notification

                        // Get the current conditions strings
                        String[] conditions = new String[]{
                                minutelySummaryString[0],
                                currentForecast.getString("summary") + " currently.",
                                //currentForecast.getString("precipIntensity"),
                                //currentForecast.getString("precipProbability"),
                                formatTemp(currentForecast.getString("temperature")) + "F.",
                                formatTemp(currentForecast.getString("apparentTemperature")) + "F feels like.",
                                //currentForecast.getString("dewPoint"),
                                currentForecast.getString("windSpeed") + "mph winds.", // + currentForecast.getString("windBearing") + "deg.",
                                //currentForecast.getString("cloudCover"),
                                Double.toString(Double.valueOf(Math.round(Double.valueOf(currentForecast.getString("humidity"))*100000))/1000) + "% humidity.",
                                //currentForecast.getString("pressure"),
                                //currentForecast.getString("visibility"),
                                currentForecast.getString("ozone") + " ozone.",
                        };

                        mForecastStrings = conditions;

                        ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.weather_info, conditions);
                        listView.setAdapter(adapter);

                        TextView textView = (TextView) findViewById(R.id.last_updated);
                        textView.setText("Last updated @ " + formattedTime.toString());

                        // Data for Precipitation Probability
                        for (int i=0; i<NUM_MINUTES; i++) {
                            dataRainSeries[i] = new GraphView.GraphViewData(i, 100 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipProbability")));
                        }
                        seriesRainChance.resetData(dataRainSeries);

                        // Data for Precipitation Intensity
                        for (int i=0; i<NUM_MINUTES; i++) {
                            dataRainIntensity[i] = new GraphView.GraphViewData(i, Math.min(100, 400 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipIntensity"))));
                        }
                        seriesRainIntensity.resetData(dataRainIntensity);

                        // Set the flipcard
                        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_layout);
                        frameLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                flipCard();
                            }
                        });

                        // Set the text for the Time / Precip Chance % / Intensity % for text views
                        // Get every ten minutes worse case
                        int minutePrecip, maxBlockPrecip, minuteIntensity, maxBlockIntensity;
                        int minuteBlock;
                        int precip[] = new int[7];
                        int intensity[] = new int[7];
                        int startBlock, endBlock;
                        for (minuteBlock=0; minuteBlock < 6; minuteBlock++) {
                            startBlock = (minuteBlock * 10);
                            endBlock = ((minuteBlock + 1) * 10 ) -1;
                            maxBlockPrecip = 0;
                            maxBlockIntensity = 0;
                            // Find max precip chance & intensity for each 10 minute block in the hour
                            for (int i=startBlock; i<endBlock; i++) {
                                minutePrecip =  Math.round(100 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipProbability")));
                                minuteIntensity = Math.round(100 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipIntensity")));
                                if (minutePrecip > maxBlockPrecip)
                                    maxBlockPrecip = minutePrecip;
                                if (minuteIntensity > maxBlockIntensity)
                                    maxBlockIntensity = minuteIntensity;
                            }
                            precip[minuteBlock] = maxBlockPrecip;
                            intensity[minuteBlock] = maxBlockIntensity;
                        }
                        weatherInfoFragment.SetPrecipAndIntensity(precip, intensity);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    updateForecastDisplay(f);
                }
            }
        }, interval);
    }

    private void updateForecastDisplay(final Forecast forecast) {
        updateForecastDisplay(forecast, updateDisplayDefaultDelay);
    }
    private void updateGeoLocationDisplay(final GeoLocation geoLocation) {
        updateGeoLocationDisplay(geoLocation, updateDisplayDefaultDelay);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
