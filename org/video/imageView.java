package org.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;

/**
 * 图片，gif和无声音视频
 */
public class imageView extends JVideo {
    private FFmpegFrameGrabber fg;
    private ImageIcon img;
    private int imgwidth, imgheight;
    private long curtimestamp;
    private boolean noaudio;
    private int totalTime;
    private Thread vp;
    public imageView(String filepath,boolean noaudio){
        super(filepath);
        this.noaudio = noaudio;
        if(noaudio){
            loadVideo();
        }else{
            loadImg();
        }
    }

    private void loadVideo() {
        loadImg();
        this.stopFlag = false;
        this.stopOK = false;
        this.pauseFlag = false;
        vp = new Thread(new Runnable() {
            @Override
            public void run() {
                imageProcess();
            }
        });
    }

    private void imageProcess() {
        Frame frame = null;
        double fps = fg.getFrameRate();
        long sleepCnt = (int) (1000/fps);
        ImageIcon img;
        while(true){
            synchronized (this){
                if(stopFlag){
                    stopOK = true;
                    notifyAll();
                    if(util.DEBUG5)
                        p("*************接收到停止信号,图像线程结束*****************");
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
                frame = fg.grabImage();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
            if(frame!=null){
                curtimestamp = frame.timestamp;
                try {
                    Thread.sleep(sleepCnt);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                img = new ImageIcon((new Java2DFrameConverter()).getBufferedImage(frame));
                setIcon(img);
            }else{
                p("播放完毕");
                close1();
                return;
            }
        }
    }

    private void loadImg() {
        fg = new FFmpegFrameGrabber(filepath);
        try {
            fg.start();
            img = new ImageIcon((new Java2DFrameConverter()).getBufferedImage(fg.grabImage()));
            imgwidth = img.getIconWidth();
            imgheight = img.getIconHeight();
            totalTime = (int) (fg.getLengthInTime()/1000000L);
            setIcon(img);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        if(noaudio){
            if(videoState == JVideo.STATE_PLAYING) return;
            vp.start();
            stateCallBack(JVideo.STATE_PLAYING);
        }
    }

    private boolean firstCheck() {
        if(videoState == JVideo.STATE_PAUSE) resume();//如果是暂停状态则先恢复再停止
        if(videoState == JVideo.STATE_PLAYED||
                videoState == JVideo.STATE_IDLE) return true;
        return false;
    }

    private volatile boolean stopFlag,stopOK,pauseFlag;
    @Override
    public void stop() {
        if(noaudio){
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
                p("图像线程结束!!!");
            }
        }
        close2();
    }
    private void close(){
        //设置为黑色
        setIcon(util.getBlackImg(imgwidth, imgheight));
        try {
            fg.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        if(util.DEBUG5)
            p("**************图像播放停止****************");
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
        if(noaudio){
            pauseFlag = true;
            stateCallBack(JVideo.STATE_PAUSE);
        }
    }

    @Override
    public void resume(){
        if(noaudio){
            synchronized (this){
                pauseFlag = false;
                notifyAll();
            }
            stateCallBack(JVideo.STATE_PLAYING);
        }
    }

    @Override
    public void setVideoWH(int w, int h) {
        try {
            fg.restart();
            fg.setImageWidth(w);
            fg.setImageHeight(h);
            setSize(w,h);
            img = new ImageIcon(new Java2DFrameConverter().getBufferedImage(fg.grabImage()));
            this.imgwidth = w;
            this.imgheight = h;
            setIcon(img);
            fg.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getVideoWidth() {
        return imgwidth;
    }

    @Override
    public int getVideoHeight() {
        return imgheight;
    }


    private boolean settimeFlag;
    private long microsec;
    @Override
    public void setTime(int second) {
        if(noaudio){
            if(videoState == JVideo.STATE_PAUSE) resume();
            if(videoState != JVideo.STATE_PLAYING) return;
            settimeFlag = true;
            microsec = ((long)second)*(long)1000000;
        }
    }

    @Override
    public int getSecTime() {
        return (int) (curtimestamp/1000000L);
    }

    @Override
    public long getMicroTime() {
        return  curtimestamp;
    }

    @Override
    public int getTotalTime() {
        return totalTime;
    }

    @Override
    public void setVol(float vol) {}

    @Override
    public float getVol() {
        return 0;
    }

    @Override
    public void restart() {}

    @Override
    public void rewind() {
        p("图像重新开始啊");
        if(noaudio){
            loadVideo();
            start();
        }else{
            loadImg();
        }
    }

    private void p(String s){
        System.out.println(s);
    }
}
