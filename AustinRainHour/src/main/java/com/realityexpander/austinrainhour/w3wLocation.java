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
public class w3wLocation {

    // Returns w3w name
    private final String API_URL_W3W = "http://api.what3words.com/position/";

    private String API_W3W_USERNAME;

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

    public w3wLocation(Double latitude, Double longitude, String username){
        this.API_W3W_USERNAME = username;

        if (latitude != 0 && longitude != 0) {
            String geoNamesUrl = buildw3wUrl(latitude, longitude);
            new FetchDataAsync().execute(geoNamesUrl);
        }
    }

    private String buildw3wUrl(Double latitude, Double longitude) {
       return API_URL_W3W + latitude.toString() +","+ longitude.toString() + "?key=" + API_W3W_USERNAME;
    }

    public HttpResponse getw3wResponse() {
        return response;
    }

    public JSONObject getw3wData() {
        return data;
    }

    public int getw3wStatus() {
        return status;
    }
}
