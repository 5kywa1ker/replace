package reple;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Replace {

    private static final String tmlPath = "tml";

    private static final String jsPath = "js";

    private static final String newTmlPath = "newTml";

    private static final String newJsPath = "newJs";

    private static final String hanJsFile = "zh-hans.json";

    private static Gson gson =  new GsonBuilder().setPrettyPrinting().create();

    /**
     * 保存提取出来的汉化key-value
     */
    private static LinkedHashMap hansMap = new LinkedHashMap<>();


    private static void repl() {

        //读取文件
        Path tmlPath = Paths.get(Replace.tmlPath);
        Path jsPath = Paths.get(Replace.jsPath);
        Path newTmlPath = Paths.get(Replace.newTmlPath);
        Path newJsPath = Paths.get(Replace.newJsPath);
        Path hanJsFile = Paths.get(Replace.hanJsFile);
        try {
            //文件夹如果不存在则先创建
            Files.createDirectory(tmlPath);
        } catch (FileAlreadyExistsException e1) {
            //文件夹若存在则不做处理
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.createDirectory(jsPath);
        } catch (FileAlreadyExistsException e1) {
            //文件夹若存在则不做处理
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.createDirectory(newTmlPath);
        } catch (FileAlreadyExistsException e1) {
            //文件夹若存在则不做处理
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.createDirectory(newJsPath);
        } catch (FileAlreadyExistsException e1) {
            //文件夹若存在则不做处理
        } catch (IOException e) {
            e.printStackTrace();
        }

        //汉化文件如果存在则读入hansMap中
        if (Files.exists(hanJsFile)) {
            try {
                Reader reader = new FileReader(hanJsFile.toFile());
                hansMap = gson.fromJson(reader, LinkedHashMap.class);

            } catch (JsonSyntaxException | FileNotFoundException e1) {
                e1.printStackTrace();
                if (e1 instanceof JsonSyntaxException)
                    throw new RuntimeException(hanJsFile.getFileName() + " 文件 json语法错误！");
            }
        } else {// 不存在则创建
            try {
                Files.createFile(hanJsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 处理tml
        try {
            replTml(tmlPath,newTmlPath,hanJsFile,hansMap);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 处理Tml
     * @param tmlPath
     * @param newTmlPath
     * @param hansMap
     */
    private static void replTml(Path tmlPath, Path newTmlPath,Path hanJsFile, LinkedHashMap hansMap) throws IOException {
        List<Path> tmlFiles = new ArrayList<>();

        // 遍历文件夹找出所有tml结尾的文件
        Files.walkFileTree(tmlPath,new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileString = file.toAbsolutePath().toString();
                if (fileString.endsWith(".tml") && !Files.isDirectory(file)){
                    tmlFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Pattern regex = Pattern.compile("[\\u4e00-\\u9fa5]+");
        int index = hansMap.size();
        // 处理每个tml
        for (Path tmlFile : tmlFiles) {
            String tmlName = tmlFile.getFileName().toString();
            System.out.println(tmlName);
            SAXReader saxReader = new SAXReader();
            try {
                Document document = saxReader.read(tmlFile.toFile());
                Element root = document.getRootElement();
                List<Element> nodes = new ArrayList<>();
                getNodes(root,nodes);
                for (Element node : nodes) {
                    String nodeText =  node.getText();
                    //如果包含中文
                    if (regex.matcher(nodeText).find()){
                        System.out.println("匹配到一个："+nodeText);
                        String key = tmlName+"."+index;
                        node.setText("${"+key+"}");
                        hansMap.put(key,nodeText);
                        index++;
                    }
                    List<Attribute> listAttr = node.attributes();//当前节点的所有属性的list
                    for (Attribute attr : listAttr) {//遍历当前节点的所有属性
                        String attrText = attr.getText().trim();
                        //如果包含中文
                        if (regex.matcher(attrText).find()){
                            System.out.println("匹配到一个："+attrText);
                            String key = tmlName+"."+index;
                            attr.setText("${"+key+"}");
                            hansMap.put(key,attrText);
                            index++;
                        }
                    }
                }

                // 将tml写回新的文件夹
                XMLWriter xmlWriter  = new XMLWriter();
                String newTml = newTmlPath.getFileName()+File.separator+tmlName;
                Path path = Paths.get(newTml);
                xmlWriter.setWriter(Files.newBufferedWriter(path));
                xmlWriter.write(document);
                xmlWriter.close();
            } catch (DocumentException e) {
                System.out.println("文件："+tmlFile.toAbsolutePath()+"解析失败！");
                e.printStackTrace();
            }
        }
        //保存json
        String json = gson.toJson(hansMap);
        Files.write(hanJsFile,json.getBytes("UTF-8"),StandardOpenOption.CREATE);
    }

    /**
     * 递归遍历当前节点所有的子节点,保存到nodes中
     * @param node
     * @param nodes
     */
    public static void getNodes(Element node,List<Element> nodes){
        List<Element> listElement=node.elements();//所有一级子节点的list
        for(Element e:listElement){//遍历所有一级子节点
            nodes.add(e);
            getNodes(e,nodes);//递归
        }
    }

    //处理文件

    /**
     * 主方法
     *
     * @param args
     */
    public static void main(String[] args) {

        repl();
    }


}
