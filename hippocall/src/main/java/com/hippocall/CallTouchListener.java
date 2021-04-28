package com.hippocall;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hippocall.confcall.IncomingJitsiCallActivity;

public class CallTouchListener implements View.OnTouchListener {

    private View swipeView;
    private Context context;
    private Boolean vibrate = false;
    float dX, dY;
    float initialdX = 0f;
    float initialdY = 0f;
    private boolean isMoving = false;
    private View itemView;
    private OnCallItemTouchListener touchListener;

    public CallTouchListener(View itemView, View swipeView, Context context) {
        this.itemView = itemView;
        this.swipeView = swipeView;
        this.context = context;
    }

    public CallTouchListener(View itemView, View swipeView, Context context, OnCallItemTouchListener touchListener) {
        this.itemView = itemView;
        this.swipeView = swipeView;
        this.context = context;
        this.touchListener = touchListener;
    }




    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                initialdX = swipeView.getX();
                initialdY = swipeView.getY();

                dX = swipeView.getX() - event.getRawX();
                dY = swipeView.getY() - event.getRawY();

                Log.e("Down", dX + "  " + dY);

                if(context instanceof FuguCallActivity) {
                    ((FuguCallActivity) context).onItemTouch(swipeView);
                }

                if(touchListener != null) {
                    touchListener.onItemTouch(swipeView);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (event.getRawY() + dY > 0) {
                    if ((event.getRawY() + dY) > 400) {
                        swipeView.animate()
                                .x(400)
                                .y(initialdY)
                                .setDuration(100)
                                .start();

                    } else {
                        swipeView.animate()
                                .x(initialdX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();

                        Log.e("Position--->", (event.getRawY() + dY) + "");

                        if (event.getRawY() + dY > 0 && event.getRawY() + dY < 150) {
                            if (!vibrate) {
                                vibrate = true;
                                Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrate.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                                } else {
                                    //deprecated in API 26
                                    vibrate.vibrate(20);
                                }
                                if(context instanceof FuguCallActivity) {
                                    ((FuguCallActivity) context).onItemAnswered(swipeView);
                                }

                                if(touchListener != null) {
                                    touchListener.onItemAnswered(swipeView);
                                }
                            }

                        }

                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                vibrate = false;
                swipeView.animate()
                        .x(initialdX)
                        .y(initialdY)
                        .setDuration(400)
                        .start();
                if(context instanceof FuguCallActivity) {
                    ((FuguCallActivity) context).onItemTouchReleased(swipeView);
                }

                if(touchListener != null) {
                    touchListener.onItemTouchReleased(swipeView);
                }

            default:
                return false;
        }
        return true;
    }

    public interface OnCallItemTouchListener {
        void onItemTouch(View swipeView);

        void onItemTouchReleased(View swipeView);

        void onItemAnswered(View swipeView);
    }

}

