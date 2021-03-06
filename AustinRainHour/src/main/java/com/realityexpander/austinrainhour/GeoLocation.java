package com.realityexpander.austinrainhour;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;


/**
 * Created by realityexpander on 8/27/13.
 */
public class GeoLocation {

    // Returns town name
     private final String API_URL_NETWORK = "http://api.geonames.org/findNearbyPlaceNameJSON?";

    // Returns street address
    private final String API_URL_GPS = "http://api.geonames.org/findNearestAddressJSON?";

    private String API_GEONAMES_USERNAME;

    protected int status;
    private HttpResponse response;
    protected JSONObject data;

    private class FetchDataAsync extends AsyncTask<String, Void, HttpResponse> {

        AndroidHttpClient client;

        @Override
        protected HttpResponse doInBackground(String... urls) {
            String link = urls[0];
            HttpGet request = new HttpGet(link);
            client = AndroidHttpClient.newInstance("Android");
            HttpResponse response = null;

            try {
                response = client.execute(request);
                if( response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    response.getEntity().writeTo(output);
                    output.close();

                    String result = output.toString();

                    data = new JSONObject(result);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            client.close();
            return response;
        }

        protected void onPostExecute(HttpResponse res){
            if (res != null) {
                status = res.getStatusLine().getStatusCode();
                response = res;
            }
        }
    }

    public GeoLocation(Double latitude, Double longitude, int serviceType, String username){
        this.API_GEONAMES_USERNAME = username;

        if (latitude != 0 && longitude != 0) {
            String geoNamesUrl = buildGeoNamesUrl(latitude, longitude, serviceType);
            new FetchDataAsync().execute(geoNamesUrl);
        }
    }

    private String buildGeoNamesUrl(Double latitude, Double longitude, int serviceType) {


        switch (serviceType) {
            case 1:
                // Returns town name
                //     private final String API_URL = "http://api.geonames.org/findNearbyPlaceNameJSON?";
                // http://api.geonames.org/findNearbyPlaceNameJSON?lat=33.2323423&lng=22.474473&username=realityexpander
                return API_URL_NETWORK + "lat=" + latitude.toString() + "&lng=" + longitude.toString() + "&username=" + API_GEONAMES_USERNAME;
                //break;

            case 2:
                // http://api.geonames.org/extendedFindNearby?lat=30.2517&lng=-97.7687&username=realityexpander
                return API_URL_GPS + "lat=" + latitude.toString() + "&lng=" + longitude.toString() + "&username=" + API_GEONAMES_USERNAME;
                //break;
        }

        return null;
    }

    public HttpResponse getGeoNamesResponse() {
        return response;
    }

    public JSONObject getGeoNamesData() {
        return data;
    }

    public int getGeoNamesStatus() {
        return status;
    }
}
