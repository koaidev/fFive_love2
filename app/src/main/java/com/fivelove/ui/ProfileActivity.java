package com.fivelove.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.login.LoginManager;
import com.fivelove.databinding.ActivityProfileBinding;
import com.fivelove.db.model.User;
import com.fivelove.utils.Constants;
import com.fivelove.viewmodel.UserViewModel;
import com.fivelove.viewmodel.UsersViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = ProfileActivity.class.getSimpleName();
    private ActivityProfileBinding binding;
    private Uri imgUrl;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        btnSave = binding.btnSave;
        btnSave.setEnabled(false);
        setContentView(binding.getRoot());

        setViewModel();
        binding.btnLogout.setOnClickListener(view -> signOut());
        binding.avtProfile.setOnClickListener(view -> chooseImage());
        btnSave.setOnClickListener(view -> {
            if (binding.edtName.getText().toString().isEmpty() || binding.edtPhone.getText().toString().isEmpty()) {
                return;
            } else {
                updateProfileOnServer(binding.edtName.getText().toString().trim(), String.valueOf(imgUrl));
            }
        });
    }

    public void setViewModel() {
        final UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.getCurrentUser().observe(this, users -> {
                    binding.setUser(users);
                }
        );
    }

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, Constants.IMAGE);
    }

    public String getExtensionImage(Uri uri) {
        ContentResolver resolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(resolver.getType(uri));
    }

    public void uploadImage(Uri imgUri) {
        final float idImage = System.currentTimeMillis();
        Constants.FIREBASE_STORAGE.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("avt")
                .child(idImage + "." + getExtensionImage(imgUri))
                .putFile(imgUri)
                .addOnSuccessListener(taskSnapshot -> {
                    getImageFromUrl(idImage + "." + getExtensionImage(imgUri));
                    btnSave.setEnabled(true);
                });
    }


    private void updateProfileOnServer(String displayName, String profileImageUrl) {
        UserProfileChangeRequest updateRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .setPhotoUri(Uri.parse(profileImageUrl))
                .build();

        FirebaseAuth.getInstance().getCurrentUser().updateProfile(updateRequest)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        User user = new User(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                                String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl()));
                        updateLocalUser(user);
                        changeToMainActivity();
                    }
                });
        ;

    }

    private void updateLocalUser(User user) {
        final UserViewModel model = new ViewModelProvider(this).get(UserViewModel.class);
        model.updateUser(user);
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            final UsersViewModel model = new ViewModelProvider(this).get(UsersViewModel.class);
            model.deleteAllUser();
            model.getAllFriends().observe(this, users -> {
                if (users.size() == 0) {
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

        }
    }

    public void getImageFromUrl(String idImage) {
        StorageReference storageImage = Constants.FIREBASE_STORAGE.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("avt").child(idImage);
        storageImage.getDownloadUrl().addOnSuccessListener(uri -> {
            Picasso.get().load(uri).into(binding.avtProfile);
            imgUrl = uri;
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imgUri = data.getData();
            binding.avtProfile.setImageURI(imgUri);
            uploadImage(imgUri);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        changeToMainActivity();
    }
    public void changeToMainActivity(){
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}