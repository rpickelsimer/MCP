package edu.rasm.pickel.mcp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ryan on 5/20/15.
 */
public class Sketch implements Parcelable{

    static final char PIN_MODE = 'p';
    static final char ANALOG_WRITE = 'a';
    static final char DIGITAL_WRITE = 'w';
    static final char DELAY = 'd';
    static final String FILE_TO_UI = "f";
    static final char GPS_TO_UI = 'g';
    static final char CAPTURE_LOCATION = 'c';
    static final char TOGGLE_SAVE = 's';

    int pinVal = 0;
    char function = ' ';
    int intVal = 0;
    String stringVal = "";

    public Sketch()          { }  // needed for initial data creation in many cases
    public Sketch(Parcel in) { readFromParcel(in); }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(pinVal);
        out.writeByte((byte)function);
        out.writeInt(intVal);
        out.writeString(stringVal);
    }

    public void readFromParcel(Parcel in) {
        pinVal = in.readInt();
        function = (char)in.readByte();
        intVal = in.readInt();
        stringVal = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toCsvString() {
        String s = "" + function + "" + pinVal + "," + intVal;
        return s;
    }

    public String toString(char code, int pin, int val) {
        if (code == ' ' || pin == 0)
            return null;
        String s = "" + code + "" + pin + "," + val;
        return s;
    }

    public String toString(char code, int val) {
        if (code == ' ')
            return null;
        String s = "" + code + val;
        return s;
    }

    public int getPinVal() {
        return pinVal;
    }

    public void setPinVal(int pinVal) {
        this.pinVal = pinVal;
    }

    public int getFunction() {
        return function;
    }

    public void setFunction(char function) {
        this.function = function;
    }

    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public String getStringVal() {
        return stringVal;
    }

    public void setStringVal(String stringVal) {
        this.stringVal = stringVal;
    }
}