package com.teraim.fieldapp.ui;

import android.view.LayoutInflater;
import android.view.View;

public interface DrawerMenuItem {
    public int getViewType();
    public View getView(LayoutInflater inflater, View convertView);
}
