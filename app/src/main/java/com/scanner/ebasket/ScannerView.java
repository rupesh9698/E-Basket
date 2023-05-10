package com.scanner.ebasket;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Objects;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerView extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    ZXingScannerView scannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);

        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        scannerView.startCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    public void handleResult(Result rawResult) {
        CollectionReference productsRef = FirebaseFirestore.getInstance().collection("Products");
        productsRef.document(rawResult.getText()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                String timestamp = String.valueOf(System.currentTimeMillis());

                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);

                NumberFormat f = NumberFormat.getInstance();

                String name = "" + documentSnapshot.getString("name");
                String price = "" + documentSnapshot.getString("price");
                String weight = "" + documentSnapshot.getString("weight");

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("name", name);
                hashMap.put("price", price);
                hashMap.put("timestamp", timestamp);
                hashMap.put("weight", weight);

                FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("Products").push().setValue(hashMap).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {

                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        String totalPriceOld = "" + ds.child("price").getValue();
                                        String totalWeightOld = "" + ds.child("weight").getValue();

                                        double priceDouble = 0;
                                        double weightDouble = 0;
                                        double totalPriceOldDouble = 0;
                                        double totalWeightOldDouble = 0;
                                        try {
                                            priceDouble = Objects.requireNonNull(f.parse(price)).doubleValue();
                                            weightDouble = Objects.requireNonNull(f.parse(weight)).doubleValue();
                                            totalPriceOldDouble = Objects.requireNonNull(f.parse(totalPriceOld)).doubleValue();
                                            totalWeightOldDouble = Objects.requireNonNull(f.parse(totalWeightOld)).doubleValue();
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }

                                        double totalPriceDouble = priceDouble + totalPriceOldDouble;
                                        double totalWeightDouble = weightDouble + totalWeightOldDouble;

                                        HashMap<String, Object> hashMap2 = new HashMap<>();
                                        hashMap2.put("price", "" + df.format(totalPriceDouble));
                                        hashMap2.put("weight", "" + df.format(totalWeightDouble));

                                        FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("Total").setValue(hashMap2).addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Intent intent = new Intent(ScannerView.this, MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });
                                    }
                                }
                                else {
                                    double priceDouble = 0;
                                    double weightDouble = 0;
                                    try {
                                        priceDouble = Objects.requireNonNull(f.parse(price)).doubleValue();
                                        weightDouble = Objects.requireNonNull(f.parse(weight)).doubleValue();
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }

                                    HashMap<String, Object> hashMap3 = new HashMap<>();
                                    hashMap3.put("price", "" + df.format(priceDouble));
                                    hashMap3.put("weight", "" + df.format(weightDouble));

                                    FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("Total").setValue(hashMap3).addOnCompleteListener(task12 -> {
                                        if (task12.isSuccessful()) {
                                            Intent intent = new Intent(ScannerView.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        Intent intent = new Intent(ScannerView.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                Toast.makeText(ScannerView.this, "Product Not Found", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ScannerView.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ScannerView.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }
}