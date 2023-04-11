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

package ch.bfh.securevote.gui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import ch.bfh.securevote.R;

/**
 * TODO: document your custom view class.
 */
public class SingleKeyLayout extends TableLayout {
    public TextView labelTextView, keyStoreView, keyTypeView, keyAttrView, keyAttrValView, keyAuthenticationView;
    public Button removeButton;

    public SingleKeyLayout(Context context) {
        this(context, null);
    }


    public SingleKeyLayout(Context context, AttributeSet attrs) {
        super(context,attrs);

        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(R.layout.single_key_layout, this, true);
        labelTextView = findViewById(R.id.key_name);
        keyStoreView = findViewById(R.id.key_store);
        keyTypeView = findViewById(R.id.key_type);
        keyAttrView = findViewById(R.id.key_attr);
        keyAttrValView = findViewById(R.id.key_attr_val);
        keyAuthenticationView = findViewById(R.id.key_auth);
        removeButton = findViewById(R.id.delete_button);
    }
}