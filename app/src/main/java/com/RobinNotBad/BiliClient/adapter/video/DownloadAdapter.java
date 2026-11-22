package com.RobinNotBad.BiliClient.adapter.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.listener.OnItemClickListener;
import com.RobinNotBad.BiliClient.listener.OnItemLongClickListener;
import com.RobinNotBad.BiliClient.model.DownloadSection;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

//下载列表Adapter（按标题分组展示）
//2024-11-24

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadHolder> {

    Context context;
    public ArrayList<DownloadGroup> groupList;
    OnItemLongClickListener longClickListener;
    OnItemClickListener clickListener;

    public DownloadAdapter(Context context, ArrayList<DownloadGroup> groupList) {
        this.context = context;
        this.groupList = groupList;
    }

    public void setOnLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public DownloadHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(this.context).inflate(R.layout.cell_video_local, parent, false);
        return new DownloadHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadHolder holder, int position) {
        if (groupList == null || position < 0 || position >= groupList.size()) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        DownloadGroup group = groupList.get(position);
        holder.show(group, context);

        holder.itemView.setOnClickListener(view -> {
            if (clickListener != null) {
                clickListener.onItemClick(position);
            }
        });

        holder.itemView.setOnLongClickListener(view -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return groupList != null ? groupList.size() : 0;
    }

    /**
     * 下载分组：同一标题（番剧/多P视频）下的所有分集归为一组
     */
    public static class DownloadGroup {
        public String title;
        public String cover;
        public ArrayList<DownloadSection> sections = new ArrayList<>();

        public DownloadGroup(String title, String cover) {
            this.title = title;
            this.cover = cover;
        }

        /**
         * 统计指定状态的集数
         */
        public int countState(String state) {
            int count = 0;
            for (DownloadSection s : sections) {
                if (s.state.equals(state))
                    count++;
            }
            return count;
        }

        /**
         * 生成状态摘要，如「已下载3 下载中1 等待2」
         */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            int done = countState("done");
            int downloading = countState("downloading");
            int error = countState("error");
            int none = countState("none");
            if (done > 0) sb.append("已下载").append(done).append(" ");
            if (downloading > 0) sb.append("下载中").append(downloading).append(" ");
            if (error > 0) sb.append("出错").append(error).append(" ");
            if (none > 0) sb.append("等待").append(none).append(" ");
            String text = sb.toString().trim();
            return text.isEmpty() ? "共" + sections.size() + "集" : text;
        }
    }

    public static class DownloadHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView extra;
        final ImageView cover;

        public DownloadHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            cover = itemView.findViewById(R.id.img_cover);
            extra = itemView.findViewById(R.id.text_extra);
        }

        @SuppressLint("SetTextI18n")
        public void show(DownloadGroup group, Context context) {
            String name = group.title;
            if (group.sections.size() > 1) {
                name = name + "（" + group.sections.size() + "集）";
            }
            title.setText(name);
            extra.setText(group.summary());

            if (group.cover != null && !group.cover.isEmpty()) {
                try {
                    Glide.with(BiliTerminal.context).asDrawable().load(group.cover)
                            .transition(GlideUtil.getTransitionOptions())
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners(ToolsUtil.dp2px(5))))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(cover);
                } catch (Exception ignored) {
                }
            } else {
                cover.setImageResource(R.mipmap.placeholder);
            }
        }
    }
}
