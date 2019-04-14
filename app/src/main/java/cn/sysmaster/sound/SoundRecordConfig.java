package cn.sysmaster.sound;

import android.content.Context;
import android.media.AudioFormat;

import java.io.File;

/**
 * @author dabo
 * @date 2019/4/12
 * @describe 录音配置参数
 */
public class SoundRecordConfig {


    private Context mContext;
    /**
     * 录音文件存放路径
     */
    private String mAudioRecordDir = "";

    /**
     * 录音时长，0为不限制时长
     */
    private long mRecordDuation = 0L;

    /**
     * 录音格式，默认WAV
     */
    private SoundRecordFormat mRecordFormat = SoundRecordFormat.WAV;
    /**
     * 声道设置：android支持双声道立体声和单声道。MONO单声道，STEREO立体声
     * 默认单声道
     */
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 编码制式和采样大小：主流16bit
     */
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 采样率：音频的采样频率，每秒钟能够采样的次数，采样率越高，音质越高。
     */
    private int mSampleRateInHz = 16000;

    private SoundRecordConfig() {
    }

    /**
     * 录音格式
     */
    public enum SoundRecordFormat {
        /**
         * wav格式
         */
        WAV(".wav"),
        /**
         * pcm格式
         */
        PCM(".pcm");

        private String extension;

        public String getExtension() {
            return extension;
        }

        SoundRecordFormat(String extension) {
            this.extension = extension;
        }
    }


    public static final class AudioRecordConfigBuilder {
        private Context mContext;
        private String mAudioRecordDir = "";
        private long mRecordDuation = 0L;
        private SoundRecordFormat mRecordFormat = SoundRecordFormat.WAV;
        private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
        private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        private int mSampleRateInHz = 16000;

        private AudioRecordConfigBuilder(Context context) {
            this.mContext = context;
            this.mAudioRecordDir = context.getExternalFilesDir(null).getAbsolutePath() + File.separator + "SoundRecord" + File.separator;
        }

        public static AudioRecordConfigBuilder create(Context context) {
            return new AudioRecordConfigBuilder(context);
        }

        public AudioRecordConfigBuilder withAudioRecordDir(String AudioRecordDir) {
            this.mAudioRecordDir = AudioRecordDir;
            return this;
        }

        public AudioRecordConfigBuilder withRecordDuation(long recordDuation) {
            this.mRecordDuation = recordDuation;
            return this;
        }

        public AudioRecordConfigBuilder withRecordFormat(SoundRecordFormat RecordFormat) {
            this.mRecordFormat = RecordFormat;
            return this;
        }

        public AudioRecordConfigBuilder withChannelConfig(int ChannelConfig) {
            this.mChannelConfig = ChannelConfig;
            return this;
        }

        public AudioRecordConfigBuilder withAudioFormat(int AudioFormat) {
            this.mAudioFormat = AudioFormat;
            return this;
        }

        public AudioRecordConfigBuilder withSampleRateInHz(int SampleRateInHz) {
            this.mSampleRateInHz = SampleRateInHz;
            return this;
        }

        public AudioRecordConfigBuilder but() {
            return create(mContext).
                    withAudioRecordDir(mAudioRecordDir).
                    withRecordDuation(mRecordDuation).
                    withRecordFormat(mRecordFormat).
                    withChannelConfig(mChannelConfig).
                    withAudioFormat(mAudioFormat).
                    withSampleRateInHz(mSampleRateInHz);
        }

        public SoundRecordConfig build() {
            SoundRecordConfig audioRecordConfig = new SoundRecordConfig();
            audioRecordConfig.mSampleRateInHz = this.mSampleRateInHz;
            audioRecordConfig.mContext = this.mContext;
            audioRecordConfig.mRecordFormat = this.mRecordFormat;
            audioRecordConfig.mChannelConfig = this.mChannelConfig;
            audioRecordConfig.mAudioRecordDir = this.mAudioRecordDir;
            audioRecordConfig.mAudioFormat = this.mAudioFormat;
            audioRecordConfig.mRecordDuation = this.mRecordDuation;
            return audioRecordConfig;
        }
    }

    public Context getContext() {
        return mContext;
    }

    public long getRecordDuation() {
        return mRecordDuation;
    }

    public String getAudioRecordDir() {
        return mAudioRecordDir;
    }

    public SoundRecordFormat getRecordFormat() {
        return mRecordFormat;
    }

    public int getChannelConfig() {
        return mChannelConfig;
    }

    public int getAudioFormat() {
        return mAudioFormat;
    }

    public int getSampleRateInHz() {
        return mSampleRateInHz;
    }

    /**
     * 当前的声道数
     *
     * @return 声道数： 0：error
     */
    public int getChannelCount() {
        if (mChannelConfig == AudioFormat.CHANNEL_IN_MONO) {
            return 1;
        } else if (mChannelConfig == AudioFormat.CHANNEL_IN_STEREO) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    public int getEncoding() {
        if (mAudioFormat == AudioFormat.ENCODING_PCM_8BIT) {
            return 8;
        } else if (mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
            return 16;
        } else {
            return 0;
        }
    }
}
