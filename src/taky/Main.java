package taky;

import javafx.application.Application;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 类名 ClassName  Main
 * 项目 ProjectName  MacNotesBackup
 * 作者 Author  郑添翼 Taky.Zheng
 * 邮箱 E-mail 275158188@qq.com
 * 时间 Date  2019-06-28 14:06 ＞ω＜
 * 描述 Description TODO
 */
public class Main extends Application {

    // srcDir = new File("/Users/zhengtianyi/Desktop/测试压缩包");
    File srcDir = null;
    File tagfile = new File("/Users/zhengtianyi/Desktop");

    //源文件夹总大小
    Number countSize = 0;
    //创建一个Zip压缩流
    ZipOutputStream zos = null;
    //记录备份实时大小
    SimpleIntegerProperty tempSize = new SimpleIntegerProperty(0);
    @Override
    public void start(Stage primaryStage) throws Exception {

        Label labelSize = new Label();
        //获取系统用户名
        String username = System.getProperty("user.name");
        String path = "/Users/"+ username + "/Library/Group Containers/group.com.apple.notes";
        srcDir = new File(path);


        Label label1 = new Label("当前备忘录路径: " + path);
        //设置保存位置按钮
        Button savePathBtn = new Button("设置保存位置");
        //开始备份按钮
        Button startBackupBtn = new Button("开始备份");
        //保存位置信息窗
        Label label2 = new Label("默认为保存到桌面:" + tagfile.getPath());
        //备份状态信息窗
        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(400);


        HBox hBox1 = new HBox(10,label1);
        hBox1.setAlignment(Pos.CENTER_LEFT);
        HBox hBox2 = new HBox(10,savePathBtn,label2);
        hBox2.setAlignment(Pos.CENTER_LEFT);
        HBox hBox3 = new HBox(10,startBackupBtn,pb,labelSize);
        hBox3.setAlignment(Pos.CENTER_LEFT);


        VBox root = new VBox(10,hBox1,hBox2,hBox3);
        root.setPadding(new Insets(10));
        primaryStage.setScene(new Scene(root, 650, 115));
        primaryStage.setTitle("MacOS备忘录备份工具 1.0 Taky QQ:275158188");
        primaryStage.show();

        //计算文件大小的任务
        MyTaskGetFileSize myTaskGetFileSize = new MyTaskGetFileSize();
        //备份的任务
        MyTask task = new MyTask();

        //监听文件大小任务
        myTaskGetFileSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                countSize = newValue;
                labelSize.setText("备份大小: " + newValue.intValue());
                myTaskGetFileSize.cancel();
            }
        });

        tempSize.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                double tempd = newValue.doubleValue() / countSize.doubleValue();
                pb.setProgress(tempd);
            }
        });

        //监听完成状态
        task.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue == Worker.State.SUCCEEDED){
                    if (zos != null){
                        try {
                            zos.close();
                            startBackupBtn.setDisable(false);
                            System.out.println("关闭成功");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("关闭流时出错!");
                        }
                    }
                }
            }
        });

        savePathBtn.setOnAction(p ->{
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("请选择保存位置");
            //获取选中的文件
            File file = dc.showDialog(primaryStage);
            if (file == null) return;
            //设置为全局变量,以便使用
            tagfile = file;
            //设置路径提示信息
            label2.setText(file.getPath());

        });

        //开始压缩的方法
        startBackupBtn.setOnAction(p -> {
            //获取数据大小
            myTaskGetFileSize.reset();
            myTaskGetFileSize.start();
            try {
                //获取当前时间,作为文件名一部分
                String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                //设置保存位置及文件名
                File file = new File(tagfile + "/" + srcDir.getName() + "-" + date + ".zip");
                //创建压缩流,将设置好的保存位置放入
                zos = new ZipOutputStream(new FileOutputStream(file));

                task.reset();
                task.start();

                startBackupBtn.setDisable(true);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

    }

    public static void main(String[] args) {
        launch(args);
    }

    //通过递归实现文件夹压缩,参数1为传入一个文件夹,参数2为文件夹路径,参数3位压缩流
    public void getZip(File srcDir,String baseDir,ZipOutputStream zos) {
        if (!srcDir.exists()) { System.out.println("文件夹不存在!"); return;}
        //创建文件缓冲区
        byte[] bufs = new byte[1024 * 10];
        //获取文件夹下所有文件
        File[] files = srcDir.listFiles();
        //如果是个空文件夹,先创建空文件夹条目,避免不创建空文件夹
        if (files.length == 0){
            ZipEntry zipEntry = new ZipEntry(baseDir);
            try {
                zos.putNextEntry(zipEntry);
            } catch (IOException e) {
                System.err.println("空文件条目创建失败");
            }
        }
        //带缓存的文件输入流
        BufferedInputStream bis = null;
        try {
            //循环所有文件
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile()) {
                    //创建压缩条目
                    ZipEntry zipEntry = new ZipEntry(baseDir + fileName);
                    zos.putNextEntry(zipEntry); //压入Zip流
                    //创建带缓冲的文件输入流
                    bis = new BufferedInputStream(new FileInputStream(file));
                    //创建读取位置记录器
                    int read = 0;
                    //循环写出到压缩流
                    while ((read = bis.read(bufs, 0, 1024 * 10)) != -1) {
                        zos.write(bufs, 0, read);
                        IntegerBinding add = tempSize.add(read);
                        tempSize.set(add.getValue());
                    }
                } else if (file.isDirectory()) { //如果是文件夹
                    //递归调用
                    getZip(file, baseDir + fileName + "/", zos);
                }
            }
        } catch (Exception e) {
            System.err.println("遍历文件时出错!");
        } finally {
            try {
                if (bis != null) bis.close(); //这里一定要判空一下,否则可能会报错
            } catch (IOException e) {
                System.err.println("关闭输入流时出错!");
            }
        }
    }

    public double getDirSize(File file){
        if (file.isDirectory()) {
            double size = 0;
            File[] files = file.listFiles();
            if (files != null && files.length > 0){
                for (File f : files) {
                    size += getDirSize(f);
                }
            }
            return size;
        }else {
            return file.length();
        }
    }

    class MyTaskGetFileSize extends Service<Number>{
        @Override
        protected Task<Number> createTask() {
            return new Task<Number>() {
                @Override
                protected Number call() throws Exception {
                    return  getDirSize(srcDir);
                }
            };
        }
    }


    class MyTask extends Service<Number> {

        @Override
        protected Task<Number> createTask() {
            return new Task<Number>() {
                @Override
                protected Number call() throws Exception {
                    //调用递归文件夹压缩方法
                    getZip(srcDir,srcDir.getName() + "/", zos);
                    return null;
                }
            };
        }
    }
}
