package org.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;

/**
 *  纯音乐播放
 */
public class audioView extends JVideo {
    private FFmpegFrameGrabber fg;
    private audioManager am;
    private int sampleFormat,audioSampleRate,audioChannels;
    private boolean hasCover = false;
    private ImageIcon coverImg = null;
    private int coverW,coverH;
    private int totalTime;
    private Thread ap;
    private boolean settimeFlag;
    private long microsec,microsec2;
    private Frame audioFrame;
    private volatile boolean stopFlag,stopOK;//停止信号
    private volatile boolean pauseFlag;

    public audioView(String filepath,boolean hasCover){
        super(filepath);
        this.hasCover = hasCover;
        loadMusic();
    }

    private void loadMusic() {
        initGrabber();
        this.am = new audioManager(sampleFormat,audioSampleRate,audioChannels);
        initThread();
        if(hasCover){
            setImg();
        }
    }

    private void setImg() {
        setSize(coverW,coverH);
        setIcon(coverImg);
    }

    private void initThread() {
        settimeFlag = false;
        stopFlag = false;
        stopOK = false;
        pauseFlag = false;
        ap = new Thread(new Runnable() {
            @Override
            public void run() {
                audioProcess();
            }
        });
        ap.setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public void start() {
        if(videoState == JVideo.STATE_PLAYING) return;
        ap.start();
        stateCallBack(JVideo.STATE_PLAYING);
    }

    private boolean firstCheck() {
        if(videoState == JVideo.STATE_PAUSE) resume();//如果是暂停状态则先恢复再停止
        if(videoState == JVideo.STATE_PLAYED||
                videoState == JVideo.STATE_IDLE) return true;
        return false;
    }

    @Override
    public void stop() {
        if(firstCheck())return;
        stopFlag = true;
        //等待线程停止
        synchronized (this){
            while(!stopOK){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        close2();
    }

    private void close() {
        try {
            fg.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        am.close();
        if(util.DEBUG5)
            p("**************音频播放停止****************");
    }
    private void close1(){
        close();
        stateCallBack(JVideo.STATE_PLAYED);
    }
    private void close2(){
        close();
        stateCallBack(JVideo.STATE_STOP);
    }

    @Override
    public void pause() {
        pauseFlag = true;
        stateCallBack(JVideo.STATE_PAUSE);
    }

    @Override
    public void resume() {
        synchronized (this){
            pauseFlag = false;
            notifyAll();
        }
        stateCallBack(JVideo.STATE_PLAYING);
    }

    @Override
    public void setVideoWH(int w, int h) {
        try {
            FFmpegFrameGrabber fg2 = new FFmpegFrameGrabber(filepath);
            fg2.start();
            fg2.setImageWidth(w);
            fg2.setImageHeight(h);
            setSize(w,h);
            coverImg = new ImageIcon(new Java2DFrameConverter().getBufferedImage(fg2.grabImage()));
            this.coverW = w;
            this.coverH = h;
            setIcon(coverImg);
            fg2.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getVideoWidth() {
        return coverW;
    }

    @Override
    public int getVideoHeight() {
        return coverH;
    }

    @Override
    public void setTime(int second) {
        if(videoState == JVideo.STATE_PAUSE) resume();
        if(videoState != JVideo.STATE_PLAYING) return;
        settimeFlag = true;
        microsec = ((long)second)*(long)1000000;
    }

    @Override
    public int getSecTime() {
        return (int) (microsec2/1000000);
    }

    @Override
    public long getMicroTime() {
        return microsec2;
    }

    @Override
    public int getTotalTime() {
        return totalTime;
    }

    @Override
    public void setVol(float vol) {
        am.vol = vol;
    }

    @Override
    public float getVol() {
        return am.vol;
    }

    @Override
    public void restart() {
        //未实现
    }

    @Override
    public void rewind() {
        try {
            fg.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        initThread();
        am.start();
        start();
    }

    private void initGrabber() {
        fg = new FFmpegFrameGrabber(filepath);
        try {
            fg.start();
            if(hasCover){
                coverImg = new ImageIcon(new Java2DFrameConverter().getBufferedImage(fg.grabImage()));
                coverW = coverImg.getIconWidth();
                coverH = coverImg.getIconHeight();
            }
            sampleFormat = fg.getSampleFormat();
            audioSampleRate = fg.getSampleRate();
            audioChannels = fg.getAudioChannels();
            totalTime = (int) (fg.getLengthInTime()/1000000);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    private void audioProcess() {
        while(true){
            synchronized (this){
                if(stopFlag){
                     stopOK = true;
                     notifyAll();
                     if(util.DEBUG5)
                        p("*************接收到停止信号,音频线程结束*****************");
                    return;
                }
                if(settimeFlag){
                    try {
                        fg.setTimestamp(microsec);
                    } catch (FrameGrabber.Exception e) {
                        e.printStackTrace();
                    }
                    settimeFlag = false;
                }
                while(pauseFlag){
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                //读取
                audioFrame = fg.grabSamples();
                //判断是否到末尾
                if(audioFrame==null){
                    if(util.DEBUG5)
                        p("*************到末尾，音频线程结束*****************");
                    close1();
                    return;
                }
                //处理
                am.processAudioFrame(audioFrame);
                //写入
                microsec2 = audioFrame.timestamp;
                am.writeData(null);
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void p(String s){
        System.out.println(s);
    }
}
