package cn.sysmaster.sound.listener;

/**
 * @author zhaolewei on 2018/7/11.
 */
public interface OnRecordSoundSizeListener {

    /**
     * 实时返回音量大小
     *
     * @param soundSize 当前音量大小
     */
    void onSoundSize(int soundSize);

}
