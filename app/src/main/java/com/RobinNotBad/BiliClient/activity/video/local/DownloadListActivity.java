package com.RobinNotBad.BiliClient.activity.video.local;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.video.DownloadAdapter;
import com.RobinNotBad.BiliClient.model.DownloadSection;
import com.RobinNotBad.BiliClient.service.DownloadService;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadListActivity extends RefreshListActivity {
    public static WeakReference<DownloadListActivity> weakRef;
    DownloadAdapter adapter;
    Timer timer;
    boolean emptyTipShown;
    boolean firstRefresh = true;
    boolean created;
    ArrayList<DownloadAdapter.DownloadGroup> groups;
    private float lastPercent = -1;
    private String lastState = null;
    private long lastDownloadingId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setPageName("下载列表");
        setRefreshing(false);
        weakRef = new WeakReference<>(this);

        CenterThreadPool.run(() -> {
            created = true;
            refreshList(false);

            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (adapter == null || !created || isDestroyed())
                        return;
                    if (DownloadService.section != null) {
                        boolean needUpdate = false;
                        if (lastDownloadingId != DownloadService.section.id) {
                            lastDownloadingId = DownloadService.section.id;
                            needUpdate = true;
                        }
                        if (lastPercent != DownloadService.percent) {
                            lastPercent = DownloadService.percent;
                            needUpdate = true;
                        }
                        if (lastState == null || !lastState.equals(DownloadService.state)) {
                            lastState = DownloadService.state;
                            needUpdate = true;
                        }
                        if (needUpdate) {
                            final int pos = findDownloadingPosition();
                            if (pos >= 0) {
                                runOnUiThread(() -> adapter.notifyItemChanged(pos));
                            }
                        }
                    }
                }
            }, 300, 500);
        });

    }

    private int findDownloadingPosition() {
        if (groups == null || DownloadService.section == null)
            return -1;
        for (int i = 0; i < groups.size(); i++) {
            DownloadAdapter.DownloadGroup group = groups.get(i);
            for (DownloadSection s : group.sections) {
                if (s.id == DownloadService.section.id) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshList(boolean fromOutside) {
        if (this.isDestroyed() || !created)
            return;
        Log.d("debug", "刷新下载列表");

        ArrayList<DownloadSection> sections = DownloadService.getAll();
        groups = groupSections(sections);

        if (groups == null || groups.isEmpty()) {
            if (!emptyTipShown) {
                runOnUiThread(() -> {
                    MsgUtil.showMsg("下载列表为空");
                    showEmptyView();
                });
                emptyTipShown = true;
            }
        } else {
            if (emptyTipShown) {
                emptyTipShown = false;
                runOnUiThread(this::hideEmptyView);
            }

            if (firstRefresh) {
                adapter = new DownloadAdapter(DownloadListActivity.this, groups);
                adapter.setOnClickListener(this::handleGroupClick);
                adapter.setOnLongClickListener(this::deleteGroup);
                runOnUiThread(() -> setAdapter(adapter));
                firstRefresh = false;
            } else {
                adapter.groupList = groups;
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        }
    }

    /**
     * 按标题分组，同一番剧/多P视频的所有分集归为一组
     */
    private ArrayList<DownloadAdapter.DownloadGroup> groupSections(ArrayList<DownloadSection> sections) {
        if (sections == null || sections.isEmpty())
            return null;
        LinkedHashMap<String, DownloadAdapter.DownloadGroup> map = new LinkedHashMap<>();
        for (DownloadSection s : sections) {
            DownloadAdapter.DownloadGroup group = map.get(s.title);
            if (group == null) {
                group = new DownloadAdapter.DownloadGroup(s.title, s.url_cover);
                map.put(s.title, group);
            }
            group.sections.add(s);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 点击分组：单集直接播放/继续下载，多集进入选集页
     */
    private void handleGroupClick(int position) {
        if (groups == null || position < 0 || position >= groups.size())
            return;
        DownloadAdapter.DownloadGroup group = groups.get(position);
        if (group.sections.size() == 1) {
            handleSection(group.sections.get(0));
        } else {
            Intent intent = new Intent(DownloadListActivity.this, DownloadPageChooseActivity.class);
            intent.putExtra("title", group.title);
            startActivity(intent);
        }
    }

    /**
     * 处理单集：已下载播放，下载中提示，否则继续下载
     */
    private void handleSection(DownloadSection section) {
        if (section.isDone()) {
            DownloadService.playSection(DownloadListActivity.this, section);
        } else if ("downloading".equals(section.state)) {
            MsgUtil.showMsg("下载中，无法操作");
        } else {
            CenterThreadPool.run(() -> {
                if ("error".equals(section.state)) {
                    DownloadService.setState(section.id, "none");
                }
                DownloadService.start(section.id);
            });
        }
    }

    /**
     * 长按分组：删除整组（文件+数据库）
     */
    private void deleteGroup(int position) {
        if (groups == null || position < 0 || position >= groups.size())
            return;
        DownloadAdapter.DownloadGroup group = groups.get(position);
        CenterThreadPool.run(() -> {
            try {
                boolean downloading = false;
                for (DownloadSection s : group.sections) {
                    if ("downloading".equals(s.state) && DownloadService.started) {
                        downloading = true;
                        break;
                    }
                }
                if (downloading) {
                    stopService(new Intent(DownloadListActivity.this, DownloadService.class));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }

                // 删除整组目录（单集即其目录，多集即父目录）
                File groupDir = FileUtil.getVideoDownloadPath(group.title, null);
                if (groupDir.exists()) {
                    FileUtil.deleteFolder(groupDir);
                }

                for (DownloadSection s : group.sections) {
                    DownloadService.deleteSection(s.id);
                }

                refreshList(false);
                MsgUtil.showMsg("删除成功");
            } catch (Exception e) {
                MsgUtil.err(e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (timer != null)
            timer.cancel();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        weakRef = null;
        super.onDestroy();
    }
}
