package org.Test.Demo;

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

/**
 * 拖拽文件到组件中播放
 */
interface dropFileListener {
    public void dropFile(String filepath);
}

public class dropE extends DropTargetAdapter{
    private dropFileListener dfl;
    private void processFiles(List<File> list){
        File f = list.get(0);
        dfl.dropFile(f.getAbsolutePath());
    }
    public void setDropFileListener(dropFileListener dfl){
        this.dfl = dfl;
    }
    @Override
    public void drop(DropTargetDropEvent dtde) {
        try
        {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor))//如果拖入的文件格式受支持
            {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);//接收拖拽来的数据,支持多个文件拖拽
                List<File> list =  (List<File>) (dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
                processFiles(list);
                dtde.dropComplete(true);//指示拖拽操作已完成
            }
            else
            {
                dtde.rejectDrop();//否则拒绝拖拽来的数据
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
