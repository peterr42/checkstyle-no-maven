package org.checkstyle.suppressionxpathfilter.needbraces;

public class InputXpathNeedBracesDo {
    /** @return helper func **/
    boolean condition()
    {
        return false;
    }

    /** Test do/while loops **/
    public void test() {
        // Invalid
        do test(); while (condition()); // warn
    }
}
