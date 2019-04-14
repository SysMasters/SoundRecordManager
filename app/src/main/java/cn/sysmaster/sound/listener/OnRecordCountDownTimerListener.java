package cn.sysmaster.sound.listener;

/**
 * @author sysmaster
 * @date 2019/4/14
 * @describe 录音计时监听
 */
public interface OnRecordCountDownTimerListener {
    /**
     * 计时回调监听，每秒回调一次，用于计时显示
     *
     * @param millis 剩余时长
     */
    void onTimer(long millis);
}
