package com.example.kota.sendwearsensorvalue;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/*
 * Created by kota on 2015/12/01.
 */
public class CSV {
    String TOTAL_FILE = Environment.getExternalStorageDirectory().getPath() + "/Android/data/TotalWearSensorCommunication.csv";
    String FILE = Environment.getExternalStorageDirectory().getPath() + "/Android/data/wearSensorCommunication.csv";

    public void totalWrite(String sensorValue){

        try{
            //全体のデータ
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(TOTAL_FILE,true),"UTF-8"));
            bw.write(sensorValue);

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void write(String sensorValue){

        try{

            //書き込み時の一時的なファイル
            BufferedWriter bw2 = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(FILE,false),"UTF-8"));
            bw2.write(sensorValue);

            bw2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] read(){
        String [] csvLine = null;
        try{
            FileReader filereader = new FileReader(FILE);
            BufferedReader bufferedreader = new BufferedReader(filereader);
            String line;
            int i = 0;
            while((line = bufferedreader.readLine()) != null) {
                if (csvLine != null) csvLine[i] = line;
                i++;
            }
            filereader.close();
        }catch(Exception e){
            System.out.println("read err");
        }
        return csvLine;
    }
}
