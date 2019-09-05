package org.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import java.awt.Dimension;


public class videoView extends JVideo {
    private FFmpegFrameGrabber fg;
    private grabThread gb;

    public audioManager am;
    public int sampleFormat,audioSampleRate,audioChannels;
    public int totalTime;
    public String totalTimeString;
    public int videoW,videoH;
    public long microsec;

    public videoView(String filepath) {
        super(filepath);
        initGrabber();
        initAudio();
        initUI();
        initGrabThread();
    }

    private void initGrabThread() {
        gb = new grabThread(fg,this);
        gb.start();
    }

    private void initAudio() {
        this.am = new audioManager(sampleFormat,audioSampleRate,audioChannels);
    }

    private void initGrabber() {
        fg = new FFmpegFrameGrabber(filepath);
        try {
            fg.start();
            sampleFormat = fg.getSampleFormat();
            audioSampleRate = fg.getSampleRate();
            audioChannels = fg.getAudioChannels();
            totalTime = (int) (fg.getLengthInTime()/1000000);
            totalTimeString = util.getTimeString(totalTime);
            videoW = fg.getImageWidth();
            videoH = fg.getImageHeight();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI(){
        //JLabel默认是透明的，所以设置背景色是无效的，因此需要设置成不透明
        setOpaque(true);
        setPreferredSize(new Dimension(videoW,videoH));
        setSize(videoW,videoH);
    }

    @Override
    public void start() {
        gb.setThreadRun();
    }

    @Override
    public void stop() {
        gb.setStopPlay();
    }

    @Override
    public void pause() {
        gb.setPausePlay();
    }

    @Override
    public void resume() {
        gb.setResumePlay();
    }

    @Override
    public void setVideoWH(int w, int h) {
        fg.setImageWidth(w);
        fg.setImageHeight(h);
        setSize(w,h);
        videoW = w;
        videoH = h;
    }

    @Override
    public int getVideoWidth() {
        return videoW;
    }

    @Override
    public int getVideoHeight() {
        return videoH;
    }

    @Override
    public void setTime(int second) {
        gb.setPlayTime(second);
    }

    @Override
    public int getSecTime() {
        return (int) (microsec/1000000);
    }

    @Override
    public long getMicroTime() {
        return microsec;
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

    }

    @Override
    public void rewind() {
        p("视频重新开始");
        try {
            fg.start();
            //用之前的参数
            fg.setImageWidth(videoW);
            fg.setImageHeight(videoH);
            am.start();
            initGrabThread();//由于线程结束了重新start是不允许的，所以线程需要重新new
            start();
            stateCallBack(STATE_PLAYING);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    public int getSampleFormat() {
        return sampleFormat;
    }

    private void p(String s){
        System.out.println(s);
    }
}
