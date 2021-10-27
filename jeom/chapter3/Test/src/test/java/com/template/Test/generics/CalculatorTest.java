package com.template.Test.generics;

import com.template.Test.generics.Calculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CalculatorTest {

    Calculator calculator;
    String numFilepath;

    @BeforeEach
    public void setUp(){
        this.calculator = new Calculator();
        this.numFilepath = "numbers.txt";
    }

    @Test
    public void concatenateStrings() throws IOException {
        assertThat(calculator.concatenate(this.numFilepath)).isEqualTo("1234");
    }

}
