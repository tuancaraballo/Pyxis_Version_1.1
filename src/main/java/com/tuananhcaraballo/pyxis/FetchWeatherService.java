package com.tuananhcaraballo.pyxis;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class FetchWeatherService extends IntentService {

    protected ResultReceiver mReceiver; // --> receiver sent by the user as an extra in the intent

    private static final String TAG = "-- FetchWeatherService --";

    public FetchWeatherService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER_WEATHER);

        if (mReceiver == null) { //--> some error checking
            Log.wtf(TAG, "No Receiver was passed");
            return;
        }

        Double Longitude = -1.0;
        Double Latitude = -1.0;

        Bundle extras = intent.getExtras();
        if(extras.containsKey(Constants.LONGITUDE) && extras.containsKey(Constants.LONGITUDE)){
            Longitude = Double.parseDouble(intent.getStringExtra(Constants.LONGITUDE));
            Latitude =  Double.parseDouble(intent.getStringExtra(Constants.LATITUDE));
        }
        if (Latitude == -1.0 || Longitude == -1.0) { // --> some error checking
            String errorMessage = "No Longitude or Latitude was provided";
            Log.wtf(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT,-1,-1,-1,errorMessage);
            return;
        }

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuffer buffer;

        // TO DO:  Hide you API key somewhere, look up where in Android you hide keys
        String htttpURl = "http://api.openweathermap.org/data/2.5/weather?lat="+ Latitude.toString() +"&lon=" +
                Longitude.toString() +"&APPID=e961668dcf266409643ffcca78dacaa9";

        try {
            URL url = new URL(htttpURl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            parseString(buffer);

        } catch (MalformedURLException e) {
            Log.wtf(TAG, "MaLFORMED INPUT");
            e.printStackTrace();
        } catch (IOException e) {
            Log.wtf(TAG, "IOException e -- 1");
            e.printStackTrace();
        } finally {
            if (connection != null) {
                Log.wtf(TAG, "CONNECTION NOT NULL");
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    Log.wtf(TAG, "CLOSING READER");
                    reader.close();
                }
            } catch (IOException e) {
                Log.wtf(TAG, "IOException e -- 2");
                e.printStackTrace();
            }
        }
    }


    private void parseString(StringBuffer buffer){
        String finalJson = buffer.toString();
        Log.wtf(TAG, "GOT TO PARSE STRING" + finalJson);

        try {
            JSONObject root = new JSONObject(finalJson);
            JSONArray weather_array = root.getJSONArray("weather");
            JSONObject weather_object = weather_array.getJSONObject(0);
            JSONObject main = root.getJSONObject("main");

            String description = weather_object.getString("description");
            String icon_ID = weather_object.getString("icon");
            Log.wtf(TAG,"Icon is " + icon_ID);

            Double min_temp_double = (main.getDouble("temp_min") - Constants.KELVIN_CONSTANT);
            Double max_temp_double = (main.getDouble("temp_max") - Constants.KELVIN_CONSTANT);
            Double ave_temp_double = (main.getDouble("temp") - Constants.KELVIN_CONSTANT);
            int min_temp = min_temp_double.intValue();
            int max_temp = max_temp_double.intValue();
            int ave_temp = ave_temp_double.intValue();

            deliverResultToReceiver(Constants.SUCCESS_RESULT,min_temp,
                    max_temp,ave_temp,description,icon_ID);


        } catch (JSONException e) {
            Log.wtf(TAG, " PARSING WAS  UNSUCCESSFUL!!");
            e.printStackTrace();
        }

    }

    // add also the icon, you need to get it into a different http Connection
    private void deliverResultToReceiver(int resultCode, int Temp_Min,
                                         int Temp_Max, int Ave_Temp,
                                         String Weather_Description,
                                         String icon_ID) {
        Log.wtf(TAG,"ITS ON DELIVER_RESULT_TO_RECEIVER!!");
        Bundle bundle = new Bundle();
        bundle.putString(Constants.WEATHER_DESCRIPTION, Weather_Description);
        if(resultCode == Constants.SUCCESS_RESULT){
            bundle.putString(Constants.ICON_ID,icon_ID);
            bundle.putInt(Constants.MAX_TEMP, Temp_Max);
            bundle.putInt(Constants.MIN_TEMP,Temp_Min);
            bundle.putInt(Constants.AVERAGE_TEMP,Ave_Temp);
        }else{
            Log.wtf(TAG,"Unsuccessful operation, !!");
        }
        mReceiver.send(resultCode,bundle);
    }
}
