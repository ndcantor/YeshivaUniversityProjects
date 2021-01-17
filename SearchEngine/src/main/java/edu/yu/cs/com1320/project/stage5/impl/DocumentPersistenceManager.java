package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {

    private final File baseDir;

    public DocumentPersistenceManager(File baseDir){
        if (baseDir == null) {
            this.baseDir = new File(System.getProperty("user.dir"));
        }
        else {
            this.baseDir = baseDir.getAbsoluteFile();
        }
    }

//    public static void main(String[] args) throws URISyntaxException, IOException {
//        URI uri = new URI("https://foo.foo/fooster");
//        String txt = "this is the foo fo$^o foo fooster of doc1";
//        int hashCode = txt.hashCode();
//        DocumentImpl document = new DocumentImpl(uri, txt, hashCode);
//        DocumentPersistenceManager dpm = new DocumentPersistenceManager(new File("hello"));
//        dpm.serialize(uri, document);
//        File file = new File("C:\\hello\\foo.foo\\fooster");
//        Scanner scanner = new Scanner(file);
//        System.out.println(scanner.next());
//        DocumentImpl deserialized = (DocumentImpl) dpm.deserialize(uri);
//        System.out.println(deserialized.getWordMap().equals(document.getWordMap()));
//        System.out.println(deserialized.getDocumentAsTxt().equals(document.getDocumentAsTxt()));
//        System.out.println(deserialized.getKey().equals(document.getKey()));
//        System.out.println(deserialized.getDocumentTextHashCode() == document.getDocumentTextHashCode());
//        System.out.println(deserialized.getDocumentAsTxt());
//        System.out.println(document.getDocumentAsTxt());
//    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        //add URI to path and save directory structure
        String filePath = this.getAuthorityAndPath(uri);
        filePath = generifyUriPathString(filePath);
        filePath = filePath + ".json";
        filePath = this.baseDir.getAbsolutePath() + File.separator + filePath;
        //filePath is now an absolute path
        filePath = this.saveUriDirStructureToSystem(filePath);
        //create JSON string
        Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentSerializer()).create();
        Type type = new TypeToken<DocumentImpl>(){}.getType();
        String jsonString = gson.toJson(val, type);
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(jsonString);
        fileWriter.close();
    }

    private String generifyUriPathString(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path string was null");
        }
        char[] chars = path.toCharArray();
        for (int i = 0; i < path.length(); i++) {
            if (chars[i] == '/') {
                chars[i] = File.separatorChar;
            }
        }
        return new String(chars);
    }

    /**
     * returns null if there is no file for this URI
     */
    @Override
    public Document deserialize(URI uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException("uri was null");
        }
        String filePath = this.getAuthorityAndPath(uri);
        filePath = this.generifyUriPathString(filePath);
        filePath = filePath + ".json";
        filePath = this.baseDir.getAbsolutePath() + File.separator + filePath;
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        Scanner scanner = new Scanner(file);
        String jsonString = "";
        while (scanner.hasNext()) {
            jsonString += scanner.next();
            jsonString += " ";
        }
        scanner.close();
        Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentDeserializer()).create();
        Type documentImplType = new TypeToken<DocumentImpl>(){}.getType();
        Document document = gson.fromJson(jsonString.trim(), documentImplType);

        //delete file and any empty directories
        Files.delete(file.toPath());
        this.postOrderDeleteEmptyDirs(file.getParentFile());
        return document;
    }

    private String getAuthorityAndPath(URI uri) {
        if (uri.getPath() == null && uri.getAuthority() == null) {
            return null;
        }
        if (uri.getPath() == null) {
            return uri.getAuthority();
        }
        if (uri.getAuthority() == null) {
            return uri.getPath();
        }
        String authority = uri.getAuthority();
        String path = uri.getPath();
        return authority + path;
    }

    /**
     * this method saves the directory structure to the system, excluding the file itself
     * (e.g.: for the URI "http://www.edu.yu.cs.com1320/doc1", the method will save a directory under
     * [base dir]/www.edu.yu.cs.com1320; this dir now exists so that you can save doc1 inside of it)
     *
     * @return - the path INCLUDING the file at the end (i.e. in the example above, it would return
     * the path including /doc1 at the end)
     */
    private String saveUriDirStructureToSystem(String absolutePathName) {
        File file = new File(absolutePathName);
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException("string absolutePathName was not an absolute path name");
        }
        File parentDir = new File(file.getParent());
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        return file.getAbsolutePath();
    }

    private void postOrderDeleteEmptyDirs(File f) throws IOException {
        if (f == null) {
            throw new  NullPointerException();
        }
        if (!f.exists()) {
            throw new IllegalArgumentException("file doesn't exist, can't delete it");
        }
        if (!f.isDirectory()) {
            throw new IllegalArgumentException("file was not a directory");
        }

        File file = f;
        File[] list = file.listFiles();
        //noinspection ConstantConditions
        while (list.length == 0) {
            Files.delete(file.toPath());
            file = file.getParentFile();
            list = file.listFiles();
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    class DocumentSerializer implements JsonSerializer<DocumentImpl> {

        @Override
        public JsonElement serialize(DocumentImpl document, Type type, JsonSerializationContext jsonSerializationContext) {
//          things that we need to serialize: 1) string version of the doc's contents;
//                                            2) URI/key;
//                                            3) document contents hashcode;
//                                            4) wordCount map

            JsonObject jsonObject = new JsonObject();
//            String docTxt = document.getDocumentAsTxt().replaceAll(" ", "`");
            String docTxt = document.getDocumentAsTxt().trim();
            URI uri = document.getKey();
            int txtHashcode = document.getDocumentTextHashCode();

            //make wordMap serializable
            String[] words = this.getDocWordSet(document);
            int[] wordCounts = this.getWordCounts(document, words);

            StringBuilder wordsList = new StringBuilder(words[0]);
            StringBuilder countsList = new StringBuilder(Integer.toString(wordCounts[0]));

            for (int i = 1; i < words.length; i++) {
                wordsList.append("`").append(words[i]);
            }
            for (int i = 1; i < wordCounts.length; i++) {
                countsList.append("`").append(wordCounts[i]);
            }

            jsonObject.addProperty("documentTxt", docTxt);
            jsonObject.addProperty("uriString", uri.toString());
            jsonObject.addProperty("txtHashCode", txtHashcode);
            jsonObject.addProperty("wordsList", wordsList.toString());
            jsonObject.addProperty("countsList", countsList.toString());

            return jsonObject;
        }

        private String[] getDocWordSet(DocumentImpl document) {
            Set<String> words = document.getWordSet();
            String[] wordArray = new String[words.size()];
            int i = 0;
            for (String word : words) {
                wordArray[i++] = word;
            }
            return wordArray;
        }

        private int[] getWordCounts(DocumentImpl document, String[] words) {
            int[] wordCounts = new int[words.length];
            int i;
            for (i = 0; i < words.length; i++) {
                wordCounts[i] = document.wordCount(words[i]);
            }
            return wordCounts;
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    class DocumentDeserializer implements JsonDeserializer<DocumentImpl> {

        @Override
        public DocumentImpl deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

            JsonObject object = jsonElement.getAsJsonObject();

            String docTxt = object.get("documentTxt").getAsString().trim();
            String uriString = object.get("uriString").getAsString();
            int txtHashCode = object.get("txtHashCode").getAsInt();
            String wordsList = object.get("wordsList").getAsString();
            String countsList = object.get("countsList").getAsString();

//            docTxt = docTxt.replaceAll("`"," ");
            URI uri = null;
            try {
                uri = new URI(uriString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            //now set the wordMap
            String[] words = this.convertToStringArray(wordsList);
            int[] wordCounts = this.convertToIntArray(countsList);
            Map<String, Integer> wordMap = new HashMap<>();
            for (int i = 0; i < words.length; i++) {
                wordMap.put(words[i], wordCounts[i]);
            }

            return new DocumentImpl(uri, docTxt, txtHashCode, wordMap);
        }

        private String[] convertToStringArray(String wordsList) {
            int numberOfWords = 0;
            char[] chars = wordsList.toCharArray();
            for (char c : chars) {
                if (c == '`') {
                    numberOfWords++;
                }
            }
            //increment numberOfWords one more time to account for the last word
            numberOfWords++;

            String[] words = new String[numberOfWords];
            int numberOfWordsAdded = 0;
            StringBuilder word = new StringBuilder();
            for (char c : chars) {
                if (c == '`') {
                    words[numberOfWordsAdded++] = word.toString();
                    word = new StringBuilder();
                } else {
                    word.append(c);
                }
            }
            //need to account for the last char
            words[numberOfWordsAdded++] = word.toString();
            return words;
        }

        private int[] convertToIntArray(String countsList) {
            int numberOfWords = 0;
            char[] chars = countsList.toCharArray();
            for (char c : chars) {
                if (c == '`') {
                    numberOfWords++;
                }
            }
            //increment numberOfWords one more time to account for the last word
            numberOfWords++;

            int[] wordCounts = new int[numberOfWords];
            int numberOfCountsAdded = 0;
            StringBuilder numberString = new StringBuilder();
            for (char aChar : chars) {
                if (aChar == '`') {
                    wordCounts[numberOfCountsAdded++] = Integer.parseInt(numberString.toString());
                    numberString = new StringBuilder();
                } else {
                    numberString.append(aChar);
                }
            }
            //have to account for the last number
            wordCounts[numberOfCountsAdded++] = Integer.parseInt(numberString.toString());
            return wordCounts;
        }
    }
}
