package cn.sysmaster.soundrecordmanager.listener;

/**
 * @author zhaolewei on 2018/7/11.
 */
public interface OnRecordStateListener {

    /**
     * 当前的录音状态发生变化
     *
     * @param state 当前状态
     * @see cn.sysmaster.soundrecordmanager.SoundRecordState
     */
    void onStateChange(int state);

    /**
     * 录音错误
     *
     * @param error 错误
     */
    void onError(String error);

}
