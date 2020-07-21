package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Button submit_btn,dowload_btn;
    private Uri fileUri;
    private String filePath;
    private String statusText;
    public static final int PICKFILE_RESULT_CODE = 1;
    public static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE=2;
    public static final String baseURL="Your IP with Port";
    ImageView status_icon;
    String fileName="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        submit_btn = findViewById(R.id.submit_btn);
        dowload_btn=findViewById(R.id.download_btn);
        status_icon=findViewById(R.id.status_icon);
        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
            }
        });

        dowload_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadfile();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == -1) {
                    fileUri = data.getData();
                    filePath = fileUri.getPath();
                }
                uploadfile(fileUri);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Uri uri) {

        String path = null;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        Cursor cursor1=getContentResolver().query(uri, null, null, null, null);
        if(cursor == null){
            path = uri.getPath();
        }
        else{
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            int columnIndex = cursor1.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
            cursor1.moveToFirst();
            path = cursor.getString(column_index);
            fileName=cursor1.getString(columnIndex);
            cursor.close();
        }
        if (fileName.indexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".pdf";
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }

    public void uploadfile(Uri fileUri){
        String filePath=getPath(fileUri);
        File file=new File(filePath);
        Log.e("TAG2",filePath);
        RequestBody requestFile=RequestBody.create(MediaType.parse("text/plain"),file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        String descriptionString = "hello, this is description speaking";
        RequestBody description = RequestBody.create(okhttp3.MultipartBody.FORM, descriptionString);
        Retrofit retrofit=new Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create()).build();
        API_Interface client=retrofit.create(API_Interface.class);
        Log.e("TAGFILENAME",fileName);
        Call<ResponseBody> call=client.upload_txt(description,body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.e("TAG","file uploaded");
                statusText="Uploaded Successfully!";
                printToast(statusText);
                status_icon.setBackgroundResource(R.drawable.correct_img);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("TAG",t.getMessage());
                statusText="Uploading Failed";
                printToast(statusText);
                status_icon.setBackgroundResource(R.drawable.wrong_img);
            }
        });
    }

    public void printToast(String statusText){
        Toast.makeText(this,statusText+"",Toast.LENGTH_LONG).show();
    }

    public void downloadfile(){
        Retrofit retrofit=new Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create()).build();
        API_Interface client=retrofit.create(API_Interface.class);
        Call<ResponseBody> call=client.download_pdf();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.e("TAGDownload", "server contacted and has file");
                    printToast("Downloaded Successfully!");
                    boolean writtenToDisk = writeResponseBodyToDisk(response.body());
                    Log.e("TAGDownloadWrite", "file download was a success? " + writtenToDisk);
                } else {
                    Log.e("TAGserverfail", "server contact failed");
                    printToast("Server Connection Failed!");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("TAGDonFailure",t.getMessage());
                printToast(t.getMessage());
            }
        });

        status_icon.setBackgroundResource(R.drawable.folder_img);
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File downloadedfile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),fileName);
            DownloadManager dm = (DownloadManager) this.getSystemService(this.DOWNLOAD_SERVICE);
            dm.addCompletedDownload(fileName, "my description", false, "application/pdf", downloadedfile.getAbsolutePath(), downloadedfile.length(), true);

            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(downloadedfile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.e("TAG20", "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}

