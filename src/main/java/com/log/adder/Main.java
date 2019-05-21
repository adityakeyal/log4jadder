package com.log.adder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.Statement;
import com.log.adder.walker.FileWalker;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, IllegalAccessException {


//        final String format = String.format(" if(%1$s.debugEnabled()) {  "
//                + " %1$s.debug(\"Entering method :  \" ); "
//                + "}", "logger");
//        System.out.println(format);
//        System.exit(1);

        final Collection<File> files = FileUtils.listFiles(new File("D:\\code\\bys\\cvs-code\\trunk\\bys\\src\\main\\java\\com\\a"), new String[]{"java"}, true);

        final long l = System.currentTimeMillis();
        int count = 0;
        for (File file : files) {
            FileWalker fw = new FileWalker(file);

            if(fw.isProcessable()){
                count++;
                if(fw.isLoggerAvailable()){

                }else{
                    fw.addLogger();
                }

                fw.addEntryLogToAllPublicMethods();
                fw.addExitLogToAllPublicMethods();
                fw.print();
            }

        }

        System.out.println("Total Time : "  + (System.currentTimeMillis() - l));
        System.out.println(count);
    }
}
