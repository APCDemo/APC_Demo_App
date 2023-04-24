/*
     This file is part of the Android app ch.bfh.securevote.
     (C) 2023 Benjamin Fehrensen (and other contributing authors)
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package ch.bfh.securevote.utils;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ch.bfh.securevote.R;

public class SharedData extends ViewModel {
    private final String TAG = SharedData.class.getName();

    private boolean connected = false;

    private String p7mPem = "";

    public void setP7mPem(String p7mPem) {
        this.p7mPem = p7mPem;
    }

    public String getP7mPem(){
        return p7mPem;
    }

    private List<Question> questions = new ArrayList<>();

    public void setConnected(boolean state){
        this.connected=state;
    }

    public boolean isConnected(){
        return this.connected;
    }

    public List<Question> getQuestions(){
        return this.questions;
    }

    /**
     * Load questions from JSON file with fallback to local resource
     * @param json
     * @param context
     */
    public void setQuestions(String json, Context context) {
        if (json != null) {
            try {
                questions = new Gson().fromJson(json, new TypeToken<List<Question>>() {
                }.getType());
                Log.i(TAG, String.format("%d questions load from questions.json", questions.size()));
            } catch (Exception ex) {
                Log.e(TAG, "Failed reading Json: " + ex.getMessage());
                setQuestionsFromLocal(context);
            }
        } else{
            setQuestionsFromLocal(context);
        }
    }

    /**
     * Load questions from local resource
     * @param context
     */
    public void setQuestionsFromLocal(Context context){
        if (questions.size()<=0) {  // load only if we don't have any data
            Log.d(TAG, "Loading questions from local JSON file.");
            try {
                InputStream is = context.getResources().openRawResource(R.raw.questions);
                Reader jr = new InputStreamReader(is, StandardCharsets.UTF_8);

                questions = new Gson().fromJson(jr, new TypeToken<List<Question>>() {
                }.getType());
                Log.i(TAG, String.format("%d questions load from questions.json", questions.size()));

            } catch (Exception ex) {
                Log.e(TAG, "Failed reading Json: " + ex.getMessage());
            }
        }
    }
}
