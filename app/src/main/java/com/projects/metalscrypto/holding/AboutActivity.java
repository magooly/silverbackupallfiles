package com.projects.metalscrypto.holding;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.projects.metalscrypto.holding.DBManager.AboutModel;
import com.projects.metalscrypto.holding.DBManager.About_DB;

import java.util.List;

public class AboutActivity extends AppCompatActivity {

    EditText et_about;
    Button btn_update;
    About_DB about_db;
    private static final String TAG ="TAG" ;
    Context mContext;
    private String dbAboutText="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mContext=AboutActivity.this;

        about_db=new About_DB(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        et_about=findViewById(R.id.et_about);
        btn_update=findViewById(R.id.btn_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String about=et_about.getText().toString();

                if (TextUtils.isEmpty(about))
                {
                    et_about.setError("Filed must not be empty");
                    et_about.requestFocus();
                    return;
                }

                saveDatainDB(about);
            }
        });


        getNoteData();


    }

    private void getNoteData() {

        List<AboutModel> notedata = about_db.getNoteData();
        if (notedata.size() > 0)
        {

            for (int i = 0; i <notedata.size() ; i++)
            {

                dbAboutText=notedata.get(i).getAbout_text();

            }
            et_about.setText(dbAboutText);


        }

    }

    private void saveDatainDB(String about) {

        if(about_db.getNoteData().size()==0)
        {
            boolean insert=about_db.insertData("",about);


            if (insert)
            {
                Log.d(TAG, "updateSilverData: insert sucess ");
                Toast.makeText(mContext, "Note Added", Toast.LENGTH_SHORT).show();
            }else {
                Log.d(TAG, "updateSilverData: insert failed ");
            }
        }else
        {
            boolean update= about_db.updateData("1",about);
            if (update)
            {
                Log.d(TAG, "updateSilverData: update sucess ");
                Toast.makeText(mContext, "Note updated", Toast.LENGTH_SHORT).show();
            }else {
                Log.d(TAG, "updateSilverData: update failed ");
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;


        }
        return super.onOptionsItemSelected(item);
    }
}