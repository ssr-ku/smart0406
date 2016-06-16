package kr.ac.korea.lee.joljak2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Created by lee on 16. 6. 10.
 */
public class ResultActivity extends Activity {
    AudioVisualizerView avv;
    private android.os.Handler handler;
    public static final int REPEAT_INTERVAL = 40;
    private boolean isPlaying;
    MediaPlayer mp;
    int myIndex = 0;
    ArrayList<Float> ar = MainActivity.ar;
    TextView eval;
    int rmsCont = 0;
    float ampl = 0;
    float rms = 0;
    int audiolength = 0;
    int currentplaytime=0;
    TextView playtime;
    TextView duration;
    ProgressBar progress;
    String format = "%1$02d";

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
        enableButton(R.id.btnStop, false);
        eval = (TextView) findViewById(R.id.eval);
        Intent intent = getIntent();
        String htmlBody = intent.getStringExtra("result");

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
            if (modkey.size() == 0) {
                break;
            }
            int start, end;
            start = htmlBody.indexOf(Keywords.keyword.get(i));
            end = start + (Keywords.keyword.get(i) + "(대체어 : " + Keywords.toword.get(i) + ")").length();
            arkey.add(new MyIntegers(start, end));

        }
        for (Integer i : modbad) {
            if (modbad.size() == 0) {
                break;
            }
            int start, end;
            start = htmlBody.indexOf(Keywords.badword.get(i));
            end = start + (Keywords.badword.get(i) + "(금기어)").length();
            arbad.add(new MyIntegers(start, end));

        }
        Spannable span = new SpannableString(htmlBody);
        for (MyIntegers i : arkey) {
            if (arkey.size() == 0) {
                break;
            }
            span.setSpan(new ForegroundColorSpan(Color.rgb(255, 0, 255)), i.start, i.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        for (MyIntegers i : arbad) {
            if (arbad.size() == 0) {
                break;
            }
            span.setSpan(new ForegroundColorSpan(Color.RED), i.start, i.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StrikethroughSpan(), i.start, i.end - "(금기어)".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        TextView tv = (TextView) findViewById(R.id.idid1);
        tv.setText(span);
        tv.setPadding(0, 5, 0, 5);
        //Log.i("ids",Integer.toString(aint.get(index)));

        playtime= (TextView)findViewById(R.id.playtime);
        duration= (TextView)findViewById(R.id.duration);
        prepareMediaPlayer();
        audiolength = mp.getDuration() / 1000;
        duration.setText(""+audiolength/60+":"+String.format(format,audiolength%60));
        progress=(ProgressBar)findViewById(R.id.playProgress);
        progress.setMax(mp.getDuration());
        avv = (AudioVisualizerView) findViewById(R.id.audio_visualizer);

        handler = new android.os.Handler();
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStartPause: {
                    if (!isPlaying) {
                        enableButton(R.id.btnStop, true);
                        ((Button) v).setText(R.string.pause);
                        startPlaying();
                        break;
                    } else if (isPlaying) {
                        ((Button) v).setText(R.string.play);;
                        enableButton(R.id.btnStop, false);
                        pausePlaying();
                        break;
                    }
                }
                case R.id.btnStop: {
                    Button btnStartPause = (Button) findViewById(R.id.btnStartPause);
                    btnStartPause.setText(R.string.play);
                    stopPlaying();
                    break;
                }
            }
        }
    };

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStartPause)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void startPlaying() {
        if (mp == null) {
            avv.clear();
            prepareMediaPlayer();
        }

        mp.start();
        isPlaying = true;

        handler.post(updateVisualizer);
    }

    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) // if we are playing
            {
                currentplaytime=mp.getCurrentPosition()/1000;
                playtime.setText(""+currentplaytime/60+":"+String.format(format,currentplaytime%60));
                progress.setProgress(mp.getCurrentPosition());
                if (myIndex == ar.size()) {
                    Button btnStartPause = (Button) findViewById(R.id.btnStartPause);
                    btnStartPause.setText(R.string.play);
                    stopPlaying();
                } else {
                    avv.addAmplitude(ar.get(myIndex));
                    ampl = ar.get(myIndex);
                    if (rmsCont < 25) {
                        rms += ampl * ampl;
                        rmsCont++;
                    } else {
                        rms += ampl * ampl;
                        rms = (float) Math.sqrt((double) rms);
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
        if (mp != null) {
            isPlaying = false;

            playtime.setText("0:00");

            mp.stop();
            mp.release();
            mp = null;
        }
    }

    private void pausePlaying() {
        if (isPlaying) {
            isPlaying = false;
            mp.pause();
        }
    }

    private void prepareMediaPlayer() {
        myIndex = 0;
        rms = 0;
        rmsCont = 0;

        try {
            mp = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav"));
            //FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav"));
            //FileDescriptor fd = fis.getFD();
            mp.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (isPlaying) {
            if (mp != null) {
                isPlaying = false;
                mp.stop();
                mp.release();
                mp = null;
            }
        }
        super.onBackPressed();
    }
}
