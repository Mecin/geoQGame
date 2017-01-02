package pl.dmcs.mecin.geoqgame;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainActivityFragment extends Fragment {

    OnClickActivityAction mCallback;

    // Interface for parent to communicate
    public interface OnClickActivityAction {
        void onButtonClick(Fragment fragment);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnClickActivityAction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnClickActivityAction");
        }
    }

    public MainActivityFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main_activity, container, false);

        Button discoverButton = (Button) view.findViewById(R.id.discoverButtonFragment);

        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DiscoverActivity.class);
                startActivity(intent);
            }
        });

        Button aboutButton = (Button) view.findViewById(R.id.aboutButtonFragment);

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onButtonClick(new AboutFragment());
            }
        });

        return view;
    }

}
