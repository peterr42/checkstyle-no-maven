///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2022 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.puppycrawl.tools.checkstyle.checks.UniquePropertiesCheck.MSG_IO_EXCEPTION_KEY;
import static com.puppycrawl.tools.checkstyle.checks.UniquePropertiesCheck.MSG_KEY;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.Violation;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

public class UniquePropertiesCheckTest extends AbstractModuleTestSupport {

    @Override
    protected String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/checks/uniqueproperties";
    }

    /* Additional test for jacoco, since valueOf()
     * is generated by javac and jacoco reports that
     * valueOf() is uncovered.
     */
    @Test
    public void testLineSeparatorOptionValueOf() {
        final LineSeparatorOption option = LineSeparatorOption.valueOf("CR");
        assertWithMessage("Invalid valueOf result")
            .that(option)
            .isEqualTo(LineSeparatorOption.CR);
    }

    /**
     * Tests the ordinal work of a check.
     */
    @Test
    public void testDefault() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(UniquePropertiesCheck.class);
        final String[] expected = {
            "3: " + getCheckMessage(MSG_KEY, "general.exception", 2),
            "5: " + getCheckMessage(MSG_KEY, "DefaultLogger.auditStarted", 2),
            "11: " + getCheckMessage(MSG_KEY, "onlineManual", 3),
            "22: " + getCheckMessage(MSG_KEY, "time stamp", 3),
            "28: " + getCheckMessage(MSG_KEY, "Support Link ", 2),
            "34: " + getCheckMessage(MSG_KEY, "failed", 2),
        };
        verify(checkConfig, getPath("InputUniqueProperties.properties"), expected);
    }

    /**
     * Tests the {@link UniquePropertiesCheck#getLineNumber(FileText, String)}
     * method return value.
     *
     * @noinspection JavadocReference Test javadocs should explain all.
     */
    @Test
    public void testNotFoundKey() throws Exception {
        final List<String> testStrings = new ArrayList<>(3);
        final Method getLineNumber = UniquePropertiesCheck.class.getDeclaredMethod(
            "getLineNumber", FileText.class, String.class);
        assertWithMessage("Get line number method should be present")
            .that(getLineNumber)
            .isNotNull();
        getLineNumber.setAccessible(true);
        testStrings.add("");
        testStrings.add("0 = 0");
        testStrings.add("445");
        final FileText fileText = new FileText(new File("some.properties"), testStrings);
        final Object lineNumber = getLineNumber.invoke(UniquePropertiesCheck.class,
                fileText, "some key");
        assertWithMessage("Line number should not be null")
            .that(lineNumber)
            .isNotNull();
        assertWithMessage("Invalid line number")
            .that(lineNumber)
            .isEqualTo(1);
    }

    @Test
    public void testDuplicatedProperty() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(UniquePropertiesCheck.class);
        final String[] expected = {
            "2: " + getCheckMessage(MSG_KEY, "key", 2),
        };
        verify(checkConfig, getPath("InputUniquePropertiesWithDuplicates.properties"), expected);
    }

    @Test
    public void testShouldNotProcessFilesWithWrongFileExtension() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(UniquePropertiesCheck.class);
        final String[] expected = CommonUtil.EMPTY_STRING_ARRAY;
        verify(checkConfig, getPath("InputUniqueProperties.txt"), expected);
    }

    /**
     * Tests IO exception, that can occur during reading of properties file.
     */
    @Test
    public void testIoException() throws Exception {
        final DefaultConfiguration checkConfig = createModuleConfig(UniquePropertiesCheck.class);
        final UniquePropertiesCheck check = new UniquePropertiesCheck();
        check.configure(checkConfig);
        final String fileName =
                getPath("InputUniquePropertiesCheckNotExisting.properties");
        final File file = new File(fileName);
        final FileText fileText = new FileText(file, Collections.emptyList());
        final SortedSet<Violation> violations =
                check.process(file, fileText);
        assertWithMessage("Wrong messages count: " + violations.size())
            .that(violations)
            .hasSize(1);
        final Violation violation = violations.iterator().next();
        final String retrievedMessage = violations.iterator().next().getKey();
        assertWithMessage("violation key '" + retrievedMessage + "' is not valid")
            .that(retrievedMessage)
            .isEqualTo("unable.open.cause");
        assertWithMessage("violation '" + violation.getViolation() + "' is not valid")
            .that(getCheckMessage(MSG_IO_EXCEPTION_KEY, fileName, getFileNotFoundDetail(file)))
            .isEqualTo(violation.getViolation());
    }

    @Test
    public void testWrongKeyTypeInProperties() throws Exception {
        final Class<?> uniquePropertiesClass = Class
                .forName("com.puppycrawl.tools.checkstyle.checks."
                    + "UniquePropertiesCheck$UniqueProperties");
        final Constructor<?> constructor = uniquePropertiesClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object uniqueProperties = constructor.newInstance();
        final Method method = uniqueProperties.getClass().getDeclaredMethod("put", Object.class,
                Object.class);
        final Object result = method.invoke(uniqueProperties, 1, "value");
        final Map<Object, Object> table = new HashMap<>();
        final Object expected = table.put(1, "value");
        assertWithMessage("Invalid result of put method")
            .that(result)
            .isEqualTo(expected);

        final Object result2 = method.invoke(uniqueProperties, 1, "value");
        final Object expected2 = table.put(1, "value");
        assertWithMessage("Value should be substituted")
            .that(result2)
            .isEqualTo(expected2);
    }

    /**
     * Method generates NoSuchFileException details. It tries to open a file that does not exist.
     *
     * @param file to be opened
     * @return localized detail message of {@link NoSuchFileException}
     */
    private static String getFileNotFoundDetail(File file) {
        // Create exception to know detail message we should wait in
        // LocalisedMessage
        try (InputStream stream = Files.newInputStream(file.toPath())) {
            throw new IllegalStateException("File " + file.getPath() + " should not exist");
        }
        catch (IOException ex) {
            return ex.getLocalizedMessage();
        }
    }

}
