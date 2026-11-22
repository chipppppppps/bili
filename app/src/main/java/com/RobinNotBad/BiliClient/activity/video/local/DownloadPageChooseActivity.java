package com.RobinNotBad.BiliClient.activity.video.local;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.adapter.video.PageChooseAdapter;
import com.RobinNotBad.BiliClient.model.DownloadSection;
import com.RobinNotBad.BiliClient.service.DownloadService;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;

import java.io.File;
import java.util.ArrayList;

//下载选集播放
//2026-11-22

public class DownloadPageChooseActivity extends BaseActivity {

    private String title;
    private final ArrayList<DownloadSection> sections = new ArrayList<>();
    private PageChooseAdapter adapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        findViewById(R.id.top).setOnClickListener(view -> finish());

        TextView textView = findViewById(R.id.pageName);
        textView.setText("选集播放");

        title = getIntent().getStringExtra("title");

        CenterThreadPool.run(() -> {
            loadSections();
            runOnUiThread(() -> {
                adapter = new PageChooseAdapter(this, buildNames());
                adapter.setOnItemClickListener(this::handleClick);
                adapter.setOnItemLongClickListener(this::deleteSection);
                recyclerView.setLayoutManager(new CustomLinearManager(this));
                recyclerView.setAdapter(adapter);
            });
        });
    }

    /**
     * 从数据库加载当前标题下的所有分集
     */
    private void loadSections() {
        sections.clear();
        ArrayList<DownloadSection> all = DownloadService.getAll();
        if (all != null) {
            for (DownloadSection s : all) {
                if (title != null && title.equals(s.title)) {
                    sections.add(s);
                }
            }
        }
    }

    private ArrayList<String> buildNames() {
        ArrayList<String> names = new ArrayList<>();
        for (DownloadSection s : sections) {
            names.add(buildName(s));
        }
        return names;
    }

    private String buildName(DownloadSection s) {
        String name = (s.child == null || s.child.isEmpty()) ? s.title : s.child;
        String state;
        switch (s.state) {
            case "done":
                state = "已下载";
                break;
            case "downloading":
                state = "下载中";
                break;
            case "error":
                state = "出错";
                break;
            default:
                state = "等待";
        }
        return name + "（" + state + "）";
    }

    /**
     * 点击分集：已下载播放，下载中提示，否则继续下载
     */
    private void handleClick(int position) {
        if (position < 0 || position >= sections.size())
            return;
        DownloadSection section = sections.get(position);
        if (section.isDone()) {
            DownloadService.playSection(this, section);
        } else if ("downloading".equals(section.state)) {
            MsgUtil.showMsg("下载中，无法操作");
        } else {
            CenterThreadPool.run(() -> {
                if ("error".equals(section.state)) {
                    DownloadService.setState(section.id, "none");
                }
                DownloadService.start(section.id);
                runOnUiThread(() -> {
                    loadSections();
                    if (adapter != null) {
                        adapter.setData(buildNames());
                    }
                });
            });
        }
    }

    /**
     * 长按分集：删除该集
     */
    private void deleteSection(int position) {
        if (position < 0 || position >= sections.size())
            return;
        DownloadSection section = sections.get(position);
        CenterThreadPool.run(() -> {
            try {
                if ("downloading".equals(section.state) && DownloadService.started) {
                    stopService(new Intent(this, DownloadService.class));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }

                File folder = section.getPath();
                if (folder != null && folder.exists()) {
                    FileUtil.deleteFolder(folder);
                }
                DownloadService.deleteSection(section.id);

                loadSections();
                runOnUiThread(() -> {
                    if (sections.isEmpty()) {
                        // 整组已删空，清理父目录
                        File parent = FileUtil.getVideoDownloadPath(title, null);
                        if (parent != null && parent.exists()) {
                            FileUtil.deleteFolder(parent);
                        }
                        MsgUtil.showMsg("已全部删除");
                        finish();
                    } else {
                        if (adapter != null) {
                            adapter.setData(buildNames());
                        }
                        MsgUtil.showMsg("删除成功");
                    }
                });
            } catch (Exception e) {
                MsgUtil.err(e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 返回下载列表时刷新，让状态同步
        if (DownloadListActivity.weakRef != null && DownloadListActivity.weakRef.get() != null) {
            DownloadListActivity.weakRef.get().refreshList(true);
        }
    }
}
