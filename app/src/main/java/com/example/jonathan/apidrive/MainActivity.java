package com.example.jonathan.apidrive;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient apiClient;
    private final static String LOGTAG = "android-drive";
    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final  int REQUEST_CODE_OPENER = 2;
    private boolean fileOperation = false;
    private Button btnCrearFichero;
    private EditText Text, Cuerpo;
    String MesageforTosk;
    private DriveId mFileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Text = (EditText)  findViewById(R.id.editText);
        Cuerpo = (EditText)  findViewById(R.id.editText2);
      /*  apiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();

        btnCrearFichero = (Button) findViewById(R.id.btnCrearFichero);


        btnCrearFichero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    @Override
                    public void run() {
                        //createFile(Text.getText()+".txt");

                    }
                }.start();
                Text.setText("");

            }
        });*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (apiClient ==null)
        {
            apiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }
        apiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.i(LOGTAG, "GoogleApiClient connection failed: " + connectionResult.toString());

        if (!connectionResult.hasResolution()) {


            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }

        try {

            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);

        } catch (IntentSender.SendIntentException e) {

            Log.e(LOGTAG, "Exception while starting resolution activity", e);
        }

    }

    public void onClickCreateFile(View view){
        fileOperation = true;

        // create new contents resource
        Drive.DriveApi.newDriveContents(apiClient)
                .setResultCallback(driveContentsCallback);

    }


    final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {

                    if (result.getStatus().isSuccess()) {

                        if (fileOperation == true) {

                            CreateFileOnGoogleDrive(result);

                        } else {

                            OpenFileFromGoogleDrive();

                        }
                    }


                }
            };


    public void OpenFileFromGoogleDrive(){

        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { "text/plain", "text/html" })
                .build(apiClient);
        try {
            startIntentSenderForResult(

                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);

        } catch (IntentSender.SendIntentException e) {

            Log.w(LOGTAG, "Unable to send intent", e);
        }

    }

    public void CreateFileOnGoogleDrive(DriveApi.DriveContentsResult result){


        final DriveContents driveContents = result.getDriveContents();

        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents
                OutputStream outputStream = driveContents.getOutputStream();
                Writer writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write(Cuerpo.getText().toString());
                    writer.close();
                    MesageforTosk = Cuerpo.getText().toString();
                } catch (IOException e) {
                    Log.e(LOGTAG, Cuerpo.getText().toString());
                }

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(Text.getText().toString())
                        .setMimeType("text/plain")
                        .setStarred(true).build();


                // create a file in root folder
                Drive.DriveApi.getRootFolder(apiClient)
                        .createFile(apiClient, changeSet, driveContents)
                        .setResultCallback(fileCallback);


            }

        }.start();

        Cuerpo.setText("");
        Text.setText("");
    }


    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (result.getStatus().isSuccess()) {

                        Toast.makeText(getApplicationContext(), "file created: "+""+
                                MesageforTosk, Toast.LENGTH_LONG).show();

                    }

                    return;

                }
            };

    public void onClickOpenFile(View view){
        fileOperation = false;

        // create new contents resource
        Drive.DriveApi.newDriveContents(apiClient)
                .setResultCallback(driveContentsCallback);
    }
    private void writeSampleText(DriveContents driveContents) {
        OutputStream outputStream = driveContents.getOutputStream();
        Writer writer = new OutputStreamWriter(outputStream);

        try {
            writer.write("Esto es un texto de prueba!");
            writer.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "Error al escribir en el fichero: " + e.getMessage());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOGTAG, "GoogleApiClient connection suspended");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(apiClient != null)
        {
        apiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        switch (requestCode) {

            case REQUEST_CODE_OPENER:

                if (resultCode == RESULT_OK) {

                    mFileId = (DriveId) data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                    Log.e("file id", mFileId.getResourceId() + "");

                    String url = "https://drive.google.com/open?id="+ mFileId.getResourceId();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }

                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
