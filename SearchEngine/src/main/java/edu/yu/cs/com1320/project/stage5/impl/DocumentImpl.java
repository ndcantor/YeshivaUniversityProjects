package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DocumentImpl implements Document {

    private URI uri;
    private int txtHash;
    private String text;
    private byte[] pdfBytes;
    private Map<String, Integer> wordMap;
    private long timeOfLastUse;

    public DocumentImpl(URI uri, String txt, int txtHash){
        if (txt == null || uri == null) {
            throw new IllegalArgumentException("cannot pass a null uri or string to the constructor");
        }
        this.uri = uri;
        this.text = txt;
        this.txtHash = txtHash;
        this.wordMap = this.getTextMap(txt);
        this.timeOfLastUse = System.nanoTime();
        this.pdfBytes = this.getDocumentAsPdf();
    }

    public DocumentImpl(URI uri, String txt, int txtHash, byte[] pdfBytes){
        this(uri, txt, txtHash);
        //noinspection ConstantConditions
        if (txt == null || uri == null || pdfBytes == null) {
            throw new IllegalArgumentException("cannot pass a null string");
        }
        this.pdfBytes = pdfBytes;
    }

    protected DocumentImpl (URI uri, String txt, int txtHash, Map<String,Integer> wordMap) {
        if (txt == null || uri == null) {
            throw new IllegalArgumentException("cannot pass a null uri or string to the constructor");
        }
        this.uri = uri;
        this.text = txt;
        this.txtHash = txtHash;
        this.setWordMap(wordMap);
        this.timeOfLastUse = System.nanoTime();
        this.pdfBytes = this.getDocumentAsPdf();
    }

    private String getUsableString(String string) {
        string = string.replaceAll("[^A-Za-z0-9 ]", "");
        string = string.trim();
        return string.toUpperCase();
    }

    protected Map<String, Integer> getTextMap(String txt) {
        Map<String,Integer> map = new HashMap<>();

        txt = txt.replaceAll("[^A-Za-z0-9 ]", "");
        txt = txt.toUpperCase();

        String[] words = txt.split(" ");
        for (String word : words) {
            word = word.trim();
            if (map.containsKey(word)) {
                map.put(word, map.get(word) + 1);
            }
            else {
                map.put(word, 1);
            }
        }
        return map;
    }

    protected Set<String> getWordSet() {
        return this.wordMap.keySet();
    }

    /**
     * @return the document as a PDF
     */
    @Override
    public byte[] getDocumentAsPdf() {
        if (this.pdfBytes != null) {
            return this.pdfBytes;
        }
        PDDocument pdfDoc = new PDDocument();
        PDPage page = new PDPage();
        pdfDoc.addPage(page);
        try {
            PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page);
            contentStream.beginText();
            PDFont font = PDType1Font.HELVETICA_BOLD;
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(20, 20);
            contentStream.showText(this.text);
            contentStream.endText();
            contentStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            pdfDoc.save(byteArrayOutputStream);
            pdfDoc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * @return the document as a Plain String
     */
    @Override
    public String getDocumentAsTxt() {
        return this.text;
    }

    /**
     * @return hash code of the plain text version of the document
     */
    @Override
    public int getDocumentTextHashCode() {
        return this.txtHash;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    @Override
    public URI getKey() {
        return this.uri;
    }

    /**
     * how many times does the given word appear in the document?
     *
     * @param word for which to find the count
     * @return the number of times the given words appears in the document
     */
    @Override
    public int wordCount(String word) {
        word = this.getUsableString(word);
        if (!(this.wordMap.containsKey(word))) {
            return 0;
        }
        return this.wordMap.get(word);
    }

    /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */
    @Override
    public long getLastUseTime() {
        return this.timeOfLastUse;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.timeOfLastUse = timeInNanoseconds;
    }

    /**
     * @return a copy of the word to count map so it can be serialized
     */
    @Override
    public Map<String, Integer> getWordMap() {
        return this.wordMap;
    }

    /**
     * This must set the word to count map during deserialization
     *
     * @param wordMap - map
     */
    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordMap = wordMap;
    }

    @Override
    public int compareTo(Document o) {
        long lastUseTime = o.getLastUseTime();
        if (this.timeOfLastUse > 0 && lastUseTime < 0) {
            return 1;
        }
        if (this.timeOfLastUse < 0 && lastUseTime > 0) {
            return -1;
        }
        if (this.timeOfLastUse == 0 && lastUseTime == 0) {
            return 0;
        }
        if (this.timeOfLastUse > 0 && lastUseTime > 0) {
            if (this.timeOfLastUse - lastUseTime > 0) {
                return 1;
            } else if (this.timeOfLastUse - lastUseTime < 0) {
                return -1;
            } else {
                return 0;
            }
        }
        else {
            if (this.timeOfLastUse - lastUseTime < 0) {
                return 1;
            } else if (this.timeOfLastUse - lastUseTime > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocumentImpl document = (DocumentImpl) o;
        return this.uri.hashCode() == document.getKey().hashCode() && this.txtHash == document.getDocumentTextHashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uri, this.text);
    }
}