package cn.sysmaster.sound;

/**
 * @author dabo
 * @date 2019/4/12
 * @describe 录制状态
 */
public class SoundRecordState {

    /**
     * 空闲状态
     */
    public static final int IDLE = 0;
    /**
     * 录音中
     */
    public static final int RECORDING = 1;
    /**
     * 暂停中
     */
    public static final int PAUSE = 2;
    /**
     * 正在停止
     */
    public static final int STOP = 3;
    /**
     * 录音流程结束（转换结束）
     */
    public static final int FINISH = 4;

}
