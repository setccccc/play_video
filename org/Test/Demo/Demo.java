package org.Test.Demo;

import org.video.JVideo;
import org.video.util;
import org.video.videoManager;
import org.video.videoStateChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalSliderUI;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.*;

/**
 * 视频播放器Demo
 */
public class Demo extends MouseAdapter implements ActionListener,dropFileListener, ChangeListener, videoStateChangeListener {
    public Demo(){
        //初始化JFrame
        initJFrame();
        initTimer();
        //初始化后video播放控件
        initVideoComponent();
        initOther();
        jf.pack();
        jf.setSize(1024,900);
    }

    //时间显示
    private void initTimer() {
        new Timer(100, new ActionListener() {
            int oldValue;
            int sec;
            @Override
            public void actionPerformed(ActionEvent e) {
                if(playState == JVideo.STATE_PLAYING){
                    sec = player.getSecTime();
                    if(sec!=oldValue){
                        oldValue = sec;
                        jsl.setValue(sec);
                        displayTime(sec,jsl.getMaximum());
                    }
                }
            }
        }).start();
    }

    private JFrame jf;
    private int contentWidth;
    private int gap2;//gap2+contentWidth = jf.getWidth()
    private float contentWidthf;
    public int jfWidth = 1024;
    public int jfHeight = 900;
    private void initJFrame() {
        jf = new JFrame("");
        jf.setSize(jfWidth,jfHeight);
        jf.setLocationRelativeTo(null);
        jf.setLayout(null);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setVisible(true);
        //得到JFrame边界和面板大小信息，注意需要先setVisible
        Insets frameInset = jf.getInsets();
        contentWidth = jf.getContentPane().getSize().width;
        contentWidthf = (float)contentWidth;
        String os = System.getProperty("os.name");
        if(os.toLowerCase().startsWith("win")){
            gap2 = jf.getWidth()-contentWidth;
        }else{
            gap2 = 0;
        }
        jp = new JPanel();
        jp.setBounds(0,0,contentWidth,jfHeight);
        jp.setLayout(null);
        jf.add(jp);
    }

    private JPanel jp;
    private JVideo player = null;
    private JPanel videoPanel = null;
    private void initVideoComponent() {
        videoPanel = new JPanel();
        videoPanel.setBackground(Color.BLACK);
        videoPanel.setLayout(null);
        videoPanel.setBounds(0,0,contentWidth,700);
        installDragFiles(videoPanel);
        jp.add(videoPanel);
    }

    private JButton start,stop,forward,backward,load;
    private JCheckBox recycle;//循环播放勾选按钮
    private JSlider jsl,volAdj;
    private JLabel jl;
    private JComboBox<String> jcb;
    private void initOther() {
        Font font = new Font("宋体",Font.PLAIN,20);
        start = new JButton("开始");
        start.setBounds(50,700+20,100,40);
        start.addActionListener(this);
        start.setFont(font);
        jp.add(start);


        stop = new JButton("停止");
        stop.setBounds(170,700+20,100,40);
        stop.addActionListener(this);
        stop.setFont(font);
        jp.add(stop);

        forward = new JButton("前进");
        forward.setBounds(290,700+20,100,40);
        forward.addActionListener(this);
        forward.setFont(font);
        jp.add(forward);

        backward = new JButton("后退");
        backward.setBounds(410,700+20,100,40);
        backward.addActionListener(this);
        backward.setFont(font);
        jp.add(backward);

        recycle = new JCheckBox("循环");
        recycle.setBounds(530,700+20,100,40);
        recycle.setFont(font);
        jp.add(recycle);
        //时间条
        jsl = new JSlider(0,100,0);
        jsl = new JSlider();
        jsl.setBounds(50,700+70,630,20);

        jsl.addMouseListener(this);
        jsl.addMouseMotionListener(this);
        jp.add(jsl);
        volAdj = new JSlider(0,100,100);
        volAdj.setBounds(700,700+70,200,20);
        volAdj.setUI(new MetalSliderUI(){
            @Override
            protected void scrollDueToClickInTrack(int direction){
                int value = slider.getValue();
                if(slider.getOrientation()==JSlider.HORIZONTAL){
                    value = this.valueForXPosition(slider.getMousePosition().x);
                }else{
                    value = this.valueForYPosition(slider.getMousePosition().y);
                }
                slider.setValue(value);
            }
        });
        volAdj.addChangeListener(this);

        jp.add(volAdj);
        load = new JButton("视频载入");
        load.setBounds(50,700+100,200,40);
        load.addActionListener(this);
        load.setFont(font);
        jp.add(load);

        jl = new JLabel();
        jl.setFont(font);
        jl.setBounds(280,700+100,250,40);
        displayTime(0,0);
        jp.add(jl);

        jf.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                JFrame window = (JFrame) e.getComponent();
                Dimension d = window.getSize();
                jp.setSize(d);
                videoPanel.setSize(d.width-gap2,d.height-200);
                if(player!=null){
                    adjSize(videoPanel.getSize());
                }
                start.setBounds(50,videoPanel.getHeight()+20,100,40);
                stop.setBounds(170,videoPanel.getHeight()+20,100,40);
                forward.setBounds(290,videoPanel.getHeight()+20,100,40);
                backward.setBounds(410,videoPanel.getHeight()+20,100,40);
                recycle.setBounds(530,videoPanel.getHeight()+20,100,40);
                jsl.setBounds(50,videoPanel.getHeight()+70,630,20);
                volAdj.setBounds(700,videoPanel.getHeight()+70,200,20);
                load.setBounds(50,videoPanel.getHeight()+100,200,40);
                jl.setBounds(280,videoPanel.getHeight()+100,250,40);
                jp.updateUI();
            }
        });
    }

    public void displayTime(int value,int maxvalue){
        jl.setText(util.getTimeString(value)+" / "+util.getTimeString(maxvalue));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        if(obj.equals((Object)start)){
            startEvent();
            return;
        }
        if(obj.equals((Object)stop)){
            stopEvent();
            return;
        }
        if(obj.equals((Object)forward)){
            forwardEvent();
            return;
        }
        if(obj.equals((Object)backward)){
            backwardEvent();
            return;
        }
        if(obj.equals((Object)load)){
            loadEvent();
            return;
        }

    }

    private int playState = JVideo.STATE_IDLE;
    private void startEvent() {
        if(player!=null){
            if(playState == JVideo.STATE_PAUSE)
                player.resume();
            else if(playState == JVideo.STATE_PLAYING)
                player.pause();
            else if(playState == JVideo.STATE_IDLE)
                player.start();
            else if(playState == JVideo.STATE_PLAYED){
                player.rewind();
            }else if(playState == JVideo.STATE_STOP){
                player.rewind();
            }
        }
    }

    private void stopEvent() {
        if(player!=null){
            player.stop();
        }
    }
    private void forwardEvent() {
        if(player!=null){
            int cursec = player.getSecTime();
            cursec+=12;//一次快进12s
            player.setTime(cursec);
        }
    }
    private void backwardEvent() {
        if(player!=null){
            int cursec = player.getSecTime();
            cursec-=12;//一次后退12秒
            if(cursec<0){
                cursec = 0;
            }
            player.setTime(cursec);
        }
    }
    private void loadEvent(){
        String filepath = util.getFile();
        loadEvent(filepath);
    }

    public void loadEvent(String filepath) {
        float preVol = 1.0f;
        if(filepath!=null){
            if(videoPanel.getComponentCount()>0){
                //需要关闭之前的
                preVol = player.getVol();
                player.stop();
                videoPanel.removeAll();
            }
            player = videoManager.getInstance(filepath);
            if(player == null) return;
            player.setStateListener(this);
            //调整player控件大小
            adjSize(new Dimension(videoPanel.getSize()));
            //重新设置JSlider的值
            jsl.setMaximum(player.getTotalTime());
            jsl.setValue(0);
            videoPanel.add(player);
            videoPanel.setBackground(Color.BLACK);
            videoPanel.updateUI();
            jf.setTitle(util.getFileName(filepath));
            player.start();
            player.setVol(preVol);
        }
    }

    private void adjSize(Dimension d) {
        int vw = player.getVideoWidth();
        int vh = player.getVideoHeight();
        if(vh>0 && vw>0 && ((double)vw/(double)vh)>((double)d.width/(double)d.height)){
            player.setLocation(0,(d.height-d.width*vh/vw)/2);
            player.setSize(new Dimension(d.width,d.width*vh/vw));
        }else if(vh>0 && vw>0){
            player.setLocation((d.width-d.height*vw/vh)/2,0);
            player.setSize(new Dimension(d.height*vw/vh,d.height));
        }
        player.updateUI();
    }


    public static void main(String[] args){
        if(args.length>0){
            new Demo().loadEvent(args[0]);
        }else{
            new Demo();
        }
    }

    private void p(String s){
        System.out.println(s);
    }


    //JSlider设置
    @Override
    public void mouseReleased(MouseEvent e) {
        jsl.setValue(setsec);
        if(player!=null){
            if(util.DEBUG_SETTIME)
            p("第一步，松开,设置时间");
            player.setTime(setsec);
        }
    }

    private int setsec;
    @Override
    public void mouseMoved(MouseEvent e){
        JSlider jsl = (JSlider)e.getSource();
        if(jsl.getMousePosition()==null)return;
        setsec = (int)(jsl.getMousePosition().getX()*jsl.getMaximum()/jsl.getWidth());
        jsl.setToolTipText(util.getTimeString((long)((double)setsec*1000000.0)));
        //p("移动"+setsec);
    }
    @Override
    public void mouseDragged(MouseEvent e){
        JSlider jsl = (JSlider)e.getSource();
        setsec = jsl.getValue();
        //p("拖拽"+setsec);
    }

    private void installDragFiles(Component c) {
        dropE de = new dropE();
        de.setDropFileListener(this);
        new DropTarget(c, DnDConstants.ACTION_COPY_OR_MOVE,de);
    }

    @Override
    public void dropFile(String filepath) {
        loadEvent(filepath);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if(player!=null){
            int value = ((JSlider)e.getSource()).getValue();
            float vol = (float)value/100.0f;
            player.setVol(vol);
        }
    }


    @Override
    public void videoStateChange(int state) {
        playState = state;
        //p("状态是"+playState);
        switch (playState)
        {
            case JVideo.STATE_PLAYING:
                    start.setText("暂停");
                break;
            case JVideo.STATE_PAUSE:
                    start.setText("开始");
                break;
            case JVideo.STATE_PLAYED:
                    start.setText("开始");
                    if(recycle.isSelected())
                        player.rewind();
                break;
            case JVideo.STATE_STOP:
                    start.setText("开始");
                break;
        }
    }
}
