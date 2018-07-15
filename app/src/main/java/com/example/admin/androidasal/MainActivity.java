package com.example.admin.androidasal;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.admin.androidasal.Common.Common;
import com.example.admin.androidasal.Model.User2;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    Button btnSignIn, btnRegister;

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    RelativeLayout rootLayout;

    private final static int PERMISSION = 1000;

    @Override
    protected void attachBaseContext(Context newBase){


        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Sau khi setContentView
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_main);

        //Init Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference(Common.users2_tbl);//Change name of References to "Users2", old name is "Users"


        //Init View
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        //Event
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRegisterDialog();
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnSignIn.setEnabled(false);
                showLoginDialog();
            }
        });
    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("ĐĂNG KÝ ");
        dialog.setMessage("Vui lòng điền email để đăng ký");

        LayoutInflater inflater = LayoutInflater.from(this);
        View register_layout = inflater.inflate(R.layout.layout_register, null);

        final MaterialEditText edtEmail = register_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = register_layout.findViewById(R.id.edtPassword);
        final MaterialEditText edtName = register_layout.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = register_layout.findViewById(R.id.edtPhone);

        dialog.setView(register_layout);

        //set button
        dialog.setPositiveButton("ĐĂNG KÝ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                //check valication
                if (TextUtils.isEmpty(edtEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Nhập địa chỉ email", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (TextUtils.isEmpty(edtPhone.getText().toString())) {
                    Snackbar.make(rootLayout, "Nhập SDT", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Nhập mật khẩu", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (edtEmail.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Mật khẩu quá ngắn !!!", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                //Register new user
                auth.createUserWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                //Save user to db
                                User2 user2 = new User2();

                                user2.setEmail(edtEmail.getText().toString());
                                user2.setName(edtName.getText().toString());
                                user2.setPhone(edtPhone.getText().toString());
                                user2.setPassword(edtPassword.getText().toString());

                                //use email to key
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user2)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(rootLayout, "Đăng ký thành công!", Snackbar.LENGTH_SHORT)
                                                        .show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(rootLayout, "Thất bại " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                                        .show();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootLayout, "Thất bại " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        });
            }
        });

        dialog.setNegativeButton("HỦY BỎ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }

    private void showLoginDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("ĐĂNG NHẬP ");
        dialog.setMessage("Vui lòng điền email để đăng nhập");

        LayoutInflater inflater = LayoutInflater.from(this);
        View login_layout = inflater.inflate(R.layout.layout_login, null);

        final MaterialEditText edtEmail = login_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = login_layout.findViewById(R.id.edtPassword);

        dialog.setView(login_layout);

        //set button
        dialog.setPositiveButton("ĐĂNG NHẬP", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();


                //Set disable button Sign In if is processing
                btnSignIn.setEnabled(false);


                //check valication
                if (TextUtils.isEmpty(edtEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Nhập địa chỉ email", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Nhập mật khẩu", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (edtEmail.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Mật khẩu quá ngắn !!!", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }


                final SpotsDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();

                //Login
                auth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();
                                startActivity(new Intent(MainActivity.this, Home.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingDialog.dismiss();
                        Snackbar.make(rootLayout, "Thất bại " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();

                        //Active button
                        btnSignIn.setEnabled(true);
                    }
                });
            }

        });

        dialog.setNegativeButton("HUỶ BỎ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }
}