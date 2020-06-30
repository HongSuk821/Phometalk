package com.example.phometalk.Setting;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.phometalk.Activity.MainActivity;
import com.example.phometalk.Firebase.Image;
import com.example.phometalk.Model.UserModel;
import com.example.phometalk.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class ProfilePageActivity extends AppCompatActivity {
    private static final String TAG = "ProfilePageActivity";

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser = mAuth.getCurrentUser();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    private int GET_GALLERY_IMAGE = 103;
    private String imageUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_edit_page);

        //이미지뷰, 텍스트뷰
        ImageView photo = (ImageView) findViewById(R.id.profile_my_image);
        final EditText name = (EditText) findViewById(R.id.my_name);
        final EditText birth = (EditText) findViewById(R.id.my_birth);

        UserInfo(photo,name,birth); //유저 정보 불러와서 보여주기

        //각 버튼
        Button backBtn = (Button) findViewById(R.id.back_btn);
        Button saveBtn = (Button) findViewById(R.id.save_btn);
        Button gallery = (Button) findViewById(R.id.profile_gallery_btn);

        //뒤로가기 버튼
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfilePageActivity.this,MainActivity.class);
                intent.putExtra("fragment", "setting");
                startActivity(intent);
                finish();
            }
        });

        //저장하기 버튼
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String changeName = name.getText().toString();
                String changeBirth = birth.getText().toString();

                //이름만 입력한 경우
                if(changeName != null && changeBirth == null){
                    HashMap<String, Object> info = new HashMap<String, Object>();
                    info.put("name", changeName);
                    database.getReference("userInfo").child(currentUser.getUid()).updateChildren(info).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent intent = new Intent(ProfilePageActivity.this, MainActivity.class);
                            intent.putExtra("fragment","setting");
                            startActivity(intent);
                            finish();
                        }
                    });
                    //생년월일만 입력한 경우
                }else if(changeName == null && changeBirth != null){
                    HashMap<String, Object> info = new HashMap<String, Object>();
                    info.put("birth", changeBirth);
                    database.getReference("userInfo").child(currentUser.getUid()).updateChildren(info).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent intent = new Intent(ProfilePageActivity.this, MainActivity.class);
                            intent.putExtra("fragment","setting");
                            startActivity(intent);
                            finish();
                        }
                    });
                    //두개다 입력 안한경우
                }else if(changeName == null && changeBirth == null){
                    Intent intent = new Intent(ProfilePageActivity.this, MainActivity.class);
                    intent.putExtra("fragment","setting");
                    startActivity(intent);
                    finish();
                }
                //두개다 입력한 경우
                else{
                    HashMap<String, Object> info = new HashMap<String, Object>();
                    info.put("name", changeName);
                    info.put("birth", changeBirth);
                    database.getReference("userInfo").child(currentUser.getUid()).updateChildren(info).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent intent = new Intent(ProfilePageActivity.this, MainActivity.class);
                            intent.putExtra("fragment","setting");
                            startActivity(intent);
                            finish();
                        }
                    });
                }



            }
        });

        //갤러리 버튼 클릭시
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, GET_GALLERY_IMAGE);
            }
        });

    }

    //갤러리 열기
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ImageView photo = (ImageView) findViewById(R.id.profile_my_image);
        if (requestCode == GET_GALLERY_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData(); //이미지 경로 원본
            String imagePath = "Users/" + currentUser.getUid(); //사진파일 경로 및 이름
            UploadFiles(selectedImageUri, imagePath); //사진 업로드

            Glide.with(this).load(selectedImageUri).apply(new RequestOptions().circleCrop()).into(photo);
        }

    }

    //파이어베이스에 사진 업로드
    public void UploadFiles(Uri uri, final String path) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        final StorageReference storageRef = storage.getReference();

        final StorageReference riversRef = storageRef.child(path);
        UploadTask uploadTask = riversRef.putFile(uri);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with the task to get the download URL
                return riversRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    imageUrl = downloadUri.toString();
                    //DB에 저장
                    DatabaseReference ref = database.getReference("userInfo").child(currentUser.getUid());
                    HashMap<String, Object> photo = new HashMap<String, Object>();
                    photo.put("profile", imageUrl);
                    ref.updateChildren(photo); //DB에 저장
                }
            }
        });
    }

    //사용자 정보 불러오기
    public void UserInfo(final ImageView profile, final EditText name, final EditText birth){
        database.getReference("userInfo").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel um = dataSnapshot.getValue(UserModel.class);
                if(!um.getProfile().equals(""))
                    Glide.with(profile.getContext()).load(um.getProfile()).into(profile);
                name.setText(um.getName());
                birth.setText(um.getBirth());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

}
