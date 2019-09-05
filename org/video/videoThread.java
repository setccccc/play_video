package org.video;


public class videoThread extends Thread{
    private stoVideo sg;
    private videoFrame mv;
    private videoView player;
    private grabThread gb;
    private volatile boolean run, loopStopFlag, pausePlay,stopPlay;
    public videoThread(stoVideo sg, videoView player,grabThread gb){
        this.sg = sg;
        this.player = player;
        this.gb = gb;
        initValue();
    }

    private void initValue(){
        this.run = false;
        this.preTime = 0;
        this.nextTime = 0;
        this.loopStopFlag = false;
        this.pausePlay = false;
        this.stopPlay = false;
        this.waitloop = false;
    }

    private volatile boolean waitloop;//设置时间戳用的标志位
    public synchronized void waitWaitLoop(){
        while(!waitloop){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public synchronized boolean getWaitLoop(){
        return waitloop;
    }
    public synchronized void setWaitLoop(boolean flag){
        waitloop = flag;
        notifyAll();
    }

    public synchronized  void setPausePlay(boolean flag){
        pausePlay = flag;
        notifyAll();
    }
    public synchronized void setLoopStop(boolean flag){
            this.loopStopFlag = flag;
            notifyAll();
    }
    public synchronized  void setStopPlay(){
            stopPlay = true;
            notifyAll();
    }
    private long preTime,nextTime;
    public synchronized void setRun(long preTime,long nextTime){
            this.preTime = preTime;
            this.nextTime = nextTime;
            if(util.DEBUG3)  p("设置当前音频帧"+preTime+"下一帧音频"+nextTime+"     ");
            run = true;
            notifyAll();
    }

    private void displayImage(){
        player.microsec = mv.timeStamp;
        player.setIcon(mv.imgIcon);
    }
    @Override
    public void run() {
        while (!stopPlay) {
            //设置时间戳后会回到这里，需要重新初始化一些值
            this.run = false;  this.preTime = 0;  this.nextTime = 0;
            mv = sg.consume();
            if(mv==null) {
                setWaitLoop(true);
                continue;
            }
            displayImage();
            while (!loopStopFlag) {
                checkPause();
                checkRun();
                if (!loopStopFlag) gb.getAP().setSleepFlag(true);
                do {
                    skipFrame();
                    /**防止赶上pre帧时也不小心超过了next帧，这发生在两个音频帧之间无视频帧的时候**/
                    if ((mv.timeStamp > nextTime) && (!loopStopFlag)) break;
                    frameDelayRough();
                    displayImage();
                    preTime = mv.timeStamp;
                    if (!loopStopFlag) {
                        mv = sg.consume();
                        if(mv==null){//当音视频正好在consume中wait时，设置时间导致consume中的wait中断
                            break;
                        }
                    }
                } while ((mv.timeStamp <= nextTime) && (!loopStopFlag));
                if (!loopStopFlag)   gb.getAP().setSleepFlag(false);
            }
            if(util.DEBUG5)  p("视频退出循环，等待重新启动");
            checkStop();
            setLoopStop(false);
            if(util.DEBUG5)  p("///////////////////视频等待结束");
            setWaitLoop(true);//线程进入等待
        }
    }

    private void checkStop() {
        synchronized (this){
            if(stopPlay){
                gb.vpEnd();
                if(util.DEBUG5)  p("*********************************************视频线程结束");
                return;
            }
        }
    }


    private void frameDelayRough() {
        double usTime =  (double) (mv.timeStamp - preTime);
        int msTime = (int)(usTime / 1000.0);
        if (util.DEBUG3)
            p("  curG  " + mv.timeStamp + "  preA  " + preTime + "  nextA  " + nextTime + "    sleep   " + msTime + "ms");
        if(msTime>=0){
            try {
                Thread.sleep(msTime);
            } catch (InterruptedException e) {
                if(util.DEBUG5)
                    p("视频线程被中断");
            }
        }else{
            if(util.DEBUG5)
                p("不延时，直接播放下一帧视频，本来需要延时" + msTime + "ms");
        }
    }


    private void frameDelay() {
        //延时微秒时间
        double usTime = (double) (mv.timeStamp - preTime);
        //转毫秒
        int msTime = (int)(usTime / 1000.0);
        //转毫秒后误差的微秒
        double usDiff = usTime - msTime * 1000;
        //误差的微秒转纳秒
        int nanoTime = (int) (usDiff * 1000);
        if (util.DEBUG3)
            p("  curG  " + mv.timeStamp + "  preA  " + preTime + "  nextA  " + nextTime + "    sleep   " + msTime + "ms" + nanoTime +"ns");
        if (msTime >= 0){
            try {
                Thread.sleep(msTime, nanoTime);
            } catch (InterruptedException g) {
                if(util.DEBUG5)
                    p("视频线程被中断");
            }
        }else {
            if(util.DEBUG5)
                p("不延时，直接播放下一帧视频，本来需要延时" + msTime + "ms");
        }
    }

    private void skipFrame() {
        synchronized (this) {
            /**
             * 视频帧不在当前音频帧和下一帧音频帧之间，所以需要丢弃
             */
            while ((mv.timeStamp < preTime) && (!loopStopFlag)) {
                if(util.DEBUG5)
                    p("视频帧落后了" + mv.timeStamp+"us");
                if (!loopStopFlag)
                    mv = sg.consume();
            }
        }
    }

    private void checkRun() {
        synchronized (this) {
            while (!run) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    if(util.DEBUG5)
                        p("视频线程被中断");
                }
                if (loopStopFlag) {
                    break;
                }
            }
            run = false;
        }
    }

    private void checkPause() {
        synchronized (this) {
            while ((pausePlay) && (!loopStopFlag)) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void p(String s){
        System.out.println(s);
    }
}
