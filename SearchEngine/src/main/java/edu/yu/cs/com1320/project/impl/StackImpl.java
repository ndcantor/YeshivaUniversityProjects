package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    private int size;
    private StackEntry<T> head;

    public StackImpl() {
        this.size = 0;
        this.head = null;
    }

    /**
     * @param value object to add to the Stack
     */
    @Override
    public void push(T value) {
        if (value == null) {
            throw new IllegalArgumentException("cannot push a null value onto stack");
        }
        if (this.head == null) {
            this.head = new StackEntry<>(value);
            this.size++;
            return;
        }
        StackEntry<T> newEntry = new StackEntry<>(value);
        newEntry.setNext(this.head);
        this.size++;
        this.head = newEntry;
    }

    /**
     * removes and returns element at the top of the stack
     *
     * @return element at the top of the stack, null if the stack is empty
     */
    @Override
    public T pop() {
        if (this.head == null) {
            return null;
        }
        StackEntry<T> popped = this.head;
        this.head = this.head.getNext();
        this.size--;
        return popped.getValue();
    }

    /**
     * @return the element at the top of the stack without removing it
     */
    @Override
    public T peek() {
        return this.head == null ? null : this.head.getValue();
    }

    /**
     * @return how many elements are currently in the stack
     */
    @Override
    public int size() {
        return this.size;
    }
}

class StackEntry<T> {

    private T value;
    private StackEntry<T> next;

    StackEntry(T value) {
        this.value = value;
        this.next = null;
    }

    StackEntry<T> getNext() {
        return next;
    }

    void setNext(StackEntry<T> next) {
        this.next = next;
    }

    T getValue() {
        return value;
    }
}