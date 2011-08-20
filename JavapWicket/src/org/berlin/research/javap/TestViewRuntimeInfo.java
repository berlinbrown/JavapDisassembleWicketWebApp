/**
 * Copyright Berlin Brown
 * Simple web application for javap, disassemble java classes
 *
 * To run - simply launch org.berlin.research.net.WebServerStart
 *
 * Tested with Java6, Jetty, Wicket1.4.13
 *
 * keywords: javap, java, java6, scala, jetty
 */
package org.berlin.research.javap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.berlin.research.javap.TestRuntimeWrapper.ExtractClassData;
import org.berlin.research.javap.TestRuntimeWrapper.IExtractClassData;

/**
 * Test Page for Create and Read Operations.
 * Complete web application for Javap operations.
 *
 * <pre>
 * Add runtime to WicketApplication.init:
 * mountBookmarkablePage("/runtime", TestViewRuntimeInfo.class);
 * </pre>
 */
public class TestViewRuntimeInfo extends WebPage {

    private final static Logger LOGGER = Logger.getLogger(TestViewRuntimeInfo.class);

    public static final class Data implements Serializable {
        private static final long serialVersionUID = 1L;
        private String val = "";

        /**
         * @return the val
         */
        public String getVal() {
            return val;
        }

        /**
         * @param val the val to set
         */
        public void setVal(String val) {
            this.val = val;
        }
    };

    public static final class ClassFilePathEntry implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public static final class RuntimeData implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<ClassFilePathEntry> listClasspathEntry = new ArrayList<ClassFilePathEntry>();
        //private String objectClass = "java.lang.String";
        private String objectClass = "java.lang.String";
        private String assemblyJavaCode = "[No Data]";
        /**
         * @return the objectClass
         */
        public String getObjectClass() {
            return objectClass;
        }
        /**
         * @param objectClass the objectClass to set
         */
        public void setObjectClass(String objectClass) {
            this.objectClass = objectClass;
        }
        /**
         * @return the assemblyJavaCode
         */
        public String getAssemblyJavaCode() {
            return assemblyJavaCode;
        }
        /**
         * @param assemblyJavaCode the assemblyJavaCode to set
         */
        public void setAssemblyJavaCode(String assemblyJavaCode) {
            this.assemblyJavaCode = assemblyJavaCode;
        }
    }

    public TestViewRuntimeInfo() {
        super();
        LOGGER.info("At constructor for view runtime info");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        final Form<RuntimeData> form = new Form<RuntimeData>("form", new CompoundPropertyModel<RuntimeData>(new LoadableDetachableModel<RuntimeData>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected RuntimeData load() {
                final RuntimeData runtime = new RuntimeData();
                final ServletContext servletContext = WebApplication.get().getServletContext();
                final String contextPath = getWebRequestCycle().getWebRequest().getHttpServletRequest().getContextPath();
                LOGGER.info("Runtime Path[Test83] : " + contextPath);
                return runtime;
            }
        }));
        TestViewRuntimeInfo.this.add(form);
        final TextField<String> tf = new TextField<String>("objectClass");
        form.add(new TextField<String>("objectClass"));
        form.add(new AjaxButton("submitButton", form) {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                final Form<RuntimeData> formLoc = (Form<RuntimeData>) form;
                final StringBuilder buf = new StringBuilder();
                final IExtractClassData classData2 = new ExtractClassData();
                classData2.setVerbose(false);
                classData2.setInputClassName(tf.getInput());
                classData2.appMain(null);
                buf.append(classData2.getResult());

                final IExtractClassData classData3 = new ExtractClassData();
                classData3.setVerbose(true);
                classData3.setInputClassName(tf.getInput());
                classData3.appMain(null);
                buf.append(classData3.getResult());
                formLoc.getModelObject().setAssemblyJavaCode(highlightSyntax(buf.toString()));
                target.addComponent(form);
            }
        });
        form.add(new Label("assemblyJavaCode").setEscapeModelStrings(false));
    }

    /**
     * Simple seach and replace to highlight syntax.
     *
     * @return
     */
    public String highlightSyntax(final String indata) {
        String data = indata;
        data = data.replaceAll("final", "<span style='color:#7F0055; font-weight:bold'>final</span>");
        data = data.replaceAll("public", "<span style='color:#7F0055; font-weight:bold'>public</span>");
        data = data.replaceAll("private", "<span style='color:#7F0055; font-weight:bold'>private</span>");
        data = data.replaceAll("const", "<span style='color:#7F0055; font-weight:bold'>const</span>");
        data = data.replaceAll("Code:", "<span style='text-decoration:underline;color:#7F0055; font-weight:bold'>Code:</span>");
        data = data.replaceAll("invokevirtual", "<span style='color:#7F0055; font-weight:bold'>invokevirtual</span>");
        data = data.replaceAll("invokestatic", "<span style='color:#7F0055; font-weight:bold'>invokevirtual</span>");
        data = data.replaceAll("invokespecial", "<span style='color:#7F0055; font-weight:bold'>invokevirtual</span>");
        data = data.replaceAll("//Method", "<span style='color:#0000C6; font-weight:bold'>//Method</span>");

        data = data.replaceAll("Asciz", "<span style='text-decoration:underline;font-weight:bold'>Asciz</span>");
        data = data.replaceAll("BUILD_NUM", "<span style='text-decoration:underline;font-weight:bold'>BUILD_NUM</span>");

        return data;
    }

    public class ColorConstants {

        /*
        final static RGB PROC_INSTR = new RGB(128, 128, 128);
        final static RGB DEFAULT = new RGB(0, 0, 0);
        final static RGB WHITE = new RGB(255, 255, 255);
        final static RGB SCALA_TYPE = new RGB(0, 0, 198); // 0000C6
        final static RGB STRING_CONSTANT = new RGB(0, 0, 198);
        final static RGB SCALA_KEYWORD   = new RGB(127, 0,   85); // 7F0055
        final static RGB SCALA_COMMENT   = new RGB(63,  127, 95);
        final static RGB JAVADOC_COMMENT = new RGB(63,  95,  191);
        final static RGB SCALA_CHAR_TOKEN = new RGB(189, 134, 8);
        final static RGB SCALA_STANDARD_TYPE = new RGB(165, 32, 247);
        */
        /*
         * Additional colors
         * ------------------
         *
         * Light purple, Emacs string constant
         * 189, 142, 140
         *
         * Red = Emacs comment
         * 255,0,0
         *
         * Emacs var gold
         * 189, 134, 8
         *
         * Bright purple (val, var, etc)
         * 165, 32, 247
         *
         * Eclipse Task, blue
         * 123, 158, 189
         */
    }

} // End of the class
