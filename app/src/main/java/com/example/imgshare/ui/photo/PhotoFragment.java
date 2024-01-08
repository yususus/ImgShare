package com.example.imgshare.ui.photo;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.imgshare.databinding.FragmentPhotoBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhotoFragment extends Fragment {

    private FragmentPhotoBinding binding;
    private int  ResultImage = 1;


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPhotoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button btnselect = binding.select;
        btnselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i,ResultImage);

            }
        });
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ResultImage && resultCode == RESULT_OK && null != data){
            Uri selectedImage = data.getData();
            String [] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null,null,null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = binding.imgView;
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            Button share = binding.share;
            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReference();
                    StorageReference photoRef = storageRef.child("posts/" + UUID.randomUUID().toString());

                    UploadTask uploadTask = photoRef.putFile(selectedImage);

                    FirebaseAuth auth = FirebaseAuth.getInstance();

                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while(!uriTask.isSuccessful());
                            Uri dowloadUrl = uriTask.getResult();

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference userRef = db.collection("users").document(auth.getUid());
                            userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    String name = documentSnapshot.getString("name");

                                    Map<String,Object> post = new HashMap<>();
                                    post.put("imageUrl",dowloadUrl.toString());
                                    post.put("name", name);

                                    List<String> label = new ArrayList<>();
                                    if(binding.car.isChecked()){
                                        label.add("Araba");
                                    }
                                    if(binding.Football.isChecked()){
                                        label.add("Futbol");
                                    }
                                    if(binding.nature.isChecked()){
                                        label.add("Doğa");
                                    }
                                    if(binding.View.isChecked()){
                                        label.add("Manzara");
                                    }

                                    post.put("label", label);

                                    db.collection("posts").document().set(post)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Log.d(TAG,"BAŞARILI BİR ŞEKİLDE YÜKLENDİ");
                                                    Toast.makeText(getActivity(),"Başarılı yükleme işlemi",Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.d(TAG,"BAŞARISIZ BİR ŞEKİLDE YÜKLENDİ");
                                                    Toast.makeText(getActivity(),"Başarısız yükleme işlemi",Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    @Override
    public  void onDestroyView(){
        super.onDestroyView();
        binding = null;
    }
}










