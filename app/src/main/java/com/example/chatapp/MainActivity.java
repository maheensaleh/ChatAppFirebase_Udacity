package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Trace;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private  static  final int RC_SIGIN = 1;
    private static  final int RC_PHOTO_PICK = 2;
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private FirebaseDatabase database;
    private DatabaseReference db_ref;
    private ChildEventListener message_child_listner ;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;
    private String mUsername;
    private FirebaseStorage firebaseStorage;
    private StorageReference stroage_ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //firebase things
        database = FirebaseDatabase.getInstance(); // access to entire database
        db_ref = database.getReference().child("messsages");// access  point to a particular part of the db , here 'messages'
        firebaseAuth = FirebaseAuth.getInstance(); // initiate the authentication objject
        firebaseStorage = FirebaseStorage.getInstance(); //initiate the storage
        stroage_ref = firebaseStorage.getReference().child("ChatPhotos"); // get a reference to the storage

        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,"Complete with"),RC_PHOTO_PICK);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                String text_message = mMessageEditText.getText().toString();
                FriendlyMessage message = new FriendlyMessage(text_message,mUsername,null);
                db_ref.push().setValue(message);
                // Clear input box
                mMessageEditText.setText("");
            }
        });


        //set message child listener
//        setMessageChildListener();

        //authentication sigin providers
        final List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.FacebookBuilder().build());

        // listener which is trigered when the activity starts or resume
        firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                // things to do when listener is trigered
                //  the variable firebaseAuth tells whether the user is signedin or not
                FirebaseUser current_user = firebaseAuth.getCurrentUser();

                if (current_user!= null){
                    //means user signed in
                    onSignIn(current_user.getDisplayName());
                    Toast.makeText(MainActivity.this," sign in successful !",Toast.LENGTH_LONG).show();


                }

                else{
                    //user not signed in
                    // now display the firebase sign in ui7jjj
                    Toast.makeText(MainActivity.this,"Not signed in !",Toast.LENGTH_LONG).show();
                    onSignOut();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .setIsSmartLockEnabled(false)
                                    .build(),
                            RC_SIGIN);



                }



            }
        };

    }

    private void onSignOut() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachMessageChildListener();
    }

    private void detachMessageChildListener() {
        if (message_child_listner!=null) {
            db_ref.removeEventListener(message_child_listner);
            message_child_listner = null;

        }
    }

    private void onSignIn(String displayName) {
        mUsername = displayName;
//        attach child listener herte as in video
        setMessageChildListener();
        
    }

    private void setMessageChildListener() {

        if (message_child_listner==null){

            message_child_listner = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    FriendlyMessage newmessage= dataSnapshot.getValue(FriendlyMessage.class); //get value function takes a class as an argument for details see: https://firebase.google.com/docs/reference/android/com/google/firebase/database/DataSnapshot
                    System.out.println("5");
                    mMessageAdapter.add(newmessage);
                    System.out.println(newmessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

            db_ref.addChildEventListener(message_child_listner); // listener which is trigered whenever a new mesage is added

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){

            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }





    }

    @Override
    protected void onResume() {
        super.onResume();
        //when resume, attach the authentication state listener
        firebaseAuth.addAuthStateListener(firebaseAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when pause, remove the authentication state listener
        firebaseAuth.removeAuthStateListener(firebaseAuthStateListener);
//    as in video, detach listener and clear teh adapter
//        detachMessageChildListener();
//        mMessageAdapter.clear();


    }

    /**
     * Dispatch incoming result to the correct fragment.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGIN){

            if (resultCode == RESULT_OK){
                Toast.makeText(MainActivity.this,"signed in now !",Toast.LENGTH_SHORT).show();
            }
            else if ( resultCode == RESULT_CANCELED) {

                Toast.makeText(MainActivity.this,"cancalled",Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        else if ( requestCode == RC_PHOTO_PICK && resultCode == RESULT_OK){
            System.out.println("1");
            Uri selected_photo = data.getData();
            final StorageReference photoRef = stroage_ref.child(selected_photo.getLastPathSegment());
            //upload file to firebase strorage
            System.out.println("2");

            photoRef.putFile(selected_photo).addOnSuccessListener(
                    this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Uri my_uri = uri;
                                    FriendlyMessage current_message = new FriendlyMessage(null,mUsername,my_uri.toString());
                                    db_ref.push().setValue(current_message);

                                }
                            });


                        }
                    }
            );

        }



    }
}