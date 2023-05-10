package com.scanner.ebasket;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@SuppressLint("SetTextI18n")
public class AdapterProductItems extends RecyclerView.Adapter<AdapterProductItems.MyHolder> {

    final Context context;
    final List<ModelProductItems> productItems;
    final TextView totalPriceTv;
    final TextView totalWeightTv;

    public AdapterProductItems(Context context, List<ModelProductItems> productItems, TextView totalPriceTv, TextView totalWeightTv) {
        this.context = context;
        this.productItems = productItems;
        this.totalPriceTv = totalPriceTv;
        this.totalWeightTv = totalWeightTv;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_product_items, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        String msgTimeStamp = productItems.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("Products");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);

        String nameTv = productItems.get(position).getName();
        String priceTv = productItems.get(position).getPrice();
        String weightTv = productItems.get(position).getWeight();

        holder.nameTv.setText(nameTv);
        holder.priceTv.setText("Price : Rs " + priceTv + "/-");
        holder.weightTv.setText("Weight : " + weightTv + "g");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (final DataSnapshot ds : snapshot.getChildren()) {

                    holder.listLayout.setOnLongClickListener(view -> {
                        Dialog dialog = new Dialog(context);
                        dialog.setContentView(R.layout.custom_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.setCanceledOnTouchOutside(false);
                        TextView resultTv = dialog.findViewById(R.id.resultTv);
                        resultTv.setText("Are you sure to remove this item ?");
                        Button removeBtn = dialog.findViewById(R.id.removeBtn);
                        removeBtn.setText("REMOVE");
                        removeBtn.setOnClickListener(v1 -> {
                            dialog.dismiss();
                            String currentProductPrice = "" + ds.child("price").getValue();
                            String currentProductWeight = "" + ds.child("weight").getValue();
                            ds.getRef().removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot1) {
                                            if (snapshot1.exists()) {
                                                for (DataSnapshot ds1 : snapshot1.getChildren()) {
                                                    DecimalFormat df = new DecimalFormat();
                                                    df.setMaximumFractionDigits(2);

                                                    NumberFormat f = NumberFormat.getInstance();

                                                    String totalPrice = "" + ds1.child("price").getValue();
                                                    String totalWeight = "" + ds1.child("weight").getValue();

                                                    double currentProductPriceDouble = 0;
                                                    double currentProductWeightDouble = 0;
                                                    double totalPriceOldDouble = 0;
                                                    double totalWeightOldDouble = 0;
                                                    try {
                                                        currentProductPriceDouble = Objects.requireNonNull(f.parse(currentProductPrice)).doubleValue();
                                                        currentProductWeightDouble = Objects.requireNonNull(f.parse(currentProductWeight)).doubleValue();
                                                        totalPriceOldDouble = Objects.requireNonNull(f.parse(totalPrice)).doubleValue();
                                                        totalWeightOldDouble = Objects.requireNonNull(f.parse(totalWeight)).doubleValue();
                                                    } catch (ParseException e) {
                                                        e.printStackTrace();
                                                    }

                                                    double totalPriceDouble = totalPriceOldDouble - currentProductPriceDouble;
                                                    double totalWeightDouble = totalWeightOldDouble - currentProductWeightDouble;

                                                    HashMap<String, Object> hashMap = new HashMap<>();
                                                    hashMap.put("price", "" + df.format(totalPriceDouble));
                                                    hashMap.put("weight", "" + df.format(totalWeightDouble));

                                                    FirebaseDatabase.getInstance("https://scanner-r18-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).child("Total").setValue(hashMap).addOnCompleteListener(task1 -> {
                                                        if (task1.isSuccessful()) {

                                                            totalPriceTv.setText("Price : Rs " + df.format(totalPriceDouble) + "/-");
                                                            totalWeightTv.setText("Weight : " + df.format(totalWeightDouble) + "g");

                                                            Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            });
                        });
                        Button cancelBtn = dialog.findViewById(R.id.cancelBtn);
                        cancelBtn.setText("CANCEL");
                        cancelBtn.setOnClickListener(view1 -> {
                            dialog.dismiss();
                            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
                        });
                        dialog.show();
                        return false;
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return productItems.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        final TextView nameTv, priceTv, weightTv;
        final LinearLayoutCompat listLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            nameTv = itemView.findViewById(R.id.nameTv);
            priceTv = itemView.findViewById(R.id.priceTv);
            weightTv = itemView.findViewById(R.id.weightTv);
            listLayout = itemView.findViewById(R.id.listLayout);
        }
    }
}
