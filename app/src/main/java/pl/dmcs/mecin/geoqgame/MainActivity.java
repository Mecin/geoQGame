package pl.dmcs.mecin.geoqgame;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends Activity implements MainActivityFragment.OnClickActivityAction {

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

    @Override
    public void onButtonClick(Fragment fragment) {
        switchFragment(fragment);
    }

    public void switchFragment(Fragment fragment) {

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.activity_main, fragment);
        fragmentTransaction.commit();
    }
}
