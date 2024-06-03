/*
 * Copyright (C) 2022-2024 XAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.x.settings.fragments.theme;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.utils.MonetUtils;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccentColorPicker extends SettingsPreferenceFragment {
    private static final String TAG = "AccentColorPicker";

    private static final String KEY_MONET_COLOR_ACCENT = "monet_engine_color_accent";

    private RecyclerView mRecyclerView;
    private MonetUtils mMonetUtils;

    private List<String> mAccentColorNames;
    private List<String> mAccentColorValues;
    private List<String> mAccentColorValuesDark;
    private List<String> mAccentColorValuesDarkRich;

    private Context mContext;
    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_accent_color_title);

        mContext = getActivity().getApplicationContext();
        mResolver = getActivity().getContentResolver();

        mMonetUtils = new MonetUtils(getActivity());

        final Resources res = getResources();
        mAccentColorNames = Arrays.asList(res.getStringArray(R.array.theme_accent_color_names));
        mAccentColorValues = Arrays.asList(res.getStringArray(R.array.theme_accent_color_values));
        mAccentColorValuesDark =
                Arrays.asList(res.getStringArray(R.array.theme_accent_color_values_dark));
        mAccentColorValuesDarkRich =
                Arrays.asList(res.getStringArray(R.array.theme_accent_color_values_dark_rich));

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.picker_recycler_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        Adapter mAdapter = new Adapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.X_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.reset_button, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reset_button) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.theme_colors_reset_accent_color_title)
                    .setMessage(R.string.theme_colors_reset_accent_color_message)
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mMonetUtils.setAccentColor(MonetUtils.ACCENT_COLOR_DEFAULT);
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel, null);
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        Context context;
        int mSelectedColor;

        public Adapter(Context context) {
            this.context = context;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.picker_option_generic, parent, false);
            CustomViewHolder vh = new CustomViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            final int selectedColor = Color.parseColor(mAccentColorValues.get(position));
            final int currentColor = mMonetUtils.getAccentColor();

            holder.image.setBackgroundResource(R.drawable.accent_background);
            final int viewColor = Color.parseColor(getViewAccentColor(position));
            holder.image.setBackgroundTintList(ColorStateList.valueOf(viewColor));
            holder.itemView.setActivated(selectedColor == currentColor);
            holder.name.setText(mAccentColorNames.get(position));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String oldColor = String.format("#%06X", (0xFFFFFF & currentColor));
                    final String newColor = String.format("#%06X", (0xFFFFFF & selectedColor));

                    updateActivatedStatus(oldColor, false);
                    updateActivatedStatus(newColor, true);

                    mMonetUtils.setAccentColor(selectedColor);
                }
            });
        }

        private String getViewAccentColor(final int position) {
            String color;
            final boolean nightMode = (mContext.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_YES) != 0;
            final boolean richerColors = mMonetUtils.isRicherColorsEnabled();

            if (nightMode) {
                color = richerColors ? mAccentColorValuesDarkRich.get(position)
                                     : mAccentColorValuesDark.get(position);
            } else {
                color = mAccentColorValues.get(position);
            }
            return color;
        }

        @Override
        public int getItemCount() {
            return mAccentColorValues.size();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image;
            public CustomViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.option_label);
                image = (ImageView) itemView.findViewById(R.id.option_thumbnail);
            }
        }

        private void updateActivatedStatus(String color, boolean isActivated) {
            int index = mAccentColorValues.indexOf(color);
            if (index < 0) {
                return;
            }
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && holder.itemView != null) {
                holder.itemView.setActivated(isActivated);
            }
        }
    }
}
