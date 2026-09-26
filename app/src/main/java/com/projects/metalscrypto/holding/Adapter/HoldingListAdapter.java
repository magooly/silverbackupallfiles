package com.projects.metalscrypto.holding.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;
import com.projects.metalscrypto.holding.R;
import com.projects.metalscrypto.holding.ResListActivity;

import java.util.List;

public class HoldingListAdapter extends RecyclerView.Adapter<HoldingListAdapter.MyHolder> {
    private static final int VIEW_TYPE_DETAILED = 1;
    private static final int VIEW_TYPE_COMPACT = 2;

    private final Context context;
    private final List<HoldingItem> items;
    private final ResListActivity resListActivity;

    public HoldingListAdapter(Context context, List<HoldingItem> items, ResListActivity resListActivity) {
        this.context = context;
        this.items = items;
        this.resListActivity = resListActivity;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == VIEW_TYPE_DETAILED ? R.layout.row_goldtlist_adapter : R.layout.row_holding_compact;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        HoldingItem item = items.get(position);
        boolean detailedType = isDetailedType(item.getItemType());

        if (detailedType) {
            holder.tvNo.setText(item.getNumber());
            holder.tvWeight.setText(item.getWeight());
            holder.tvWhat.setText(item.getWhat());
            holder.tvTotal.setText(item.getTotal());
        } else {
            holder.tvName.setText(HoldingsDb.normalizeItemType(item.getItemType()));
            holder.tvValue.setText(item.getTotal());
        }

        View.OnClickListener clickListener = v -> resListActivity.displayPopUp(
                item.getItemType(),
                item.getId(),
                item.getNumber(),
                item.getWeight(),
                item.getWhat(),
                item.getTotal()
        );

        holder.llMain.setOnClickListener(clickListener);
        if (detailedType) {
            holder.tvNo.setOnClickListener(clickListener);
            holder.tvWeight.setOnClickListener(clickListener);
            holder.tvWhat.setOnClickListener(clickListener);
            holder.tvTotal.setOnClickListener(clickListener);
        } else {
            holder.tvName.setOnClickListener(clickListener);
            holder.tvValue.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isDetailedType(items.get(position).getItemType()) ? VIEW_TYPE_DETAILED : VIEW_TYPE_COMPACT;
    }

    private boolean isDetailedType(String itemType) {
        String normalized = HoldingsDb.normalizeItemType(itemType);
        return "Silver".equalsIgnoreCase(normalized) || "Gold".equalsIgnoreCase(normalized);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        final TextView tvNo;
        final TextView tvWeight;
        final TextView tvWhat;
        final TextView tvTotal;
        final TextView tvName;
        final TextView tvValue;
        final LinearLayout llMain;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            tvNo = itemView.findViewById(R.id.tv_no);
            tvWeight = itemView.findViewById(R.id.tv_weight);
            tvWhat = itemView.findViewById(R.id.tv_what);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvName = itemView.findViewById(R.id.tv_name);
            tvValue = itemView.findViewById(R.id.tv_value);
            llMain = itemView.findViewById(R.id.ll_main);
        }
    }
}