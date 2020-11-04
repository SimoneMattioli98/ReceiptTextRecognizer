package com.example.frontendapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import android.graphics.Color;

import android.graphics.Paint;

import android.graphics.PixelFormat;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import android.graphics.drawable.Drawable;
import android.hardware.Camera;

import android.net.Uri;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import android.view.Display;

import android.view.Surface;

import android.view.SurfaceHolder;

import android.view.SurfaceView;

import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * custom camera used to take pictures
 */
public class CustomCamera extends AppCompatActivity implements SurfaceHolder.Callback{

    private SurfaceHolder holder,holderTransparent;

    private Camera camera;
    private final int WIDTHOFFSET = 150;
    private final int CORNEROFFSET = 100;

    private FloatingActionButton btn_light;
    private int deviceHeight,deviceWidth;

    private AppCompatActivity activity;

    private Rect rect;
    private Uri uri;
    private String name;
    private Bitmap imageBitmap;
    private static boolean previewing = false;

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        activity = this;

        setContentView(R.layout.activity_custom_camera);

        FloatingActionButton floatingActionButton = findViewById(R.id.btn_capture);
        btn_light = findViewById(R.id.btn_light);
        final SurfaceView cameraView = findViewById(R.id.CameraView);

        btn_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera.Parameters param;
                param = camera.getParameters();
                if(!param.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)) {
                    Toast.makeText(getApplicationContext(),"ON",Toast.LENGTH_SHORT).show();
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                }else{
                    Toast.makeText(getApplicationContext(),"OFF",Toast.LENGTH_SHORT).show();
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                camera.setParameters(param);
            }
        });
        holder = cameraView.getHolder();

        holder.addCallback(this);

        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraView.setSecure(true);

        // Create second surface with another holder (holderTransparent)

        SurfaceView transparentView = findViewById(R.id.TransparentView);

        holderTransparent = transparentView.getHolder();

        holderTransparent.addCallback(this);

        holderTransparent.setFormat(PixelFormat.TRANSLUCENT);

        transparentView.setZOrderMediaOverlay(true);

        //getting the device heigth and width

        deviceWidth=getScreenWidth();

        deviceHeight=getScreenHeight();

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(camera!= null){
                    camera.takePicture(myShutterCallback,myPictureCallback_RAW,myPictureCallback_JPG);
                }
            }
        });
    }

    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback(){

        public void onShutter() {
            // TODO Auto-generated method stub
        }};

    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
        }};

    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);


            int y = (bitmapPicture.getHeight()*rect.left)/deviceWidth;

            int x = (bitmapPicture.getWidth()*rect.top)/deviceHeight;
            int width = (bitmapPicture.getWidth()*rect.height())/deviceHeight;
            int height = (bitmapPicture.getHeight()*rect.width())/deviceWidth;

//            Log.d("LOLOLOLOLOLOL",String.valueOf(y));
//            Log.d("LOLOLOLOLOLOL",String.valueOf(rect.top));
//            Log.d("LOLOLOLOLOLOL",String.valueOf(height));
//            Log.d("LOLOLOLOLOLOL",String.valueOf(bitmapPicture.getHeight()));
//            Log.d("LOLOLOLOLOLOL",String.valueOf(bitmapPicture.getWidth()));

            imageBitmap = Bitmap.createBitmap(bitmapPicture, x, y, width + WIDTHOFFSET, height, null, true);




            createFileAndUri();

            if (imageBitmap != null){
                try {
                    saveImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra("URI",uri.toString());
            intent.putExtra("NAME",name);
            startActivity(intent);
            finish();
        }};


    /**
     * Save the image on the "Pictures" folder in the Gallery
     */
    private void saveImage() throws IOException {
        if(name == null){
            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY).format(new Date());
            name = "Image_" + timeStamp + "";
        }

        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + "");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        imageBitmap = Utilities.rotate(imageBitmap,90);
        OutputStream fos = resolver.openOutputStream(uri);

        //setta la qualità dell'immagine
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        if (fos != null) {
            fos.close();
        }
    }


    /**
     * Create a file and a uri for the image that have to be saved
     */
    private void createFileAndUri() {
        File photoFile;
        try {
            photoFile = createImageFile();// Continue only if the File was successfully created
            if (photoFile != null) {
                ///storage/emulated/0/Android/data/com.example.edengardens/files/Pictures/JPEG_20200501_175825_4857740767044905810.jpg
                uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            }
        } catch (IOException ex) {
            // Error occurred while creating the File
            ex.printStackTrace();
        }
    }

    /**
     *  create the file inside the Gallery and the image name
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY).format(new Date());
        name = "Image_" + timeStamp + ".jpg";

        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                name, /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        String currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    public static int getScreenWidth() {

        return Resources.getSystem().getDisplayMetrics().widthPixels;

    }



    public static int getScreenHeight() {

        return Resources.getSystem().getDisplayMetrics().heightPixels;

    }

    /**
     * Create the rectangular guidelines
     */
    private void Draw()
    {
        Canvas canvas = holderTransparent.lockCanvas(null);

        Paint  paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.CYAN);

        paint.setStrokeWidth(10);

        float rectLeft = 100;

        float rectTop = 100;

        float rectRight = deviceWidth - 100;

        float rectBottom = deviceHeight - 500;

        rect=new Rect((int) rectLeft,(int) rectTop,(int) rectRight,(int) rectBottom);

        canvas.drawRect(rect,paint);

        holderTransparent.unlockCanvasAndPost(canvas);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {

            synchronized(holder)

            {Draw();}   //call a draw method

            camera = Camera.open(); //open a camera

        }

        catch (Exception e) {

            Log.i("Exception", e.toString());

            return;

        }

        Camera.Parameters param;

        param = camera.getParameters();
        Camera.Size size = determineBestPreviewSize(param);

        param.setPreviewSize(size.width,size.height);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        Display display = ((WindowManager) Objects.requireNonNull(getSystemService(WINDOW_SERVICE))).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            camera.setDisplayOrientation(90);
        }

        camera.setParameters(param);

        try {

            camera.setPreviewDisplay(holder);

            camera.startPreview();

        }

        catch (Exception e) {
            e.printStackTrace();
        }

    }



    @Override

    protected void onDestroy() {

        super.onDestroy();

    }

    @Override

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        refreshCamera(); //call method for refress camera

    }

    public void refreshCamera() {

        if (holder.getSurface() == null) {

            return;

        }

        try {

            camera.stopPreview();

        }

        catch (Exception e) {
            e.printStackTrace();
        }

        try {

            camera.setPreviewDisplay(holder);

            camera.startPreview();

        }

        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override

    public void surfaceDestroyed(SurfaceHolder holder) {

        camera.release(); //for release a camera

    }

    public static Camera.Size determineBestPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        return determineBestSize(sizes);
    }

    public static Camera.Size determineBestPictureSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        return determineBestSize(sizes);
    }

    protected static Camera.Size determineBestSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMemory = Runtime.getRuntime().maxMemory() - used;
        for (Camera.Size currentSize : sizes) {
            int newArea = currentSize.width * currentSize.height;
            long neededMemory = newArea * 4 * 4; // newArea * 4 Bytes/pixel * 4 needed copies of the bitmap (for safety :) )
            boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
            boolean isSafe = neededMemory < availableMemory;
            if (isDesiredRatio && isBetterSize && isSafe) {
                bestSize = currentSize;
            }
        }
        if (bestSize == null) {
            return sizes.get(0);
        }
        return bestSize;
    }

}
