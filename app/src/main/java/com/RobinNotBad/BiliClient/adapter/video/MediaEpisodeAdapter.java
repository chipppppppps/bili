package com.RobinNotBad.BiliClient.adapter.video;

import android.annotation.SuppressLint;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.listener.OnItemClickListener;
import com.RobinNotBad.BiliClient.listener.OnItemLongClickListener;
import com.RobinNotBad.BiliClient.model.Bangumi;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaEpisodeAdapter extends RecyclerView.Adapter<MediaEpisodeAdapter.EpisodeHolder> {
    private List<Bangumi.Episode> episodeList;

    public OnItemClickListener listener;
    public OnItemLongClickListener longClickListener;
    public int selectedItemIndex = 0;
    private boolean useVerticalLayout;

    // 批量勾选模式相关
    private boolean multiSelectMode = false;
    private final Set<Integer> selectedSet = new HashSet<>();

    public MediaEpisodeAdapter() {
    }

    public MediaEpisodeAdapter(boolean useVerticalLayout) {
        this.useVerticalLayout = useVerticalLayout;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setSelectedItemIndex(int selectedItemIndex) {
        // Cancel previous selected item and set current item
        int previousSelectedIndex = this.selectedItemIndex;
        this.selectedItemIndex = selectedItemIndex;
        notifyItemChanged(previousSelectedIndex);
        notifyItemChanged(selectedItemIndex);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<Bangumi.Episode> episodeList) {
        this.episodeList = episodeList;
        selectedItemIndex = 0;
        multiSelectMode = false;
        selectedSet.clear();
        notifyDataSetChanged();
    }

    /**
     * 是否处于批量勾选模式
     */
    public boolean isMultiSelectMode() {
        return multiSelectMode;
    }

    /**
     * 进入批量勾选模式，并默认勾选指定位置的一集
     */
    public void enterMultiSelect(int position) {
        multiSelectMode = true;
        selectedSet.clear();
        if (position >= 0) {
            selectedSet.add(position);
        }
        notifyDataSetChanged();
    }

    /**
     * 退出批量勾选模式，清空勾选
     */
    public void exitMultiSelect() {
        multiSelectMode = false;
        selectedSet.clear();
        notifyDataSetChanged();
    }

    /**
     * 切换某集的勾选状态
     */
    public void toggleSelect(int position) {
        if (selectedSet.contains(position)) {
            selectedSet.remove(position);
        } else {
            selectedSet.add(position);
        }
        notifyItemChanged(position);
    }

    /**
     * 已勾选的数量
     */
    public int getSelectedCount() {
        return selectedSet.size();
    }

    /**
     * 已勾选的索引列表（升序）
     */
    public List<Integer> getSelectedIndices() {
        List<Integer> indices = new ArrayList<>(selectedSet);
        java.util.Collections.sort(indices);
        return indices;
    }

    @NonNull
    @Override
    public EpisodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(parent.getContext(), R.style.Theme_BiliClient);
        View view = LayoutInflater.from(contextWrapper)
                .inflate(useVerticalLayout ? R.layout.cell_item_vertical : R.layout.cell_episode, parent, false);
        return new EpisodeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeHolder holder, int position) {
        if (position < 0 || episodeList == null || position >= episodeList.size())
            return;
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return episodeList != null ? episodeList.size() : 0;
    }

    public class EpisodeHolder extends RecyclerView.ViewHolder {

        private final MaterialButton button;

        public EpisodeHolder(View view) {
            super(view);
            button = itemView.findViewById(R.id.btn);
        }

        void bind(int currentIndex) {
            if (currentIndex < 0 || episodeList == null || currentIndex >= episodeList.size())
                return;
            refreshStyle(currentIndex);
            button.setOnClickListener(v -> {
                if (multiSelectMode) {
                    // 批量勾选模式：点击切换勾选
                    toggleSelect(currentIndex);
                } else {
                    setSelectedItemIndex(currentIndex);
                }
                if (listener != null) {
                    listener.onItemClick(currentIndex);
                }
            });
            button.setOnLongClickListener(v -> {
                if (!multiSelectMode && longClickListener != null) {
                    longClickListener.onItemLongClick(currentIndex);
                    return true;
                }
                return false;
            });
        }

        private void refreshStyle(int currentIndex) {
            boolean checked = multiSelectMode && selectedSet.contains(currentIndex);
            String title = episodeList.get(currentIndex).title;
            button.setText(checked ? "✓ " + title : title);

            boolean highlight = multiSelectMode ? checked : (selectedItemIndex == currentIndex);
            if (highlight) {
                button.setTextColor(0xcc262626);
                ViewCompat.setBackgroundTintList(button, AppCompatResources.getColorStateList(itemView.getContext(),
                        R.color.background_button_selected));
            } else {
                button.setTextColor(0xffebe0e2);
                ViewCompat.setBackgroundTintList(button,
                        AppCompatResources.getColorStateList(itemView.getContext(), R.color.background_button));
            }
        }
    }
}
