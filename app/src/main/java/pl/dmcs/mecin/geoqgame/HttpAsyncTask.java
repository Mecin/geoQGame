package pl.dmcs.mecin.geoqgame;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class HttpAsyncTask extends AsyncTask<Object, Void, JSONObject> {

    public AsyncResponse delegate = null;

    public interface AsyncResponse {
        void getQuests(JSONObject quests);
        void notifyFailedOperation(String msg);
    }

    @Override
    protected JSONObject doInBackground(Object... params) {

        String operation = (String)params[1];
        try {
            if(operation.equals(Tables.API_SERVER + Tables.API_GET_QUEST)) {
                Log.d("OP", "Operation: " + operation);
                return Tables.POST((JSONObject) params[0], (String) params[1]);
            } else {
                Log.d("OP", "Unexpected operation: " + operation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JSONObject();
    }
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(JSONObject result) {
        Log.d("onPostExecute POST", result.toString());

        try {
            if(result.getString("success").equals("true")) {
                //Toast.makeText(getActivity().getApplicationContext(), "Data Sent!", Toast.LENGTH_SHORT).show();
                delegate.getQuests(result);
            } else {
                if(result.get("message") != null) {
                    //Toast.makeText(getActivity().getApplicationContext(), result.getString("message"), Toast.LENGTH_SHORT).show();
                    delegate.notifyFailedOperation(result.getString("message"));
                } else {
                    //Toast.makeText(getActivity().getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}