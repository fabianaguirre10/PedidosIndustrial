package ar.com.syswork.sysmobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ar.com.syswork.sysmobile.daos.DaoCliente;
import ar.com.syswork.sysmobile.daos.DaoVisitasUio;
import ar.com.syswork.sysmobile.daos.DataManager;
import ar.com.syswork.sysmobile.entities.VisitasUio;
import ar.com.syswork.sysmobile.shared.AppSysMobile;


public class visita extends Activity
{
    private String mDirAbsoluto = null;

    private static ImageView imageView;
    private static TextView textView1;
    private static TextView textView2;
    private static TextView longitudeValueGPS, latitudeValueGPS;
    private  static  Button btnguardarvisita;

    private Uri imageUri;



    private static final int REQUEST_CODE_CAMARA = 1;
    private static final int SCALE_FACTOR_IMAGE_VIEW = 4;
    private static final String ALBUM = "PhotoAndImageView";
    private static final String EXTENSION_JPEG = ".jpg";
    private static LocationManager locationManager;
    double longitudeGPS, latitudeGPS;
    private static  String codCliente = "";
    private AppSysMobile app;

    private DataManager dataManager;

    private DaoVisitasUio daoVisitasUio;
    private String codigoVendedor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visita);
        app = (AppSysMobile) this.getApplication();
        dataManager = app.getDataManager();
        daoVisitasUio = dataManager.getDaoVisitasUio();
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
                if (extras.containsKey("cliente"))
                    codCliente = extras.getString("cliente");

        }
        codigoVendedor = app.getVendedorLogueado();
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_fragment_main, container,
                    false);
            imageView = (ImageView) rootView.findViewById(R.id.image_view);
            textView1 = (TextView) rootView.findViewById(R.id.text_view_1);
            textView2 = (TextView) rootView.findViewById(R.id.text_view_2);
            btnguardarvisita = (Button) rootView.findViewById(R.id.btnguardarvisita);

            longitudeValueGPS = (TextView) rootView.findViewById(R.id.longitudeValueGPS);
            latitudeValueGPS = (TextView) rootView.findViewById(R.id.latitudeValueGPS);
            locationManager = (LocationManager) rootView.getContext().getSystemService(Context.LOCATION_SERVICE);
            return rootView;
        }
    }
    private final LocationListener locationListenerGPS = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeGPS = location.getLongitude();
            latitudeGPS = location.getLatitude();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    longitudeValueGPS.setText(longitudeGPS + "");
                    latitudeValueGPS.setText(latitudeGPS + "");
                    Toast.makeText(getApplication(), "GPS Provider update", Toast.LENGTH_SHORT).show();
                    btnguardarvisita.setVisibility(View.VISIBLE);
                }
            });
        }
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }
        @Override
        public void onProviderDisabled(String s) {
        }
    };
    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }
    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Su ubicación esta desactivada.\npor favor active su ubicación " +
                        "usa esta app")
                .setPositiveButton("Configuración de ubicación", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }
    @SuppressLint("MissingPermission")
    public void toggleGPSUpdates(View view) {
        if (!checkLocation())
            return;
        Button button = (Button) view;
        if (button.getText().equals(getResources().getString(R.string.pause))) {
            locationManager.removeUpdates(locationListenerGPS);
            button.setText(R.string.resume);
        } else {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 2 * 20 * 1000, 10, locationListenerGPS);
            button.setText(R.string.pause);
        }
    }
    public  void  guardarvisita(View view){
        String fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());
        VisitasUio objnuevo= new VisitasUio();
        objnuevo.setLatitud(Double.valueOf((String) latitudeValueGPS.getText()));
        objnuevo.setLongitud(Double.valueOf((String) longitudeValueGPS.getText()));
        objnuevo.setCodcliente(codCliente);
        objnuevo.setLinkfotoexterior("https://mardisenginefotos.blob.core.windows.net/industrialmolineravisitas/"+textView1.getText().toString().replace("Nombre: ",""));
        objnuevo.setFechavisita(fechaHora);
        objnuevo.setCodvendedor(codigoVendedor);
        daoVisitasUio.save(objnuevo);
        UploadImage();

        Intent i = null;
        i = new Intent(this,ar.com.syswork.sysmobile.plistavisitas.VisitaActivity.class);
        this.startActivity(i);
        this.finish();


    }
    static final int REQUEST_TAKE_PHOTO = 1;
    public void onClickButton(View view) {
        btnguardarvisita.setVisibility(View.INVISIBLE);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = null;

        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            startActivityForResult(intent, REQUEST_CODE_CAMARA);

        }

    }
    String mCurrentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" +codCliente+ timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );
        textView1.setText("Nombre: " + image.getName());
        textView2.setText("Dir. Absoluto: " + image.getAbsolutePath());
        mDirAbsoluto = image.getAbsolutePath();
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void UploadImage()
    {
        try {
            final InputStream imageStream = getContentResolver().openInputStream(this.imageUri);
            final int imageLength = imageStream.available();

            final Handler handler = new Handler();

            Thread th = new Thread(new Runnable() {
                public void run() {

                    try {

                        final String imageName = com.microsoft.photouploader.ImageManager.UploadImage(imageStream, imageLength,textView1.getText().toString().replace("Nombre: ",""));

                        handler.post(new Runnable() {

                            public void run() {
                                Toast.makeText(getApplication(), "Image Uploaded Successfully. Name = " + imageName, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    catch(Exception ex) {
                        final String exceptionMessage = ex.getMessage();
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplication(), exceptionMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }});
            th.start();
        }
        catch(Exception ex) {

            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_CAMARA:
                if (resultCode == RESULT_OK) {
                    File oupus=null;
                    try {
                         oupus= createImageFile();
                         if(!oupus.exists())
                             oupus.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Bitmap bitmap = escalarBitmap(mDirAbsoluto,
                      //      SCALE_FACTOR_IMAGE_VIEW);
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(imageBitmap);
                    OutputStream os = null;
                    try {
                        os = new BufferedOutputStream(new FileOutputStream(oupus));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    imageBitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
                break;
            default:
                break;
        }

    }
    private Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    /**
     * Crea un Archivo con Extensión .JPG
     *
     * @param context
     * @param album
     * @param nombre
     * @param extension
     * @return File
     * @throws IOException
     */
    private File nombrarArchivo(Context context, String album, String nombre,
                                String extension) throws IOException {
        return new File(obtenerDirectorioPublico(context, album), nombre
                + extension);
    }

    /**
     * Obtiene el Directorio Publico del Almacenamiento Externo
     *
     * @param context
     * @param album
     * @return File
     */
    private File obtenerDirectorioPublico(Context context, String album) {
        File file = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {
            file = new File(
                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    album);
            if (file != null) {
                if (!file.mkdirs()) {
                    if (!file.exists()) {
                        Toast.makeText(context,
                                "Error al crear el directorio.",
                                Toast.LENGTH_SHORT).show();
                        return null;
                    }
                }
            }
        } else {
            Toast.makeText(context, "Tarjeta SD no disponible.",
                    Toast.LENGTH_SHORT).show();
            file = new File(context.getFilesDir(), album);
        }
        return file;
    }

    /**
     * Escala un Bitmap
     *
     * @param uri
     * @param factor
     * @return Bitmap
     */
    public Bitmap escalarBitmap(String uri, Integer factor) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = factor;
        bmOptions.inPurgeable = true;
        return rotarBitmap(uri, BitmapFactory.decodeFile(uri, bmOptions));
    }

    /**
     * Hace la Rotación de un Bitmap
     *
     * @param Url
     * @param bitmap
     * @return Bitmap
     */
    private Bitmap rotarBitmap(String Url, Bitmap bitmap) {
        try {
            ExifInterface exifInterface = new ExifInterface(Url);
            int orientacion = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 1);
            Matrix matrix = new Matrix();

            if (orientacion == 6) {
                matrix.postRotate(90);
            } else if (orientacion == 3) {
                matrix.postRotate(180);
            } else if (orientacion == 8) {
                matrix.postRotate(270);
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true); // rotating bitmap
        } catch (Exception e) {
            // TODO:
        }
        return bitmap;
    }

}