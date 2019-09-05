package org.video;


import javax.sound.sampled.SourceDataLine;

/**
 * 音频播放线程
 */
public class audioThread extends Thread{
    private stoAudio sa;
    private audioFrame curf,nextf;
    private SourceDataLine sourceDataLine;
    private long curTimeStamp;
    private grabThread gb;
    private int lineBufSize;
    private int highLimit;
    private volatile boolean stopPlay, pausePlay, loopStopFlag,sleepFlag;
    public audioThread(stoAudio sa,grabThread gb){
        this.sa = sa;
        this.gb = gb;
        this.sourceDataLine = gb.am.sourceDataLine;
        initValue();
    }

    private void initValue(){
        this.curTimeStamp = 0;
        this.stopPlay = false;
        this.pausePlay = false;
        this.loopStopFlag = false;
        this.sleepFlag  = false;
        this.waitloop = false;
        lineBufSize = sourceDataLine.getBufferSize();
        highLimit = lineBufSize*9/10;
    }

    public synchronized void setStopPlay(){
            stopPlay = true;
            notifyAll();
    }

    public synchronized void setPausePlay(boolean flag){
            pausePlay = flag;
            notifyAll();
    }

    public synchronized void setLoopStop(boolean flag){
            this.loopStopFlag = flag;
            notifyAll();
    }

    public synchronized void setSleepFlag(boolean f){
        sleepFlag = f;
        notifyAll();
    }

    private volatile boolean waitloop;//设置时间戳用的
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

    public long getCurTimeStamp(){
        synchronized (this){
            return curTimeStamp;
        }
    }


    long c1 = 0,c2 = 0;//如果音频缓冲区看了，可能写入发生堵塞所以需要额外计算耗费的时间??
    long nextTimeStamp;
    long factor = 0;
    @Override
    public void run() {
        while (!stopPlay) {
            this.sleepFlag = false;
            curf = sa.consume();
            if(curf==null) {//当音视频正好在consume中wait时，设置时间导致consume中的wait中断
                setWaitLoop(true);
                continue;
            }
            curTimeStamp = curf.timeStamp;//
            c1 = System.currentTimeMillis();
            while(!loopStopFlag){
                checkPause();
                nextf = sa.consume();
                if(nextf==null){//当音视频正好在consume中wait时，设置时间导致consume中的wait中断
                    break;
                }
                nextTimeStamp = nextf.timeStamp;
                checkSleep();
                //通知视频线程当前和下一个音频帧的时间戳情况
                gb.getVP().setRun(curTimeStamp, nextTimeStamp);
                //调整帧间延时
                setFactor();
                //写入当前帧数据
                writeData();
                //帧间延时
                frameDelayRough();
                curf = nextf;
                curTimeStamp = nextTimeStamp;
        }
        if(util.DEBUG5)
            p("音频退出循环，等待重新启动");
        synchronized (this){
            if(stopPlay){
                gb.apEnd();
                if(util.DEBUG5)
                    p("*********************************************音频线程结束");
                return;
            }
               setLoopStop(false);
               if(util.DEBUG5)
                    p("///////////////////音频等待结束");
               setWaitLoop(true);
        }
    }
    }

    private void frameDelayRough(){
        double usTime = (double) (nextTimeStamp - curTimeStamp + factor);
        int msTime = (int)(usTime / 1000.0);
        if (msTime >= 0){
            try {
                Thread.sleep(msTime);//其实我觉得可以直接传入msTime???
            } catch (InterruptedException g) {
                if(util.DEBUG5)
                    p("音频线程被中断");
            }
        }
        else {
            if(util.DEBUG5)
                p("不延时，直接播放下一帧音频，本来需要延时" + msTime + "ms");
        }
    }

     private void frameDelay() {
        //延时微秒时间
        double usTime = (double) (nextTimeStamp - curTimeStamp + factor);
        //转毫秒
        int msTime = (int)(usTime / 1000.0);
        //转毫秒后误差的微秒
        double usDiff = usTime - msTime * 1000;
        //误差的微秒转纳秒
        int nanoTime = (int) (usDiff * 1000);
        if (msTime >= 0){
            try {
                Thread.sleep(msTime, nanoTime);//其实我觉得可以直接传入msTime???
            } catch (InterruptedException g) {
                if(util.DEBUG5)
                    p("音频线程被中断");
            }
        }
        else {
            if(util.DEBUG5)
                p("不延时，直接播放下一帧音频，本来需要延时" + msTime + "ms");
        }
    }

    private void checkSleep() {
        synchronized (this) {
            while (sleepFlag) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (loopStopFlag) {
                    break;
                }
            }
            sleepFlag = true;
            notifyAll();
        }
    }

    private void checkPause() {
        synchronized (this) {
            while (pausePlay) {
                try {
                    sourceDataLine.flush();
                    wait();
                } catch (InterruptedException e) {
                    if(util.DEBUG5)
                        p("音频线程被中断");
                }
            }
        }
    }

    private void setFactor() {
        if(util.DEBUG1)
            p("Line内部缓冲区可写入"+sourceDataLine.available()+"字节内部缓冲区总大小"+sourceDataLine.getBufferSize()+"字节");
        if(sourceDataLine.available()> (lineBufSize-100)) {
            if(util.DEBUG5)
                p("音频卡了，因为缓冲区快空了，需要数据填充");
        }
        if(sourceDataLine.available()> highLimit){
            factor -= 1000;//这是1ms
            //p("+++++++++++++++++++++++++++++++++调整因子为"+factor);
        }else{
            factor = 0;
        }
    }

    private void writeData(){
        gb.am.writeData(curf);
    }

    private void p(String s){
        System.out.println(s);
    }
}
