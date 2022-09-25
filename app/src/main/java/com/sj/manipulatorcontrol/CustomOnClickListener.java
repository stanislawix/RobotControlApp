package com.sj.manipulatorcontrol;

import android.view.View;

public interface CustomOnClickListener extends View.OnClickListener {



    @Override
    default void onClick(View view) {

    }
}
