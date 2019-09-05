package org.Test.simpleDemo;

import org.video.JVideo;
import org.video.videoManager;

import javax.swing.*;

/**
 *  单纯载入文件播放
 */
public class simpleDemo {

    public static void main(String[] args){
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //JVideo player = videoManager.getInstance("th.jpeg");
        //JVideo player = videoManager.getInstance("La.mp3");
        //JVideo player = videoManager.getInstance("a.flv");
        JVideo player = videoManager.getInstance("http://flashmedia.eastday.com/newdate/news/2016-11/shznews1125-19.mp4");
        jf.add(player);
        jf.setSize(player.getSize());
        jf.setLocationRelativeTo(null);//居中
        jf.setVisible(true);
        player.start();
    }
}
