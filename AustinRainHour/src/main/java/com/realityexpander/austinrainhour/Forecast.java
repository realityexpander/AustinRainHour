package com.realityexpander.austinrainhour;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Vic on 13/8/26.
 * ude@learnovatelabs.com
 */
public class Forecast {

    private class FetchDataAsync extends AsyncTask <String, Void, HttpResponse> {

        @Override
        protected HttpResponse doInBackground(String... urls) {
            String link = urls[0];
            HttpGet request = new HttpGet(link);
            AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            HttpResponse response = null;

            try {
                response = client.execute(request);
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    response.getEntity().writeTo(output);
                    output.close();

                    String result = output.toString();

                    data = new JSONObject(result);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        protected void onPostExecute(HttpResponse res){
            status = res.getStatusLine().getStatusCode();
            response = res;
        }
    }

    private final String API_URL = "https://api.forecast.io/forecast/";
    private final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private String API_KEY;

    protected int status;
    private HttpResponse response;
    protected JSONObject data;

    public Forecast(Double latitude, Double longitude, String API_KEY){
        this.API_KEY = API_KEY;
        String forecastUrl = buildForecastUrl(latitude, longitude);
        new FetchDataAsync().execute(forecastUrl);
    }

    private String buildForecastUrl(Double latitude, Double longitude) {
        Locale[] locales = DateFormat.getAvailableLocales();
        Locale locale = (locales.length > 0) ? locales[0] : Locale.US;
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, locale);
        sdf.setTimeZone(TimeZone.getTimeZone(locale.toString()));
        return API_URL + API_KEY + "/" + latitude.toString() + "," + longitude.toString();// + "," + sdf.format(new Date());
    }

    public HttpResponse getResponse() {
        return response;
    }

    public JSONObject getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }
}
