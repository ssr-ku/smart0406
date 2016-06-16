package kr.ac.korea.lee.joljak2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.support.annotation.MainThread;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.text.Html;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;


import static kr.ac.korea.lee.joljak2.R.layout.activity_main;
import kr.ac.korea.lee.joljak2.Keywords;

public class MainActivity extends Activity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int bufferSize = 0;
    private String pcmFilePath;
    private String myFileName = "first set";

    public TextView t;
    public String lang = "ko-KR";

    AudioVisualizerView avv;
    private android.os.Handler handler;
    public static final int REPEAT_INTERVAL = 40;
    double amplitude = 0;
    private boolean isPlaying;
    public static ArrayList<Float> ar = new ArrayList<>();
    int myIndex=0;
    String speed = "";
    float audiolength = 0;
    MediaPlayer mp;
    int ratioCnt = 0;
    float ratio = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_main);
        t = (TextView) findViewById(R.id.textvieww);
        Spannable sp = new SpannableString("hello");
        sp.setSpan(new ForegroundColorSpan(Color.RED), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StrikethroughSpan(), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        String html = "<strike><font color='blue'>hello</font></strike>";
        t.setText("");
        setButtonHandlers();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        avv=(AudioVisualizerView)findViewById(R.id.audio_visualizer);

        handler = new android.os.Handler();

    }



    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    avv.clear();
                    startRecording();
                    break;
                }
                case R.id.btnStop: {
                    enableButtons(false);
                    enableButtons2(false);

                    stopRecording();
                    isPlaying = false;
                    break;
                }
                case R.id.btnPlay: {
                    enableButtons2(true);
                    myIndex = 0;
                    isPlaying = true;
                    try{
                        mp = new MediaPlayer();
                        FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav"));
                        FileDescriptor fd = fis.getFD();
                        mp.setDataSource(fd);
                        mp.prepare();
                        mp.start();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    avv.clear();
                    handler.post(updateVisualizer);
                    break;
                }
            }
        }
    };

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        findViewById(R.id.btnPlay).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }
    private void enableButton2(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    private void enableButtons2(boolean isPlaying) {
        enableButton(R.id.btnPlay, !isPlaying);
        enableButton(R.id.btnStop, isPlaying);
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize * BytesPerElement);

        handler.post(updateVisualizer);
        ar.clear();
        ratioCnt = 0;
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                verifyStoragePermissions(MainActivity.this);
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }


    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isRecording) // if we are already recording
            {
                // get the current amplitude
                ar.add((float)amplitude);
                avv.addAmplitude((float) amplitude); // update the VisualizeView
                avv.invalidate(); // refresh the VisualizerView
                // update in 40 milliseconds
                handler.postDelayed(this, REPEAT_INTERVAL);
            } else if(isPlaying && (myIndex < ar.size())) {
                avv.addAmplitude(ar.get(myIndex));
                avv.invalidate();
                myIndex++;
                handler.postDelayed(this, REPEAT_INTERVAL);
                Log.i("isPlaying",Integer.toString(myIndex));
                if(myIndex == ar.size()) {
                    //isPlaying = false;
                    enableButtons2(false);
                }
            }
        }
    };

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        pcmFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/voice_44100_16bit_mono.pcm";
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"/voice_44100_16bit_mono.pcm"));
            //os = new FileOutputStream(pcmFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            double sum=0;
            int readSize=recorder.read(sData, 0, BufferElements2Rec);
            for (int i = 0; i < readSize; i++) {
                sum += sData[i] * sData[i];
            }

            if (readSize > 0) {
                amplitude = Math.sqrt(sum / readSize);
                if(amplitude < 500) {
                    ratioCnt++;
                }
                //ar.add(amplitude);
            }
            System.out.println("Short writing to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
        if (null != mp) {
            isPlaying = false;
            mp.stop();
            mp.release();
            mp = null;
        }
        ratio = ((float)ar.size() - (float)ratioCnt)/((float)ar.size());
        Log.i("ratioCnt",Integer.toString(ratioCnt));
        if(!isPlaying) {
            //for (float f : avv.amplitudes) {
            //    ar.add((double) f);
            //}
        }
        //data.stopThread();
        //EditText et = (EditText)findViewById(R.id.fileName);
        //myFileName = et.getText().toString();
        if(myFileName.equals(null)) {
            myFileName = "Null";
        }
        copyWaveFile(pcmFilePath, Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav");
        deleteTempFile();
    }


    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath);

        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + myFileName + ".wav");
    }

    private void deleteTempFile() {
        File file = new File(pcmFilePath);
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            //AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException
    {
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    private class Task extends AsyncTask<File, Void, ArrayList<String>> {
        private Spannable result;
        private File sendFile;
        private OkHttpClient client;


        public Task() {
            try {
                client = new OkHttpClient.Builder()
                        .connectTimeout(100, TimeUnit.SECONDS)
                        .writeTimeout(100, TimeUnit.SECONDS)
                        .readTimeout(100, TimeUnit.SECONDS)
                        .authenticator(new Authenticator() {
                            @Override
                            public Request authenticate(Route route, Response response) throws IOException {
                                return null;
                            }
                        })
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected ArrayList<String> doInBackground(File... file) {
            sendFile = file[0];
            ArrayList<String> resultSpan = gogo(sendFile);
            Log.i("asdf", "doInBackground success");
            return resultSpan;
        }

        @Override
        protected void onPostExecute(ArrayList<String> s) {
            //super.onPostExecute(s);
            if (isCancelled()) {
                Log.i("asf", "onPost Canceled");
            } else {
                Intent intent = new Intent(MainActivity.this, SelectResultActivity.class);
                intent.putExtra("result", s);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
                //t.setText(s);
            }
            Log.i("zzz", "onPostExecute success?");
            return;
        }



        private ArrayList<String> gogo(File file) {
            try {
                MediaPlayer mp = MediaPlayer.create(MainActivity.this, Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav"));
                audiolength = mp.getDuration()/1000;
                RequestBody myBody = RequestBody.create(MediaType.parse("audio/L16; rate=16000"), file);

                Request request = new Request.Builder()
                        .addHeader("Content-Type", "audio/l16; rate=16000;")
                        .url("http://www.google.com/speech-api/v2/recognize?output=json&lang=" + lang + "&key=AIzaSyCMNS-pA84FIIfk8mzcszyqOF5Mfl8Sj88")
                        .post(myBody)

                        .addHeader("cache-control", "no-cache")
                        .addHeader("postman-token", "20e386e6-4143-4768-a1df-d1c2932fefae")
                        .build();

                Log.i("header", request.header("content-type"));
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    Response response = client.newCall(request).execute();
                    InputStream is = response.body().byteStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String rst = "";

                    String line = reader.readLine();
                    rst = line;
                    while ((line = reader.readLine()) != null) {
                        rst += line;
                    }
                    response.body().close();
                    Log.i("response","response body closed");
                    int begin=0;
                    ArrayList<String> arstr = new ArrayList<>();
                    while(rst.indexOf("transcript",begin) > -1) {
                        int a = rst.indexOf("transcript", begin);
                        int b = rst.indexOf("\"", a+13);
                        begin = b;
                        String htmlBody = rst.substring(a + 12, b+1);
                        Log.i("htmlBody", htmlBody);
                        float chrlength = htmlBody.replaceAll("\\s","").length();
                        audiolength *= ratio;
                        Log.i("chrlength", Float.toString(chrlength));
                        Log.i("audiolength", Float.toString(audiolength));
                        Log.i("ratio",Float.toString(ratio));
                        htmlBody += "\n평균속도 : 초당" + Float.toString(chrlength / audiolength) + "글자";
                        /*ArrayList<MyIntegers> arkey = new ArrayList<>();
                        ArrayList<MyIntegers> arbad = new ArrayList<>();
                        ArrayList<Integer> modkey = new ArrayList<>();
                        ArrayList<Integer> modbad = new ArrayList<>();
                        for (String s : Keywords.keyword) {
                            String toword = "";
                            int start, end;
                            if ((start = htmlBody.indexOf(s)) > -1) {
                                Log.i("keyword", "keyword found");
                                end = start + s.length();
                                toword = Keywords.toword.get(Keywords.keyword.indexOf(s));
                                String before = htmlBody.substring(0, start);
                                String after = htmlBody.substring(end);
                                htmlBody = before + s + "(대체어 : " + toword + ")" +
                                        after;
                                modkey.add(Keywords.keyword.indexOf(s));
                                //span = new SpannableString(htmlBody);
                                //span.setSpan(new ForegroundColorSpan(Color.RED), start, ("(대체어 : " + toword).length() + end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        for (String s : Keywords.badword) {
                            String toword = "";
                            int start, end;
                            if ((start = htmlBody.indexOf(s)) > -1) {
                                end = start + s.length();
                                String before = htmlBody.substring(0, start);
                                String after = htmlBody.substring(end);
                                htmlBody = before + s + "(금기어)" + after;
                                modbad.add(Keywords.badword.indexOf(s));
                                //span = new SpannableString(htmlBody);
                                //span.setSpan(new ForegroundColorSpan(Color.RED), start, end + ("(금기어)").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                //span.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        for (Integer i : modkey) {
                            int start, end;
                            start = htmlBody.indexOf(Keywords.keyword.get(i));
                            end = start + (Keywords.keyword.get(i) + "(대체어 : " + Keywords.toword.get(i) + ")").length();
                            arkey.add(new MyIntegers(start, end));

                        }
                        for (Integer i : modbad) {
                            int start, end;
                            start = htmlBody.indexOf(Keywords.badword.get(i));
                            end = start + (Keywords.badword.get(i) + "(금기어)").length();
                            arbad.add(new MyIntegers(start, end));

                        }
                        Spannable span = new SpannableString(htmlBody);
                        for (MyIntegers i : arkey) {
                            span.setSpan(new ForegroundColorSpan(Color.rgb(255, 0, 255)), i.start, i.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        for (MyIntegers i : arbad) {
                            span.setSpan(new ForegroundColorSpan(Color.RED), i.start, i.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            span.setSpan(new StrikethroughSpan(), i.start, i.end - "(금기어)".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        //return htmlBody;
                        ParcelableSpan pc = (ParcelableSpan)span;
                        */
                        arstr.add(htmlBody);
                    }
                    Log.i("Number of htmlBody",Integer.toString(arstr.size()));
                    if(arstr.size() == 0) {
                        arstr.add("Empty Result");
                        return arstr;
                    } else {
                        return arstr;
                    }
                } else {
                    t.setText("No network connection available.");
                    Log.i("aaa", "nework connection bad");
                }
            } catch (Exception e) {
                Log.i("asdf", "gogo newCall fail", e);
                return new ArrayList<String>(Arrays.asList("gogo newCall Error"));
            }
            return new ArrayList<String>(Arrays.asList("gogo..."));
        }
    }




    public void haha(View view){
        RadioGroup rg = (RadioGroup)findViewById(R.id.radio);
        //EditText et = (EditText)findViewById(R.id.fileName);
        //myFileName = et.getText().toString();
        if(myFileName.equals(null)) {
            myFileName = "Null";
        }
        int id = rg.getCheckedRadioButtonId();
        if(id == (R.id.radioButton)) {
            lang = "en-US";
        } else {
            lang = "ko-KR";
        }
        verifyStoragePermissions(MainActivity.this);
        File file=null;
        try{
            //file=new File(pcmFilePath);
            //file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"/record.wav");
            file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav");
            Log.i("Test","good file");
        }
        catch (Exception e){
            Log.i("tst", "bad file");
        }

        if(file != null) {
            new Task().execute(file);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {

        if(isPlaying) {
            if(mp != null) {
                isPlaying = false;
                mp.stop();
                mp.release();
                mp = null;
            }
        }
        super.onBackPressed();
    }
}