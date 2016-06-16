package kr.ac.korea.lee.joljak2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by lee on 16. 6. 10.
 */
public class SelectResultActivity extends Activity{
    ArrayList<Integer> btns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_result);
        btns = new ArrayList<>(Arrays.asList(R.id.btn1, R.id.btn2,
                R.id.btn3, R.id.btn4, R.id.btn5));
        Intent intent = getIntent();
        ArrayList<String> rst = intent.getStringArrayListExtra("result");
        for(int i = 0; i < rst.size(); i++) {
            Button bt = (Button)findViewById(btns.get(i));
            bt.setText(rst.get(i));
            bt.setOnClickListener(gogogo);

        }

    }

    public View.OnClickListener gogogo = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            String rst = ((Button) findViewById(id)).getText().toString();
            Intent intent = new Intent(SelectResultActivity.this, ResultActivity.class);
            intent.putExtra("result", rst);
            startActivity(intent);
            finish();
        }
    };
}
