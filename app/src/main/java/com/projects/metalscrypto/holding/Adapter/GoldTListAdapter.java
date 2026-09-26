package com.projects.metalscrypto.holding.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.projects.metalscrypto.holding.DBManager.SilverT_DBModel;
import com.projects.metalscrypto.holding.R;
import com.projects.metalscrypto.holding.ResListActivity;

import java.util.List;

public class GoldTListAdapter extends RecyclerView.Adapter<GoldTListAdapter.MyHolder> {

    Context mContext;
    ResListActivity resListActivity;
    List<SilverT_DBModel> alGoldList;
    View view;
    String id = "", no = "", weight = "", what = "", total = "";

    public GoldTListAdapter(Context mContext, List<SilverT_DBModel> alGoldList, ResListActivity resListActivity) {
        this.mContext = mContext;
        this.alGoldList = alGoldList;
        this.resListActivity = resListActivity;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.row_goldtlist_adapter, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position)
    {

        id = alGoldList.get(position).getX_ID();
        no = alGoldList.get(position).getX_S_NO();
        weight = alGoldList.get(position).getX_S_WEIGHT();
        what = alGoldList.get(position).getX_S_WHAT();
        total = alGoldList.get(position).getX_S_TOTAL();

        holder.tv_no.setText(no);
        holder.tv_weight.setText(weight);
        holder.tv_what.setText(what);
        holder.tv_total.setText(total);

        holder.ll_main.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                id = alGoldList.get(position).getX_ID();
                no = alGoldList.get(position).getX_S_NO();
                weight = alGoldList.get(position).getX_S_WEIGHT();
                what = alGoldList.get(position).getX_S_WHAT();
                total = alGoldList.get(position).getX_S_TOTAL();
                resListActivity.displayPopUp("G",id,no,weight,what,total);
            }
        });

        holder.tv_total.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                id = alGoldList.get(position).getX_ID();
                no = alGoldList.get(position).getX_S_NO();
                weight = alGoldList.get(position).getX_S_WEIGHT();
                what = alGoldList.get(position).getX_S_WHAT();
                total = alGoldList.get(position).getX_S_TOTAL();
                resListActivity.displayPopUp("G",id,no,weight,what,total);
            }
        });

        holder.tv_no.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                id = alGoldList.get(position).getX_ID();
                no = alGoldList.get(position).getX_S_NO();
                weight = alGoldList.get(position).getX_S_WEIGHT();
                what = alGoldList.get(position).getX_S_WHAT();
                total = alGoldList.get(position).getX_S_TOTAL();
                resListActivity.displayPopUp("G",id,no,weight,what,total);
            }
        });

        holder.tv_weight.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                id = alGoldList.get(position).getX_ID();
                no = alGoldList.get(position).getX_S_NO();
                weight = alGoldList.get(position).getX_S_WEIGHT();
                what = alGoldList.get(position).getX_S_WHAT();
                total = alGoldList.get(position).getX_S_TOTAL();
                resListActivity.displayPopUp("G",id,no,weight,what,total);
            }
        });

        holder.tv_what.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                id = alGoldList.get(position).getX_ID();
                no = alGoldList.get(position).getX_S_NO();
                weight = alGoldList.get(position).getX_S_WEIGHT();
                what = alGoldList.get(position).getX_S_WHAT();
                total = alGoldList.get(position).getX_S_TOTAL();
                resListActivity.displayPopUp("G",id,no,weight,what,total);
            }
        });

    }

    @Override
    public int getItemCount() {
        return alGoldList.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        TextView tv_no, tv_weight, tv_what, tv_total;
        CardView cv_main;
        LinearLayout ll_main;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            tv_no = itemView.findViewById(R.id.tv_no);
            tv_weight = itemView.findViewById(R.id.tv_weight);
            tv_what = itemView.findViewById(R.id.tv_what);
            tv_total = itemView.findViewById(R.id.tv_total);
            ll_main = itemView.findViewById(R.id.ll_main);

        }
    }
}
