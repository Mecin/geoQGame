package pl.dmcs.mecin.geoqgame;

import android.provider.BaseColumns;
import android.util.Log;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Tables {
    public Tables() {

    }

    public static final String API_SERVER = "http://mecin.cba.pl/";
    public static final String API_GET_QUEST = "get_quest.php";

    public static final class Position implements BaseColumns {
        private Position() {

        }

        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
    }

    protected static JSONObject POST(JSONObject object, String url) throws JSONException, IOException {
        InputStream inputStream = null;
        JSONObject result;
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            String json = "";

            // 3. build jsonObject
            // done

            // 4. convert JSONObject to JSON to String
            json = object.toString();
            Log.d("JSON POST", json);

            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);

            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
            httpPost.setEntity(se);

            // 7. Set some headers to inform server about the type of the content
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            Log.d("JSON POST", "before execute httpPost");
            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);
            Log.d("JSON POST", "after execute httpPost");

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            //if(inputStream != null)
            //    result = convertInputStreamToJSONObject(inputStream);
            //else
            //    result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        return convertInputStreamToJSONObject(inputStream, url);
    }

    private static JSONObject convertInputStreamToJSONObject(InputStream inputStream, String operation) throws JSONException, IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            if(line.equals("db_connect.php")) {
                Log.d("int2json", "db_connect " + line);
            } else {
                result += line;
            }
        }
        inputStream.close();
        Log.d("int2json result: ", result);
        JSONObject resultJSNObject = new JSONObject(result);

        try {
            if(operation.equals(Tables.API_SERVER + Tables.API_GET_QUEST)) {
                Log.d("OP POST", "Operation: " + operation);
                Log.d("GET QUEST", resultJSNObject.toString());
                if(resultJSNObject.getString("success").equals("true")) {
                    Log.d("OP POST", Tables.API_GET_QUEST + " success.");
                } else {
                    Log.d("OP POST", Tables.API_GET_QUEST + " fail.");
                }
            } else {
                Log.d("OP", "Unexpected operation: " + operation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultJSNObject;
    }
}

