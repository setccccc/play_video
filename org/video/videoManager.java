package org.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;


public class videoManager {
    private final static int TYPE_VIDEO = 0;
    private final static int TYPE_AUDIO = 1;
    private final static int TYPE_AUDIO_COVER = 2;
    private final static int TYPE_IMAGE = 3;
    private final static int TYPE_IMAGES = 4;//无声音视频或者gif
    private final static int TYPE_ERROR = 5;
    private static int fileType = -1;


    public static JVideo getInstance(String filepath){
        checkType(filepath);
        switch (fileType)
        {
            case TYPE_IMAGE:
                   return new imageView(filepath,false);
            case TYPE_IMAGES:
                   return new imageView(filepath,true);
            case TYPE_AUDIO:
                   return new audioView(filepath,false);
            case TYPE_AUDIO_COVER:
                   return new audioView(filepath,true);
            case TYPE_VIDEO:
                   return new videoView(filepath);
        }
        return null;
    }
    private static void checkType(String filepath){
        FFmpegFrameGrabber fg = new FFmpegFrameGrabber(filepath);
        try {
            fg.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        Frame f = null;
        //先判断是不是图像
        if(fg.getAudioStream()==-1 && fg.getVideoStream()>=0){
            //这个可能是无声音的视频或者一张图像
            try {
                //连续监测两帧
                f = fg.grabImage();
                f = fg.grabImage();
                if(f!=null){
                    p("是无声音视频或者gif");
                    fileType =TYPE_IMAGES;
                }else{
                    p("是图像");
                    fileType = TYPE_IMAGE;
                }
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }else if(fg.getVideoStream()==-1 && fg.getAudioStream()>=0){//应该是无封面的音乐文件
            fileType = TYPE_AUDIO;
        } else if (fg.getVideoStream() >= 0 && fg.getAudioStream() >= 0) {
            //连续检测两帧
            try {
                f = fg.grabImage();
                f = fg.grabImage();
                if(f!=null){
                    p("是视频");
                    fileType = TYPE_VIDEO;
                }else{
                    p("是有封面的音乐");
                    fileType = TYPE_AUDIO_COVER;
                }
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }else{
            p("不知道什么文件");
            fileType = TYPE_ERROR;
        }
        try {
            fg.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }
    private static void p(String s){
        System.out.println(s);
    }
}
