package com.example.chatapp.Fragments;

import com.example.chatapp.Notifications.MyResponse;
import com.example.chatapp.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAvRlWc00:APA91bGzrhWqAzKfjsGyxw97dOARvPF23ZEBBgUCzXBXbGTA7lYbl7mt_MXr6NBOeb9TEqVNqhy6d_X4gvqKvc_BpwXTWw3uBR6sA9teE4U-s-1kwidOyDg_64Nut5YT2_87I-_MGmj4"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
