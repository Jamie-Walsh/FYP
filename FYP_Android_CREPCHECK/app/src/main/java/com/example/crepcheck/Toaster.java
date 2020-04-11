package com.example.crepcheck;

import android.content.Context;
import android.widget.Toast;

public class Toaster
{
    Context c;

    // The constructor finds the relevant context to display the toast message.
    public Toaster(Context context){
        c = context;
    }

    public void makeToast(String Msg)
    {
        Toast.makeText(c, Msg, Toast.LENGTH_SHORT).show();
    }

    public void makeLongToast(String Msg)
    {
        Toast.makeText(c, Msg, Toast.LENGTH_LONG).show();
    }
}
