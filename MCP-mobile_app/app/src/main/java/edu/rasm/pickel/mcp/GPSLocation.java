package edu.rasm.pickel.mcp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ryan on 5/20/15.
 */
public class GPSLocation implements Parcelable{

    int id;
    String name;
    long lat;       // these may need to be floats -> we'll see after I test data on SD card
    long lng;

    public GPSLocation(){}
    public GPSLocation(Parcel in) {readFromParcel(in);}
    public GPSLocation(String name, long lat, long lng) {
        //this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(id);
        out.writeString(name);
        out.writeLong(lat);
        out.writeLong(lng);
    }

    public void readFromParcel(Parcel in) {
        id = in.readInt();
        name = in.readString();
        lat = in.readLong();
        lng = in.readLong();
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLat() {
        return lat;
    }

    public void setLat(long lat) {
        this.lat = lat;
    }

    public long getLng() {
        return lng;
    }

    public void setLng(long lng) {
        this.lng = lng;
    }
}
