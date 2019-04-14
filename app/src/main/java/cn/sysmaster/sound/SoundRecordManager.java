package cn.sysmaster.sound;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.sysmaster.sound.fftlib.FFT;
import cn.sysmaster.sound.listener.OnRecordCountDownTimerListener;
import cn.sysmaster.sound.listener.OnRecordDataListener;
import cn.sysmaster.sound.listener.OnRecordFftDataListener;
import cn.sysmaster.sound.listener.OnRecordResultListener;
import cn.sysmaster.sound.listener.OnRecordSoundSizeListener;
import cn.sysmaster.sound.listener.OnRecordStateListener;
import cn.sysmaster.sound.utils.ByteUtils;
import cn.sysmaster.sound.utils.WavUtils;

import static cn.sysmaster.sound.SoundRecordConfig.AudioRecordConfigBuilder;

/**
 * @author dabo
 * @date 2019/4/12
 * @describe 录制音频管理类
 */
public class SoundRecordManager {

    /**
     * 当前录制状态
     */
    private volatile int mState = SoundRecordState.IDLE;
    /**
     * 录音参数配置类
     */
    private SoundRecordConfig mSoundRecordConfig;

    /**
     * 录制文件集合，如果有暂停操作会生成多个文件，用于最后合成
     */
    private List<File> mPcmFiles = new ArrayList<>();
    /**
     * 录音文件
     */
    private File mResultFile;
    /**
     * pcm源文件
     */
    private File mPcmFile;

    /**
     * 录音执行子线程
     */
    private AudioRecordThread mAudioRecordThread;

    /**
     * 录音计时器
     */
    private CountDownTimer mCountDownTimer;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 录音计时监听
     */
    private OnRecordCountDownTimerListener mOnRecordCountDownTimerListener;
    /**
     * 当前录制状态监听
     */
    private OnRecordStateListener mRecordStateListener;
    /**
     * 录制音量大小监听
     */
    private OnRecordSoundSizeListener mRecordSoundSizeListener;
    /**
     * 录制监听
     */
    private OnRecordDataListener mRecordDataListener;
    /**
     * 录音文件
     */
    private OnRecordResultListener mRecordResultListener;
    /**
     * 录音可视化数据
     */
    private OnRecordFftDataListener mRecordFftDataListener;

    private SoundRecordManager() {
    }

    public SoundRecordManager(Context context) {
        mSoundRecordConfig = AudioRecordConfigBuilder
                .create(context)
                .build();
    }

    public void setSoundRecordConfig(SoundRecordConfig config) {
        this.mSoundRecordConfig = config;
    }

    /**
     * 初始化计时器
     */
    private void initCountDownTimer() {
        if (mSoundRecordConfig.getRecordDuation() <= 0) {
            return;
        }
        mCountDownTimer = new CountDownTimer(mSoundRecordConfig.getRecordDuation() * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (null != mOnRecordCountDownTimerListener) {
                    mOnRecordCountDownTimerListener.onTimer(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                stop();
            }
        };
    }

    /**
     * 开始录制
     */
    public void start() {
        if (mState != SoundRecordState.IDLE) {
            return;
        }

        // 录音文件
        mResultFile = new File(getResultFilePath());
        String pcmFilePath = getPcmFilePath();
        mPcmFile = new File(pcmFilePath);

        // 开启录制线程
        mAudioRecordThread = new AudioRecordThread();
        mAudioRecordThread.start();
    }

    /**
     * 停止录制
     */
    public void stop() {
        if (mState == SoundRecordState.IDLE) {
            // 状态异常
            return;
        }

        if (mState == SoundRecordState.PAUSE) {
            makeFile();
            mState = SoundRecordState.IDLE;
        } else {
            mState = SoundRecordState.STOP;
        }
        notifyState();
    }

    /**
     * 暂停录制
     */
    public void pause() {
        if (mState != SoundRecordState.RECORDING) {
            return;
        }
        mState = SoundRecordState.PAUSE;
        notifyState();
    }

    /**
     * 继续录制
     */
    public void resume() {
        if (mState != SoundRecordState.PAUSE) {
            return;
        }
        String pcmFilePath = getPcmFilePath();
        mPcmFile = new File(pcmFilePath);
        mAudioRecordThread = new AudioRecordThread();
        mAudioRecordThread.start();
    }


    private class AudioRecordThread extends Thread {
        /**
         * 录制缓冲区大小
         */
        private int bufferSize;
        private AudioRecord mAudioRecord;

        AudioRecordThread() {
            bufferSize = AudioRecord.getMinBufferSize(
                    mSoundRecordConfig.getSampleRateInHz(),
                    mSoundRecordConfig.getChannelConfig(),
                    mSoundRecordConfig.getAudioFormat());
            mAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    mSoundRecordConfig.getSampleRateInHz(),
                    mSoundRecordConfig.getChannelConfig(),
                    mSoundRecordConfig.getAudioFormat(),
                    bufferSize);
        }

        @Override
        public void run() {
            super.run();
            startPcmRecorder();
        }

        /**
         * 开始pcm录制
         * pcm是{@link android.media.AudioRecord}录制的源文件，不能播放，之后需要自己转换操作
         */
        private void startPcmRecorder() {
            // 当前状态：录制中
            mState = SoundRecordState.RECORDING;
            // 同步录制状态
            notifyState();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mPcmFile);
                mAudioRecord.startRecording();
                byte[] byteBuffer = new byte[bufferSize];

                while (mState == SoundRecordState.RECORDING) {
                    int end = mAudioRecord.read(byteBuffer, 0, byteBuffer.length);
                    notifyData(byteBuffer);
                    fos.write(byteBuffer, 0, end);
                    fos.flush();
                }
                mAudioRecord.stop();
                mPcmFiles.add(mPcmFile);
                if (mState == SoundRecordState.STOP) {
                    makeFile();
                } else {
                    // 暂停
                }
            } catch (Exception e) {
                e.printStackTrace();
                notifyError("录音失败");
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mState != SoundRecordState.PAUSE) {
                mState = SoundRecordState.IDLE;
                notifyState();
            }

        }
    }

    /**
     * 录制完成，根据文件类型进行合并
     */
    private void makeFile() {
        // 指定的录音格式，例如 wav
        switch (mSoundRecordConfig.getRecordFormat()) {
            case WAV:
                mergePcmFile();
                makeWav();
                break;
            case PCM:
                mergePcmFile();
                break;
            default:
                break;
        }
        notifyFinish();
    }


    /**
     * 录制状态通知
     */
    private void notifyState() {
        if (mRecordStateListener == null) {
            return;
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                mRecordStateListener.onStateChange(mState);
            }
        });

        if (mState == SoundRecordState.STOP || mState == SoundRecordState.PAUSE) {
            // 暂停、停止录音时，音量为0，很对
            if (mRecordSoundSizeListener != null) {
                mRecordSoundSizeListener.onSoundSize(0);
            }
        }
    }

    /**
     * 录制中数据通知
     *
     * @param data 录制的音频
     */
    private void notifyData(final byte[] data) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRecordDataListener != null) {
                    mRecordDataListener.onData(data);
                }

                if (mRecordFftDataListener != null) {
                    byte[] fftData = makeData(data);
                    if (fftData != null) {
                        if (mRecordSoundSizeListener != null) {
                            mRecordSoundSizeListener.onSoundSize(getDb(fftData));
                        }
                        mRecordFftDataListener.onFftData(fftData);
                    }
                }

            }
        });
    }

    /**
     * 录制完成通知
     */
    private void notifyFinish() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRecordStateListener != null) {
                    mRecordStateListener.onStateChange(SoundRecordState.FINISH);
                }
                if (mRecordResultListener != null) {
                    mRecordResultListener.onResult(mResultFile);
                }
            }
        });
    }

    /**
     * 录制失败通知
     *
     * @param error 失败信息
     */
    private void notifyError(final String error) {
        if (mRecordStateListener == null) {
            return;
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                mRecordStateListener.onError(error);
            }
        });
    }


    /**
     * 合并文件
     */
    private void mergePcmFile() {
        boolean mergeSuccess = mergePcmFiles(mResultFile, mPcmFiles);
        if (!mergeSuccess) {
            notifyError("合并失败");
        }
    }

    /**
     * 合并Pcm文件
     *
     * @param recordFile 输出文件
     * @param files      多个文件源
     * @return 是否成功
     */
    private boolean mergePcmFiles(File recordFile, List<File> files) {
        if (recordFile == null || files == null || files.size() <= 0) {
            return false;
        }

        FileOutputStream fos = null;
        BufferedOutputStream outputStream = null;
        byte[] buffer = new byte[1024];
        try {
            fos = new FileOutputStream(recordFile);
            outputStream = new BufferedOutputStream(fos);

            for (int i = 0; i < files.size(); i++) {
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(files.get(i)));
                int readCount;
                while ((readCount = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, readCount);
                }
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < files.size(); i++) {
            files.get(i).delete();
        }
        files.clear();
        return true;
    }

    /**
     * 添加Wav头文件
     */
    private void makeWav() {
        if (!WavUtils.isFile(mResultFile) || mResultFile.length() == 0) {
            return;
        }
        byte[] header = WavUtils.generateWavFileHeader((int) mResultFile.length(), mSoundRecordConfig.getSampleRateInHz(), mSoundRecordConfig.getChannelCount(), mSoundRecordConfig.getEncoding());
        WavUtils.writeHeader(mResultFile, header);
    }

    private byte[] makeData(byte[] data) {//data.length = 1280
        if (data.length < 1024) {
            return null;
        }
        try {
            double[] ds = toDouble(ByteUtils.toShorts(data));

            double[] fft = FFT.fft(ds, 62);
            //start
            double[] newFft = new double[128];
            for (int i = 16; i < 16 + newFft.length; i++) {
                if (i < 24) {
                    newFft[i - 16] = fft[i] * 0.2;
                } else if (i < 36) {
                    newFft[i - 16] = fft[i] * 0.4;
                } else if (i < 48) {
                    newFft[i - 16] = fft[i] * 0.6;
                } else {
                    newFft[i - 16] = fft[i];
                }
                if (newFft[i - 16] < 10 * 128) {
                    newFft[i - 16] = newFft[i - 16] * 0.6;
                }
            }
            fft = newFft;
            //end
            int step = fft.length / 128;
            byte[] fftBytes = new byte[128];

            //压缩128基准
            int scale = 128;
            double max = getMax(fft);
            //高音优化
            if (max > 128 * 128) {
                scale = (int) (max / 128) + 2;
            }

            for (int i = 0; i < fftBytes.length; i++) {
                double tmp = fft[i * step] / scale;
                if (tmp > 127) {
                    fftBytes[i] = 127;
                } else if (tmp < -128) {
                    fftBytes[i] = -127;

                } else {
                    fftBytes[i] = (byte) tmp;
                }
            }
            return fftBytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private double getMax(double[] data) {
        double max = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }

        return max;
    }

    public int getDb(byte[] data) {
        double sum = 0;
        double ave;
        int length = data.length > 128 ? 128 : data.length;
        for (int i = 0; i < length; i++) {
            sum += data[i];
        }
        ave = sum / length;
        sum += (Math.pow(ave, 4) / Math.pow((128F - ave), 2));
        int i = (int) (Math.log10((sum / length) * 53536F) * 10);
        return i < 0 ? 27 : i;
    }

    private double[] toDouble(short[] bytes) {
        int length = 512;
        double[] ds = new double[length];
        for (int i = 0; i < length; i++) {
            ds[i] = bytes[i];
        }
        return ds;
    }

    /**
     * 根据当前的时间生成相应的文件名
     * 实例 record_20160101_13_15_12
     */
    private String getPcmFilePath() {
        String fileDir = mSoundRecordConfig.getAudioRecordDir() + "pcm/";
        createOrExistsDir(new File(fileDir));
        String fileName = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.SIMPLIFIED_CHINESE).format(new Date(System.currentTimeMillis()));
        return String.format(Locale.getDefault(), "%s%s.pcm", fileDir, fileName);
    }

    private String getResultFilePath() {
        String fileDir = mSoundRecordConfig.getAudioRecordDir();
        createOrExistsDir(new File(fileDir));
        String fileName = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.SIMPLIFIED_CHINESE).format(new Date(System.currentTimeMillis()));
        return String.format(Locale.getDefault(), "%s%s%s", fileDir, fileName, mSoundRecordConfig.getRecordFormat().getExtension());
    }

    public boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }


    public void setRecordStateListener(OnRecordStateListener recordStateListener) {
        mRecordStateListener = recordStateListener;
    }

    public void setOnRecordCountDownTimerListener(OnRecordCountDownTimerListener onRecordCountDownTimerListener) {
        mOnRecordCountDownTimerListener = onRecordCountDownTimerListener;
    }

    public void setRecordSoundSizeListener(OnRecordSoundSizeListener recordSoundSizeListener) {
        mRecordSoundSizeListener = recordSoundSizeListener;
    }

    public void setRecordDataListener(OnRecordDataListener recordDataListener) {
        mRecordDataListener = recordDataListener;
    }

    public void setRecordResultListener(OnRecordResultListener recordResultListener) {
        mRecordResultListener = recordResultListener;
    }

    public void setRecordFftDataListener(OnRecordFftDataListener recordFftDataListener) {
        mRecordFftDataListener = recordFftDataListener;
    }
}
