package com.example.daniel.baratieri;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Login extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String TAG=Login.class.getName();
    private ProgressDialog progressDialog;
    private RelativeLayout loginLayout;
    private DatabaseReference mDatabase;

    private EditText login_email;
    private EditText login_password;
    private Snackbar snackbar;

    private LoginButton button_fb;
    private CallbackManager mCallbackManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        loginLayout=(RelativeLayout) findViewById(R.id.login_layout);

        login_email=(EditText) findViewById(R.id.login_email);
        login_password=(EditText) findViewById(R.id.login_password);

        mCallbackManager= CallbackManager.Factory.create();
        button_fb = (LoginButton) findViewById(R.id.login_facebook);
        button_fb.setReadPermissions(Arrays.asList("email"));

        button_fb.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "onSuccess: "+loginResult.getAccessToken().toString());
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel: Cancelado");

            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "onError: "+error.toString());
            }
        });

        mAuth=FirebaseAuth.getInstance();

        mAuthListener= new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();
                if(user!=null){
                    Log.d(TAG, "onAuthStateChanged: signed_in "+ user);
                    Intent i = new Intent(Login.this,ActivityMain.class);
                    startActivity(i);
                }else{
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                }

            }
        };

    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        Log.d(TAG, "handleFacebookAccessToken: "+accessToken.toString());
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull final Task<AuthResult> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(getApplicationContext(),"Error "+task.toString(),Toast.LENGTH_LONG).show();
                }else{
                    mDatabase = FirebaseDatabase.getInstance().getReference("baratieri").child("users");
                    mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            boolean exist=false;
                            String uid=task.getResult().getUser().getUid();
                            String name=task.getResult().getUser().getDisplayName();
                            String email=task.getResult().getUser().getEmail();
                            User newUser= new User(email,name);
                            Log.d(TAG, "onDataChangeFbAuth: "+name);
                            for (DataSnapshot snap:dataSnapshot.getChildren()){
                                if( snap.getValue(User.class).getEmail().equals(newUser.getEmail()) ){
                                    exist=true;
                                    break;
                                }
                            }
                            if(!exist){
                                mDatabase.child(uid).setValue(newUser);
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });


                }
            }

        });
    }


    public void onClick_Login(View view) {

        if (!TextUtils.isEmpty(login_email.getText().toString()) && !TextUtils.isEmpty(login_password.getText().toString())){
            if(Patterns.EMAIL_ADDRESS.matcher(login_email.getText().toString()).matches()){
                performLogin(login_email.getText().toString(),login_password.getText().toString());
            }else{
                Toast.makeText(this,"Digite email valido",Toast.LENGTH_SHORT);
            }
        }else{
            if(TextUtils.isEmpty(login_email.getText().toString())){
                Toast.makeText(this,"Digite email",Toast.LENGTH_SHORT);
            }else{
                Toast.makeText(this,"Digite password",Toast.LENGTH_SHORT);
            }
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }



    public void onClick_registrarse(View view) {
        showRegisterDialog();
    }

    public void onClick_recuperar(View view) { showResetPswdDialog();   }


    private void performLogin(final String email, final String password){

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        if(!task.isSuccessful())
                        {
                            if(password.length() < 6)
                            {
                                Toast.makeText(Login.this,"La contraseña debe tener mas de 6 caracteres",Toast.LENGTH_SHORT).show();


                            }else  if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                Toast.makeText(Login.this,"Email no valido",Toast.LENGTH_SHORT).show();

                            }else {
                                Toast.makeText(Login.this,"Email/Username o contraseña incorrecta! Verifiquelo e intente de nuevo",Toast.LENGTH_SHORT).show();

                            }
                        }
                        else{
                            mDatabase = FirebaseDatabase.getInstance().getReference("baratieri").child("users");
                            String uid=task.getResult().getUser().getUid();
                            String name=task.getResult().getUser().getDisplayName();
                            String email=task.getResult().getUser().getEmail();
                            User newUser= new User(email,name);
                            mDatabase.child(uid).setValue(newUser);

                            Intent i = new Intent(Login.this,ActivityMain.class);
                            startActivity(i);
                        }

                    }
                });
    }
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void showResetPswdDialog()
    {
        LayoutInflater li = LayoutInflater.from(this);

        View prompt = li.inflate(R.layout.resetpwd_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(prompt);


        final EditText email = (EditText) prompt.findViewById(R.id.login_email);
        final EditText cemail = (EditText) prompt.findViewById(R.id.login_cemail);


        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Ok", null) //Set to null. We override the onclick
                .setNegativeButton("Cancelar", null);

        AlertDialog alertDialog=alertDialogBuilder.create();


        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {


            @Override
            public void onShow(final DialogInterface dialog) {


                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                            if (!TextUtils.isEmpty(cemail.getText().toString()) && !TextUtils.isEmpty(email.getText().toString())){
                                if(email.getText().toString().equals(cemail.getText().toString())) {
                                    progressDialog = new ProgressDialog(Login.this);
                                    progressDialog.setIndeterminate(true);
                                    progressDialog.setMessage("Enviando Correo");
                                    progressDialog.show();
                                    mAuth.sendPasswordResetEmail(email.getText().toString())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Log.d(TAG, "Email sent.");
                                                    }
                                                    progressDialog.dismiss();
                                                    dialog.dismiss();
                                                }
                                            });
                                }

                            }   else{
                                Toast.makeText(Login.this,"Hay Campos Vacios",Toast.LENGTH_SHORT).show();
                            }


                    }
                });
            }
        });
        alertDialog.show();
        alertDialog.getButton(alertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

    }

    private void showRegisterDialog() {
        LayoutInflater li = LayoutInflater.from(this);

        View prompt = li.inflate(R.layout.register_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(prompt);

        final EditText username = (EditText) prompt.findViewById(R.id.login_username);
        final EditText email = (EditText) prompt.findViewById(R.id.login_email);
        final EditText password = (EditText) prompt.findViewById(R.id.login_password);
        final TextView title=(TextView) prompt.findViewById(R.id.toolbar_title);

        title.setText("Registro");


        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Ok", null) //Set to null. We override the onclick
                .setNegativeButton("Cancelar", null);

        AlertDialog alertDialog=alertDialogBuilder.create();


        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {


            @Override
            public void onShow(final DialogInterface dialog) {


                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!TextUtils.isEmpty(username.getText().toString()) && !TextUtils.isEmpty(email.getText().toString())){
                            progressDialog=new ProgressDialog(Login.this);
                            progressDialog.setIndeterminate(true);
                            progressDialog.setMessage("Creando usuario...");
                            progressDialog.show();

                            mAuth.createUserWithEmailAndPassword(email.getText().toString(),password.getText().toString())
                                    .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if(!task.isSuccessful())
                                            {
                                                snackbar = Snackbar.make(loginLayout,"Error: "+task.getException(), Snackbar.LENGTH_SHORT);
                                                snackbar.show();
                                                progressDialog.dismiss();

                                            }
                                            else{
                                                mDatabase = FirebaseDatabase.getInstance().getReference("baratieri").child("users");
                                                User user= new User(email.getText().toString(),username.getText().toString());
                                                mDatabase.child(mAuth.getCurrentUser().getUid()).setValue(user);

                                                Toast.makeText(Login.this,"Registro exitoso! ",Toast.LENGTH_SHORT).show();

                                                progressDialog.dismiss();
                                                dialog.dismiss();

                                            }
                                        }
                                    });

                        }   else{
                            Toast.makeText(Login.this,"Hay Campos Vacios",Toast.LENGTH_SHORT).show();
                        }


                    }
                });
            }
        });
        alertDialog.show();
        alertDialog.getButton(alertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        alertDialog.getButton(alertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
    }

}
