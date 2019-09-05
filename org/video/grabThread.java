package org.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

public class grabThread extends Thread{
    public int audioSampleRate,audioChannels,sampleFormat;
    public stoAudio sa;
    public stoVideo sv;
    public audioManager am;

    private videoThread vp;
    private audioThread ap;
    private videoView player;
    private FFmpegFrameGrabber fg;

    private volatile boolean run = false;
    private volatile boolean stopPlayFlag,waitSet;
    private volatile boolean vpEnd,apEnd;
    private volatile boolean clearEnd;

    public grabThread(FFmpegFrameGrabber fg, videoView p){
        this.fg = fg;
        this.player = p;
        this.am = p.am;
        this.sampleFormat = p.sampleFormat;
        this.audioSampleRate = p.audioSampleRate;
        this.audioChannels = p.audioChannels;
        initFlag();
        initFIFO();
        initThread();
    }

    private void setStopPlay0(){
        if(firstCheck()) return;
        sendThreadStopSignal();
        waitThreadEnd();
        close();
        if(util.DEBUG5)
            System.out.println("----------------音频和视频线程结束--------------------");
    }

    private void close() {
        am.close();
        try {
            fg.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        if(util.DEBUG5)
            System.out.println("*********************************************结束帧捕获线程"+
                    "\n----------------播放结束----------------");
    }

    private void setStopPlay1(){
        setStopPlay0();
        player.stateCallBack(JVideo.STATE_PLAYED);
    }
    public void setStopPlay(){
        setStopPlay0();
        player.stateCallBack(JVideo.STATE_STOP);
    }

    private boolean firstCheck() {
        if(player.videoState == JVideo.STATE_PAUSE) setResumePlay();
        if(player.videoState == JVideo.STATE_IDLE||
                player.videoState == JVideo.STATE_PLAYED||
                player.videoState == JVideo.STATE_STOP) return true;
        return false;
    }

    private void sendThreadStopSignal() {
        synchronized (this){
            vpEnd = false;
            apEnd = false;
            stopPlayFlag = true;
            vp.setStopPlay();
            vp.setLoopStop(true);
            ap.setStopPlay();
            ap.setLoopStop(true);
            vp.interrupt();
            ap.interrupt();
            notifyAll();
        }
    }

    private void waitThreadEnd() {
        synchronized (this){
            while(!vpEnd){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        synchronized (this){
            while(!apEnd){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setThreadRun(){
        if(player.videoState == JVideo.STATE_PLAYING) return;
        vp.start();
        ap.start();
        setGrabberStart();
        player.stateCallBack(JVideo.STATE_PLAYING);
    }


    public void setPausePlay() {
        synchronized (this){
            ap.setPausePlay(true);
            vp.setPausePlay(true);
        }
        player.stateCallBack(JVideo.STATE_PAUSE);
    }
    public void setResumePlay() {
        synchronized (this){
            ap.setPausePlay(false);
            vp.setPausePlay(false);
        }
        player.stateCallBack(JVideo.STATE_PLAYING);
    }


    private long sett1,sett2;
    private int newTime;
    public void setPlayTime(int sec) {
        //防止点太快
        sett2 = System.currentTimeMillis();
        if(sett2-sett1<500) return;
        else sett1 = sett2;

        if (player.videoState == JVideo.STATE_PAUSE) setResumePlay();
        if (player.videoState != JVideo.STATE_PLAYING) return;
        if(util.DEBUG_SETTIME)
            p("第二步，设置清空FIFO标志位和让捕捉线程进入等待的标志位");
        newTime = sec;
        if(newTime>player.totalTime-5) return;//防止前进到末尾
        clearEnd = false;
        setWaitSet(true);
        if(util.DEBUG_SETTIME)
            p("第三步，等待FIFO清空");
        synchronized (this) {
            while (!clearEnd) {
                try {
                    wait();//等待捕获线程清空FIFO。
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(util.DEBUG_SETTIME)
            System.out.println("第六步，清空FIFO成功，准备让线程进入新的状态");
        ap.setLoopStop(true);//暂停音频线程
        vp.setLoopStop(true);
        ap.interrupt();
        vp.interrupt();
        vp.waitWaitLoop();
        vp.setWaitLoop(false);
        vp.getWaitLoop();
        if(util.DEBUG_SETTIME)
            p("&&&&&&&&&&&视频线程进入等待");
        ap.waitWaitLoop();
        ap.setWaitLoop(false);
        if(util.DEBUG_SETTIME)
            p("&&&&&&&&&&&音频线程进入等待");
        if(util.DEBUG_SETTIME)
            System.out.println("第七步，线程成功进入新的状态，发送设置时间戳命令");
        setWaitSet(false);
    }

    public void vpEnd(){
        synchronized (this){
            vpEnd = true;
            notifyAll();
        }
    }
    public void apEnd(){
        synchronized (this){
            apEnd = true;
            notifyAll();
        }
    }
    public videoThread getVP() {
        return vp;
    }
    public audioThread getAP() {
        return ap;
    }


    @Override
    public void run(){
        synchronized (this){
            while(!run){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        startPlay();
    }


    private void initThread(){
        vp = new videoThread(sv,player,this);
        vp.setPriority(Thread.MAX_PRIORITY);
        ap = new audioThread(sa,this);
        ap.setPriority(Thread.MAX_PRIORITY);
    }

    private void clearEnd(){
        synchronized (this){
            clearEnd = true;
            notifyAll();
        }
    }
    private void setGrabberStart() {
        synchronized (this) {
            run = true;
            notifyAll();
        }
    }
    private void startPlay() {
        Frame curFrame = null;
        while(true){
            try {
                curFrame = fg.grabFrame();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
            //没有捕捉到帧表示视频结束了
            if(curFrame==null){
                if(util.DEBUG5)
                    System.out.println("帧捕捉结束");
                setStopPlay1();
                return;
            }
            //如果是图像，就添加到图像FIFO
            if(curFrame.image != null) {
                if(util.DEBUG4){
                    System.out.println("捕捉到视频帧,时间戳为"+fg.getTimestamp()+"微秒");
                }
                /**
                 * 缓冲区太大会占内存。
                 *太小会造成卡顿
                 *为了防止太小卡顿，生产延时只发生在满足最小值20的条件下设置上限50
                 */
                while((sv.getCur()>50) && (sa.getCur()>20)&&(!stopPlayFlag) ) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sv.produce(new videoFrame(curFrame,fg.getTimestamp()));
            }
            //如果是音频，就添加到音频FIFO
            if(curFrame.samples!=null){
                if(util.DEBUG4){
                    System.out.println("捕捉到音频帧,时间戳为"+fg.getTimestamp()+"微秒");
                }
                while((sa.getCur()>50) && (sv.getCur()>20) && (!stopPlayFlag)){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sa.produce(new audioFrame(curFrame,fg.getTimestamp(),am));
            }
            //如果有设置时间的信号就清空FIFO后设置新的时间
            synchronized (this){
                if(waitSet){
                    if(util.DEBUG_SETTIME)
                        p("第四步，有清空FIFO的信号");
                    sv.clearFIFO();
                    sa.clearFIFO();
                    clearEnd();
                    if(util.DEBUG_SETTIME)
                        p("第五步，清空FIFO完成，发送结束信号，进入等待线程待命");
                    synchronized (this) {//这里是等待FIFO清空完毕才去设置新的时间戳
                        while (waitSet)
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    }
                    if(util.DEBUG_SETTIME)
                        p("第八步，线程待命完成，准备设置时间戳");
                    try {
                        fg.setTimestamp((long)newTime*1000000L,true);
                        curFrame = fg.grabSamples();//捕获音频帧
                        if(curFrame!=null)
                            sa.produce(new audioFrame(curFrame,fg.getTimestamp(),am));
                        else{
                            if(util.DEBUG_SETTIME)
                                System.out.println("设置时间失败，没有音频帧");
                        }
                        if(util.DEBUG_SETTIME)
                            System.out.println("第九步，设置新时间戳成功"+newTime+"秒"+"实际第"+fg.getTimestamp()+"微秒");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (this){
                if(stopPlayFlag){
                    return;
                }
            }
        }

    }

    private synchronized void setWaitSet(boolean f){
          waitSet = f;
          notifyAll();
    }


    private void initFlag(){
        stopPlayFlag = false;
        waitSet = false;
    }
    private void initFIFO() {
        sa = new stoAudio();
        sv = new stoVideo();
    }

    private void p(String s){
        System.out.println(s);
    }

}
