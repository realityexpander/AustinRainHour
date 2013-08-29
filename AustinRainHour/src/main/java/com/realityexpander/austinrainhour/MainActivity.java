package com.realityexpander.austinrainhour;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {

    /* API variables */
    private final String API_KEY = "e432b91f50911786e5653ab22eb3073a";
    private final String API_GEONAMES_USERNAME = "realityexpander";
    private static final String TAG = "0xD3ADB33F MainActivity"; // debug helper

    /* MainActivity variables */
    protected Location location;
    private static long updateForecastDefaultDelay = 5000;
    private static long updateDisplayDefaultDelay = 1000;

    private double oldLong, oldLat;

    /* loading layout variables */
    private boolean contentLoaded = false;

    private Context thisMainActivity = this;


    private class WeatherListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {

            location = loc;
            // Log.i(TAG, loc.getLatitude() + "," + loc.getLongitude());

            updateForecast(); // Allow update until I know how to optimize

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        WeatherListener weatherListener = new WeatherListener();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, weatherListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, weatherListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateForecast();

        // draw initial graph
        int num = 60;
        GraphView.GraphViewData[] data = new GraphView.GraphViewData[num];
        for (int i=0; i<num; i++) {
            data[i] = new GraphView.GraphViewData(i, 0);
        }
        // graph with dynamically generated horizontal and vertical labels
        GraphView graphView;
        graphView = new LineGraphView(
                this
                , "Chance of Rain %"
        );
        // add data
        graphView.addSeries(new GraphViewSeries(data));
        ((LineGraphView) graphView).setBackgroundColor(0xFFFFFF);
        ((LineGraphView) graphView).setDrawBackground(true);
        // set view port, start=0, size=60
        graphView.setViewPort(0, 59);
        graphView.setScrollable(true);
        graphView.getGraphViewStyle().setTextSize(10);
        graphView.setManualYAxisBounds(100,0);
        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
        layout.addView(graphView);
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

                    updateDisplay(
                            new Forecast(                                       // instantiate a forecast.io forecast
                                    MainActivity.this.location.getLatitude(),   // the Location.getLatitude()
                                    MainActivity.this.location.getLongitude(),  // the Location.getLongitude()
                                    API_KEY                                     // your unique forecast.io api_key
                            ),

                            // Get the new GeoLocation name from location
                            new GeoLocation(
                                    MainActivity.this.location.getLatitude(),
                                    MainActivity.this.location.getLongitude(),
                                    API_GEONAMES_USERNAME
                            )
                    );
                    h.removeCallbacks(this);
                } else {
                    updateForecast();
                }
            }
        }, interval); /* todo:simulate a slow network */
    }

    private void updateForecast() {
        updateForecast(updateForecastDefaultDelay);
    }

    private void updateDisplay(final Forecast forecast, final GeoLocation geoLocation, long interval) {
        final Handler h = new Handler();
        final Forecast f = forecast;
        final GeoLocation g = geoLocation;

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (g.getGeoNamesStatus() == HttpStatus.SC_OK) {
                    //g.removeCallbacks(this); // Why doesnt this work?
                    // Set the geolocationName
                    TextView textView = (TextView) findViewById(R.id.text_view);
                    textView.setText("");

                        try {
                            ListView listView = (ListView)findViewById(R.id.list_view);
                            JSONObject currentLocation = geoLocation.getGeoNamesData();

                            String locationName;
                            int     numItems = currentLocation.length();
                            if (currentLocation.length() <= 2)
                                locationName = "Location error.";
                            else
                                locationName =
                                    currentLocation.getJSONArray("geonames").getJSONObject(0).getString("name") + ", " +
                                    currentLocation.getJSONArray("geonames").getJSONObject(0).getString("adminCode1");
                            textView.setText(locationName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //updateDisplay(f,g);
                }

                // Get the weather data
                if (f.getStatus() == HttpStatus.SC_OK) {
                    h.removeCallbacks(this);

                    ProgressBar spinner = (ProgressBar) findViewById(R.id.loading_spinner);

                    spinner.setVisibility(View.GONE);

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
                        String[] minutelyBlockString = new String[]{
                                minutelyBlockArray.getJSONObject(0).getString("time"),
                                minutelyBlockArray.getJSONObject(0).getString("precipIntensity"),
                                minutelyBlockArray.getJSONObject(0).getString("precipProbability")
                        };

                        String[] conditions = new String[]{
                                formattedTime.toString(),
                                minutelySummaryString[0],
                                currentForecast.getString("summary") + " currently.",
                                //currentForecast.getString("precipIntensity"),
                                //currentForecast.getString("precipProbability"),
                                currentForecast.getString("temperature") + "F.",
                                currentForecast.getString("apparentTemperature") + "F feels like.",
                                //currentForecast.getString("dewPoint"),
                                currentForecast.getString("windSpeed") + "mph bearing " + currentForecast.getString("windBearing") + "deg.",
                                //currentForecast.getString("cloudCover"),
                                Double.toString(Double.valueOf(currentForecast.getString("humidity"))*100) + "% humidity.",
                                //currentForecast.getString("pressure"),
                                //currentForecast.getString("visibility"),
                                currentForecast.getString("ozone") + " ozone.",
                        };

                        ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.weather_info, conditions);
                        listView.setAdapter(adapter);

                        Log.e(TAG, "minute summary=" + minutelySummaryString[0]);
                        Log.e(TAG, "minute time=" + minutelySummaryString[0]);


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
                            data[i] = new GraphView.GraphViewData(i, 100 * Float.valueOf(minutelyBlockArray.getJSONObject(i).getString("precipIntensity")));
                        }
                        GraphViewSeries seriesRainIntensity = new GraphViewSeries("Intensity", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(230, 230, 30), 3), data);

                        // graph with dynamically generated horizontal and vertical labels
                        GraphView graphView;
                        graphView = new LineGraphView(
                                // thisMainActivity //
                                MainActivity.this
                                , "Rain Chance & Intensity"
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
                        layout.removeViewAt(0);
                        //layout.setVisibility(layout.GONE);
                        //layout.setVisibility(layout.VISIBLE);
                        layout.addView(graphView);
                        //layout.setVisibility(View.VISIBLE);



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    updateDisplay(f,g);
                }
            }
        }, interval);
    }

    private void updateDisplay(final Forecast forecast, final GeoLocation geoLocation) {
        updateDisplay(forecast, geoLocation, updateDisplayDefaultDelay);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
