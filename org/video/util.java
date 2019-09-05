package org.video;


import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class util {
    /**
     * 打印音频缓冲区情况
     */
    public static boolean DEBUG1 = false;
    /**
     * 打印生产消费情况
     */
    public static boolean DEBUG2 = false;
    /**
     * 打印音视频同步情况
     */
    public static boolean DEBUG3 = false;
    /**
     * 打印帧捕捉情况
     */
    public static boolean DEBUG4 = false;
    /**
     * 打印其他信息
     */
    public static boolean DEBUG5 = false;

    /**
     * 打印设置时间戳过程
     */
    public static boolean DEBUG_SETTIME = false;

    //选择文件
    public static String getFile(){
        if(File.separator.equals("\\")){
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());//设置为Window风格文件管理器
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        JFileChooser jfc = new JFileChooser(".");
        int retVal = jfc.showOpenDialog(null);
        //恢复风格
        try {
            UIManager.setLookAndFeel(javax.swing.plaf.metal.MetalLookAndFeel.class.getName());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        //如果有选择就返回文件路径，否则返回null
        if(retVal == JFileChooser.APPROVE_OPTION){
            return jfc.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public static String getFileName(String path){
        return path.substring(path.lastIndexOf(File.separator)+1);
    }

    public static ByteBuffer shortToByteValue(ShortBuffer arr, float vol) {
        int len  = arr.capacity();
        ByteBuffer bb = ByteBuffer.allocate(len * 2);
        for(int i = 0;i<len;i++){
            bb.putShort(i*2,(short)((float)arr.get(i)*vol));
        }
        return bb; // 默认转为大端序
    }
    public static ByteBuffer floatToByteForm(FloatBuffer arr) {
        //这个函数仅仅将float数据转为了float的字节代表形式，不代表float的值。
        ByteBuffer bb = ByteBuffer.allocate(arr.capacity() * 4);
        bb.asFloatBuffer().put(arr);
        return bb;
    }

    public static ByteBuffer floatToByteValue(FloatBuffer arr,float vol){
        int len = arr.capacity();
        float f,v;
        ByteBuffer res = ByteBuffer.allocate(len*2);
        v = 32768.0f*vol;
        for(int i=0;i<len;i++){
            f = arr.get(i)*v;
            if(f>v) f = v;
            if(f<-v) f = v;
            //默认转为大端序
            res.putShort(i*2,(short)f);//乘以2是因为一次写入两个字节。
        }
        return res;
    }


    public static ImageIcon getBlackImg(int w,int h){
        return new ImageIcon(new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB));
    }


    public static String getTimeString(int timesec){
        return timeConvert(timesec/3600)+":"+timeConvert(timesec/60-timesec/3600*60)+":"+timeConvert(timesec%60);
    }

    public static String getTimeString(long timeUS){
        return getTimeString((int)(timeUS/1000000));
    }
    private static String timeConvert(long time){
        String str = String.valueOf(time);
        return str.length()==1? "0"+str:str;
    }

    //可以加入其他处理比如添加水印等，用opencv的api就可以
    public static ImageIcon processImage(Frame frame) {
        return new ImageIcon((new Java2DFrameConverter()).getBufferedImage(frame));
    }
}
