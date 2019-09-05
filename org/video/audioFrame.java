package org.video;

import org.bytedeco.javacv.Frame;

public class audioFrame {
    public long timeStamp;
    public Frame frame;
    public byte[] combine;//一帧音频数据

    public audioFrame(Frame frame,long timeStamp,audioManager am){
        this.frame = frame;
        this.timeStamp = timeStamp;
        am.processAudioFrame(frame,this);
    }
}
