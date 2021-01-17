package edu.yu.cs.com1320.project.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Scanner;

import static org.junit.Assert.*;

public class BTreeImplTest {

    public PersistenceManager<Integer,String> getStringIntegerPersistenceManager() {
        return new PersistenceManager<Integer, String>() {
            @Override
            public void serialize(Integer i, String val) throws IOException {
                File file = new File(i + ".txt");
                file.createNewFile();
                Gson gson = new Gson();
                String jsonString = gson.toJson(val);
                FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
                fileWriter.write(jsonString);
                fileWriter.close();
            }

            @Override
            public String deserialize(Integer i) throws IOException {
                File file = new File(i + ".txt");
                if (!file.exists()) {
                    throw new IllegalArgumentException("file doesn't exist");
                }
                Scanner scanner = new Scanner(file);
                String jsonString = scanner.next().trim();
                scanner.close();
                Gson gson = new Gson();
                Type stringType = new TypeToken<String>(){}.getType();
                String string = gson.fromJson(jsonString, stringType);
                Files.delete(file.toPath());
                return string;
            }
        };
    }

    public BTreeImpl<Integer,String> getBTree() {
        BTreeImpl<Integer,String> stringIntegerBTree = new BTreeImpl<>();
        stringIntegerBTree.setPersistenceManager(this.getStringIntegerPersistenceManager());
        return stringIntegerBTree;
    }

    public BTreeImpl<Integer,String> getSetUpBtree() {
        BTreeImpl<Integer,String> bTree = this.getBTree();
        bTree.put(0, "zero");
        bTree.put(1, "one");
        bTree.put(2, "two");
        bTree.put(3, "three");
        bTree.put(4, "four");
        bTree.put(5, "five");
        bTree.put(6, "six");
        bTree.put(7, "seven");
        bTree.put(8, "eight");
        bTree.put(9, "nine");
        bTree.put(10, "ten");
        bTree.put(11, "eleven");
        bTree.put(12, "twelve");
        bTree.put(13, "thirteen");
        bTree.put(14, "fourteen");
        return bTree;
    }

    @Test
    public void testBTreePutAndGet() {
        BTreeImpl<Integer,String> bTree = this.getBTree();
        bTree.put(0, "zero");
        bTree.put(1, "one");
        bTree.put(2, "two");
        bTree.put(3, "three");
        bTree.put(4, "four");
        bTree.put(5, "five");
        bTree.put(6, "six");
        bTree.put(7, "seven");
        bTree.put(8, "eight");
        bTree.put(9, "nine");
        bTree.put(10, "ten");
        bTree.put(11, "eleven");
        bTree.put(12, "twelve");
        bTree.put(13, "thirteen");
        bTree.put(14, "fourteen");
        assertEquals("zero", bTree.get(0));
        assertEquals("one", bTree.get(1));
        assertEquals("two", bTree.get(2));
        assertEquals("three", bTree.get(3));
        assertEquals("four", bTree.get(4));
        assertEquals("five", bTree.get(5));
        assertEquals("six", bTree.get(6));
        assertEquals("seven", bTree.get(7));
        assertEquals("eight", bTree.get(8));
        assertEquals("nine", bTree.get(9));
        assertEquals("ten", bTree.get(10));
        assertEquals("eleven", bTree.get(11));
        assertEquals("twelve", bTree.get(12));
        assertEquals("thirteen", bTree.get(13));
        assertEquals("fourteen", bTree.get(14));
    }

    @Test
    public void testMoveToDisk() throws Exception {
        BTreeImpl<Integer,String> bTree = this.getSetUpBtree();
        bTree.moveToDisk(0);
        bTree.moveToDisk(3);
        bTree.moveToDisk(5);
        bTree.moveToDisk(7);
        bTree.moveToDisk(9);
        bTree.moveToDisk(11);
        bTree.moveToDisk(13);
        assertEquals("zero", bTree.get(0));
        assertEquals("one", bTree.get(1));
        assertEquals("two", bTree.get(2));
        assertEquals("three", bTree.get(3));
        assertEquals("four", bTree.get(4));
        assertEquals("five", bTree.get(5));
        assertEquals("six", bTree.get(6));
        assertEquals("seven", bTree.get(7));
        assertEquals("eight", bTree.get(8));
        assertEquals("nine", bTree.get(9));
        assertEquals("ten", bTree.get(10));
        assertEquals("eleven", bTree.get(11));
        assertEquals("twelve", bTree.get(12));
        assertEquals("thirteen", bTree.get(13));
        assertEquals("fourteen", bTree.get(14));
        bTree.moveToDisk(0);
        bTree.moveToDisk(3);
        bTree.moveToDisk(5);
        bTree.moveToDisk(7);
        bTree.moveToDisk(9);
        bTree.moveToDisk(11);
        bTree.moveToDisk(13);
        assertTrue(bTree.get(bTree.getRoot(), 0, bTree.getHeight()).storedOnDisk);
        assertTrue(bTree.get(bTree.getRoot(), 3, bTree.getHeight()).storedOnDisk);
        assertTrue(bTree.get(bTree.getRoot(), 5, bTree.getHeight()).storedOnDisk);
        assertTrue(bTree.get(bTree.getRoot(), 7, bTree.getHeight()).storedOnDisk);
        assertTrue(bTree.get(bTree.getRoot(), 9, bTree.getHeight()).storedOnDisk);
        assertTrue(bTree.get(bTree.getRoot(), 11, bTree.getHeight()).storedOnDisk);
        assertTrue(bTree.get(bTree.getRoot(), 13, bTree.getHeight()).storedOnDisk);
        assertNull(bTree.get(bTree.getRoot(), 0, bTree.getHeight()).val);
        assertNull(bTree.get(bTree.getRoot(), 3, bTree.getHeight()).val);
        assertNull(bTree.get(bTree.getRoot(), 5, bTree.getHeight()).val);
        assertNull(bTree.get(bTree.getRoot(), 7, bTree.getHeight()).val);
        assertNull(bTree.get(bTree.getRoot(), 9, bTree.getHeight()).val);
        assertNull(bTree.get(bTree.getRoot(), 11, bTree.getHeight()).val);
        assertNull(bTree.get(bTree.getRoot(), 13, bTree.getHeight()).val);
        assertTrue(new File("0.txt").exists());
        assertTrue(new File("3.txt").exists());
        assertTrue(new File("5.txt").exists());
        assertTrue(new File("7.txt").exists());
        assertTrue(new File("9.txt").exists());
        assertTrue(new File("11.txt").exists());
        assertTrue(new File("13.txt").exists());
        bTree.get(0);
        bTree.get(3);
        bTree.get(5);
        bTree.get(7);
        bTree.get(9);
        bTree.get(11);
        bTree.get(13);
        assertFalse(new File("0.txt").exists());
        assertFalse(new File("3.txt").exists());
        assertFalse(new File("5.txt").exists());
        assertFalse(new File("7.txt").exists());
        assertFalse(new File("9.txt").exists());
        assertFalse(new File("11.txt").exists());
        assertFalse(new File("13.txt").exists());
    }
}
