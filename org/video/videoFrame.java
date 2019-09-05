package org.video;

import org.bytedeco.javacv.Frame;

import javax.swing.*;

public class videoFrame {
    public ImageIcon imgIcon;
    public long timeStamp;

    public videoFrame(Frame f,long timeStamp){
        this.timeStamp = timeStamp;
        imgIcon = util.processImage(f);
    }
}
