package pl.dmcs.mecin.geoqgame;


import android.app.Fragment;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends ListFragment {

    protected ArrayAdapter<String> adapter;
    private List<String> getHistoryArrayList = null;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Log.d("onActivityCreated", "savedInstanceState != null");
            String[] values = savedInstanceState.getStringArray("storedAdapter");
            if (values != null) {
                Log.d("onActivityCreated", "values != null");
                adapter.clear();
                adapter.addAll(values);
                adapter.notifyDataSetChanged();
                //setListAdapter(adapter);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        // To avoid null pointer exception when fragment is shadowed
        if(adapter != null) {
            String[] storedAdapter = new String[adapter.getCount()];
            for (int i = 0; i < adapter.getCount(); i++) {
                storedAdapter[i] = adapter.getItem(i);
            }

            savedState.putStringArray("storedAdapter", storedAdapter);
        }
        super.onSaveInstanceState(savedState);
        Log.d("onSaveInstanceState", "savedState put data");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_history, container, false);

        final DatabaseHandler db = new DatabaseHandler(getActivity());

        getHistoryArrayList = db.getHistory(10);

        adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, getHistoryArrayList);
        setListAdapter(adapter);

        final TextView noOfTextView = (TextView) v.findViewById(R.id.get_no_of);

        Button getButton = (Button) v.findViewById(R.id.get_last_history);

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int noOf;
                if(noOfTextView.getText().toString().equals("")) {
                    noOf = -13;
                } else {
                    noOf = Integer.valueOf(noOfTextView.getText().toString());
                }

                if(noOf > 0) {
                    getHistoryArrayList.clear();
                    getHistoryArrayList.addAll(db.getHistory(noOf));
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getActivity(), "Wrong value of last: " + noOf + ".", Toast.LENGTH_SHORT).show();
                }

            }
        });
        //adapter.notifyDataSetChanged();

        return v;
    }

}
