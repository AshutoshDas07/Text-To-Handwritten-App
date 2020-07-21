package com.example.myapplication;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;

public interface API_Interface {
    @Multipart
    @POST("upload")
    Call<ResponseBody> upload_txt(
        @Part("description") RequestBody description,
        @Part MultipartBody.Part text_file
    );


    @GET("download")
    Call<ResponseBody> download_pdf();
}
