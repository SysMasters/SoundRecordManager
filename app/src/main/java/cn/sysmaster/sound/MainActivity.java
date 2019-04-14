package cn.sysmaster.sound;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import cn.sysmaster.sound.listener.OnRecordCountDownTimerListener;
import cn.sysmaster.sound.listener.OnRecordFftDataListener;
import cn.sysmaster.sound.listener.OnRecordSoundSizeListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button startBtn;
    Button stopBtn;
    AudioView audioView;
    TextView tvSoundSize;
    TextView tvTime;

    private boolean isPause = false;
    private boolean isStart = false;


    private SoundRecordManager mSoundRecordManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = findViewById(R.id.start);
        stopBtn = findViewById(R.id.stop);
        audioView = findViewById(R.id.audioView);
        tvSoundSize = findViewById(R.id.sound);
        tvTime = findViewById(R.id.timer);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);

        audioView.setStyle(AudioView.ShowStyle.STYLE_HOLLOW_LUMP, AudioView.ShowStyle.STYLE_HOLLOW_LUMP);
        mSoundRecordManager = new SoundRecordManager(this);
        mSoundRecordManager.setSoundRecordConfig(SoundRecordConfig.AudioRecordConfigBuilder
                .create(this)
                .withRecordDuation(180L)
                .build());
        mSoundRecordManager.setRecordFftDataListener(new OnRecordFftDataListener() {
            @Override
            public void onFftData(byte[] data) {
                audioView.setWaveData(data);
            }
        });
        mSoundRecordManager.setRecordSoundSizeListener(new OnRecordSoundSizeListener() {
            @Override
            public void onSoundSize(int soundSize) {
                tvSoundSize.setText(String.format(Locale.getDefault(), "声音大小：%s db", soundSize));
            }
        });
        mSoundRecordManager.setOnRecordCountDownTimerListener(new OnRecordCountDownTimerListener() {
            @Override
            public void onTimer(long millis) {
                tvTime.setText(String.valueOf(millis / 1000));
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                if (isStart) {
                    startBtn.setText("继续");
                    mSoundRecordManager.pause();
                    isPause = true;
                    isStart = false;
                } else {
                    if (isPause) {
                        mSoundRecordManager.resume();
                    } else {
                        mSoundRecordManager.start();
                    }
                    startBtn.setText("暂停");
                    isStart = true;
                }
                break;
            case R.id.stop:
                mSoundRecordManager.stop();
                startBtn.setText("开始");
                isPause = false;
                isStart = false;
                break;
            default:
                break;
        }
    }
}
