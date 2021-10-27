package com.template.Test.generics;

public interface LineCallback<T> {
        T doSomethingWithLine(String line, T value);
}
