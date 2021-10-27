package com.template.Test.bufferedreader;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Slf4j
public class Calculator {

    public Integer fileReadTemplate(String filepath, BufferedReaderCallback callback)
            throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filepath));
            int ret = callback.doSomethingWithReader(br);
            //콜백 오브젝트 호출 , 템플리에서  만든 컨텍스트 정보인 BufferedReader를 전달해주고 결과를 받아옴
            return ret;
        } catch (IOException e) {
            log.info(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

    public Integer calcSum(String filepath) throws IOException {
        BufferedReaderCallback sumCallback =
                new BufferedReaderCallback() {
                    public Integer doSomethingWithReader(BufferedReader br) throws
                            IOException {
                        Integer sum = 0;
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            sum += Integer.valueOf(line);
                        }
                        return sum;
                    }
                };
        return fileReadTemplate(filepath, sumCallback);
    }



    public Integer calcMultiply(String filepath) throws IOException {
        BufferedReaderCallback multiplyCallback =
                new BufferedReaderCallback() {
                    public Integer doSomethingWithReader(BufferedReader br) throws
                            IOException {
                        Integer multiply = 1;
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            multiply *= Integer.valueOf(line);
                        }
                        return multiply;
                    }
                };
        return fileReadTemplate(filepath, multiplyCallback);
    }
}
