package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable> extends MinHeap<E> {

    private Map<E,Integer> elementsToArrayIndex;
    private Comparable[] elements;

    public MinHeapImpl() {
        this.elements = new Comparable[10];
        this.count = 0;
        this.elementsToArrayIndex = new HashMap<E,Integer>(this.count);
    }

    protected Map<E,Integer> getElementsToArrayIndex() {
        return this.elementsToArrayIndex;
    }

    @Override
    public void reHeapify(E element) {
        Integer integer = (Integer) this.elementsToArrayIndex.get(element);
        int index = (int) integer;
        this.upHeap(index);
        index = (int) this.elementsToArrayIndex.get(element);
        this.downHeap(index);
    }

    /**
     * @param element element
     * @return if element doesn't exist in the heap, returns -1
     */
    @Override
    protected int getArrayIndex(E element) {
        if (!this.elementsToArrayIndex.containsKey(element)) {
            throw new NoSuchElementException("element doesn't exist in this minHeap");
        }
        return (int) this.elementsToArrayIndex.get(element);
    }

    @Override
    protected void doubleArraySize() {
        Comparable[] newArray = new Comparable[this.elements.length * 2];
        if (this.count >= 0) System.arraycopy(this.elements, 0, newArray, 0, this.count + 1);
        this.elements = newArray;
    }

    @Override
    protected  boolean isEmpty()
    {
        return this.count == 0;
    }

    /**
     * is elements[i] > elements[j]?
     */
    @Override
    protected  boolean isGreater(int i, int j) {
        //noinspection unchecked
        return this.elements[i].compareTo(this.elements[j]) > 0;
    }

    /**
     * swap the values stored at elements[i] and elements[j], keeping track of them
     * in Element -> Integer map
     */
    @Override
    protected void swap(int i, int j) {
        Comparable temp = this.elements[i];
        this.elements[i] = this.elements[j];
        this.elements[j] = temp;
        temp = this.elements[i];
        //noinspection unchecked
        this.elementsToArrayIndex.put((E)temp, i);
        temp = this.elements[j];
        //noinspection unchecked
        this.elementsToArrayIndex.put((E)temp, j);
    }

    /**
     *while the key at index k is less than its
     *parent's key, swap its contents with its parent’s
     */
    @Override
    protected void upHeap(int k) {
        while (k > 1 && this.isGreater(k / 2, k))
        {
            this.swap(k, k / 2);
            k = k / 2;
        }
    }

    /**
     * move an element down the heap until it is less than
     * both its children or is at the bottom of the heap
     */
    @Override
    protected void downHeap(int k) {
        while (2 * k <= this.count)
        {
            //identify which of the 2 children are smaller
            int j = 2 * k;
            if (j < this.count && this.isGreater(j, j + 1))
            {
                j++;
            }
            //if the current value is < the smaller child, we're done
            if (!this.isGreater(k, j))
            {
                break;
            }
            //if not, swap and continue testing
            this.swap(k, j);
            k = j;
        }
    }

    /**
     *
     * @param x to insert
     *
     * overrides insert method in parent, adding the ability to keep track of new insert
     * in the Element -> Integer map
     */
    @Override
    public void insert(E x) {
        // double size of array if necessary
        if (this.count >= this.elements.length - 1) {
            this.doubleArraySize();
        }
        //add x to the bottom of the heap and put in Element -> Integer map
        this.elements[++this.count] = x;
        this.elementsToArrayIndex.put(x, this.count);
        //percolate it up to maintain heap order property
        this.upHeap(this.count);
    }

    /**
     *
     */
    @Override
    public E removeMin() {
        if (isEmpty())
        {
            throw new NoSuchElementException("Heap is empty");
        }
        @SuppressWarnings("unchecked") E min = (E) this.elements[1];
        //swap root with last, decrement count
        this.swap(1, this.count--);
        //move new root down as needed
        this.downHeap(1);
        this.elementsToArrayIndex.remove(min); //remove from Element -> Integer map
        this.elements[this.count + 1] = null; //null it to prepare for GC
        return min;
    }
}