package pl.dmcs.mecin.geoqgame;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            Fragment newFragment = new MainActivityFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.activity_main, newFragment).commit();
        }
    }
}
