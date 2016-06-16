package kr.ac.korea.lee.joljak2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import kr.ac.korea.lee.joljak2.Keywords;


/**
 * Created by lee on 16. 6. 10.
 */
public class ResultActivity extends Activity {

    AudioVisualizerView avv;
    private android.os.Handler handler;
    public static final int REPEAT_INTERVAL = 40;
    double amplitude = 0;
    private boolean isPlaying;
    MediaPlayer mp;
    int myIndex = 0;
    ArrayList<Float> ar = MainActivity.ar;
    TextView eval;
    int rmsCont = 0;
    float ampl = 0;
    float rms = 0;


    public class MyIntegers {
        public Integer start;
        public Integer end;

        public MyIntegers(Integer s, Integer e) {
            this.start = s;
            this.end = e;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        setButtonHandlers();
        enableButtons(false);
        eval = (TextView)findViewById(R.id.eval);
        Intent intent = getIntent();
        String htmlBody = intent.getStringExtra("result");
        int index = 0;




        ArrayList<MyIntegers> arkey = new ArrayList<>();
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
            if(modkey.size() == 0) {
                break;
            }
            int start, end;
            start = htmlBody.indexOf(Keywords.keyword.get(i));
            end = start + (Keywords.keyword.get(i) + "(대체어 : " + Keywords.toword.get(i) + ")").length();
            arkey.add(new MyIntegers(start, end));

        }
        for (Integer i : modbad) {
            if(modbad.size() == 0) {
                break;
            }
            int start, end;
            start = htmlBody.indexOf(Keywords.badword.get(i));
            end = start + (Keywords.badword.get(i) + "(금기어)").length();
            arbad.add(new MyIntegers(start, end));

        }
        Spannable span = new SpannableString(htmlBody);
        for (MyIntegers i : arkey) {
            if(arkey.size() == 0) {
                break;
            }
            span.setSpan(new ForegroundColorSpan(Color.rgb(255, 0, 255)), i.start, i.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        for (MyIntegers i : arbad) {
            if(arbad.size() == 0) {
                break;
            }
            span.setSpan(new ForegroundColorSpan(Color.RED), i.start, i.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StrikethroughSpan(), i.start, i.end - "(금기어)".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        TextView tv = (TextView)findViewById(R.id.idid1);
        tv.setText(span);
        tv.setPadding(0, 5, 0, 5);
        //Log.i("ids",Integer.toString(aint.get(index)));



        avv=(AudioVisualizerView)findViewById(R.id.audio_visualizer);

        handler = new android.os.Handler();
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    avv.clear();
                    startPlaying();
                    break;
                }
                case R.id.btnStop: {
                    enableButtons(false);
                    stopPlaying();
                    isPlaying = false;
                    break;
                }
            }
        }
    };

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
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

    private void startPlaying() {
        myIndex = 0;
        rms = 0;
        rmsCont = 0;
        try {
            mp = new MediaPlayer();
            FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav"));
            FileDescriptor fd = fis.getFD();
            mp.setDataSource(fd);
            mp.prepare();
            mp.start();
            isPlaying = true;
        }catch (Exception e) {
            e.printStackTrace();
        }
        handler.post(updateVisualizer);
        //ar.clear();


    }

    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) // if we are playing
            {
                if(myIndex == ar.size()) {
                    stopPlaying();
                } else {
                    avv.addAmplitude(ar.get(myIndex));
                    ampl = ar.get(myIndex);
                    if(rmsCont < 25) {
                        rms += ampl*ampl;
                        rmsCont++;
                    } else {
                        rms += ampl*ampl;
                        rms = (float)Math.sqrt((double)rms);
                        if (rms < 500) {
                            eval.setText("목소리가 인식되지 않습니다.");
                        } else if (rms < 2500) {
                            eval.setText("적당한 크기입니다.");
                        } else if (rms < 7000) {
                            eval.setText("적당한 크기입니다.");
                        } else {
                            eval.setText("목소리가 너무 큽니다.");
                        }
                        rmsCont = 0;
                        rms = 0;
                    }
                    avv.invalidate();
                    Log.i("isPlaying", Integer.toString(myIndex));
                    Log.i("amplitude value", Float.toString(ar.get(myIndex)));
                    myIndex++;
                    handler.postDelayed(this, REPEAT_INTERVAL);

                }
            }
        }
    };

    private void stopPlaying() {
        // stops the recording activity
        if (null != mp) {
            isPlaying = false;
            mp.stop();
            mp.release();
            mp = null;
        }
        enableButtons(false);

    }

}
