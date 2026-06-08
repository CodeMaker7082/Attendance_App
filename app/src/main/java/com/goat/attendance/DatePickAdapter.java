package com.goat.attendance;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.util.*;

public class DatePickAdapter extends RecyclerView.Adapter<DatePickAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> data;

    public static int   ITEM_WIDTH = 150;
    public static float TEXT_SIZE  = 30f;

    // Owned internally — no static field on ClasslistActivity needed.
    private int selectedIndex = 0;

    public DatePickAdapter(ArrayList<HashMap<String, Object>> data) {
        this.data = data;
    }

    /** Call this whenever the selected day changes, then notifyDataSetChanged(). */
    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.daysitem_top, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView tv = holder.itemView.findViewById(R.id.textview1);

        tv.setText(data.get(position).get("date").toString());

        // Apply item width / height from the measured values set by ClasslistActivity
        RecyclerView.LayoutParams lp =
                (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        lp.width  = ITEM_WIDTH;
        lp.height = RecyclerView.LayoutParams.MATCH_PARENT;
        holder.itemView.setLayoutParams(lp);

        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, TEXT_SIZE);

        if (position == selectedIndex) {
            tv.setBackgroundResource(R.drawable.selected_bg);
            tv.setTextColor(Color.WHITE);
            tv.setScaleX(0.87f);
            tv.setScaleY(0.87f);
        } else {
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setTextColor(Color.BLACK);
            tv.setScaleX(0.5f);
            tv.setScaleY(0.5f);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) { super(v); }
    }
}
