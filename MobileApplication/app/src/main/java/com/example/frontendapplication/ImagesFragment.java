package com.example.frontendapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**main fragment of our application*/
public class ImagesFragment extends Fragment {

    private Activity activity;
    private boolean permissions_verified = false;
    private ImageView imageView;

    private TextView textView;
    private  Connection connection;
    private FloatingActionButton btn_send;
    private FloatingActionButton btn_confirm;
    private static final String TAG = "SERVER_CONNECTION";
    private final String url = Utilities.URL_SERVER+Utilities.IMAGE_SERVICE;
    private Bitmap imageBitmap;
    private String id_image;
    private RelativeLayout loading_panel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.images_fragment,container,false);

        activity = getActivity();
        if(activity!=null) {

            loading_panel = view.findViewById(R.id.loadingPanel);
            loading_panel.setVisibility(View.GONE);

            FloatingActionButton floatingActionButton = view.findViewById(R.id.btn_floating);
            btn_send = view.findViewById(R.id.btn_send);
            btn_confirm = view.findViewById(R.id.btn_confirm);
            textView = view.findViewById(R.id.textView);
            connection = new Connection(getContext(), activity.findViewById(R.id.fragment_container), activity, TAG);


            imageView = view.findViewById(R.id.imageView);



            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    verifyPermissionsAndStartIntent();
                }
            });

            btn_confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(id_image != null){
                        loading_panel.setVisibility(View.VISIBLE);
                        getTextFromImage();
                    }
                }
            });

            btn_send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(((MainActivity)activity).getImageUri() != null && connection.isConnected()) {
                        loading_panel.setVisibility(View.VISIBLE);
                        postImage();
                    }
                    else if (((MainActivity)activity).getImageUri() == null){
                        Toast.makeText(getContext(),"NO IMAGE TO SEND",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),"NO CONNECTION",Toast.LENGTH_SHORT).show();
                    }
                }
            });

            verifyPermissions();


            Bundle bundle = getArguments();
            if(bundle != null){
                Uri uri = Uri.parse(bundle.getString("URI"));
                String name = bundle.getString("NAME");
                Bitmap bitmap = ((MainActivity)activity).getImageBitmap(uri);
                imageView.setImageBitmap(bitmap);
//                Utils.bitmapToMat(bitmap, mat);
//                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
//                Utils.matToBitmap(mat, bitmap);
                ((MainActivity)activity).setImageUri(uri);
                ((MainActivity)activity).setImageName(name);
                FloatingActionButton btn_confirm = view.findViewById(R.id.btn_confirm);
                FloatingActionButton btn_send = view.findViewById(R.id.btn_send);
                textView.setText("");
                btn_send.setVisibility(View.VISIBLE);
                btn_confirm.setVisibility(View.INVISIBLE);
            }


        }

        return view;
    }



    /**function that send a POST request to the server by confirming the cropped image and in return gets the Text*/
    private void getTextFromImage(){

        String url = Utilities.URL_SERVER+Utilities.TESSERACT_SERVICE;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            loading_panel.setVisibility(View.GONE);
                            JSONObject jsonObject = new JSONObject(response);
                            imageView.setImageResource(0);
                            textView.setText(jsonObject.getString("text"));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("id",id_image);
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setTag(TAG);
        connection.addRequest(stringRequest);

    }


    /**send a POST request to the server sending the image taken or picked from the Gallery and in return
    gets the cropped image*/
    private void postImage() {
        Uri imageUri = ((MainActivity)activity).getImageUri();
        Bitmap imageBitmap = ((MainActivity)activity).getImageBitmap(imageUri);


        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
        final String encodedImage = Base64.encodeToString(stream.toByteArray(),Base64.DEFAULT);
        String url = Utilities.URL_SERVER+Utilities.IMAGE_SERVICE;


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            loading_panel.setVisibility(View.GONE);
                            JSONObject jsonObject = new JSONObject(response);
                            id_image = jsonObject.getString("id");
                            byte [] encodeByte = Base64.decode(jsonObject.getString("image"),Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                            imageView.setImageBitmap(bitmap);
                            btn_send.setVisibility(View.INVISIBLE);
                            btn_confirm.setVisibility(View.VISIBLE);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("name",((MainActivity)activity).getImageName());
                params.put("image", encodedImage);
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setTag(TAG);
        connection.addRequest(stringRequest);



    }


    private void verifyPermissionsAndStartIntent() {
        if(permissions_verified) {
            selectImage();
        }

    }

    /**function that handle the image selection that can be done by camera or gallery*/
    private void selectImage(){
        final CharSequence[] items={"Camera", "Gallery","Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Image");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (items[which].toString()){
                    case "Camera":{
                        Intent intent = new Intent(getActivity(), CustomCamera.class);
                        startActivity(intent);
                    }break;
                    case "Gallery":{
                       getPictureFromGallery();
                    }break;
                    case "Cancel":{
                        dialog.dismiss();
                    }break;
                    default:break;

                }

            }
        });
        builder.show();
    }

   /**intent to get the image from the gallery*/
    private void getPictureFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(Intent.createChooser(intent,"Select File"), Utilities.GALLERY_CODE);
    }


    /**function that verify permissions */
    private void verifyPermissions(){
        if(ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.READ_EXTERNAL_STORAGE)+
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)+
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)!=
                PackageManager.PERMISSION_GRANTED){
            requestPermissions();
        }else{
            Toast.makeText(getContext(),"Already granted",Toast.LENGTH_SHORT).show();
            permissions_verified = true;
        }
    }


    private void requestPermissions() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.CAMERA)){
            new AlertDialog.Builder(this.getContext()).setTitle("Permission needed")
                    .setMessage("This permission is needed")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.CAMERA},
                                    Utilities.PERMISSIONS_CODE);

                        }
                    })
                    .setNegativeButton("calcel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        }else{
            requestPermissions(new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA},
                    Utilities.PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == Utilities.PERMISSIONS_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getContext(),"Permission granted",Toast.LENGTH_SHORT).show();
                permissions_verified = true;
            }else{
                permissions_verified = false;
                Toast.makeText(getContext(),"Permission denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        connection.registerNetworkCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        connection.close();
    }

}
