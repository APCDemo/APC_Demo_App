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

package ch.bfh.securevote;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.net.URLEncoder;
import ch.bfh.securevote.databinding.FragmentBrowserBinding;
import ch.bfh.securevote.utils.Constants;
import ch.bfh.securevote.utils.SharedData;

public class BrowserFragment extends Fragment {

    private FragmentBrowserBinding binding;
    private static final String TAG = BrowserFragment.class.getName();
    private SharedData sharedData;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentBrowserBinding.inflate(inflater, container, false);
        sharedData = new ViewModelProvider(requireActivity()).get(SharedData.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String pkcs7msg = sharedData.getP7mPem();
        try {
            Log.d(TAG, String.format("Sending P7M message to %s", Constants.P7M_URL));
            String post = String.format("%s=%s", "p7m", URLEncoder.encode(pkcs7msg, "UTF-8"));
            binding.webContent.postUrl(Constants.P7M_URL, post.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}