package com.example.hackinhome2021_stankinfood.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.hackinhome2021_stankinfood.R;
import com.example.hackinhome2021_stankinfood.activities.MainActivity;

public class AuthRegChooseFragment extends Fragment implements View.OnClickListener {
    private Button buttonAuthorization;
    private Button buttonRegistration;

    public AuthRegChooseFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth_reg_choose, container, false);

        initButtons(view);

        return view;
    }

    private void initButtons(View view) {
        buttonAuthorization = view.findViewById(R.id.buttonAuthorization);
        buttonRegistration = view.findViewById(R.id.buttonRegistration);

        buttonRegistration.setOnClickListener(this);
        buttonAuthorization.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        ((MainActivity) getActivity()).addFragmentAuthRegFragment(v.getId() != R.id.buttonAuthorization);
    }
}