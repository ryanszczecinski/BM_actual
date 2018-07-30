package com.beerme.beerme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.w3c.dom.Text;

public class SettingsActivity extends AppCompatActivity {
    Button saveSettingsBtn;
    EditText weightSetting;
    CheckBox maleCheckBox, femaleCheckBox;
    public final static String WEIGHT = "Weight";
    public final static String GENDER = "Gender";
    public final static String PREFERENCES = "BeerMePreferences";
    public final static String LAST_TIME_DRINKING = "Previous";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        saveSettingsBtn = findViewById(R.id.saveSettings);
        weightSetting = findViewById(R.id.setWeightField);
        maleCheckBox = findViewById(R.id.male_check_box);
        femaleCheckBox = findViewById(R.id.female_check_box);

        SharedPreferences sharedPreferences = this.getSharedPreferences(PREFERENCES,MODE_PRIVATE);
        if(sharedPreferences.contains(WEIGHT)){
            weightSetting.setText(""+sharedPreferences.getInt(WEIGHT,0));
            if(sharedPreferences.getBoolean(GENDER,true)) maleCheckBox.setChecked(true);
            else femaleCheckBox.setChecked(true);
        }
    }
    //these on click methods are used to only have one check box checked at once
    public void maleCheckBoxChecked(View v){
        maleCheckBox.setChecked(true);
        if(femaleCheckBox.isChecked()) femaleCheckBox.setChecked(false);
    }
    public void femaleCheckBoxChecked(View v){
        femaleCheckBox.setChecked(true);
        if(maleCheckBox.isChecked()) maleCheckBox.setChecked(false);
    }
    //save the settings filled in the boxes into shared preferences
    //forces the person to fill out a weight and gender, will toast the needed setting
    public void saveSettings(View v){
       //make sure all the data is filled out
        if(!femaleCheckBox.isChecked()&&!maleCheckBox.isChecked()) {
            Toast noGender = Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT);
            noGender.show();
            return;
        }
        if(weightSetting.getText().toString().equals("")){
            Toast noWeight = Toast.makeText(this, "Please enter a weight", Toast.LENGTH_SHORT);
            noWeight.show();
            return;
        }
        //save the system settings
        SharedPreferences sharedPreferences = this.getSharedPreferences(PREFERENCES,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GENDER,maleCheckBox.isChecked());
        editor.putInt(WEIGHT, Integer.valueOf(weightSetting.getText().toString()));
        editor.commit();
        Intent i = new Intent(this, MainActivity.class);
        this.startActivity(i);

    }

}
