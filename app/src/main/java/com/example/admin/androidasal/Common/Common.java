package com.example.admin.androidasal.Common;

//import com.example.admin.androidasal.Remote.IGoogleAPI;
//import com.example.admin.androidasal.Remote.RetrofitClient;

import com.example.admin.androidasal.Remote.IGoogleAPI;
import com.example.admin.androidasal.Remote.RetrofitClient;

public class Common {

    public static final String signs_tbl = "Signs";
    public static final String users_tbl = "Users";
    public static final String users2_tbl = "Users2";
    public static final String baseURL = "https://maps.googleapis.com";
    public static IGoogleAPI getGoogleAPI() {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
