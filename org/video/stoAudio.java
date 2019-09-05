package org.video;

import java.util.ArrayList;


public class stoAudio {
    private final int MAX = 500;
    private ArrayList<audioFrame> list = new ArrayList<>();
    public void produce(audioFrame f){
        synchronized(list){
            while(list.size()+1>MAX){
                try{
                    if(util.DEBUG2)
                        System.out.println("音频缓冲区已满，需要等待消费");
                    list.wait();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            list.add(f);
            list.notifyAll();
        }
    }
    public audioFrame consume(){
        audioFrame f;
        synchronized (list){
            while(list.size()<1){
                try{
                    if(util.DEBUG2)
                        System.out.println("音频缓冲区为空，需要等待生产");
                    list.wait();
                }catch(Exception e){
                    e.printStackTrace();
                    if(util.DEBUG5)
                        System.out.println("音频FIFO中断异常");
                    return null;
                }
            }
            if(util.DEBUG2)
                System.out.println("音频缓冲区当前数量"+list.size());
            f = list.remove(0);
            list.notifyAll();
            return f;
        }
    }
    public int getMax(){
        return MAX;
    }
    public int getCur(){
        return list.size();
    }
    public void clearFIFO(){
        synchronized (list){
            list.clear();
        }
    }
}
