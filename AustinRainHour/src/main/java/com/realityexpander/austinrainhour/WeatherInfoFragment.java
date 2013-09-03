package com.realityexpander.austinrainhour;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Created by Kin on 9/3/13.
 */
public class WeatherInfoFragment extends Fragment {

    int precip[];
    int intensity[];

    TextView textView;

    public WeatherInfoFragment( int[] startPrecip, int[] startIntensity ){

        precip = startPrecip;
        intensity = startIntensity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_time_prob_intense, container, false);

        // text_time, text_precip, text_intense
        textView = (TextView) rootView.findViewById(R.id.text_time);
        textView.setText("Time\nNow\n+15 min\n+30 min\n+45 min\n+60 min");
        textView = (TextView) rootView.findViewById(R.id.text_precip);
        textView.setText("Precip Chance\n" + precip[0]+"%\n"+ precip[1]+"%\n"+ precip[2]+"%\n" + precip[3]+"%\n");
        textView = (TextView) rootView.findViewById(R.id.text_intense);
        textView.setText("Intensity\n" + intensity[0]+"%\n"+ intensity[1]+"%\n"+ intensity[2]+"%\n" + intensity[3]+"%\n");

        return rootView;
    }

    public void SetPrecipAndIntensity(int[] curPrecip, int[] curIntensity){
        precip = curPrecip;
        intensity = curIntensity;
    }
}


