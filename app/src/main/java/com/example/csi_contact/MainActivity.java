package com.example.csi_contact;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.csi_contact.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // view Binding
    private ActivityMainBinding binding;
    // TAG
    private static final String TAG = "CONTACT_TAG";
    private static final int WRITE_CONTACT_PERMISSION_CODE = 100;
    private static final int IMAGE_PICK_GALLERY_CODE = 200;
    private String[] contactPermissions;

    private Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init permission array, need only write contact permission
        contactPermissions = new String[]{android.Manifest.permission.WRITE_CONTACTS};

        binding.thumbnailTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGalleryIntent();
            }
        });

        binding.fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if (isWriteContactPermissionEnabled()){
                   saveContact();
               } else {
                   requestWriteContactPermission();
               }
            }
        });
    }

    private void saveContact() {
        String firstName = binding.firstNameEt.getText().toString().trim();
        String lastName = binding.lastNameEt.getText().toString().trim();
        String phoneMobile = binding.phoneMobileEt.getText().toString().trim();
        String phoneHome = binding.phoneHomeEt.getText().toString().trim();
        String email = binding.emailEt.getText().toString().trim();
        String address = binding.addressEt.getText().toString().trim();

        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

        int rawContactId = cpo.size();

        // ajout de id
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        // ajout du nom, prénom
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                .build());

        // ajout du numéro de téléphone mobile
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneMobile)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        // ajout du numéro de téléphone de la maison
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneHome)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());

        // ajout email
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .build());

        // ajout adresse
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.SipAddress.DATA, address)
                .withValue(ContactsContract.CommonDataKinds.SipAddress.TYPE, ContactsContract.CommonDataKinds.SipAddress.TYPE_WORK)
                .build());

        // recuperation et conversion des images pour le stocker avec le contact
        byte[] imageBytes = imageUriToBytes();
        if (imageBytes != null) {
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes)
                    .build());
        } else {
            // contact sans image
        }

        // Sauvegarde contact
        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, cpo);
            Log.d(TAG, "Sauvegarde du contact: sauvegarde...");
            Toast.makeText(this, "Sauvegardé", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Sauvegarde du contact:" + e.getMessage());
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private byte[] imageUriToBytes () {
        Bitmap bitmap;
        ByteArrayOutputStream baos = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);

            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            return baos.toByteArray();

        } catch (Exception e) {
            Log.d(TAG, "imageUriToBytes:" + e.getMessage());
            return null;
        }

    }

    private void openGalleryIntent () {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private boolean isWriteContactPermissionEnabled(){
        boolean result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestWriteContactPermission(){
        ActivityCompat.requestPermissions(this, contactPermissions, WRITE_CONTACT_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // handle permission results

        if (grantResults.length > 0){
            if (grantResults.length > 0){
                boolean haveWriteContactPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (haveWriteContactPermission){
                    saveContact();
                }else {
                    Toast.makeText(this, "Permission refusé", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult ( int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (resultCode == IMAGE_PICK_GALLERY_CODE) {
                image_uri = data.getData();
                binding.thumbnailTv.setImageURI(image_uri);
            }
        } else {
            Toast.makeText(this, "Annuler...", Toast.LENGTH_SHORT).show();
        }
    }
}