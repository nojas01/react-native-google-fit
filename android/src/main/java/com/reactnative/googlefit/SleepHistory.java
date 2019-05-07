package com.reactnative.googlefit;

import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.reactnative.googlefit.GoogleFitManager;

import com.facebook.react.bridge.ReadableArray;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getTimeInstance;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SleepHistory {
    // Create the sleep session
    private static final String TAG = "RNGoogleFitSleepHistory";
    private static final String sessionName = "SleepHistorySession";
    private static final String identifier = "SleepHistorySession";
    private static final String description = "SleepHistorySession";
    private static ArrayList<Session> sessionsSleep = null;
    private static ArrayList<SleepData> sleepList = null;
    private static ArrayList<SleepInfo> sleepInfoList = null;
    private static Date lastSleepSession = null;

    private ReactContext reactContext;
    private GoogleFitManager googleFitManager;

    // [START parse_dataset]
    private static void dumpDataSet(DataSet dataSet, WritableArray map) {
        //Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();

        float sleepHours;
        for (DataPoint dp : dataSet.getDataPoints()) {
            //Log.i(TAG, dp.getOriginalDataSource().getStreamIdentifier().toString());
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            WritableMap sleepMap = Arguments.createMap();
            int i = 0;
            String day = dateFormat.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));

            for (Field field : dp.getDataType().getFields()) {
                sleepMap.putString("day", day);
                sleepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                sleepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                if (dp.getOriginalDataSource().getAppPackageName().toString().contains("sleep") && field.getName().contains("duration")) {
                    Value value = dp.getValue(field);
                    sleepHours = (float) (Math.round((value.asInt() * 2.778 * 0.0000001 * 10.0)) / 10.0);
                    Log.i(TAG, "\tField: Sleep duration in h " + sleepHours);
                    sleepMap.putDouble("value", sleepHours);
                }
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));

                map.pushMap(sleepMap);
            }
        }
    }
// [END parse_dataset]

    public SleepHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
        this.reactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray readSleepByDate(long startTime, long endTime) {

        WritableArray map = Arguments.createArray();
        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

//        Session session = new Session.Builder()
//                .setName(sessionName)
//                .setIdentifier(identifier)
//                .setDescription(description)
//                .setStartTime(startTime, MILLISECONDS)
//                .setEndTime(endTime, MILLISECONDS)
//                .setActivity(FitnessActivities.SLEEP)
//                .build();

        // Build a session read request
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .setSessionName(sessionName)
                .build();


        // Insert the session into Fit platform
        Log.i(TAG, "Inserting the session in the Sessions API");
        SessionReadResult sessionReadResult = Fitness.SessionsApi
                .readSession(googleFitManager.getGoogleApiClient(), readRequest)
                .await(1, TimeUnit.MINUTES);

        // Get a list of the sessions that match the criteria to check the result.
        List<Session> sessions = sessionReadResult.getSessions();
        Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                + sessions.size());

        for (Session session : sessions) {
            // Process the session
//                            dumpSession(session);

            // Process the data sets for this session
            List<DataSet> dataSets = sessionReadResult.getDataSet(session);
            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
                    if (session.getActivity() == FitnessActivities.SLEEP) {
                        lastSleepSession = new Date(dp.getStartTime(TimeUnit.MILLISECONDS));
                    }
                }
                dumpDataSet(dataSet, map);
            }
        }

        return null;
    }
}
