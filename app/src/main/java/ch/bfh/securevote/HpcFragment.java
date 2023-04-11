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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import ch.bfh.securevote.databinding.FragmentHpcBinding;
import ch.bfh.securevote.utils.Constants;
import ch.bfh.securevote.utils.HpcUtility;
import ch.bfh.securevote.gui.ZoomOutPageTransformer;
import ch.bfh.securevote.utils.NetworkJsonReceiver;
import ch.bfh.securevote.utils.SharedData;


public class HpcFragment extends Fragment {

    private FragmentHpcBinding binding;
    private SharedData sharedData;
    private static final String TAG = HpcFragment.class.getName();
    private Animation zoom;


    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        sharedData = new ViewModelProvider(requireActivity()).get(SharedData.class);
        binding = FragmentHpcBinding.inflate(inflater, container, false);
        viewPager = binding.pager;
        zoom = AnimationUtils.loadAnimation(getContext(), R.anim.zoom_in_out);

        //get questions online
        if (sharedData.getQuestions().size() <= 0) {
            Log.d(TAG, String.format("Retrieving questions online from %s",Constants.QUESTIONS_URL));
            NetworkJsonReceiver net = new NetworkJsonReceiver(Constants.QUESTIONS_URL);
            net.setResultListener(result -> requireActivity().runOnUiThread(() -> setQuestions(result)));
            Executors.newSingleThreadExecutor().execute(net);
        }else {
            Log.d(TAG, "Offline case: Retrieving questions from resource files.");
            setQuestions();
        }
        return binding.getRoot();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (HpcUtility.hasKey()){
            setHasKey();
        }else{
            setHasNoKey();
        }
        binding.buttonCheckKey.setVisibility(View.VISIBLE);
        binding.buttonCheckKey.startAnimation(zoom);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(Fragment fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position < getItemCount()-1) {
                Bundle b = new Bundle();
                b.putInt("position", position);
                ScreenSlideFragment page = new ScreenSlideFragment();
                page.setArguments(b);
                return page;
            } else return new JoinAllianceFragment();
        }

        @Override
        public int getItemCount() {
            return sharedData.getQuestions().size() + 1; //Adding JoinAllianceFragment to the questions
        }

    }

    private void setQuestions(String result){
        sharedData.setQuestions(result, getContext());
        setQuestions();
    }

    private void setQuestions(){
        /*
         * The pager adapter, which provides the pages to the view pager widget.
         */
        FragmentStateAdapter pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(new ZoomOutPageTransformer());
        new TabLayoutMediator(binding.tabs, viewPager, (tab, position) -> {
            if (position < sharedData.getQuestions().size()) { // The last tab asks to join the alliance
                tab.setText(HpcFragment.this.getResources().getString(R.string.question) + position);
            } else {
                tab.setText(HpcFragment.this.getResources().getString(R.string.join_alliance));
            }
        }).attach();
    }

    private void setHasKey(){
        //NavController nav = NavHostFragment.findNavController(HpcFragment.this);
        //Log.d(TAG, nav.getGraph().toString());

        binding.buttonCheckKey.setImageResource(R.drawable.seal_button_green);
        binding.buttonCheckKey.setOnClickListener(view1 -> NavHostFragment.findNavController(HpcFragment.this)
                .navigate(R.id.action_CheckKeyFragment));
    }

    private void setHasNoKey(){
        //NavController nav = NavHostFragment.findNavController(HpcFragment.this);
        //Log.d(TAG, nav.getGraph().toString());

        binding.buttonCheckKey.setImageResource(R.drawable.seal_button_gray);
        binding.buttonCheckKey.setOnClickListener(view1 -> NavHostFragment.findNavController(HpcFragment.this)
                .navigate(R.id.action_RegisterFragment));

    }

    /**
     * Automatically navigate to next tab
     */
    public void navigateToNext(){
        int pos = viewPager.getCurrentItem();
        if (pos == sharedData.getQuestions().size()) {
            pos=0;
        } else {
            pos=pos+1;
        }
        viewPager.setCurrentItem(pos, true);
    }
}