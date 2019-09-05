package org.video;

import javax.swing.*;
import java.awt.*;

/**
 *  显示图片/视频
 */
public abstract class JVideo extends JLabel{
    public final static int STATE_IDLE = 0;//还没开始播放视频
    public final static int STATE_PAUSE = 1;//暂停状态
    public final static int STATE_PLAYING = 2;//正在播放
    public final static int STATE_PLAYED = 3;//播放完成
    public final static int STATE_STOP = 4;//还没播放完就点击了停止
    private videoStateChangeListener vsl;

    public final static int INTERPOLATION_NEAREST_NEIGHBOR = 0;
    public final static int INTERPOLATION_BILINEAR = 1;
    public final static int INTERPOLATION_BICUBIC = 2;
    private Object Interpolation;

    public int videoState;
    public String filepath;

    public JVideo(String filepath){
        this.videoState = STATE_IDLE;
        this.vsl = null;
        this.filepath = filepath;
        setVideoInterpolation(INTERPOLATION_BILINEAR);
    }
    public void setStateListener(videoStateChangeListener vsl){
        this.vsl = vsl;
    }
    public void stateCallBack(int state){
        videoState = state;
        if(vsl!=null)
            vsl.videoStateChange(videoState);
    }

    //设置视频缩放质量
    public void setVideoInterpolation(int quality){
        switch(quality){
            case INTERPOLATION_NEAREST_NEIGHBOR:
                 Interpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                break;
            case INTERPOLATION_BILINEAR:
                 Interpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                break;
            case INTERPOLATION_BICUBIC:
                 Interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                break;
        }
    }

    public abstract void start();
    public abstract void stop();
    public abstract void pause();
    public abstract void resume();
    public abstract void setVideoWH(int w,int h);//重新设置视频大小
    public abstract int getVideoWidth();
    public abstract int getVideoHeight();
    public abstract void setTime(int second);
    public abstract int getSecTime();//返回当前播放的秒时间
    public abstract long getMicroTime();//返回当前播放的微秒时间
    public abstract int getTotalTime();//得到视频总时长，单位为秒
    public abstract void setVol(float vol);//设置音量0到1
    public abstract float getVol();
    public abstract void restart();//全部关闭后再启动
    public abstract void rewind();//回到初态

    //显示的图片自适应控件大小，用于窗口拉动时视频能够自动缩放
    protected void paintComponent(Graphics g) {
        ImageIcon icon = (ImageIcon) getIcon();
        //设置为双线性插值
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if(icon!=null){
            g.drawImage(icon.getImage(), 0, 0, getWidth(),getHeight(),
                    icon.getImageObserver());
        }
    }
}
