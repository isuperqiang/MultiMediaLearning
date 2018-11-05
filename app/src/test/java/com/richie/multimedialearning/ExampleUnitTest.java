package com.richie.multimedialearning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void ptClassLoader() {
        ClassLoader classLoader = getClass().getClassLoader();
        System.out.println("app:" + classLoader);
        while (classLoader.getParent() != null) {
            classLoader = classLoader.getParent();
            System.out.println("parent:" + classLoader);
        }
    }

}