package com.realityexpander.austinrainhour;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

    /* loading layout variables */
    private boolean contentLoaded = false;

    private UserLocationManager mWeatherListener;











    // Card flip stuff
    private static boolean mShowingBack = false;

    /**
     * A handler object, used for deferring UI operations.
     */
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the GPS listener
        mWeatherListener = new UserLocationManager(this);

        updateForecast(0);  // not needed?
        updateGeoLocation(0);

//            context = MyApplication.getAppContext();

        if (savedInstanceState == null) {

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.bar_graph_fragment, new BarGraphFragment())
                    .commit();
        } else {
            mShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);
        }

        getFragmentManager().addOnBackStackChangedListener(this);

    }

    private void flipCard() {
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
                .replace(R.id.bar_graph_fragment, new WeatherInfoFragment())
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











    // set the location from the GPS (this is a call-back)
    public void setLocation( Location loc){
        location = loc;
        updateForecast(0);
        updateGeoLocation(0);
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

                        String locationName;
                        int     numItems = currentLocation.length();
                        //if (currentLocation.length() <= 2)
                        //    locationName = "Location error.";
                        //else
                        locationName =
                                currentLocation.getJSONArray("geonames").getJSONObject(0).getString("name") + ", " +
                                        currentLocation.getJSONArray("geonames").getJSONObject(0).getString("adminCode1");
                        textView.setText(locationName);
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

                        // Get the current conditions strings
                        String[] conditions = new String[]{
                                minutelySummaryString[0],
                                currentForecast.getString("summary") + " currently.",
                                //currentForecast.getString("precipIntensity"),
                                //currentForecast.getString("precipProbability"),
                                currentForecast.getString("temperature") + "F.",
                                currentForecast.getString("apparentTemperature") + "F feels like.",
                                //currentForecast.getString("dewPoint"),
                                currentForecast.getString("windSpeed") + "mph bearing " + currentForecast.getString("windBearing") + "deg.",
                                //currentForecast.getString("cloudCover"),
                                Double.toString(Double.valueOf(Math.round(Double.valueOf(currentForecast.getString("humidity"))*100000))/1000) + "% humidity.",
                                //currentForecast.getString("pressure"),
                                //currentForecast.getString("visibility"),
                                currentForecast.getString("ozone") + " ozone level.",
                        };

                        ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.weather_info, conditions);
                        listView.setAdapter(adapter);

                        TextView textView = (TextView) findViewById(R.id.last_updated);
                        textView.setText("Last updated @ " + formattedTime.toString());


                        //Log.e(TAG, "minute summary=" + minutelySummaryString[0]);
                        //Log.e(TAG, "minute time=" + minutelySummaryString[0]);

                        // Data for Precipitation Probability
                        int num = 60;
                        GraphView.GraphViewData[] data = new GraphView.GraphViewData[num];
                        for (int i=0; i<num; i++) {
                            data[i] = new GraphView.GraphViewData(i, 100 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipProbability")));
                        }
                        GraphViewSeries seriesRainChance = new GraphViewSeries("% Chance", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(20, 20, 230), 3), data);

                        // Data for Precipitation Intensity
                        data = new GraphView.GraphViewData[num];
                        for (int i=0; i<num; i++) {
                            data[i] = new GraphView.GraphViewData(i, Math.min(100, 400 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipIntensity"))));
                        }
                        GraphViewSeries seriesRainIntensity = new GraphViewSeries("Intensity", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(230, 230, 30), 3), data);

                        // graph with dynamically generated horizontal and vertical labels
                        GraphView graphView;
                        graphView = new LineGraphView(
                                // thisMainActivity //
                                MainActivity.this
                                , "60 minute Rain Chance & Intensity"
                        );

                        // add data
                        //graphView.addSeries(new GraphViewSeries(data));
                        graphView.addSeries(seriesRainChance);
                        graphView.addSeries(seriesRainIntensity);
                        // set view port, start=0, size=60
                        graphView.setShowLegend(true);
                        graphView.setViewPort(0, 59);
                        graphView.setScrollable(false);
                        //graphView.setScalable(false);
                        //((LineGraphView) graphView).setBackgroundColor(0xFFFFFF);
                        ((LineGraphView) graphView).setDrawBackground(true);
                        graphView.getGraphViewStyle().setTextSize(10);
                        graphView.setManualYAxisBounds(100, 0);
                        graphView.setManualYAxis(true);

                        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
                        //layout.addView(graphView);
                        //spinner.setVisibility(View.GONE);
                        //layout.setVisibility(View.INVISIBLE);

                        //layout.setVisibility(layout.GONE);
                        //layout.setVisibility(layout.VISIBLE);
                        //layout.setVisibility(View.VISIBLE);

                        // This works, but its jumpy
                        layout.removeViewAt(0);
                        layout.addView(graphView);

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
