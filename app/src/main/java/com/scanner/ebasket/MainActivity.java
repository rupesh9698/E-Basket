package com.scanner.ebasket;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements PaymentResultListener {

    TextView totalPriceTv, totalWeightTv;
    Button scanBtn, payBtn, clearAllBtn;
    RecyclerView recyclerView;
    AdapterProductItems adapterProductItems;
    List<ModelProductItems> productItems;
    FirebaseUser currentUser;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = findViewById(R.id.scanBtn);
        clearAllBtn = findViewById(R.id.clearAllBtn);
        payBtn = findViewById(R.id.payBtn);
        totalPriceTv = findViewById(R.id.totalPriceTv);
        totalWeightTv = findViewById(R.id.totalWeightTv);
        recyclerView = findViewById(R.id.listRv);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);

        productItems = new ArrayList<>();

        getProductList();

        try {
            FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String totalPrice = "" + ds.child("price").getValue();
                            String totalWeight = "" + ds.child("weight").getValue();

                            payBtn.setVisibility(View.VISIBLE);

                            DecimalFormat df = new DecimalFormat();
                            df.setMaximumFractionDigits(3);

                            NumberFormat f = NumberFormat.getInstance();

                            double totalPriceDouble;
                            double totalWeightDouble;
                            try {
                                totalPriceDouble = Objects.requireNonNull(f.parse(totalPrice)).doubleValue();
                                totalWeightDouble = Objects.requireNonNull(f.parse(totalWeight)).doubleValue();

                                double kilograms = totalWeightDouble / 1000;
                                totalPriceTv.setText("Price : Rs " + df.format(totalPriceDouble) + "/-");
                                totalWeightTv.setText("Weight : " + df.format(kilograms + 0.850) + "kg");
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        totalPriceTv.setText("Price : Rs 0/-");
                        totalWeightTv.setText("Weight : 0g");
                        payBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        catch (Exception ignored) {}

        scanBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ScannerView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        clearAllBtn.setOnClickListener(view -> {

            Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.custom_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);
            TextView resultTv = dialog.findViewById(R.id.resultTv);
            resultTv.setText("Are you sure to remove all items ?");
            Button removeBtn = dialog.findViewById(R.id.removeBtn);
            removeBtn.setText("REMOVE ALL");
            removeBtn.setOnClickListener(v1 -> {
                dialog.dismiss();
                FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Removed all items", Toast.LENGTH_SHORT).show();
                        totalPriceTv.setText("Price : Rs 0/-");
                        totalWeightTv.setText("Weight : 0g");
                        payBtn.setVisibility(View.INVISIBLE);
                    }
                });
            });
            Button cancelBtn = dialog.findViewById(R.id.cancelBtn);
            cancelBtn.setText("CANCEL");
            cancelBtn.setOnClickListener(view1 -> {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
            });
            dialog.show();
        });

        payBtn.setOnClickListener(view -> {
            try {
                FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String totalPrice = "" + ds.child("price").getValue();

                                NumberFormat f = NumberFormat.getInstance();

                                double totalPriceDouble = 0;
                                try {
                                    totalPriceDouble = Objects.requireNonNull(f.parse(totalPrice)).doubleValue();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                int amount = (int) (Math.abs(totalPriceDouble) * 100);
                                Checkout checkout = new Checkout();
                                checkout.setKeyID("rzp_test_vY6jKPSkGm8qvE");
                                JSONObject object = new JSONObject();
                                try {
                                    object.put("name", "E-Basket");
                                    object.put("description", "Payment Gateway");
                                    object.put("currency", "INR");
                                    object.put("amount", amount);
                                    object.put("prefill.contact", "9876543210");
                                    object.put("prefill.email", "e-basket@rzp.com");
                                    object.put("readonly.email", true);
                                    object.put("readonly.contact", true);
                                    checkout.open(MainActivity.this, object);
                                } catch (JSONException e) {
                                    Toast.makeText(MainActivity.this, "Payment Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Cart is Empty..!!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
            catch (Exception ignored) {
            }
        });
    }

    private void getProductList() {
        try {
            FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("Products").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    productItems.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ModelProductItems model = ds.getValue(ModelProductItems.class);
                        productItems.add(model);
                    }
                    adapterProductItems = new AdapterProductItems(MainActivity.this, productItems, totalPriceTv, totalWeightTv);
                    recyclerView.setAdapter(adapterProductItems);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        catch (Exception ignored) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (currentUser == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnCompleteListener(MainActivity.this, task -> {
                        if (task.isSuccessful()) {
                            currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "" + task.getException(), Toast.LENGTH_SHORT).show();
                            System.exit(0);
                        }
                    });
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onPaymentSuccess(String s) {
        FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Payment Successful", Toast.LENGTH_SHORT).show();
                totalPriceTv.setText("Price : Rs 0/-");
                totalWeightTv.setText("Weight : 0g");
                payBtn.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onPaymentError(int i, String s) {

    }
}