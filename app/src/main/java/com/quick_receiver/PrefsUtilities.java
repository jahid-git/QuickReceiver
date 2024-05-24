package com.quick_receiver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrefsUtilities {
    public static final String FIRST_TIME = "first_time";
    private static SharedPreferences sp;

    public static void init(Context context){
        sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }
    public static void setPrefs(String key, String value){
        sp.edit().putString(key, value).apply();
    }

    public static void setPrefs(String key, int value){
        sp.edit().putInt(key, value).apply();
    }

    public static void setPrefs(String key, Boolean value){
        sp.edit().putBoolean(key, value).apply();
    }

    public static String getPrefs(String key, String defaultValue){
        return sp.getString(key, defaultValue);
    }

    public static int getPrefs(String key, int defaultValue){
        return sp.getInt(key, defaultValue);
    }

    public static boolean getPrefs(String key, boolean defaultValue){
        return sp.getBoolean(key, defaultValue);
    }
    public static List<String> getAllLocations() {
        List<String> keyValueList = new ArrayList<>();
        Map<String, ?> allEntries = sp.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(!key.equalsIgnoreCase(FIRST_TIME)) {
                keyValueList.add(key + ":" + value);
            }
        }
        return keyValueList;
    }

    public static void removePrefs(String key){
        sp.edit().remove(key).apply();
    }
    public static void clearPrefs(){
        sp.edit().clear();
        setPrefs(FIRST_TIME, false);
    }
}
