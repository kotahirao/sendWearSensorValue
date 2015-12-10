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

/**
 * Created by kota on 2015/12/01.
 */
public class CSV {
    String TOTAL_FILE = Environment.getExternalStorageDirectory().getPath() + "/Android/data/TotalWearSensorCommunication.csv";
    String FILE = Environment.getExternalStorageDirectory().getPath() + "/Android/data/wearSensorCommunication.csv";

    public void totalWrite(String sensorValue){

        try{
            //全体のデータ
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(TOTAL_FILE,false),"UTF-8"));
            String value = sensorValue;
            bw.write(value);

            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String sensorValue){

        try{

            //書き込み時の一時的なファイル
            BufferedWriter bw2 = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(FILE,false),"UTF-8"));
            String value2 = sensorValue;
            bw2.write(value2);

            bw2.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
                csvLine[i] = line;
                i++;
            }
            filereader.close();
        }catch(Exception e){
            System.out.println("read err");
        }
        return csvLine;
    }
}
