package com.example.kota.sendwearsensorvalue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.jar.Attributes;

/**
 * Created by kota on 2015/12/04.
 */
class DrawableView extends View {
    //グラフの値
    private String[] time;
    private float[]ax,ay,az,heartRate;
    private boolean isAttached;

    //表
    private int SENSOR_NUM = 4;
    private int MARGIN = 20;
    private int GRAPH_HEIGHT = 300;
    public DrawableView(Context context) {
        super(context);
        init();
    }

    public DrawableView(Context context,AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public DrawableView(Context context,AttributeSet attrs,int defStyle){
        super(context,attrs,defStyle);
        init();
    }

    private void init(){
//        if (isInEditMode()) {
//            // 編集モードだったら処理終了
//            return ;
//        }
        setFocusable(true);
        time = new String[256];
        ax = new float[256];
        ay = new float[256];
        az = new float[256];
        heartRate = new float[256];
        for(int i = 0;i<ax.length;i++){
            ax[i] = ay[i] = az[i] = heartRate[i] = 0.0f;
            time[i] = "";
        }
    }
    public void setSensorValue(String _time,float x,float y,float z,float hr){
        for(int j = ax.length-1;j>0;j--){
            time[j] = time[j-1];
            ax[j] = ax[j-1];
            ay[j] = ay[j-1];
            az[j] = az[j-1];
            heartRate[j] = heartRate[j-1];
        }
        time[0] = _time;
        ax[0] = -((GRAPH_HEIGHT/2)/20)*x;
        ay[0] = -((GRAPH_HEIGHT/2)/20)*y;
        az[0] = -((GRAPH_HEIGHT/2)/20)*z;
        heartRate[0] = -hr;
//        String str = x + ":" + y + ":" +z + ":" + hr;
//        Log.i("sensor",str);
    }

    protected void onDraw(Canvas c){
        super.onDraw(c);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
//        c.drawText(time, 50, 50, p);
        //基準線
        p.setARGB(255,0,0,0);
        for(int h = 1;h < SENSOR_NUM;h++){
            c.drawLine(0, GRAPH_HEIGHT*h-(GRAPH_HEIGHT/2)+(h-1)*MARGIN, 1200, GRAPH_HEIGHT*h-(GRAPH_HEIGHT/2)+(h-1)*MARGIN, p);
            c.drawLine(0, GRAPH_HEIGHT*h+(h-1)*MARGIN, 1200, GRAPH_HEIGHT*h+(h-1)*MARGIN, p);
            c.drawLine(0, GRAPH_HEIGHT*h+(GRAPH_HEIGHT/2)+(h-1)*MARGIN, 1200, GRAPH_HEIGHT*h+(GRAPH_HEIGHT/2)+(h-1)*MARGIN, p);
        }
        for (int i =0; i < ax.length-1; i++) {
            //時間
            //c.drawText(time[i], i*10, 50, p);
            //加速度X
            p.setARGB(255, 128, 128, 128);
            c.drawLine(i*10, ax[i]+GRAPH_HEIGHT, (i+1)*10, ax[i+1]+GRAPH_HEIGHT, p);
            //加速度Y
            p.setARGB(255, 0, 255, 0);
            c.drawLine(i*10, ay[i]+GRAPH_HEIGHT*2 + MARGIN*1, (i+1)*10, ay[i+1]+GRAPH_HEIGHT*2 + MARGIN*1, p);
            //加速度Z
            p.setARGB(255, 255, 0, 255);
            c.drawLine(i*10, az[i]+GRAPH_HEIGHT*3 + MARGIN*2, +(i+1)*10, az[i+1]+GRAPH_HEIGHT*3 + MARGIN*2, p);
            //心拍数
            p.setARGB(255, 255, 0, 0);
            c.drawLine(i*10, heartRate[i]+GRAPH_HEIGHT*4 + MARGIN*3, +(i+1)*10, heartRate[i+1]+GRAPH_HEIGHT*4 + MARGIN*3, p);
        }
    }
    private Handler handler = new Handler(){
        public void handleMessage(Message message){
            if(isAttached){
                invalidate();
                sendEmptyMessageDelayed(0,10);
            }
        }


    };

    protected void onAttachedToWindow() {
        isAttached = true;
        handler.sendEmptyMessageDelayed(0, 10);
        super.onAttachedToWindow();
    }

    protected void onDetachedFromWindow(){
        isAttached = false;
        super.onDetachedFromWindow();
    }
}