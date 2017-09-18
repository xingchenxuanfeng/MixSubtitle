package com.xc.mixsubtitle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xc.mixsubtitle.subtitle.model.SubtitleCue;
import com.xc.mixsubtitle.subtitle.model.SubtitleParsingException;
import com.xc.mixsubtitle.subtitle.model.SubtitleText;
import com.xc.mixsubtitle.subtitle.srt.SrtObject;
import com.xc.mixsubtitle.subtitle.srt.SrtParser;
import com.xc.mixsubtitle.subtitle.srt.SrtWriter;
import com.xc.mixsubtitle.subtitle.util.SubtitlePlainText;
import com.xc.mixsubtitle.subtitle.util.SubtitleTextLine;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String COURSERA_PACKAGENAME = "org.coursera.android";
    private static final String TGA = "MixSubtitle";
    private static boolean DEBUG = true;
    private Button mBtn;
    private TextView mTips;
    private File mCourseraFile;
    private Context mCourseraContext = null;
    private Handler mHandler;
    public String mFirstLanguage = "en";
    public List<String> mSecondLanguage = new ArrayList<>();

    {
        mSecondLanguage.add("zh-TW");
        mSecondLanguage.add("zh-CN");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mBtn = (Button) findViewById(R.id.btn);
        mTips = (TextView) findViewById(R.id.tips);
        mTips.setText("如果软件无效的话，请尝试打开手机自带的下载管理的设置，然后关闭使用迅雷等下载引擎的选项，再删除已下载的视频重新下载。"
                + "\n有任何疑问可点此反馈" +
                "\nhttps://github.com/xingchenxuanfeng/FlowLayout/issues/new");
        if (!isAppInstalled(getApplicationContext(), COURSERA_PACKAGENAME)) {
            mBtn.setEnabled(false);
            Toast.makeText(getApplicationContext(), "您没有安装 coursera", Toast.LENGTH_LONG).show();
            return;
        } else {
            mBtn.setEnabled(true);
        }
        try {
            mCourseraContext = createPackageContext(COURSERA_PACKAGENAME, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestPermission()) {
                    new WorkAsyncTask().execute();
                }
            }
        });

    }


    private void addSecond(File[] firstFiles, File courseraFile) throws IOException, SubtitleParsingException {
        for (File firstFile : firstFiles) {
            String firstFileName = firstFile.getName();
            File[] secondSubtitle = getSecondSubtitle(firstFileName.substring(0, firstFileName.lastIndexOf(",")), courseraFile);
            for (File secondFile : secondSubtitle) {
                doAdd(firstFile, secondFile, secondFile);
            }
        }

    }

    private SrtParser srtParser = new SrtParser("utf-8");
    private SrtWriter srtWriter = new SrtWriter("utf-8");

    private void doAdd(final File first, final File second, File out) throws IOException, SubtitleParsingException {
        if (DEBUG) {
            Log.d(TGA, "尝试合成\n" +
                    first + "  +  " + second);
        }
        if (getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean(first.getAbsolutePath(), false)
                || getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean(second.getAbsolutePath(), false)
                ) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "重复合成字幕会造成文字重复，已取消 " +
                            "\n" + first.getName() + "  +  " + second.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            if (DEBUG) {
                Log.d(TGA, "    重复合成字幕\n" +
                        first + "  +  " + second);
            }
            return;
        }
        SrtObject firstSrtObject = srtParser.parse(new FileInputStream(first));
        SrtObject secondSrtObject = srtParser.parse(new FileInputStream(second));
        List<SubtitleCue> firstSrtObjectCues = firstSrtObject.getCues();
        List<SubtitleCue> secondSrtObjectCues = secondSrtObject.getCues();
        int j = 0;
        firstLoop:
        for (int i = 0; i < firstSrtObjectCues.size(); i++) {
            SubtitleCue firstCue = firstSrtObjectCues.get(i);
            for (; j < secondSrtObjectCues.size(); j++) {
                SubtitleCue secondCue = secondSrtObjectCues.get(j);
                if (secondCue.getStartTime().compareTo(firstCue.getStartTime()) == 0 && secondCue.getEndTime().compareTo(firstCue.getEndTime()) == 0) {
                    firstCue.getLines().addAll(secondCue.getLines());
                    firstCue.getLines().add(new SubtitleTextLine(Arrays.asList((SubtitleText) new SubtitlePlainText("\n"))));
                    if (DEBUG) {
                        Log.d(TGA, firstCue.getId() + "\n" +
                                firstCue.getStartTime().toString() + "\n" +
                                firstCue.getEndTime().toString() + "\n" +
                                firstCue.getLines().toString() + "\n" +
                                firstCue.getText());
                    }
                    continue firstLoop;
                }
            }

        }
        srtWriter.write(firstSrtObject, new FileOutputStream(out));
        getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).edit().putBoolean(out.getAbsolutePath(), true).apply();
    }

    private File[] getFirstSubtitle(File courseraFile) {
        return courseraFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.contains(",") && !name.contains("."))
                        && mFirstLanguage.equals(name.substring(name.lastIndexOf(",") + 1));
            }
        });
    }

    private File[] getSecondSubtitle(final String firstFileNameTopName, File courseraFile) {
        return courseraFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.contains(",") && !name.contains("."))
                        && firstFileNameTopName.equals(name.substring(0, name.lastIndexOf(",")))
                        && mSecondLanguage.contains(name.substring(name.lastIndexOf(",") + 1));
            }
        });
    }


    /**
     * check the app is installed
     */
    private boolean isAppInstalled(Context context, String packagename) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        return packageInfo != null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        if (args != null && args.length > 0) {
            if ("enableDebug".equals(args[0])) {
                DEBUG = true;
            } else if ("disableDebug".equals(args[0])) {
                DEBUG = false;
            }
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private boolean requestPermission() {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(
                MainActivity.this,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        );
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new WorkAsyncTask().execute();
                }
                break;
        }
    }

    private class WorkAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (mCourseraContext != null) {
                mCourseraFile = mCourseraContext.getExternalFilesDir("Download");
            } else {
                mCourseraFile = new File("/storage/emulated/0/Android/data/org.coursera.android/files/Download");
            }
            if (mCourseraFile != null && mCourseraFile.exists()) {
                File[] first = getFirstSubtitle(mCourseraFile);
                if (first != null) {
                    try {
                        addSecond(first, mCourseraFile);
                    } catch (IOException | SubtitleParsingException e) {
                        e.printStackTrace();
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "没有找到字幕,您是否还没有下载字幕文件，尝试在coursera中点击 Saved for Offline 或者先播放一下原字幕？\n" +
                                            mCourseraFile, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "没有找到字幕路径 \n" + mCourseraFile, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return null;
        }
    }

}
