package com.example.hackinhome2021_stankinfood.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.example.hackinhome2021_stankinfood.activities.MainActivity;
import com.google.firebase.firestore.Exclude;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Order implements Parcelable {
    private String orderId;
    private String name;
    private Timestamp pickupTime;
    private boolean isDone;
    private List<Product> positions = new ArrayList<>();

    public Order() {
    }

    protected Order(Parcel in) {
        orderId = in.readString();
        name = in.readString();
        isDone = in.readByte() != 0;
        positions = in.createTypedArrayList(Product.CREATOR);
    }

    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel in) {
            return new Order(in);
        }

        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };


    @Exclude
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Timestamp getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(Timestamp pickupTime) {
        this.pickupTime = pickupTime;
    }


    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }


    public List<Product> getPositions() {
        return positions;
    }

    public void setPositions(List<Product> positions) {
        this.positions = positions;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(orderId);
        dest.writeString(name);
        dest.writeByte((byte) (isDone ? 1 : 0));
        dest.writeTypedList(positions);
    }


    public void clearPositions() {
        positions.clear();
        Log.d("LOG_MESSAGE", positions.toString());
    }


    @Override
    public String toString() {
        return "\nOrder {" + "\n" +
                "\torderId = '" + orderId + '\'' + ",\n" +
                "\tname = '" + name + '\'' + ",\n" +
                "\tpickupTime = " + pickupTime + ",\n" +
                "\tisDone = " + isDone + ",\n" +
                "\tpositions = " + positions + "\n" +
                '}' + "\n";
    }
}
