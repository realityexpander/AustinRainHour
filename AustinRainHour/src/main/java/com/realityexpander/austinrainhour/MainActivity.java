package com.realityexpander.austinrainhour;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.realityexpander.austinrainhour.io.network.requests.INetworkRequest;
import com.realityexpander.austinrainhour.io.network.responses.INetworkResponse;
import com.realityexpander.austinrainhour.io.network.responses.NetworkResponse;
import com.realityexpander.austinrainhour.io.toolbox.NetworkServiceTask;
import com.realityexpander.austinrainhour.io.v2.network.services.ForecastService;
import com.realityexpander.austinrainhour.io.v2.transfer.DataBlock;
import com.realityexpander.austinrainhour.io.v2.transfer.DataPoint;
import com.realityexpander.austinrainhour.io.v2.transfer.LatLng;

import java.util.List;

public class MainActivity extends Activity {

    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                longitude = location.getLongitude();
                latitude = location.getLatitude();

                //if (latitude != 0 && longitude != 0 ){
                    // Called when a new location is found by the network location provider.
                    Log.e("0xD3ADB33F Location.getLatitude()=", Double.toString(latitude) );
                    Log.e("0xD3ADB33F Location.getLongitude()=", Double.toString(longitude) );
                //} else {
                //    Log.e("0xD3ADB33F Location No GPS","");
                //}

                LatLng latLng = LatLng.newBuilder()
                        .setLatitude(37.8267) //latitude) // 37.422006)
                        .setLongitude(-122.423) // longitude) // -122.084095)
                        //.setTime(System.currentTimeMillis())
                        .build();

                ForecastService.Request request = ForecastService.Request.newBuilder( "e432b91f50911786e5653ab22eb3073a" )
                        .setLatLng(latLng)
                        .build();

                // v2
                AsyncTask<INetworkRequest,Void,INetworkResponse> execute = new NetworkServiceTask() {
                    @Override
                    protected void onPostExecute(INetworkResponse network) {
                        if (network == null || network.getStatus() == NetworkResponse.Status.FAIL) {
                            Toast.makeText(MainActivity.this, "FORECAST ERROR", Toast.LENGTH_SHORT).show();

                            return;
                        }

                        ForecastService.Response response = (ForecastService.Response) network;

                        DataBlock temp = response.getForecast().getMinutely();
                        List<DataPoint> tempMinute = temp.getData();
                        DataPoint tempPoint = tempMinute.get(0);
                        long sunrise = tempPoint.getSunriseTime();

                        Toast.makeText(MainActivity.this, response.getForecast() != null ?
                                //response.getForecast().getCurrently().getSummary() : "FORECAST", Toast.LENGTH_SHORT ).show();
                                //Double.toString(response.getForecast().getCurrently().getTemperature()) : "FORECAST", Toast.LENGTH_SHORT ).show();
                                response.getForecast().getHourly().getSummary() : "FORECAST", Toast.LENGTH_SHORT).show();

                    }
                }.execute(request);

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        try{
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (Exception e) {
            Log.e("0xD3ADB33F Location", "NETWORK PROVIDER FAILED");
        }

        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (Exception e) {
            Log.e("0xD3ADB33F Location", "GPS PROVIDER FAILED");
        }






    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
