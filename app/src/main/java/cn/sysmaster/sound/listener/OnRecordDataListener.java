package cn.sysmaster.sound.listener;

/**
 * @author zhaolewei on 2018/7/11.
 */
public interface OnRecordDataListener {

    /**
     * 当前的录音状态发生变化
     *
     * @param data 当前音频数据
     */
    void onData(byte[] data);

}
