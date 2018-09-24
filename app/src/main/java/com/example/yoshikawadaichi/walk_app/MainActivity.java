package com.example.yoshikawadaichi.walk_app;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;


/*public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //public void change_label(View view){
    //  TextView tv = (TextView)findViewById(R.id.H1);
    //  tv.setText("Changed!!");
    //}
}
*/

public class MainActivity extends Activity implements Runnable, SensorEventListener {
    SensorManager sm;
    TextView tv;
    TextView acc_tv;
    TextView statusview;
    Handler h;
    double gx, gy, gz,gxyz;
    int counter10=0;
    int flag=0;
    int flag_get_acc = 0;
    int flag_get_state = 0;
    String DirName ="/result1/";
    String str;
    String filename;
    String[] alltype = {"walk","run","stay"};
    String output = "サンプル";

    ConverterUtils.DataSource source;
    Instances instances;
    Classifier classifier;
    FastVector out = new FastVector(3);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.textView);
        acc_tv= findViewById(R.id.textView2);

        h = new Handler();
        h.postDelayed(this, 500);
        tv.setText("事前データ収集");
        verifyStoragePermissions(this);

        File dir = new File(Environment.getExternalStorageDirectory().getPath() + DirName);
        dir.mkdir();


        findViewById(R.id.button_walk).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tv.setText("歩行状態を計測中");
                        filename = "walk";
                        flag_get_state = 1;
                        Dialog();
                    }
                });

        findViewById(R.id.button_run).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tv.setText("走り状態を計測中");
                        filename = "run";
                        flag_get_state = 2;
                        Dialog();
                    }
                });

        findViewById(R.id.button_stay).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tv.setText("静止状態を計測中");
                        filename = "stay";
                        flag_get_state = 3;
                        Dialog();
                    }
                });

        findViewById(R.id.button_next).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        make_arff_file();
                    }
                });

        findViewById(R.id.button_del).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetFile();
                        flag = 0;
                    }
                });


    }


    @Override
    public void run() {
        acc_tv.setText("X-axis : " + gx + "\n"
                + "Y-axis : " + gy + "\n"
                + "Z-axis : " + gz + "\n"
                + "XYZ-mix : " + gxyz + "\n");
        h.postDelayed(this, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors =
                sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (0 < sensors.size()) {
            sm.registerListener(this, sensors.get(0),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacks(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        gx = event.values[0];
        gy = event.values[1];
        gz = event.values[2];
        gxyz = Math.sqrt( Math.pow(gx,2) + Math.pow(gy,2) + Math.pow(gz,2));

        FileOutputStream outputStream;

        if(flag_get_acc == 1) {
            if(flag_get_state == 1) {
                str = (gxyz + "," + filename + "\n");
                saveFile(filename, str);
            }
            if(flag_get_state == 2){
                str = (gxyz + "," + filename + "\n");
                saveFile(filename,str);
            }
            if(flag_get_state == 3){
                str = (gxyz + "," + filename + "\n");
                saveFile(filename,str);
            }
        }


        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(output.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        counter10++;
        if(counter10%10 == 0 && flag == 1){
            System.out.println("make_attribute呼び出し");
            make_attribute();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static void verifyStoragePermissions(Activity activity) {
        int readPermission = ContextCompat.checkSelfPermission(activity, mPermissions[0]);
        int writePermission = ContextCompat.checkSelfPermission(activity, mPermissions[1]);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    mPermissions,
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }

    public void saveFile(String filename, String str){
        try {
            FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + DirName
                    + filename + ".csv", true);
            fw.write(str);
            fw.close();


            System.out.println("保存完了");
        }catch(IOException e){
            e.printStackTrace();

            System.out.println("保存失敗");

        }
    }

    public void resetFile(){

        File fw1 = new File(Environment.getExternalStorageDirectory().getPath() + DirName
                + "walk" + ".csv");
        fw1.delete();

        File fw2 = new File(Environment.getExternalStorageDirectory().getPath() + DirName
                + "run" + ".csv");
        fw2.delete();

        File fw3 = new File(Environment.getExternalStorageDirectory().getPath() + DirName
                + "stay" + ".csv");
        fw3.delete();
        File fw4 = new File(Environment.getExternalStorageDirectory().getPath() + DirName
                + "weka1" + ".arff");
        fw4.delete();


    }

    public void make_arff_file() {
        int i;
        try {
            FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + DirName
                    + "weka1" + ".arff", false);

            fw.write("@relation\tmovestate\n\n" +
                    "@attribute\tacceleration\treal\n" +
                    "@attribute\tstate\t{walk,run,stay}\n\n" +
                    "@data\n");

            for(i=0;i<3;i++) {
                try {
                    FileReader fr = new FileReader(Environment.getExternalStorageDirectory().getPath() + DirName
                            + alltype[i] + ".csv");
                    BufferedReader br = new BufferedReader(fr);

                    String line;
                    while ((line = br.readLine()) != null) {
                        fw.write(line + "\n");
                    }
                    br.close();
                    System.out.println("arffファイル出力成功");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("arffファイル出力失敗");
                }
            }
            fw.close();
            //makeClassifier();
            System.out.println("作業完了");
            makeClassifier();
            flag = 1;
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("作業失敗");
        }


    }

    public  void make_attribute(){
        System.out.println("WEKAやります");
        try{
            Evaluation eval = new Evaluation(instances);
            eval.evaluateModel(classifier, instances);
            System.out.println(eval.toSummaryString());


            FastVector out = new FastVector(2);
            out.addElement("walk");
            out.addElement("run");
            out.addElement("stay");
            Attribute acceleration  = new Attribute("acceleration", 0);
            Attribute state = new Attribute("state", out, 1);
            FastVector win = new FastVector(2);


            Instance instance = new DenseInstance(3);
            instance.setValue(acceleration, gxyz);

            instance.setDataset(instances);
            double result = classifier.classifyInstance(instance);
            System.out.println(result);

            statusview = findViewById(R.id.status);
            if(result == 0.0){
                statusview.setText("歩きスマホです | "+result);
            }
            if(result == 1.0){
                statusview.setText("走りスマホです | "+result);
            }
            if(result == 2.0){
                statusview.setText("止まっています | "+result);
            }
            System.out.println("警告終了");
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("weka属性分類失敗");
        }
    }

    public void makeClassifier(){
        try{
            source = new ConverterUtils.DataSource(Environment.getExternalStorageDirectory().getPath()
                    + DirName + "weka1" + ".arff");
            instances = source.getDataSet();
            instances.setClassIndex(1);
            classifier = new J48();
            classifier.buildClassifier(instances);
            System.out.println("分類機作成完了");
        }catch(Exception e){
            System.out.println("分類機作成失敗");
        }
    }
    public void Dialog() {
        flag_get_acc = 1;
        System.out.println("ダイアログ呼び出した");
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("10秒以上継続してください");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                flag_get_acc = 0;

            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                flag_get_acc = 0;
            }
        });
        builder.show();
        // Create the AlertDialog object and return it
    }

}
