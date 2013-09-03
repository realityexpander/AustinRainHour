package com.realityexpander.austinrainhour;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/**
 * Created by Kin on 9/3/13.
 */
public class BarGraphFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bargraph, container, false);

        // draw initial graph
        int num = 60;
        GraphView.GraphViewData[] data = new GraphView.GraphViewData[num];
        for(int i=0; i<num; i++) {
            data[i] = new GraphView.GraphViewData(i, 0);
        }
        // graph with dynamically generated horizontal and vertical labels
        GraphView graphView;
        graphView = new LineGraphView(getActivity(), "Chance of Rain %");
        // add data
        graphView.addSeries(new GraphViewSeries(data));
        ((LineGraphView) graphView).setBackgroundColor(0xFFFFFF);
        ((LineGraphView) graphView).setDrawBackground(true);
        // set view port, start=0, size=60
        graphView.setViewPort(0, 59);
        graphView.setScrollable(true);
        graphView.getGraphViewStyle().setTextSize(10);
        graphView.setManualYAxisBounds(100,0);
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.graph1);
        layout.addView(graphView);

        return rootView;
    }
}
