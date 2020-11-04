package com.example.frontendapplication;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theartofdev.edmodo.cropper.CropImageView;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Main activity
 */
public class MainActivity extends AppCompatActivity {

    private Bitmap imageBitmap;
    private String imageName;
    private Uri imageUri;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;
        Bundle bundle = getIntent().getExtras();
        Utilities.simpleReplaceFragment(new ImagesFragment(), R.id.fragment_container,this,bundle);
    }

    /**
     * Gets gallery image from intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK ){
            imageUri = data.getData();

            imageBitmap = getImageBitmap(imageUri);

            String absolutePathFile = Utilities.getPath(imageUri,activity);
            imageName= absolutePathFile;
            try {
                imageBitmap = Utilities.modifyOrientation(imageBitmap,absolutePathFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            setupView();

        }
    }



    public void setupView(){
        FloatingActionButton btn_confirm = findViewById(R.id.btn_confirm);
        FloatingActionButton btn_send = findViewById(R.id.btn_send);
        btn_send.setVisibility(View.VISIBLE);
        btn_confirm.setVisibility(View.INVISIBLE);
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageURI(imageUri);
        TextView textView = findViewById(R.id.textView);
        textView.setText("");
    }

    public Uri getImageUri() {
        return imageUri;
    }
    public String getImageName() {
        return imageName;
    }


    /**
     * Gets the image using the URI
     * @param currentPhotoUri uri of the image
     * @return the image bitmap
     */
    public Bitmap getImageBitmap(Uri currentPhotoUri){
        ContentResolver resolver = this.getApplicationContext()
                .getContentResolver();
        try {
            InputStream stream = resolver.openInputStream(currentPhotoUri);
            if(stream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                stream.close();
                return bitmap;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }





    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
