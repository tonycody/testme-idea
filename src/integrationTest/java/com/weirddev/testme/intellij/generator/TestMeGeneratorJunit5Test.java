package com.weirddev.testme.intellij.generator;

import com.weirddev.testme.intellij.template.FileTemplateConfig;
import com.weirddev.testme.intellij.template.TemplateRegistry;
import com.weirddev.testme.intellij.template.context.Language;

/**
 * Date: 13/12/2016
 *
 * @author Yaron Yamin
 */
public class TestMeGeneratorJunit5Test extends TestMeGeneratorTestBase {

    public TestMeGeneratorJunit5Test() {
        super(TemplateRegistry.JUNIT5_MOCKITO_JAVA_TEMPLATE, "testJunit5", Language.Java);
    }

    public void testSimpleClass() throws Exception {
        doTest();
    }
    public void testVariousFieldTypes() throws Exception {
        doTest();
    }
    public void testArrays() throws Exception {
        doTest(false, false, true);
    }
    public void testMockReturned() throws Exception {
        doTest(FileTemplateConfig.getInstance());
    }

}
