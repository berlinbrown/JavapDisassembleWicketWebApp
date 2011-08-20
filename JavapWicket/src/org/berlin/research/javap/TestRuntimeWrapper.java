/**
 * Copyright Berlin Brown
 * Simple web application for javap, disassemble java classes
 * View Java bytecode in assembly format.
 * 
 * Tested with Java6, Jetty, Wicket1.4.13
 * 
 * keywords: javap, java, java6, scala, jetty, asm, bytecode
 * 
 * Visit: http://localhost:7181/run/
 */
package org.berlin.research.javap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Find runtime information.
 * Complete web application for Javap operations. 
 */
public class TestRuntimeWrapper {

    private final Vector<Object> classList = new Vector<Object>();
    private PrintWriter out;
    private final JavapEnvironment env = new JavapEnvironment();
    private static boolean errorOccurred = false;
    private static final String progname = "javap";
    
    /**
     * Constructor.
     * 
     * @param out
     */
    public TestRuntimeWrapper(PrintWriter out) {
        this.out = out;
    }

    public static void main(String argv[]) {
        /*
        simpleTest();
        */        
        entry(argv);
        if (errorOccurred) {
            System.exit(1);
        }        
    }

    /**
     * Example usage.
     */
    public static void simpleTest() {
        System.out.print("Running Simple Test");        
        final IExtractClassData classData2 = new ExtractClassData();
        classData2.setVerbose(false);
        classData2.setInputClassName("java.lang.String");
        classData2.appMain(null);        
        System.out.print(classData2.getResult());
        
        final IExtractClassData classData3 = new ExtractClassData();
        classData3.setVerbose(true);
        classData3.setInputClassName("java.lang.String");
        classData3.appMain(null);        
        System.out.print(classData3.getResult());
    }
    
    /**
     * Entry point for tool if you don't want System.exit() called.
     */
    public static void entry(String argv[]) {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        try {

            final TestRuntimeWrapper jpmain = new TestRuntimeWrapper(out);
            jpmain.perform(argv);

        } finally {
            out.close();
        }
    }

    /**
     * Process the arguments and perform the desired action
     */
    private void perform(String argv[]) {
        if (parseArguments(argv)) {
            displayResults();

        }
    }

    private void error(String msg) {
        errorOccurred = true;
        System.err.println(msg);
        System.err.flush();
    }

    /**
     * Print usage information
     */
    private void usage() {
        java.io.PrintStream out = System.out;
        out.println("Usage: " + progname + " <options> <classes>...");
        out.println();
        out.println("where options include:");
        out.println("   -c                        Disassemble the code");
        out.println("   -classpath <pathlist>     Specify where to find user class files");
        out.println("   -extdirs <dirs>           Override location of installed extensions");
        out.println("   -help                     Print this usage message");
        out.println("   -J<flag>                  Pass <flag> directly to the runtime system");
        out.println("   -l                        Print line number and local variable tables");
        out.println("   -public                   Show only public classes and members");
        out.println("   -protected                Show protected/public classes and members");
        out.println("   -package                  Show package/protected/public classes");
        out.println("                             and members (default)");
        out.println("   -private                  Show all classes and members");
        out.println("   -s                        Print internal type signatures");
        out.println("   -bootclasspath <pathlist> Override location of class files loaded");
        out.println("                             by the bootstrap class loader");
        out.println("   -verbose                  Print stack size, number of locals and args for methods");
        out.println("                             If verifying, print reasons for failure");
        out.println();
    }

    /**
     * Parse the command line arguments. Set flags, construct the class list and
     * create environment.
     */
    private boolean parseArguments(String argv[]) {

        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-")) {

                if (arg.equals("-l")) {
                    env.showLineAndLocal = true;
                } else if (arg.equals("-private") || arg.equals("-p")) {
                    env.showAccess = env.PRIVATE;
                } else if (arg.equals("-package")) {
                    env.showAccess = env.PACKAGE;
                } else if (arg.equals("-protected")) {
                    env.showAccess = env.PROTECTED;
                } else if (arg.equals("-public")) {
                    env.showAccess = env.PUBLIC;
                } else if (arg.equals("-c")) {
                    env.showDisassembled = true;
                } else if (arg.equals("-s")) {
                    env.showInternalSigs = true;
                } else if (arg.equals("-verbose")) {
                    env.showVerbose = true;
                } else if (arg.equals("-v")) {
                    env.showVerbose = true;
                } else if (arg.equals("-h")) {
                    error("-h is no longer available - use the 'javah' program");
                    return false;
                } else if (arg.equals("-verify")) {
                    error("-verify is no longer available - use 'java -verify'");
                    return false;
                } else if (arg.equals("-verify-verbose")) {
                    error("-verify is no longer available - use 'java -verify'");
                    return false;
                } else if (arg.equals("-help")) {
                    usage();
                    return false;
                } else if (arg.equals("-classpath")) {
                    if ((i + 1) < argv.length) {
                        env.classPathString = argv[++i];
                    } else {
                        error("-classpath requires argument");
                        usage();
                        return false;
                    }
                } else if (arg.equals("-bootclasspath")) {
                    if ((i + 1) < argv.length) {
                        env.bootClassPathString = argv[++i];
                    } else {
                        error("-bootclasspath requires argument");
                        usage();
                        return false;
                    }
                } else if (arg.equals("-extdirs")) {
                    if ((i + 1) < argv.length) {
                        env.extDirsString = argv[++i];
                    } else {
                        error("-extdirs requires argument");
                        usage();
                        return false;
                    }
                } else if (arg.equals("-all")) {
                    env.showallAttr = true;
                } else {
                    error("invalid flag: " + arg);
                    usage();
                    return false;
                }
            } else {
                classList.addElement(arg);
                env.nothingToDo = false;
            }
        }
        if (env.nothingToDo) {
            System.out.print("No classes were specified on the command line [v199.1].  Try -help.");
            errorOccurred = true;
            return false;
        }
        return true;
    } // End of Parse Arguments //

    /**
     * Display results
     */
    private void displayResults() {

        for (int i = 0; i < classList.size(); i++) {
            final String Name = (String) classList.elementAt(i);
            InputStream classin = env.getFileInputStream(Name);
            try {
                // actual do display
                JavapPrinter printer = new JavapPrinter(classin, out, env);
                printer.print();

            } catch (IllegalArgumentException exc) {
                error(exc.getMessage());
            }
        }

    } // End of the method //

    // /////////////////////////////////////////////////////
    //
    // /////////////////////////////////////////////////////

    /**
     * Stores exception table data in code attribute.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class TrapData {
        short start_pc, end_pc, handler_pc, catch_cpx;

        int num;

        /**
         * Read and store exception table data in code attribute.
         */
        public TrapData(DataInputStream in, int num) throws IOException {
            this.num = num;
            start_pc = in.readShort();
            end_pc = in.readShort();
            handler_pc = in.readShort();
            catch_cpx = in.readShort();
        }

        /**
         * returns recommended identifier
         */
        public String ident() {
            return "t" + num;
        }

    }

    public static class Tables implements Constants {
        /**
         * Define mnemocodes table.
         */
        static Hashtable mnemocodes = new Hashtable(301, 0.5f);

        static String opcExtNamesTab[] = new String[128];

        static String opcPrivExtNamesTab[] = new String[128];

        static void defineNonPriv(int opc, String mnem) {
            mnemocodes.put(opcExtNamesTab[opc] = mnem, new Integer(opc_nonpriv * 256 + opc));
        }

        static void definePriv(int opc, String mnem) {
            mnemocodes.put(opcPrivExtNamesTab[opc] = "priv_" + mnem, new Integer(opc_priv * 256 + opc));
        }

        static void defineExt(int opc, String mnem) {
            defineNonPriv(opc, mnem);
            definePriv(opc, mnem);
        }

        static {
            int k;
            for (k = 0; k < opc_wide; k++) {
                mnemocodes.put(opcNamesTab[k], new Integer(k));
            }
            for (k = opc_wide + 1; k < opcNamesTab.length; k++) {
                mnemocodes.put(opcNamesTab[k], new Integer(k));
            }
            mnemocodes.put("invokenonvirtual", new Integer(opc_invokespecial));

            mnemocodes.put("iload_w", new Integer(opc_iload_w));
            mnemocodes.put("lload_w", new Integer(opc_lload_w));
            mnemocodes.put("fload_w", new Integer(opc_fload_w));
            mnemocodes.put("dload_w", new Integer(opc_dload_w));
            mnemocodes.put("aload_w", new Integer(opc_aload_w));
            mnemocodes.put("istore_w", new Integer(opc_istore_w));
            mnemocodes.put("lstore_w", new Integer(opc_lstore_w));
            mnemocodes.put("fstore_w", new Integer(opc_fstore_w));
            mnemocodes.put("dstore_w", new Integer(opc_dstore_w));
            mnemocodes.put("astore_w", new Integer(opc_astore_w));
            mnemocodes.put("ret_w", new Integer(opc_ret_w));
            mnemocodes.put("iinc_w", new Integer(opc_iinc_w));

            mnemocodes.put("nonpriv", new Integer(opc_nonpriv));
            mnemocodes.put("priv", new Integer(opc_priv));

            defineExt(0, "load_ubyte");
            defineExt(1, "load_byte");
            defineExt(2, "load_char");
            defineExt(3, "load_short");
            defineExt(4, "load_word");
            defineExt(10, "load_char_oe");
            defineExt(11, "load_short_oe");
            defineExt(12, "load_word_oe");
            defineExt(16, "ncload_ubyte");
            defineExt(17, "ncload_byte");
            defineExt(18, "ncload_char");
            defineExt(19, "ncload_short");
            defineExt(20, "ncload_word");
            defineExt(26, "ncload_char_oe");
            defineExt(27, "ncload_short_oe");
            defineExt(28, "ncload_word_oe");
            defineExt(30, "cache_flush");
            defineExt(32, "store_byte");
            defineExt(34, "store_short");
            defineExt(36, "store_word");
            defineExt(42, "store_short_oe");
            defineExt(44, "store_word_oe");
            defineExt(48, "ncstore_byte");
            defineExt(50, "ncstore_short");
            defineExt(52, "ncstore_word");
            defineExt(58, "ncstore_short_oe");
            defineExt(60, "ncstore_word_oe");
            defineExt(62, "zero_line");
            defineNonPriv(5, "ret_from_sub");
            defineNonPriv(63, "enter_sync_method");
            definePriv(5, "ret_from_trap");
            definePriv(6, "read_dcache_tag");
            definePriv(7, "read_dcache_data");
            definePriv(14, "read_icache_tag");
            definePriv(15, "read_icache_data");
            definePriv(22, "powerdown");
            definePriv(23, "read_scache_data");
            definePriv(31, "cache_index_flush");
            definePriv(38, "write_dcache_tag");
            definePriv(39, "write_dcache_data");
            definePriv(46, "write_icache_tag");
            definePriv(47, "write_icache_data");
            definePriv(54, "reset");
            definePriv(55, "write_scache_data");
            for (k = 0; k < 32; k++) {
                definePriv(k + 64, "read_reg_" + k);
            }
            for (k = 0; k < 32; k++) {
                definePriv(k + 96, "write_reg_" + k);
            }
        }

        public static int opcLength(int opc) throws ArrayIndexOutOfBoundsException {
            switch (opc >> 8) {
            case 0:
                return opcLengthsTab[opc];
            case opc_wide:
                switch (opc & 0xFF) {
                case opc_aload:
                case opc_astore:
                case opc_fload:
                case opc_fstore:
                case opc_iload:
                case opc_istore:
                case opc_lload:
                case opc_lstore:
                case opc_dload:
                case opc_dstore:
                case opc_ret:
                    return 4;
                case opc_iinc:
                    return 6;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            case opc_nonpriv:
            case opc_priv:
                return 2;
            default:
                throw new ArrayIndexOutOfBoundsException();
            }
        }

        public static String opcName(int opc) {
            try {
                switch (opc >> 8) {
                case 0:
                    return opcNamesTab[opc];
                case opc_wide: {
                    String mnem = opcNamesTab[opc & 0xFF] + "_w";
                    if (mnemocodes.get(mnem) == null)
                        return null; // non-existent opcode
                    return mnem;
                }
                case opc_nonpriv:
                    return opcExtNamesTab[opc & 0xFF];
                case opc_priv:
                    return opcPrivExtNamesTab[opc & 0xFF];
                default:
                    return null;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                switch (opc) {
                case opc_nonpriv:
                    return "nonpriv";
                case opc_priv:
                    return "priv";
                default:
                    return null;
                }
            }
        }

        public static int opcode(String mnem) {
            Integer Val = (Integer) (mnemocodes.get(mnem));
            if (Val == null)
                return -1;
            return Val.intValue();
        }

        /**
         * Initialized keyword and token Hashtables
         */
        static Vector keywordNames = new Vector(40);

        private static void defineKeywordName(String id, int token) {

            if (token >= keywordNames.size()) {
                keywordNames.setSize(token + 1);
            }
            keywordNames.setElementAt(id, token);
        }

        public static String keywordName(int token) {
            if (token == -1)
                return "EOF";
            if (token >= keywordNames.size())
                return null;
            return (String) keywordNames.elementAt(token);
        }

        static {
            defineKeywordName("ident", IDENT);
            defineKeywordName("STRINGVAL", STRINGVAL);
            defineKeywordName("intVal", INTVAL);
            defineKeywordName("longVal", LONGVAL);
            defineKeywordName("floatVal", FLOATVAL);
            defineKeywordName("doubleVal", DOUBLEVAL);
            defineKeywordName("SEMICOLON", SEMICOLON);
            defineKeywordName("COLON", COLON);
            defineKeywordName("LBRACE", LBRACE);
            defineKeywordName("RBRACE", RBRACE);
        }

        static Hashtable keywords = new Hashtable(40);

        public static int keyword(String idValue) {
            Integer Val = (Integer) (keywords.get(idValue));
            if (Val == null)
                return IDENT;
            return Val.intValue();
        }

        private static void defineKeyword(String id, int token) {
            keywords.put(id, new Integer(token));
            defineKeywordName(id, token);
        }

        static {
            // Modifier keywords
            defineKeyword("private", Constants.PRIVATE);
            defineKeyword("public", Constants.PUBLIC);
            defineKeyword("protected", Constants.PROTECTED);
            defineKeyword("static", Constants.STATIC);
            defineKeyword("transient", Constants.TRANSIENT);
            defineKeyword("synchronized", Constants.SYNCHRONIZED);
            defineKeyword("super", Constants.SUPER);
            defineKeyword("native", Constants.NATIVE);
            defineKeyword("abstract", Constants.ABSTRACT);
            defineKeyword("volatile", Constants.VOLATILE);
            defineKeyword("final", Constants.FINAL);
            defineKeyword("interface", Constants.INTERFACE);
            defineKeyword("synthetic", Constants.SYNTHETIC);
            defineKeyword("strict", Constants.STRICT);

            // Declaration keywords
            defineKeyword("package", Constants.PACKAGE);
            defineKeyword("class", Constants.CLASS);
            defineKeyword("extends", Constants.EXTENDS);
            defineKeyword("implements", Constants.IMPLEMENTS);
            defineKeyword("const", Constants.CONST);
            defineKeyword("throws", Constants.THROWS);
            defineKeyword("interface", Constants.INTERFACE);
            defineKeyword("Method", Constants.METHODREF);
            defineKeyword("Field", Constants.FIELDREF);
            defineKeyword("stack", Constants.STACK);
            defineKeyword("locals", Constants.LOCAL);

            // used in switchtables
            defineKeyword("default", DEFAULT);

            // used in inner class declarations
            defineKeyword("InnerClass", INNERCLASS);
            defineKeyword("of", OF);

            // misc
            defineKeyword("bits", BITS);
            defineKeyword("Infinity", INF);
            defineKeyword("Inf", INF);
            defineKeyword("NaN", NAN);
        }

        /**
         * Define tag table.
         */
        private static Vector tagNames = new Vector(10);

        private static Hashtable Tags = new Hashtable(10);
        static {
            defineTag("Asciz", RuntimeConstants.CONSTANT_UTF8);
            defineTag("int", RuntimeConstants.CONSTANT_INTEGER);
            defineTag("float", RuntimeConstants.CONSTANT_FLOAT);
            defineTag("long", RuntimeConstants.CONSTANT_LONG);
            defineTag("double", RuntimeConstants.CONSTANT_DOUBLE);
            defineTag("class", RuntimeConstants.CONSTANT_CLASS);
            defineTag("String", RuntimeConstants.CONSTANT_STRING);
            defineTag("Field", RuntimeConstants.CONSTANT_FIELD);
            defineTag("Method", RuntimeConstants.CONSTANT_METHOD);
            defineTag("InterfaceMethod", RuntimeConstants.CONSTANT_INTERFACEMETHOD);
            defineTag("NameAndType", RuntimeConstants.CONSTANT_NAMEANDTYPE);
        }

        private static void defineTag(String id, int val) {
            Tags.put(id, new Integer(val));
            if (val >= tagNames.size()) {
                tagNames.setSize(val + 1);
            }
            tagNames.setElementAt(id, val);
        }

        public static String tagName(int tag) {
            if (tag >= tagNames.size())
                return null;
            return (String) tagNames.elementAt(tag);
        }

        public static int tagValue(String idValue) {
            Integer Val = (Integer) (Tags.get(idValue));
            if (Val == null)
                return 0;
            return Val.intValue();
        }

        /**
         * Define type table. These types used in "newarray" instruction only.
         */
        private static Vector typeNames = new Vector(10);

        private static Hashtable Types = new Hashtable(10);
        static {
            defineType("int", RuntimeConstants.T_INT);
            defineType("long", RuntimeConstants.T_LONG);
            defineType("float", RuntimeConstants.T_FLOAT);
            defineType("double", RuntimeConstants.T_DOUBLE);
            defineType("class", RuntimeConstants.T_CLASS);
            defineType("boolean", RuntimeConstants.T_BOOLEAN);
            defineType("char", RuntimeConstants.T_CHAR);
            defineType("byte", RuntimeConstants.T_BYTE);
            defineType("short", RuntimeConstants.T_SHORT);
        }

        private static void defineType(String id, int val) {
            Types.put(id, new Integer(val));
            if (val >= typeNames.size()) {
                typeNames.setSize(val + 1);
            }
            typeNames.setElementAt(id, val);
        }

        public static int typeValue(String idValue) {
            Integer Val = (Integer) (Types.get(idValue));
            if (Val == null)
                return -1;
            return Val.intValue();
        }

        public static String typeName(int type) {
            if (type >= typeNames.size())
                return null;
            return (String) typeNames.elementAt(type);
        }

        /**
         * Define MapTypes table. These constants used in stackmap tables only.
         */
        private static Vector mapTypeNames = new Vector(10);

        private static Hashtable MapTypes = new Hashtable(10);
        static {
            defineMapType("bogus", RuntimeConstants.ITEM_Bogus);
            defineMapType("int", RuntimeConstants.ITEM_Integer);
            defineMapType("float", RuntimeConstants.ITEM_Float);
            defineMapType("double", RuntimeConstants.ITEM_Double);
            defineMapType("long", RuntimeConstants.ITEM_Long);
            defineMapType("null", RuntimeConstants.ITEM_Null);
            defineMapType("this", RuntimeConstants.ITEM_InitObject);
            defineMapType("CP", RuntimeConstants.ITEM_Object);
            defineMapType("uninitialized", RuntimeConstants.ITEM_NewObject);
        }

        private static void defineMapType(String id, int val) {
            MapTypes.put(id, new Integer(val));
            if (val >= mapTypeNames.size()) {
                mapTypeNames.setSize(val + 1);
            }
            mapTypeNames.setElementAt(id, val);
        }

        public static int mapTypeValue(String idValue) {
            Integer Val = (Integer) (MapTypes.get(idValue));
            if (Val == null)
                return -1;
            return Val.intValue();
        }

        public static String mapTypeName(int type) {
            if (type >= mapTypeNames.size())
                return null;
            return (String) mapTypeNames.elementAt(type);
        }

    }

    /*
     * represents one entry of StackMapTable attribute
     */
    public static class StackMapTableData {

        final int frameType;

        int offsetDelta;

        StackMapTableData(int frameType) {
            this.frameType = frameType;
        }

        void print(JavapPrinter p) {
            p.out.print("   frame_type = " + frameType);
        }

        static class SameFrame extends StackMapTableData {
            SameFrame(int frameType, int offsetDelta) {
                super(frameType);
                this.offsetDelta = offsetDelta;
            }

            void print(JavapPrinter p) {
                super.print(p);
                if (frameType < RuntimeConstants.SAME_FRAME_BOUND) {
                    p.out.println(" /* same */");
                } else {
                    p.out.println(" /* same_frame_extended */");
                    p.out.println("     offset_delta = " + offsetDelta);
                }
            }
        }

        static class SameLocals1StackItem extends StackMapTableData {
            final int[] stack;

            SameLocals1StackItem(int frameType, int offsetDelta, int[] stack) {
                super(frameType);
                this.offsetDelta = offsetDelta;
                this.stack = stack;
            }

            void print(JavapPrinter p) {
                super.print(p);
                if (frameType == RuntimeConstants.SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
                    p.out.println(" /* same_locals_1_stack_item_frame_extended */");
                    p.out.println("     offset_delta = " + offsetDelta);
                } else {
                    p.out.println(" /* same_locals_1_stack_item */");
                }
                p.printMap("     stack = [", stack);
            }
        }

        static class ChopFrame extends StackMapTableData {
            ChopFrame(int frameType, int offsetDelta) {
                super(frameType);
                this.offsetDelta = offsetDelta;
            }

            void print(JavapPrinter p) {
                super.print(p);
                p.out.println(" /* chop */");
                p.out.println("     offset_delta = " + offsetDelta);
            }
        }

        static class AppendFrame extends StackMapTableData {
            final int[] locals;

            AppendFrame(int frameType, int offsetDelta, int[] locals) {
                super(frameType);
                this.offsetDelta = offsetDelta;
                this.locals = locals;
            }

            void print(JavapPrinter p) {
                super.print(p);
                p.out.println(" /* append */");
                p.out.println("     offset_delta = " + offsetDelta);
                p.printMap("     locals = [", locals);
            }
        }

        static class FullFrame extends StackMapTableData {
            final int[] locals;

            final int[] stack;

            FullFrame(int offsetDelta, int[] locals, int[] stack) {
                super(RuntimeConstants.FULL_FRAME);
                this.offsetDelta = offsetDelta;
                this.locals = locals;
                this.stack = stack;
            }

            void print(JavapPrinter p) {
                super.print(p);
                p.out.println(" /* full_frame */");
                p.out.println("     offset_delta = " + offsetDelta);
                p.printMap("     locals = [", locals);
                p.printMap("     stack = [", stack);
            }
        }

        static StackMapTableData getInstance(DataInputStream in, MethodData method) throws IOException {

            int frameType = in.readUnsignedByte();

            if (frameType < RuntimeConstants.SAME_FRAME_BOUND) {
                // same_frame
                return new SameFrame(frameType, frameType);
            } else if (RuntimeConstants.SAME_FRAME_BOUND <= frameType
                    && frameType < RuntimeConstants.SAME_LOCALS_1_STACK_ITEM_BOUND) {
                // same_locals_1_stack_item_frame
                // read additional single stack element
                return new SameLocals1StackItem(frameType, (frameType - RuntimeConstants.SAME_FRAME_BOUND),
                        StackMapData.readTypeArray(in, 1, method));
            } else if (frameType == RuntimeConstants.SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
                // same_locals_1_stack_item_extended
                return new SameLocals1StackItem(frameType, in.readUnsignedShort(), StackMapData.readTypeArray(in, 1,
                        method));
            } else if (RuntimeConstants.SAME_LOCALS_1_STACK_ITEM_EXTENDED < frameType
                    && frameType < RuntimeConstants.SAME_FRAME_EXTENDED) {
                // chop_frame or same_frame_extended
                return new ChopFrame(frameType, in.readUnsignedShort());
            } else if (frameType == RuntimeConstants.SAME_FRAME_EXTENDED) {
                // chop_frame or same_frame_extended
                return new SameFrame(frameType, in.readUnsignedShort());
            } else if (RuntimeConstants.SAME_FRAME_EXTENDED < frameType && frameType < RuntimeConstants.FULL_FRAME) {
                // append_frame
                return new AppendFrame(frameType, in.readUnsignedShort(), StackMapData.readTypeArray(in, frameType
                        - RuntimeConstants.SAME_FRAME_EXTENDED, method));
            } else if (frameType == RuntimeConstants.FULL_FRAME) {
                // full_frame
                int offsetDelta = in.readUnsignedShort();
                int locals_size = in.readUnsignedShort();
                int[] locals = StackMapData.readTypeArray(in, locals_size, method);
                int stack_size = in.readUnsignedShort();
                int[] stack = StackMapData.readTypeArray(in, stack_size, method);
                return new FullFrame(offsetDelta, locals, stack);
            } else {
                throw new ClassFormatError("unrecognized frame_type in StackMapTable");
            }
        }

    } // End of the Class //

    /**
     * Returns java type signature.
     * 
     * @author Sucheta Dambalkar
     */
    public static class TypeSignature {

        String parameters = null;

        String returntype = null;

        String fieldtype = null;

        int argumentlength = 0;

        public TypeSignature(String JVMSignature) {

            if (JVMSignature != null) {
                if (JVMSignature.indexOf("(") == -1) {
                    // This is a field type.
                    this.fieldtype = getFieldTypeSignature(JVMSignature);
                } else {
                    String parameterdes = null;
                    if ((JVMSignature.indexOf(")") - 1) > (JVMSignature.indexOf("("))) {
                        // Get parameter signature.
                        parameterdes = JVMSignature.substring(JVMSignature.indexOf("(") + 1, JVMSignature.indexOf(")"));
                        this.parameters = getParametersHelper(parameterdes);
                    } else
                        this.parameters = "()";
                    // Get return type signature.
                    String returndes = JVMSignature.substring(JVMSignature.lastIndexOf(")") + 1);
                    this.returntype = getReturnTypeHelper(returndes);
                }
            }
        }

        /**
         * Returns java type signature of a field.
         */
        public String getFieldTypeSignature(String fielddes) {
            if (fielddes.startsWith("L")) {
                return (getObjectType(fielddes));
            } else if (fielddes.startsWith("[")) {
                return (getArrayType(fielddes));
            } else
                return (getBaseType(fielddes));
        }

        /**
         * Returns java type signature of a parameter.
         */
        public String getParametersHelper(String parameterdes) {
            Vector parameters = new Vector();
            int startindex = -1;
            int endindex = -1;
            String param = "";

            while (parameterdes != null) {

                if (parameterdes.startsWith("L")) {
                    // parameter is a object.
                    startindex = parameterdes.indexOf("L");
                    endindex = parameterdes.indexOf(";");
                    if (startindex < parameterdes.length()) {
                        if (endindex == parameterdes.length() - 1) {
                            // last parameter
                            param = parameterdes.substring(startindex);
                            parameterdes = null;
                        } else if (endindex + 1 < parameterdes.length()) {
                            // rest parameters
                            param = parameterdes.substring(startindex, endindex + 1);
                            parameterdes = parameterdes.substring(endindex + 1);

                        }
                        parameters.add(getObjectType(param));
                    }
                } else if (parameterdes.startsWith("[")) {
                    // parameter is an array.
                    String componentType = "";
                    int enddim = -1;
                    int st = 0;
                    while (true) {
                        if (st < parameterdes.length()) {
                            if (parameterdes.charAt(st) == '[') {

                                enddim = st;
                                st++;
                            } else
                                break;
                        } else
                            break;
                    }

                    if (enddim + 1 < parameterdes.length()) {
                        /* Array dimension. */
                        param = parameterdes.substring(0, enddim + 1);

                    }

                    int stotherparam = param.lastIndexOf("[") + 1;

                    if (stotherparam < parameterdes.length()) {
                        componentType = parameterdes.substring(stotherparam);
                    }

                    if (componentType.startsWith("L")) {
                        // parameter is array of objects.
                        startindex = parameterdes.indexOf("L");
                        endindex = parameterdes.indexOf(";");

                        if (endindex == parameterdes.length() - 1) {
                            // last parameter
                            param += parameterdes.substring(startindex);
                            parameterdes = null;
                        } else if (endindex + 1 < parameterdes.length()) {
                            // rest parameters
                            param += parameterdes.substring(startindex, endindex + 1);
                            parameterdes = parameterdes.substring(endindex + 1);
                        }
                    } else {
                        // parameter is array of base type.
                        if (componentType.length() == 1) {
                            // last parameter.
                            param += componentType;
                            parameterdes = null;
                        } else if (componentType.length() > 1) {
                            // rest parameters.
                            param += componentType.substring(0, 1);
                            parameterdes = componentType.substring(1);
                        }
                    }
                    parameters.add(getArrayType(param));

                } else {

                    // parameter is of base type.
                    if (parameterdes.length() == 1) {
                        // last parameter
                        param = parameterdes;
                        parameterdes = null;
                    } else if (parameterdes.length() > 1) {
                        // rest parameters.
                        param = parameterdes.substring(0, 1);
                        parameterdes = parameterdes.substring(1);
                    }
                    parameters.add(getBaseType(param));
                }
            }

            /* number of arguments of a method. */
            argumentlength = parameters.size();

            /* java type signature. */
            String parametersignature = "(";
            int i;

            for (i = 0; i < parameters.size(); i++) {
                parametersignature += (String) parameters.elementAt(i);
                if (i != parameters.size() - 1) {
                    parametersignature += ", ";
                }
            }
            parametersignature += ")";
            return parametersignature;
        }

        /**
         * Returns java type signature for a return type.
         */
        public String getReturnTypeHelper(String returndes) {
            return getFieldTypeSignature(returndes);
        }

        /**
         * Returns java type signature for a base type.
         */
        public String getBaseType(String baseType) {
            if (baseType != null) {
                if (baseType.equals("B"))
                    return "byte";
                else if (baseType.equals("C"))
                    return "char";
                else if (baseType.equals("D"))
                    return "double";
                else if (baseType.equals("F"))
                    return "float";
                else if (baseType.equals("I"))
                    return "int";
                else if (baseType.equals("J"))
                    return "long";
                else if (baseType.equals("S"))
                    return "short";
                else if (baseType.equals("Z"))
                    return "boolean";
                else if (baseType.equals("V"))
                    return "void";
            }
            return null;
        }

        /**
         * Returns java type signature for a object type.
         */
        public String getObjectType(String JVMobjectType) {
            String objectType = "";
            int startindex = JVMobjectType.indexOf("L") + 1;
            int endindex = JVMobjectType.indexOf(";");
            if ((startindex != -1) && (endindex != -1)) {
                if ((startindex < JVMobjectType.length()) && (endindex < JVMobjectType.length())) {
                    objectType = JVMobjectType.substring(startindex, endindex);
                }
                objectType = objectType.replace('/', '.');
                return objectType;
            }
            return null;
        }

        /**
         * Returns java type signature for array type.
         */
        public String getArrayType(String arrayType) {
            if (arrayType != null) {
                String dimention = "";

                while (arrayType.indexOf("[") != -1) {
                    dimention += "[]";

                    int startindex = arrayType.indexOf("[") + 1;
                    if (startindex <= arrayType.length()) {
                        arrayType = arrayType.substring(startindex);
                    }
                }

                String componentType = "";
                if (arrayType.startsWith("L")) {
                    componentType = getObjectType(arrayType);
                } else {
                    componentType = getBaseType(arrayType);
                }
                return componentType + dimention;
            }
            return null;
        }

        /**
         * Returns java type signature for parameters.
         */
        public String getParameters() {
            return parameters;
        }

        /**
         * Returns java type signature for return type.
         */
        public String getReturnType() {
            return returntype;
        }

        /**
         * Returns java type signature for field type.
         */
        public String getFieldType() {
            return fieldtype;
        }

        /**
         * Return number of arguments of a method.
         */
        public int getArgumentlength() {
            return argumentlength;
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    public interface RuntimeConstants {

        /* Signature Characters */
        public static final char SIGC_VOID = 'V';

        public static final String SIG_VOID = "V";

        public static final char SIGC_BOOLEAN = 'Z';

        public static final String SIG_BOOLEAN = "Z";

        public static final char SIGC_BYTE = 'B';

        public static final String SIG_BYTE = "B";

        public static final char SIGC_CHAR = 'C';

        public static final String SIG_CHAR = "C";

        public static final char SIGC_SHORT = 'S';

        public static final String SIG_SHORT = "S";

        public static final char SIGC_INT = 'I';

        public static final String SIG_INT = "I";

        public static final char SIGC_LONG = 'J';

        public static final String SIG_LONG = "J";

        public static final char SIGC_FLOAT = 'F';

        public static final String SIG_FLOAT = "F";

        public static final char SIGC_DOUBLE = 'D';

        public static final String SIG_DOUBLE = "D";

        public static final char SIGC_ARRAY = '[';

        public static final String SIG_ARRAY = "[";

        public static final char SIGC_CLASS = 'L';

        public static final String SIG_CLASS = "L";

        public static final char SIGC_METHOD = '(';

        public static final String SIG_METHOD = "(";

        public static final char SIGC_ENDCLASS = ';';

        public static final String SIG_ENDCLASS = ";";

        public static final char SIGC_ENDMETHOD = ')';

        public static final String SIG_ENDMETHOD = ")";

        public static final char SIGC_PACKAGE = '/';

        public static final String SIG_PACKAGE = "/";

        /* Class File Constants */
        public static final int JAVA_MAGIC = 0xcafebabe;

        public static final int JAVA_VERSION = 45;

        public static final int JAVA_MINOR_VERSION = 3;

        /* Constant table */
        public static final int CONSTANT_UTF8 = 1;

        public static final int CONSTANT_UNICODE = 2;

        public static final int CONSTANT_INTEGER = 3;

        public static final int CONSTANT_FLOAT = 4;

        public static final int CONSTANT_LONG = 5;

        public static final int CONSTANT_DOUBLE = 6;

        public static final int CONSTANT_CLASS = 7;

        public static final int CONSTANT_STRING = 8;

        public static final int CONSTANT_FIELD = 9;

        public static final int CONSTANT_METHOD = 10;

        public static final int CONSTANT_INTERFACEMETHOD = 11;

        public static final int CONSTANT_NAMEANDTYPE = 12;

        /* Access Flags */
        public static final int ACC_PUBLIC = 0x00000001;

        public static final int ACC_PRIVATE = 0x00000002;

        public static final int ACC_PROTECTED = 0x00000004;

        public static final int ACC_STATIC = 0x00000008;

        public static final int ACC_FINAL = 0x00000010;

        public static final int ACC_SYNCHRONIZED = 0x00000020;

        public static final int ACC_SUPER = 0x00000020;

        public static final int ACC_VOLATILE = 0x00000040;

        public static final int ACC_TRANSIENT = 0x00000080;

        public static final int ACC_NATIVE = 0x00000100;

        public static final int ACC_INTERFACE = 0x00000200;

        public static final int ACC_ABSTRACT = 0x00000400;

        public static final int ACC_STRICT = 0x00000800;

        public static final int ACC_EXPLICIT = 0x00001000;

        public static final int ACC_SYNTHETIC = 0x00010000; // actually, this is

        // an attribute

        /* Type codes */
        public static final int T_CLASS = 0x00000002;

        public static final int T_BOOLEAN = 0x00000004;

        public static final int T_CHAR = 0x00000005;

        public static final int T_FLOAT = 0x00000006;

        public static final int T_DOUBLE = 0x00000007;

        public static final int T_BYTE = 0x00000008;

        public static final int T_SHORT = 0x00000009;

        public static final int T_INT = 0x0000000a;

        public static final int T_LONG = 0x0000000b;

        /* Type codes for StackMap attribute */
        public static final int ITEM_Bogus = 0; // an unknown or uninitialized

        // value

        public static final int ITEM_Integer = 1; // a 32-bit integer

        public static final int ITEM_Float = 2; // not used

        public static final int ITEM_Double = 3; // not used

        public static final int ITEM_Long = 4; // a 64-bit integer

        public static final int ITEM_Null = 5; // the type of null

        public static final int ITEM_InitObject = 6; // "this" in constructor

        public static final int ITEM_Object = 7; // followed by 2-byte index of

        // class name

        public static final int ITEM_NewObject = 8; // followed by 2-byte ref to

        // "new"

        /* Constants used in StackMapTable attribute */
        public static final int SAME_FRAME_BOUND = 64;

        public static final int SAME_LOCALS_1_STACK_ITEM_BOUND = 128;

        public static final int SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;

        public static final int SAME_FRAME_EXTENDED = 251;

        public static final int FULL_FRAME = 255;

        /* Opcodes */
        public static final int opc_dead = -2;

        public static final int opc_label = -1;

        public static final int opc_nop = 0;

        public static final int opc_aconst_null = 1;

        public static final int opc_iconst_m1 = 2;

        public static final int opc_iconst_0 = 3;

        public static final int opc_iconst_1 = 4;

        public static final int opc_iconst_2 = 5;

        public static final int opc_iconst_3 = 6;

        public static final int opc_iconst_4 = 7;

        public static final int opc_iconst_5 = 8;

        public static final int opc_lconst_0 = 9;

        public static final int opc_lconst_1 = 10;

        public static final int opc_fconst_0 = 11;

        public static final int opc_fconst_1 = 12;

        public static final int opc_fconst_2 = 13;

        public static final int opc_dconst_0 = 14;

        public static final int opc_dconst_1 = 15;

        public static final int opc_bipush = 16;

        public static final int opc_sipush = 17;

        public static final int opc_ldc = 18;

        public static final int opc_ldc_w = 19;

        public static final int opc_ldc2_w = 20;

        public static final int opc_iload = 21;

        public static final int opc_lload = 22;

        public static final int opc_fload = 23;

        public static final int opc_dload = 24;

        public static final int opc_aload = 25;

        public static final int opc_iload_0 = 26;

        public static final int opc_iload_1 = 27;

        public static final int opc_iload_2 = 28;

        public static final int opc_iload_3 = 29;

        public static final int opc_lload_0 = 30;

        public static final int opc_lload_1 = 31;

        public static final int opc_lload_2 = 32;

        public static final int opc_lload_3 = 33;

        public static final int opc_fload_0 = 34;

        public static final int opc_fload_1 = 35;

        public static final int opc_fload_2 = 36;

        public static final int opc_fload_3 = 37;

        public static final int opc_dload_0 = 38;

        public static final int opc_dload_1 = 39;

        public static final int opc_dload_2 = 40;

        public static final int opc_dload_3 = 41;

        public static final int opc_aload_0 = 42;

        public static final int opc_aload_1 = 43;

        public static final int opc_aload_2 = 44;

        public static final int opc_aload_3 = 45;

        public static final int opc_iaload = 46;

        public static final int opc_laload = 47;

        public static final int opc_faload = 48;

        public static final int opc_daload = 49;

        public static final int opc_aaload = 50;

        public static final int opc_baload = 51;

        public static final int opc_caload = 52;

        public static final int opc_saload = 53;

        public static final int opc_istore = 54;

        public static final int opc_lstore = 55;

        public static final int opc_fstore = 56;

        public static final int opc_dstore = 57;

        public static final int opc_astore = 58;

        public static final int opc_istore_0 = 59;

        public static final int opc_istore_1 = 60;

        public static final int opc_istore_2 = 61;

        public static final int opc_istore_3 = 62;

        public static final int opc_lstore_0 = 63;

        public static final int opc_lstore_1 = 64;

        public static final int opc_lstore_2 = 65;

        public static final int opc_lstore_3 = 66;

        public static final int opc_fstore_0 = 67;

        public static final int opc_fstore_1 = 68;

        public static final int opc_fstore_2 = 69;

        public static final int opc_fstore_3 = 70;

        public static final int opc_dstore_0 = 71;

        public static final int opc_dstore_1 = 72;

        public static final int opc_dstore_2 = 73;

        public static final int opc_dstore_3 = 74;

        public static final int opc_astore_0 = 75;

        public static final int opc_astore_1 = 76;

        public static final int opc_astore_2 = 77;

        public static final int opc_astore_3 = 78;

        public static final int opc_iastore = 79;

        public static final int opc_lastore = 80;

        public static final int opc_fastore = 81;

        public static final int opc_dastore = 82;

        public static final int opc_aastore = 83;

        public static final int opc_bastore = 84;

        public static final int opc_castore = 85;

        public static final int opc_sastore = 86;

        public static final int opc_pop = 87;

        public static final int opc_pop2 = 88;

        public static final int opc_dup = 89;

        public static final int opc_dup_x1 = 90;

        public static final int opc_dup_x2 = 91;

        public static final int opc_dup2 = 92;

        public static final int opc_dup2_x1 = 93;

        public static final int opc_dup2_x2 = 94;

        public static final int opc_swap = 95;

        public static final int opc_iadd = 96;

        public static final int opc_ladd = 97;

        public static final int opc_fadd = 98;

        public static final int opc_dadd = 99;

        public static final int opc_isub = 100;

        public static final int opc_lsub = 101;

        public static final int opc_fsub = 102;

        public static final int opc_dsub = 103;

        public static final int opc_imul = 104;

        public static final int opc_lmul = 105;

        public static final int opc_fmul = 106;

        public static final int opc_dmul = 107;

        public static final int opc_idiv = 108;

        public static final int opc_ldiv = 109;

        public static final int opc_fdiv = 110;

        public static final int opc_ddiv = 111;

        public static final int opc_irem = 112;

        public static final int opc_lrem = 113;

        public static final int opc_frem = 114;

        public static final int opc_drem = 115;

        public static final int opc_ineg = 116;

        public static final int opc_lneg = 117;

        public static final int opc_fneg = 118;

        public static final int opc_dneg = 119;

        public static final int opc_ishl = 120;

        public static final int opc_lshl = 121;

        public static final int opc_ishr = 122;

        public static final int opc_lshr = 123;

        public static final int opc_iushr = 124;

        public static final int opc_lushr = 125;

        public static final int opc_iand = 126;

        public static final int opc_land = 127;

        public static final int opc_ior = 128;

        public static final int opc_lor = 129;

        public static final int opc_ixor = 130;

        public static final int opc_lxor = 131;

        public static final int opc_iinc = 132;

        public static final int opc_i2l = 133;

        public static final int opc_i2f = 134;

        public static final int opc_i2d = 135;

        public static final int opc_l2i = 136;

        public static final int opc_l2f = 137;

        public static final int opc_l2d = 138;

        public static final int opc_f2i = 139;

        public static final int opc_f2l = 140;

        public static final int opc_f2d = 141;

        public static final int opc_d2i = 142;

        public static final int opc_d2l = 143;

        public static final int opc_d2f = 144;

        public static final int opc_i2b = 145;

        public static final int opc_int2byte = 145;

        public static final int opc_i2c = 146;

        public static final int opc_int2char = 146;

        public static final int opc_i2s = 147;

        public static final int opc_int2short = 147;

        public static final int opc_lcmp = 148;

        public static final int opc_fcmpl = 149;

        public static final int opc_fcmpg = 150;

        public static final int opc_dcmpl = 151;

        public static final int opc_dcmpg = 152;

        public static final int opc_ifeq = 153;

        public static final int opc_ifne = 154;

        public static final int opc_iflt = 155;

        public static final int opc_ifge = 156;

        public static final int opc_ifgt = 157;

        public static final int opc_ifle = 158;

        public static final int opc_if_icmpeq = 159;

        public static final int opc_if_icmpne = 160;

        public static final int opc_if_icmplt = 161;

        public static final int opc_if_icmpge = 162;

        public static final int opc_if_icmpgt = 163;

        public static final int opc_if_icmple = 164;

        public static final int opc_if_acmpeq = 165;

        public static final int opc_if_acmpne = 166;

        public static final int opc_goto = 167;

        public static final int opc_jsr = 168;

        public static final int opc_ret = 169;

        public static final int opc_tableswitch = 170;

        public static final int opc_lookupswitch = 171;

        public static final int opc_ireturn = 172;

        public static final int opc_lreturn = 173;

        public static final int opc_freturn = 174;

        public static final int opc_dreturn = 175;

        public static final int opc_areturn = 176;

        public static final int opc_return = 177;

        public static final int opc_getstatic = 178;

        public static final int opc_putstatic = 179;

        public static final int opc_getfield = 180;

        public static final int opc_putfield = 181;

        public static final int opc_invokevirtual = 182;

        public static final int opc_invokenonvirtual = 183;

        public static final int opc_invokespecial = 183;

        public static final int opc_invokestatic = 184;

        public static final int opc_invokeinterface = 185;

        // public static final int opc_xxxunusedxxx = 186;
        public static final int opc_new = 187;

        public static final int opc_newarray = 188;

        public static final int opc_anewarray = 189;

        public static final int opc_arraylength = 190;

        public static final int opc_athrow = 191;

        public static final int opc_checkcast = 192;

        public static final int opc_instanceof = 193;

        public static final int opc_monitorenter = 194;

        public static final int opc_monitorexit = 195;

        public static final int opc_wide = 196;

        public static final int opc_multianewarray = 197;

        public static final int opc_ifnull = 198;

        public static final int opc_ifnonnull = 199;

        public static final int opc_goto_w = 200;

        public static final int opc_jsr_w = 201;

        /* Pseudo-instructions */
        public static final int opc_bytecode = 203;

        public static final int opc_try = 204;

        public static final int opc_endtry = 205;

        public static final int opc_catch = 206;

        public static final int opc_var = 207;

        public static final int opc_endvar = 208;

        public static final int opc_localsmap = 209;

        public static final int opc_stackmap = 210;

        /* PicoJava prefixes */
        public static final int opc_nonpriv = 254;

        public static final int opc_priv = 255;

        /* Wide instructions */
        public static final int opc_iload_w = (opc_wide << 8) | opc_iload;

        public static final int opc_lload_w = (opc_wide << 8) | opc_lload;

        public static final int opc_fload_w = (opc_wide << 8) | opc_fload;

        public static final int opc_dload_w = (opc_wide << 8) | opc_dload;

        public static final int opc_aload_w = (opc_wide << 8) | opc_aload;

        public static final int opc_istore_w = (opc_wide << 8) | opc_istore;

        public static final int opc_lstore_w = (opc_wide << 8) | opc_lstore;

        public static final int opc_fstore_w = (opc_wide << 8) | opc_fstore;

        public static final int opc_dstore_w = (opc_wide << 8) | opc_dstore;

        public static final int opc_astore_w = (opc_wide << 8) | opc_astore;

        public static final int opc_ret_w = (opc_wide << 8) | opc_ret;

        public static final int opc_iinc_w = (opc_wide << 8) | opc_iinc;

        /* Opcode Names */
        public static final String opcNamesTab[] = { "nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1",
                "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1",
                "fconst_2", "dconst_0", "dconst_1", "bipush", "sipush", "ldc", "ldc_w", "ldc2_w", "iload", "lload",
                "fload", "dload", "aload", "iload_0", "iload_1", "iload_2", "iload_3", "lload_0", "lload_1", "lload_2",
                "lload_3", "fload_0", "fload_1", "fload_2", "fload_3", "dload_0", "dload_1", "dload_2", "dload_3",
                "aload_0", "aload_1", "aload_2", "aload_3", "iaload", "laload", "faload", "daload", "aaload", "baload",
                "caload", "saload", "istore", "lstore", "fstore", "dstore", "astore", "istore_0", "istore_1",
                "istore_2", "istore_3", "lstore_0", "lstore_1", "lstore_2", "lstore_3", "fstore_0", "fstore_1",
                "fstore_2", "fstore_3", "dstore_0", "dstore_1", "dstore_2", "dstore_3", "astore_0", "astore_1",
                "astore_2", "astore_3", "iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore",
                "sastore", "pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd",
                "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv",
                "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr",
                "lshr", "iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f", "i2d",
                "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl",
                "fcmpg", "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne",
                "if_icmplt", "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ret",
                "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn", "dreturn", "areturn", "return",
                "getstatic", "putstatic", "getfield", "putfield", "invokevirtual",
                "invokespecial", // was "invokenonvirtual",
                "invokestatic", "invokeinterface",
                "bytecode 186", // "xxxunusedxxx",
                "new", "newarray", "anewarray", "arraylength", "athrow", "checkcast", "instanceof", "monitorenter",
                "monitorexit", null, // "wide",
                "multianewarray", "ifnull", "ifnonnull", "goto_w", "jsr_w", "bytecode 202", // "breakpoint",
                "bytecode", "try", "endtry", "catch", "var", "endvar", "locals_map", "stack_map" };

        /* Opcode Lengths */
        public static final int opcLengthsTab[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3, 3, 2,
                2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2,
                2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3,
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 99, 99, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 0, 3, 2, 3, 1, 1,
                3, 3, 1, 1, 0, // wide
                4, 3, 3, 5, 5, 1, 1, 0, 0, 0, 0, 0 // pseudo
        };

    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Strores method data informastion.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class MethodData {

        ClassData cls;

        int access;

        int name_index;

        int descriptor_index;

        int attributes_count;

        byte[] code;

        Vector exception_table = new Vector(0);

        Vector lin_num_tb = new Vector(0);

        Vector loc_var_tb = new Vector(0);

        StackMapTableData[] stackMapTable;

        StackMapData[] stackMap;

        int[] exc_index_table = null;

        Vector attrs = new Vector(0);

        Vector code_attrs = new Vector(0);

        int max_stack, max_locals;

        boolean isSynthetic = false;

        boolean isDeprecated = false;

        public MethodData(ClassData cls) {
            this.cls = cls;
        }

        /**
         * Read method info.
         */
        public void read(DataInputStream in) throws IOException {
            access = in.readUnsignedShort();
            name_index = in.readUnsignedShort();
            descriptor_index = in.readUnsignedShort();
            int attributes_count = in.readUnsignedShort();
            for (int i = 0; i < attributes_count; i++) {
                int attr_name_index = in.readUnsignedShort();

                readAttr: {
                    if (cls.getTag(attr_name_index) == RuntimeConstants.CONSTANT_UTF8) {
                        String attr_name = cls.getString(attr_name_index);
                        if (attr_name.equals("Code")) {
                            readCode(in);
                            AttrData attr = new AttrData(cls);
                            attr.read(attr_name_index);
                            attrs.addElement(attr);
                            break readAttr;
                        } else if (attr_name.equals("Exceptions")) {
                            readExceptions(in);
                            AttrData attr = new AttrData(cls);
                            attr.read(attr_name_index);
                            attrs.addElement(attr);
                            break readAttr;
                        } else if (attr_name.equals("Synthetic")) {
                            if (in.readInt() != 0)
                                throw new ClassFormatError("invalid Synthetic attr length");
                            isSynthetic = true;
                            AttrData attr = new AttrData(cls);
                            attr.read(attr_name_index);
                            attrs.addElement(attr);
                            break readAttr;
                        } else if (attr_name.equals("Deprecated")) {
                            if (in.readInt() != 0)
                                throw new ClassFormatError("invalid Synthetic attr length");
                            isDeprecated = true;
                            AttrData attr = new AttrData(cls);
                            attr.read(attr_name_index);
                            attrs.addElement(attr);
                            break readAttr;
                        }
                    }
                    AttrData attr = new AttrData(cls);
                    attr.read(attr_name_index, in);
                    attrs.addElement(attr);
                }
            }
        }

        /**
         * Read code attribute info.
         */
        public void readCode(DataInputStream in) throws IOException {

            int attr_length = in.readInt();
            max_stack = in.readUnsignedShort();
            max_locals = in.readUnsignedShort();
            int codelen = in.readInt();

            code = new byte[codelen];
            int totalread = 0;
            while (totalread < codelen) {
                totalread += in.read(code, totalread, codelen - totalread);
            }
            // in.read(code, 0, codelen);
            int clen = 0;
            readExceptionTable(in);
            int code_attributes_count = in.readUnsignedShort();

            for (int k = 0; k < code_attributes_count; k++) {
                int table_name_index = in.readUnsignedShort();
                int table_name_tag = cls.getTag(table_name_index);
                AttrData attr = new AttrData(cls);
                if (table_name_tag == RuntimeConstants.CONSTANT_UTF8) {
                    String table_name_tstr = cls.getString(table_name_index);
                    if (table_name_tstr.equals("LineNumberTable")) {
                        readLineNumTable(in);
                        attr.read(table_name_index);
                    } else if (table_name_tstr.equals("LocalVariableTable")) {
                        readLocVarTable(in);
                        attr.read(table_name_index);
                    } else if (table_name_tstr.equals("StackMapTable")) {
                        readStackMapTable(in);
                        attr.read(table_name_index);
                    } else if (table_name_tstr.equals("StackMap")) {
                        readStackMap(in);
                        attr.read(table_name_index);
                    } else {
                        attr.read(table_name_index, in);
                    }
                    code_attrs.addElement(attr);
                    continue;
                }

                attr.read(table_name_index, in);
                code_attrs.addElement(attr);
            }
        }

        /**
         * Read exception table info.
         */
        void readExceptionTable(DataInputStream in) throws IOException {
            int exception_table_len = in.readUnsignedShort();
            exception_table = new Vector(exception_table_len);
            for (int l = 0; l < exception_table_len; l++) {
                exception_table.addElement(new TrapData(in, l));
            }
        }

        /**
         * Read LineNumberTable attribute info.
         */
        void readLineNumTable(DataInputStream in) throws IOException {
            int attr_len = in.readInt(); // attr_length
            int lin_num_tb_len = in.readUnsignedShort();
            lin_num_tb = new Vector(lin_num_tb_len);
            for (int l = 0; l < lin_num_tb_len; l++) {
                lin_num_tb.addElement(new LineNumData(in));
            }
        }

        /**
         * Read LocalVariableTable attribute info.
         */
        void readLocVarTable(DataInputStream in) throws IOException {
            int attr_len = in.readInt(); // attr_length
            int loc_var_tb_len = in.readUnsignedShort();
            loc_var_tb = new Vector(loc_var_tb_len);
            for (int l = 0; l < loc_var_tb_len; l++) {
                loc_var_tb.addElement(new LocVarData(in));
            }
        }

        /**
         * Read Exception attribute info.
         */
        public void readExceptions(DataInputStream in) throws IOException {
            int attr_len = in.readInt(); // attr_length in prog
            int num_exceptions = in.readUnsignedShort();
            exc_index_table = new int[num_exceptions];
            for (int l = 0; l < num_exceptions; l++) {
                int exc = in.readShort();
                exc_index_table[l] = exc;
            }
        }

        /**
         * Read StackMapTable attribute info.
         */
        void readStackMapTable(DataInputStream in) throws IOException {
            int attr_len = in.readInt(); // attr_length
            int stack_map_tb_len = in.readUnsignedShort();
            stackMapTable = new StackMapTableData[stack_map_tb_len];
            for (int i = 0; i < stack_map_tb_len; i++) {
                stackMapTable[i] = StackMapTableData.getInstance(in, this);
            }
        }

        /**
         * Read StackMap attribute info.
         */
        void readStackMap(DataInputStream in) throws IOException {
            int attr_len = in.readInt(); // attr_length
            int stack_map_len = in.readUnsignedShort();
            stackMap = new StackMapData[stack_map_len];
            for (int i = 0; i < stack_map_len; i++) {
                stackMap[i] = new StackMapData(in, this);
            }
        }

        /**
         * Return access of the method.
         */
        public String[] getAccess() {

            Vector v = new Vector();
            if ((access & RuntimeConstants.ACC_PUBLIC) != 0)
                v.addElement("public");
            if ((access & RuntimeConstants.ACC_PRIVATE) != 0)
                v.addElement("private");
            if ((access & RuntimeConstants.ACC_PROTECTED) != 0)
                v.addElement("protected");
            if ((access & RuntimeConstants.ACC_STATIC) != 0)
                v.addElement("static");
            if ((access & RuntimeConstants.ACC_FINAL) != 0)
                v.addElement("final");
            if ((access & RuntimeConstants.ACC_SYNCHRONIZED) != 0)
                v.addElement("synchronized");
            if ((access & RuntimeConstants.ACC_NATIVE) != 0)
                v.addElement("native");
            if ((access & RuntimeConstants.ACC_ABSTRACT) != 0)
                v.addElement("abstract");
            if ((access & RuntimeConstants.ACC_STRICT) != 0)
                v.addElement("strictfp");

            String[] accflags = new String[v.size()];
            v.copyInto(accflags);
            return accflags;
        }

        /**
         * Return name of the method.
         */
        public String getName() {
            return cls.getStringValue(name_index);
        }

        /**
         * Return internal siganature of the method.
         */
        public String getInternalSig() {
            return cls.getStringValue(descriptor_index);
        }

        /**
         * Return java return type signature of method.
         */
        public String getReturnType() {

            String rttype = (new TypeSignature(getInternalSig())).getReturnType();
            return rttype;
        }

        /**
         * Return java type parameter signature.
         */
        public String getParameters() {
            String ptype = (new TypeSignature(getInternalSig())).getParameters();

            return ptype;
        }

        /**
         * Return code attribute data of a method.
         */
        public byte[] getCode() {
            return code;
        }

        /**
         * Return LineNumberTable size.
         */
        public int getnumlines() {
            return lin_num_tb.size();
        }

        /**
         * Return LineNumberTable
         */
        public Vector getlin_num_tb() {
            return lin_num_tb;
        }

        /**
         * Return LocalVariableTable size.
         */
        public int getloc_var_tbsize() {
            return loc_var_tb.size();
        }

        /**
         * Return LocalVariableTable.
         */
        public Vector getloc_var_tb() {
            return loc_var_tb;
        }

        /**
         * Return StackMap.
         */
        public StackMapData[] getStackMap() {
            return stackMap;
        }

        /**
         * Return StackMapTable.
         */
        public StackMapTableData[] getStackMapTable() {
            return stackMapTable;
        }

        /**
         * Return number of arguments of that method.
         */
        public int getArgumentlength() {
            return new TypeSignature(getInternalSig()).getArgumentlength();
        }

        /**
         * Return true if method is static
         */
        public boolean isStatic() {
            if ((access & RuntimeConstants.ACC_STATIC) != 0)
                return true;
            return false;
        }

        /**
         * Return max depth of operand stack.
         */
        public int getMaxStack() {
            return max_stack;
        }

        /**
         * Return number of local variables.
         */
        public int getMaxLocals() {
            return max_locals;
        }

        /**
         * Return exception index table in Exception attribute.
         */
        public int[] get_exc_index_table() {
            return exc_index_table;
        }

        /**
         * Return exception table in code attributre.
         */
        public Vector getexception_table() {
            return exception_table;
        }

        /**
         * Return method attributes.
         */
        public Vector getAttributes() {
            return attrs;
        }

        /**
         * Return code attributes.
         */
        public Vector getCodeAttributes() {
            return code_attrs;
        }

        /**
         * Return true if method id synthetic.
         */
        public boolean isSynthetic() {
            return isSynthetic;
        }

        /**
         * Return true if method is deprecated.
         */
        public boolean isDeprecated() {
            return isDeprecated;
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Strores LocalVariableTable data information.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class LocVarData {
        short start_pc, length, name_cpx, sig_cpx, slot;

        public LocVarData() {
        }

        /**
         * Read LocalVariableTable attribute.
         */
        public LocVarData(DataInputStream in) throws IOException {
            start_pc = in.readShort();
            length = in.readShort();
            name_cpx = in.readShort();
            sig_cpx = in.readShort();
            slot = in.readShort();

        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Strores LineNumberTable data information.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class LineNumData {

        short start_pc, line_number;

        public LineNumData() {
        }

        /**
         * Read LineNumberTable attribute.
         */
        public LineNumData(DataInputStream in) throws IOException {
            start_pc = in.readShort();
            line_number = in.readShort();

        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Program to print information about class files
     * 
     * @author Sucheta Dambalkar
     */
    public static class JavapPrinter {

        JavapEnvironment env;

        ClassData cls;

        byte[] code;

        String lP = "";

        PrintWriter out;

        public JavapPrinter(InputStream cname, PrintWriter out, JavapEnvironment env) {
            this.out = out;
            this.cls = new ClassData(cname);
            this.env = env;
        }

        /**
         * Entry point to print class file information.
         */
        public void print() {
            printclassHeader();
            printfields();
            printMethods();
            printend();
        }

        /**
         * Print a description of the class (not members).
         */
        public void printclassHeader() {
            String srcName = "";
            if ((srcName = cls.getSourceName()) != "null") // requires debug
                // info
                out.println("Compiled from " + javaclassname(srcName));

            if (cls.isInterface()) {
                // The only useful access modifier of an interface is
                // public; interfaces are always marked as abstract and
                // cannot be final.
                out.print((cls.isPublic() ? "public " : "") + "interface " + javaclassname(cls.getClassName()));
            } else if (cls.isClass()) {
                String[] accflags = cls.getAccess();
                printAccess(accflags);
                out.print("class " + javaclassname(cls.getClassName()));

                if (cls.getSuperClassName() != null) {
                    out.print(" extends " + javaclassname(cls.getSuperClassName()));
                }
            }

            String[] interfacelist = cls.getSuperInterfaces();
            if (interfacelist.length > 0) {
                if (cls.isClass()) {
                    out.print(" implements ");
                } else if (cls.isInterface()) {
                    out.print(" extends ");
                }

                for (int j = 0; j < interfacelist.length; j++) {
                    out.print(javaclassname(interfacelist[j]));

                    if ((j + 1) < interfacelist.length) {
                        out.print(",");
                    }
                }
            }

            // Print class attribute information.
            if ((env.showallAttr) || (env.showVerbose)) {
                printClassAttributes();
            }
            // Print verbose output.
            if (env.showVerbose) {
                printverbosecls();
            }
            out.println("{");
        }

        /**
         * Print verbose output.
         */
        public void printverbosecls() {
            out.println("  minor version: " + cls.getMinor_version());
            out.println("  major version: " + cls.getMajor_version());
            out.println("  Constant pool:");
            printcp();
            env.showallAttr = true;
        }

        /**
         * Print class attribute information.
         */
        public void printClassAttributes() {
            out.println();
            AttrData[] clsattrs = cls.getAttributes();
            for (int i = 0; i < clsattrs.length; i++) {
                String clsattrname = clsattrs[i].getAttrName();
                if (clsattrname.equals("SourceFile")) {
                    out.println("  SourceFile: " + cls.getSourceName());
                } else if (clsattrname.equals("InnerClasses")) {
                    printInnerClasses();
                } else {
                    printAttrData(clsattrs[i]);
                }
            }
        }

        /**
         * Print the fields
         */
        public void printfields() {
            FieldData[] fields = cls.getFields();
            for (int f = 0; f < fields.length; f++) {
                String[] accflags = fields[f].getAccess();
                if (checkAccess(accflags)) {
                    if (!(env.showLineAndLocal || env.showDisassembled || env.showVerbose || env.showInternalSigs || env.showallAttr)) {
                        out.print("    ");
                    }
                    printAccess(accflags);
                    out.println(fields[f].getType() + " " + fields[f].getName() + ";");
                    if (env.showInternalSigs) {
                        out.println("  Signature: " + (fields[f].getInternalSig()));
                    }

                    // print field attribute information.
                    if (env.showallAttr) {
                        printFieldAttributes(fields[f]);

                    }
                    if ((env.showDisassembled) || (env.showLineAndLocal)) {
                        out.println();
                    }
                }
            }
        }

        /* print field attribute information. */
        public void printFieldAttributes(FieldData field) {
            Vector fieldattrs = field.getAttributes();
            for (int j = 0; j < fieldattrs.size(); j++) {
                String fieldattrname = ((AttrData) fieldattrs.elementAt(j)).getAttrName();
                if (fieldattrname.equals("ConstantValue")) {
                    printConstantValue(field);
                } else if (fieldattrname.equals("Deprecated")) {
                    out.println("Deprecated: " + field.isDeprecated());
                } else if (fieldattrname.equals("Synthetic")) {
                    out.println("  Synthetic: " + field.isSynthetic());
                } else {
                    printAttrData((AttrData) fieldattrs.elementAt(j));
                }
            }
            out.println();
        }

        /**
         * Print the methods
         */
        public void printMethods() {
            MethodData[] methods = cls.getMethods();
            for (int m = 0; m < methods.length; m++) {
                String[] accflags = methods[m].getAccess();
                if (checkAccess(accflags)) {
                    if (!(env.showLineAndLocal || env.showDisassembled || env.showVerbose || env.showInternalSigs || env.showallAttr)) {
                        out.print("    ");
                    }
                    printMethodSignature(methods[m], accflags);
                    printExceptions(methods[m]);
                    out.println(";");

                    // Print internal signature of method.
                    if (env.showInternalSigs) {
                        out.println("  Signature: " + (methods[m].getInternalSig()));
                    }

                    // Print disassembled code.
                    if (env.showDisassembled && !env.showallAttr) {
                        printcodeSequence(methods[m]);
                        printExceptionTable(methods[m]);
                        out.println();
                    }

                    // Print line and local variable attribute information.
                    if (env.showLineAndLocal) {
                        printLineNumTable(methods[m]);
                        printLocVarTable(methods[m]);
                        out.println();
                    }

                    // Print method attribute information.
                    if (env.showallAttr) {
                        printMethodAttributes(methods[m]);
                    }
                }
            }
        }

        /**
         * Print method signature.
         */
        public void printMethodSignature(MethodData method, String[] accflags) {
            printAccess(accflags);

            if ((method.getName()).equals("<init>")) {
                out.print(javaclassname(cls.getClassName()));
                out.print(method.getParameters());
            } else if ((method.getName()).equals("<clinit>")) {
                out.print("{}");
            } else {
                out.print(method.getReturnType() + " ");
                out.print(method.getName());
                out.print(method.getParameters());
            }
        }

        /**
         * print method attribute information.
         */
        public void printMethodAttributes(MethodData method) {
            Vector methodattrs = method.getAttributes();
            Vector codeattrs = method.getCodeAttributes();
            for (int k = 0; k < methodattrs.size(); k++) {
                String methodattrname = ((AttrData) methodattrs.elementAt(k)).getAttrName();
                if (methodattrname.equals("Code")) {
                    printcodeSequence(method);
                    printExceptionTable(method);
                    for (int c = 0; c < codeattrs.size(); c++) {
                        String codeattrname = ((AttrData) codeattrs.elementAt(c)).getAttrName();
                        if (codeattrname.equals("LineNumberTable")) {
                            printLineNumTable(method);
                        } else if (codeattrname.equals("LocalVariableTable")) {
                            printLocVarTable(method);
                        } else if (codeattrname.equals("StackMapTable")) {
                            // Java SE JSR 202 stack map tables
                            printStackMapTable(method);
                        } else if (codeattrname.equals("StackMap")) {
                            // Java ME CLDC stack maps
                            printStackMap(method);
                        } else {
                            printAttrData((AttrData) codeattrs.elementAt(c));
                        }
                    }
                } else if (methodattrname.equals("Exceptions")) {
                    out.println("  Exceptions: ");
                    printExceptions(method);
                } else if (methodattrname.equals("Deprecated")) {
                    out.println("  Deprecated: " + method.isDeprecated());
                } else if (methodattrname.equals("Synthetic")) {
                    out.println("  Synthetic: " + method.isSynthetic());
                } else {
                    printAttrData((AttrData) methodattrs.elementAt(k));
                }
            }
            out.println();
        }

        /**
         * Print exceptions.
         */
        public void printExceptions(MethodData method) {
            int[] exc_index_table = method.get_exc_index_table();
            if (exc_index_table != null) {
                if (!(env.showLineAndLocal || env.showDisassembled || env.showVerbose || env.showInternalSigs || env.showallAttr)) {
                    out.print("    ");
                }
                out.print("   throws ");
                int k;
                int l = exc_index_table.length;

                for (k = 0; k < l; k++) {
                    out.print(javaclassname(cls.getClassName(exc_index_table[k])));
                    if (k < l - 1)
                        out.print(", ");
                }
            }
        }

        /**
         * Print code sequence.
         */
        public void printcodeSequence(MethodData method) {
            code = method.getCode();
            if (code != null) {
                out.println("  Code:");
                if (env.showVerbose) {
                    printVerboseHeader(method);
                }

                for (int pc = 0; pc < code.length;) {
                    out.print("   " + pc + ":\t");
                    pc = pc + printInstr(pc);
                    out.println();
                }
            }
        }

        /**
         * Print instructions.
         */
        public int printInstr(int pc) {
            int opcode = getUbyte(pc);
            int opcode2;
            String mnem;
            switch (opcode) {
            case RuntimeConstants.opc_nonpriv:
            case RuntimeConstants.opc_priv:
                opcode2 = getUbyte(pc + 1);
                mnem = Tables.opcName((opcode << 8) + opcode2);
                if (mnem == null)
                    // assume all (even nonexistent) priv and nonpriv
                    // instructions
                    // are 2 bytes long
                    mnem = Tables.opcName(opcode) + " " + opcode2;
                out.print(mnem);
                return 2;
            case RuntimeConstants.opc_wide: {
                opcode2 = getUbyte(pc + 1);
                mnem = Tables.opcName((opcode << 8) + opcode2);
                if (mnem == null) {
                    // nonexistent opcode - but we have to print something
                    out.print("bytecode " + opcode);
                    return 1;
                }
                out.print(mnem + " " + getUShort(pc + 2));
                if (opcode2 == RuntimeConstants.opc_iinc) {
                    out.print(", " + getShort(pc + 4));
                    return 6;
                }
                return 4;
            }
            }
            mnem = Tables.opcName(opcode);
            if (mnem == null) {
                // nonexistent opcode - but we have to print something
                out.print("bytecode " + opcode);
                return 1;
            }
            if (opcode > RuntimeConstants.opc_jsr_w) {
                // pseudo opcodes should be printed as bytecodes
                out.print("bytecode " + opcode);
                return 1;
            }
            out.print(Tables.opcName(opcode));
            switch (opcode) {
            case RuntimeConstants.opc_aload:
            case RuntimeConstants.opc_astore:
            case RuntimeConstants.opc_fload:
            case RuntimeConstants.opc_fstore:
            case RuntimeConstants.opc_iload:
            case RuntimeConstants.opc_istore:
            case RuntimeConstants.opc_lload:
            case RuntimeConstants.opc_lstore:
            case RuntimeConstants.opc_dload:
            case RuntimeConstants.opc_dstore:
            case RuntimeConstants.opc_ret:
                out.print("\t" + getUbyte(pc + 1));
                return 2;
            case RuntimeConstants.opc_iinc:
                out.print("\t" + getUbyte(pc + 1) + ", " + getbyte(pc + 2));
                return 3;
            case RuntimeConstants.opc_tableswitch: {
                int tb = align(pc + 1);
                int default_skip = getInt(tb); /* default skip pamount */
                int low = getInt(tb + 4);
                int high = getInt(tb + 8);
                int count = high - low;
                out.print("{ //" + low + " to " + high);
                for (int i = 0; i <= count; i++)
                    out.print("\n\t\t" + (i + low) + ": " + lP + (pc + getInt(tb + 12 + 4 * i)) + ";");
                out.print("\n\t\tdefault: " + lP + (default_skip + pc) + " }");
                return tb - pc + 16 + count * 4;
            }

            case RuntimeConstants.opc_lookupswitch: {
                int tb = align(pc + 1);
                int default_skip = getInt(tb);
                int npairs = getInt(tb + 4);
                out.print("{ //" + npairs);
                for (int i = 1; i <= npairs; i++)
                    out.print("\n\t\t" + getInt(tb + i * 8) + ": " + lP + (pc + getInt(tb + 4 + i * 8)) + ";");
                out.print("\n\t\tdefault: " + lP + (default_skip + pc) + " }");
                return tb - pc + (npairs + 1) * 8;
            }
            case RuntimeConstants.opc_newarray:
                int type = getUbyte(pc + 1);
                switch (type) {
                case RuntimeConstants.T_BOOLEAN:
                    out.print(" boolean");
                    break;
                case RuntimeConstants.T_BYTE:
                    out.print(" byte");
                    break;
                case RuntimeConstants.T_CHAR:
                    out.print(" char");
                    break;
                case RuntimeConstants.T_SHORT:
                    out.print(" short");
                    break;
                case RuntimeConstants.T_INT:
                    out.print(" int");
                    break;
                case RuntimeConstants.T_LONG:
                    out.print(" long");
                    break;
                case RuntimeConstants.T_FLOAT:
                    out.print(" float");
                    break;
                case RuntimeConstants.T_DOUBLE:
                    out.print(" double");
                    break;
                case RuntimeConstants.T_CLASS:
                    out.print(" class");
                    break;
                default:
                    out.print(" BOGUS TYPE:" + type);
                }
                return 2;

            case RuntimeConstants.opc_anewarray: {
                int index = getUShort(pc + 1);
                out.print("\t#" + index + "; //");
                PrintConstant(index);
                return 3;
            }

            case RuntimeConstants.opc_sipush:
                out.print("\t" + getShort(pc + 1));
                return 3;

            case RuntimeConstants.opc_bipush:
                out.print("\t" + getbyte(pc + 1));
                return 2;

            case RuntimeConstants.opc_ldc: {
                int index = getUbyte(pc + 1);
                out.print("\t#" + index + "; //");
                PrintConstant(index);
                return 2;
            }

            case RuntimeConstants.opc_ldc_w:
            case RuntimeConstants.opc_ldc2_w:
            case RuntimeConstants.opc_instanceof:
            case RuntimeConstants.opc_checkcast:
            case RuntimeConstants.opc_new:
            case RuntimeConstants.opc_putstatic:
            case RuntimeConstants.opc_getstatic:
            case RuntimeConstants.opc_putfield:
            case RuntimeConstants.opc_getfield:
            case RuntimeConstants.opc_invokevirtual:
            case RuntimeConstants.opc_invokespecial:
            case RuntimeConstants.opc_invokestatic: {
                int index = getUShort(pc + 1);
                out.print("\t#" + index + "; //");
                PrintConstant(index);
                return 3;
            }

            case RuntimeConstants.opc_invokeinterface: {
                int index = getUShort(pc + 1), nargs = getUbyte(pc + 3);
                out.print("\t#" + index + ",  " + nargs + "; //");
                PrintConstant(index);
                return 5;
            }

            case RuntimeConstants.opc_multianewarray: {
                int index = getUShort(pc + 1), dimensions = getUbyte(pc + 3);
                out.print("\t#" + index + ",  " + dimensions + "; //");
                PrintConstant(index);
                return 4;
            }
            case RuntimeConstants.opc_jsr:
            case RuntimeConstants.opc_goto:
            case RuntimeConstants.opc_ifeq:
            case RuntimeConstants.opc_ifge:
            case RuntimeConstants.opc_ifgt:
            case RuntimeConstants.opc_ifle:
            case RuntimeConstants.opc_iflt:
            case RuntimeConstants.opc_ifne:
            case RuntimeConstants.opc_if_icmpeq:
            case RuntimeConstants.opc_if_icmpne:
            case RuntimeConstants.opc_if_icmpge:
            case RuntimeConstants.opc_if_icmpgt:
            case RuntimeConstants.opc_if_icmple:
            case RuntimeConstants.opc_if_icmplt:
            case RuntimeConstants.opc_if_acmpeq:
            case RuntimeConstants.opc_if_acmpne:
            case RuntimeConstants.opc_ifnull:
            case RuntimeConstants.opc_ifnonnull:
                out.print("\t" + lP + (pc + getShort(pc + 1)));
                return 3;

            case RuntimeConstants.opc_jsr_w:
            case RuntimeConstants.opc_goto_w:
                out.print("\t" + lP + (pc + getInt(pc + 1)));
                return 5;

            default:
                return 1;
            }
        }

        /**
         * Print code attribute details.
         */
        public void printVerboseHeader(MethodData method) {
            int argCount = method.getArgumentlength();
            if (!method.isStatic())
                ++argCount; // for 'this'

            out.println("   Stack=" + method.getMaxStack() + ", Locals=" + method.getMaxLocals() + ", Args_size="
                    + argCount);

        }

        /**
         * Print the exception table for this method code
         */
        void printExceptionTable(MethodData method) {// throws IOException
            Vector exception_table = method.getexception_table();
            if (exception_table.size() > 0) {
                out.println("  Exception table:");
                out.println("   from   to  target type");
                for (int idx = 0; idx < exception_table.size(); ++idx) {
                    TrapData handler = (TrapData) exception_table.elementAt(idx);
                    printFixedWidthInt(handler.start_pc, 6);
                    printFixedWidthInt(handler.end_pc, 6);
                    printFixedWidthInt(handler.handler_pc, 6);
                    out.print("   ");
                    int catch_cpx = handler.catch_cpx;
                    if (catch_cpx == 0) {
                        out.println("any");
                    } else {
                        out.print("Class ");
                        out.println(cls.getClassName(catch_cpx));
                        out.println("");
                    }
                }
            }
        }

        /**
         * Print LineNumberTable attribute information.
         */
        public void printLineNumTable(MethodData method) {
            int numlines = method.getnumlines();
            Vector lin_num_tb = method.getlin_num_tb();
            if (lin_num_tb.size() > 0) {
                out.println("  LineNumberTable: ");
                for (int i = 0; i < numlines; i++) {
                    LineNumData linnumtb_entry = (LineNumData) lin_num_tb.elementAt(i);
                    out.println("   line " + linnumtb_entry.line_number + ": " + linnumtb_entry.start_pc);
                }
            }
            out.println();
        }

        /**
         * Print LocalVariableTable attribute information.
         */
        public void printLocVarTable(MethodData method) {
            int siz = method.getloc_var_tbsize();
            if (siz > 0) {
                out.println("  LocalVariableTable: ");
                out.print("   ");
                out.println("Start  Length  Slot  Name   Signature");
            }
            Vector loc_var_tb = method.getloc_var_tb();

            for (int i = 0; i < siz; i++) {
                LocVarData entry = (LocVarData) loc_var_tb.elementAt(i);

                out.println("   " + entry.start_pc + "      " + entry.length + "      " + entry.slot + "    "
                        + cls.StringValue(entry.name_cpx) + "       " + cls.StringValue(entry.sig_cpx));
            }
            out.println();
        }

        /**
         * Print StackMap attribute information.
         */
        public void printStackMap(MethodData method) {
            StackMapData[] stack_map_tb = method.getStackMap();
            int number_of_entries = stack_map_tb.length;
            if (number_of_entries > 0) {
                out.println("  StackMap: number_of_entries = " + number_of_entries);

                for (StackMapData frame : stack_map_tb) {
                    frame.print(this);
                }
            }
            out.println();
        }

        /**
         * Print StackMapTable attribute information.
         */
        public void printStackMapTable(MethodData method) {
            StackMapTableData[] stack_map_tb = method.getStackMapTable();
            int number_of_entries = stack_map_tb.length;
            if (number_of_entries > 0) {
                out.println("  StackMapTable: number_of_entries = " + number_of_entries);

                for (StackMapTableData frame : stack_map_tb) {
                    frame.print(this);
                }
            }
            out.println();
        }

        void printMap(String name, int[] map) {
            out.print(name);
            for (int i = 0; i < map.length; i++) {
                int fulltype = map[i];
                int type = fulltype & 0xFF;
                int argument = fulltype >> 8;
                switch (type) {
                case RuntimeConstants.ITEM_Object:
                    out.print(" ");
                    PrintConstant(argument);
                    break;
                case RuntimeConstants.ITEM_NewObject:
                    out.print(" " + Tables.mapTypeName(type));
                    out.print(" " + argument);
                    break;
                default:
                    out.print(" " + Tables.mapTypeName(type));
                }
                out.print((i == (map.length - 1) ? ' ' : ','));
            }
            out.println("]");
        }

        /**
         * Print ConstantValue attribute information.
         */
        public void printConstantValue(FieldData field) {
            out.print("  Constant value: ");
            int cpx = (field.getConstantValueIndex());
            byte tag = 0;
            try {
                tag = cls.getTag(cpx);

            } catch (IndexOutOfBoundsException e) {
                out.print("Error:");
                return;
            }
            switch (tag) {
            case RuntimeConstants.CONSTANT_METHOD:
            case RuntimeConstants.CONSTANT_INTERFACEMETHOD:
            case RuntimeConstants.CONSTANT_FIELD: {
                CPX2 x = (CPX2) (cls.getCpoolEntry(cpx));
                if (x.cpx1 == cls.getthis_cpx()) {
                    // don't print class part for local references
                    cpx = x.cpx2;
                }
            }
            }
            out.print(cls.TagString(tag) + " " + cls.StringValue(cpx));
        }

        /**
         * Print InnerClass attribute information.
         */
        public void printInnerClasses() {// throws ioexception

            InnerClassData[] innerClasses = cls.getInnerClasses();
            if (innerClasses != null) {
                if (innerClasses.length > 0) {
                    out.print("  ");
                    out.println("InnerClass: ");
                    for (int i = 0; i < innerClasses.length; i++) {
                        out.print("   ");
                        // access
                        String[] accflags = innerClasses[i].getAccess();
                        if (checkAccess(accflags)) {
                            printAccess(accflags);
                            if (innerClasses[i].inner_name_index != 0) {
                                out.print("#" + innerClasses[i].inner_name_index + "= ");
                            }
                            out.print("#" + innerClasses[i].inner_class_info_index);
                            if (innerClasses[i].outer_class_info_index != 0) {
                                out.print(" of #" + innerClasses[i].outer_class_info_index);
                            }
                            out.print("; //");
                            if (innerClasses[i].inner_name_index != 0) {
                                out.print(cls.getName(innerClasses[i].inner_name_index) + "=");
                            }
                            PrintConstant(innerClasses[i].inner_class_info_index);
                            if (innerClasses[i].outer_class_info_index != 0) {
                                out.print(" of ");
                                PrintConstant(innerClasses[i].outer_class_info_index);
                            }
                            out.println();
                        }
                    }

                }
            }
        }

        /**
         * Print constant pool information.
         */
        public void printcp() {
            int cpx = 1;

            while (cpx < cls.getCpoolCount()) {
                out.print("const #" + cpx + " = ");
                cpx += PrintlnConstantEntry(cpx);
            }
            out.println();
        }

        /**
         * Print constant pool entry information.
         */
        public int PrintlnConstantEntry(int cpx) {
            int size = 1;
            byte tag = 0;
            try {
                tag = cls.getTag(cpx);
            } catch (IndexOutOfBoundsException e) {
                out.println("  <Incorrect CP index>");
                return 1;
            }
            out.print(cls.StringTag(cpx) + "\t");
            Object x = cls.getCpoolEntryobj(cpx);
            if (x == null) {
                switch (tag) {
                case RuntimeConstants.CONSTANT_LONG:
                case RuntimeConstants.CONSTANT_DOUBLE:
                    size = 2;
                }
                out.println("null;");
                return size;
            }
            String str = cls.StringValue(cpx);

            switch (tag) {
            case RuntimeConstants.CONSTANT_CLASS:
            case RuntimeConstants.CONSTANT_STRING:
                out.println("#" + (((CPX) x).cpx) + ";\t//  " + str);
                break;
            case RuntimeConstants.CONSTANT_FIELD:
            case RuntimeConstants.CONSTANT_METHOD:
            case RuntimeConstants.CONSTANT_INTERFACEMETHOD:
                out.println("#" + ((CPX2) x).cpx1 + ".#" + ((CPX2) x).cpx2 + ";\t//  " + str);
                break;
            case RuntimeConstants.CONSTANT_NAMEANDTYPE:
                out.println("#" + ((CPX2) x).cpx1 + ":#" + ((CPX2) x).cpx2 + ";//  " + str);
                break;
            case RuntimeConstants.CONSTANT_LONG:
            case RuntimeConstants.CONSTANT_DOUBLE:
                size = 2;
            default:
                out.println(str + ";");
            }
            return size;
        }

        /**
         * Checks access of class, field or method.
         */
        public boolean checkAccess(String accflags[]) {

            boolean ispublic = false;
            boolean isprotected = false;
            boolean isprivate = false;
            boolean ispackage = false;

            for (int i = 0; i < accflags.length; i++) {
                if (accflags[i].equals("public"))
                    ispublic = true;
                else if (accflags[i].equals("protected"))
                    isprotected = true;
                else if (accflags[i].equals("private"))
                    isprivate = true;
            }

            if (!(ispublic || isprotected || isprivate))
                ispackage = true;

            if ((env.showAccess == env.PUBLIC) && (isprotected || isprivate || ispackage))
                return false;
            else if ((env.showAccess == env.PROTECTED) && (isprivate || ispackage))
                return false;
            else if ((env.showAccess == env.PACKAGE) && (isprivate))
                return false;
            else
                return true;
        }

        /**
         * Prints access of class, field or method.
         */
        public void printAccess(String[] accflags) {
            for (int j = 0; j < accflags.length; j++) {
                out.print(accflags[j] + " ");
            }
        }

        /**
         * Print an integer so that it takes 'length' characters in the output.
         * Temporary until formatting code is stable.
         */
        public void printFixedWidthInt(long x, int length) {
            CharArrayWriter baStream = new CharArrayWriter();
            PrintWriter pStream = new PrintWriter(baStream);
            pStream.print(x);
            String str = baStream.toString();
            for (int cnt = length - str.length(); cnt > 0; --cnt)
                out.print(' ');
            out.print(str);
        }

        protected int getbyte(int pc) {
            return code[pc];
        }

        protected int getUbyte(int pc) {
            return code[pc] & 0xFF;
        }

        int getShort(int pc) {
            return (code[pc] << 8) | (code[pc + 1] & 0xFF);
        }

        int getUShort(int pc) {
            return ((code[pc] << 8) | (code[pc + 1] & 0xFF)) & 0xFFFF;
        }

        protected int getInt(int pc) {
            return (getShort(pc) << 16) | (getShort(pc + 2) & 0xFFFF);
        }

        /**
         * Print constant value at that index.
         */
        void PrintConstant(int cpx) {
            if (cpx == 0) {
                out.print("#0");
                return;
            }
            byte tag = 0;
            try {
                tag = cls.getTag(cpx);

            } catch (IndexOutOfBoundsException e) {
                out.print("#" + cpx);
                return;
            }
            switch (tag) {
            case RuntimeConstants.CONSTANT_METHOD:
            case RuntimeConstants.CONSTANT_INTERFACEMETHOD:
            case RuntimeConstants.CONSTANT_FIELD: {
                // CPX2 x=(CPX2)(cpool[cpx]);
                CPX2 x = (CPX2) (cls.getCpoolEntry(cpx));
                if (x.cpx1 == cls.getthis_cpx()) {
                    // don't print class part for local references
                    cpx = x.cpx2;
                }
            }
            }
            out.print(cls.TagString(tag) + " " + cls.StringValue(cpx));
        }

        protected static int align(int n) {
            return (n + 3) & ~3;
        }

        public void printend() {
            out.println("}");
            out.println();
        }

        public String javaclassname(String name) {
            return name.replace('/', '.');
        }

        /**
         * Print attribute data in hex.
         */
        public void printAttrData(AttrData attr) {
            byte[] data = attr.getData();
            int i = 0;
            int j = 0;
            out.print("  " + attr.getAttrName() + ": ");
            out.println("length = " + cls.toHex(attr.datalen));

            out.print("   ");

            while (i < data.length) {
                String databytestring = cls.toHex(data[i]);
                if (databytestring.equals("0x"))
                    out.print("00");
                else if (databytestring.substring(2).length() == 1) {
                    out.print("0" + databytestring.substring(2));
                } else {
                    out.print(databytestring.substring(2));
                }

                j++;
                if (j == 16) {
                    out.println();
                    out.print("   ");
                    j = 0;
                } else
                    out.print(" ");
                i++;
            }
            out.println();
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Strores flag values according to command line options and sets path where
     * to find classes.
     * 
     * @author Sucheta Dambalkar
     */
    public static class JavapEnvironment {

        // Access flags
        public static final int PRIVATE = 0;

        public static final int PROTECTED = 1;

        public static final int PACKAGE = 2;

        public static final int PUBLIC = 3;

        // search path flags.
        private static final int start = 0;

        private static final int cmdboot = 1;

        private static final int sunboot = 2;

        private static final int javaclass = 3;

        private static final int cmdextdir = 4;

        private static final int javaext = 5;

        private static final int cmdclasspath = 6;

        private static final int envclasspath = 7;

        private static final int javaclasspath = 8;

        private static final int currentdir = 9;

        // JavapEnvironment flag settings
        boolean showLineAndLocal = false;

        int showAccess = PACKAGE;

        boolean showDisassembled = false;

        boolean showVerbose = false;

        boolean showInternalSigs = false;

        String classPathString = null;

        String bootClassPathString = null;

        String extDirsString = null;

        boolean extDirflag = false;

        boolean nothingToDo = true;

        boolean showallAttr = false;

        String classpath = null;

        int searchpath = start;

        /**
         * According to which flags are set, returns file input stream for
         * classfile to disassemble.
         */
        public InputStream getFileInputStream(String Name) {
            InputStream fileInStream = null;
            searchpath = cmdboot;
            try {
                if (searchpath == cmdboot) {
                    if (bootClassPathString != null) {
                        // search in specified bootclasspath.
                        classpath = bootClassPathString;
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path.
                        else
                            searchpath = cmdextdir;
                    } else
                        searchpath = sunboot;
                }

                if (searchpath == sunboot) {
                    if (System.getProperty("sun.boot.class.path") != null) {
                        // search in sun.boot.class.path
                        classpath = System.getProperty("sun.boot.class.path");
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path
                        else
                            searchpath = cmdextdir;
                    } else
                        searchpath = javaclass;
                }

                if (searchpath == javaclass) {
                    if (System.getProperty("java.class.path") != null) {
                        // search in java.class.path
                        classpath = System.getProperty("java.class.path");
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path
                        else
                            searchpath = cmdextdir;
                    } else
                        searchpath = cmdextdir;
                }

                if (searchpath == cmdextdir) {
                    if (extDirsString != null) {
                        // search in specified extdir.
                        classpath = extDirsString;
                        extDirflag = true;
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path
                        else {
                            searchpath = cmdclasspath;
                            extDirflag = false;
                        }
                    } else
                        searchpath = javaext;
                }

                if (searchpath == javaext) {
                    if (System.getProperty("java.ext.dirs") != null) {
                        // search in java.ext.dirs
                        classpath = System.getProperty("java.ext.dirs");
                        extDirflag = true;
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path
                        else {
                            searchpath = cmdclasspath;
                            extDirflag = false;
                        }
                    } else
                        searchpath = cmdclasspath;
                }
                if (searchpath == cmdclasspath) {
                    if (classPathString != null) {
                        // search in specified classpath.
                        classpath = classPathString;
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path
                        else
                            searchpath = 8;
                    } else
                        searchpath = envclasspath;
                }

                if (searchpath == envclasspath) {
                    if (System.getProperty("env.class.path") != null) {
                        // search in env.class.path
                        classpath = System.getProperty("env.class.path");
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path.
                        else
                            searchpath = javaclasspath;
                    } else
                        searchpath = javaclasspath;
                }

                if (searchpath == javaclasspath) {
                    if (("application.home") == null) {
                        // search in java.class.path
                        classpath = System.getProperty("java.class.path");
                        if ((fileInStream = resolvefilename(Name)) != null)
                            return fileInStream;
                        // no classes found in search path.
                        else
                            searchpath = currentdir;
                    } else
                        searchpath = currentdir;
                }

                if (searchpath == currentdir) {
                    classpath = ".";
                    // search in current dir.
                    if ((fileInStream = resolvefilename(Name)) != null)
                        return fileInStream;
                    else {
                        // no classes found in search path.
                        error("Could not find " + Name);
                        System.exit(1);
                    }
                }

            } catch (SecurityException excsec) {
                excsec.printStackTrace();
                error("fatal exception");
            } catch (NullPointerException excnull) {
                excnull.printStackTrace();
                error("fatal exception");
            } catch (IllegalArgumentException excill) {
                excill.printStackTrace();
                error("fatal exception");
            }

            return null;
        }

        public void error(String msg) {
            System.err.println("ERROR:" + msg);
        }

        /**
         * Resolves file name for classfile to disassemble.
         */
        public InputStream resolvefilename(String name) {
            String classname = name.replace('.', '/') + ".class";
            while (true) {
                InputStream instream = extDirflag ? resolveExdirFilename(classname) : resolveclasspath(classname);
                if (instream != null)
                    return instream;
                int lastindex = classname.lastIndexOf('/');
                if (lastindex == -1)
                    return null;
                classname = classname.substring(0, lastindex) + "$" + classname.substring(lastindex + 1);
            }
        }

        /**
         * Resolves file name for classfile to disassemble if flag exdir is set.
         */
        public InputStream resolveExdirFilename(String classname) {
            if (classpath.indexOf(File.pathSeparator) != -1) {
                // separates path
                StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
                while (st.hasMoreTokens()) {
                    String path = st.nextToken();
                    InputStream in = resolveExdirFilenamehelper(path, classname);
                    if (in != null)
                        return in;
                }
            } else
                return (resolveExdirFilenamehelper(classpath, classname));

            return null;
        }

        /**
         * Resolves file name for classfile to disassemble.
         */
        public InputStream resolveclasspath(String classname) {
            if (classpath.indexOf(File.pathSeparator) != -1) {
                StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
                // separates path.
                while (st.hasMoreTokens()) {
                    String path = (st.nextToken()).trim();
                    InputStream in = resolveclasspathhelper(path, classname);
                    if (in != null)
                        return in;

                }
                return null;
            } else
                return (resolveclasspathhelper(classpath, classname));
        }

        /**
         * Returns file input stream for classfile to disassemble if exdir is
         * set.
         */
        public InputStream resolveExdirFilenamehelper(String path, String classname) {
            File fileobj = new File(path);
            if (fileobj.isDirectory()) {
                // gets list of files in that directory.
                File[] filelist = fileobj.listFiles();
                for (int i = 0; i < filelist.length; i++) {
                    try {
                        // file is a jar file.
                        if (filelist[i].toString().endsWith(".jar")) {
                            JarFile jfile = new JarFile(filelist[i]);
                            if ((jfile.getEntry(classname)) != null) {

                                InputStream filein = jfile.getInputStream(jfile.getEntry(classname));
                                int bytearraysize = filein.available();
                                byte[] b = new byte[bytearraysize];
                                int totalread = 0;
                                while (totalread < bytearraysize) {
                                    totalread += filein.read(b, totalread, bytearraysize - totalread);
                                }
                                InputStream inbyte = new ByteArrayInputStream(b);
                                filein.close();
                                return inbyte;
                            }
                        } else {
                            // not a jar file.
                            String filename = path + "/" + classname;
                            File file = new File(filename);
                            if (file.isFile()) {
                                return (new FileInputStream(file));
                            }
                        }
                    } catch (FileNotFoundException fnexce) {
                        fnexce.printStackTrace();
                        error("cant read file");
                        error("fatal exception");
                    } catch (IOException ioexc) {
                        ioexc.printStackTrace();
                        error("fatal exception");
                    }
                }
            }

            return null;
        }

        /**
         * Returns file input stream for classfile to disassemble.
         */
        public InputStream resolveclasspathhelper(String path, String classname) {
            File fileobj = new File(path);
            try {
                if (fileobj.isDirectory()) {
                    // is a directory.
                    String filename = path + "/" + classname;
                    File file = new File(filename);
                    if (file.isFile()) {
                        return (new FileInputStream(file));
                    }

                } else if (fileobj.isFile()) {
                    if (fileobj.toString().endsWith(".jar")) {
                        // is a jar file.
                        JarFile jfile = new JarFile(fileobj);
                        if ((jfile.getEntry(classname)) != null) {
                            InputStream filein = jfile.getInputStream(jfile.getEntry(classname));
                            int bytearraysize = filein.available();
                            byte[] b = new byte[bytearraysize];
                            int totalread = 0;
                            while (totalread < bytearraysize) {
                                totalread += filein.read(b, totalread, bytearraysize - totalread);
                            }
                            InputStream inbyte = new ByteArrayInputStream(b);
                            filein.close();
                            return inbyte;
                        }
                    }
                }
            } catch (FileNotFoundException fnexce) {
                fnexce.printStackTrace();
                error("cant read file");
                error("fatal exception");
            } catch (IOException ioexce) {
                ioexce.printStackTrace();
                error("fatal exception");
            }
            return null;
        }

        /**
         * @return the showLineAndLocal
         */
        public boolean isShowLineAndLocal() {
            return showLineAndLocal;
        }

        /**
         * @param showLineAndLocal
         *            the showLineAndLocal to set
         */
        public void setShowLineAndLocal(boolean showLineAndLocal) {
            this.showLineAndLocal = showLineAndLocal;
        }

        /**
         * @return the showAccess
         */
        public int getShowAccess() {
            return showAccess;
        }

        /**
         * @param showAccess
         *            the showAccess to set
         */
        public void setShowAccess(int showAccess) {
            this.showAccess = showAccess;
        }

        /**
         * @return the showDisassembled
         */
        public boolean isShowDisassembled() {
            return showDisassembled;
        }

        /**
         * @param showDisassembled
         *            the showDisassembled to set
         */
        public void setShowDisassembled(boolean showDisassembled) {
            this.showDisassembled = showDisassembled;
        }

        /**
         * @return the showVerbose
         */
        public boolean isShowVerbose() {
            return showVerbose;
        }

        /**
         * @param showVerbose
         *            the showVerbose to set
         */
        public void setShowVerbose(boolean showVerbose) {
            this.showVerbose = showVerbose;
        }

        /**
         * @return the showInternalSigs
         */
        public boolean isShowInternalSigs() {
            return showInternalSigs;
        }

        /**
         * @param showInternalSigs
         *            the showInternalSigs to set
         */
        public void setShowInternalSigs(boolean showInternalSigs) {
            this.showInternalSigs = showInternalSigs;
        }

        /**
         * @return the showallAttr
         */
        public boolean isShowallAttr() {
            return showallAttr;
        }

        /**
         * @param showallAttr
         *            the showallAttr to set
         */
        public void setShowallAttr(boolean showallAttr) {
            this.showallAttr = showallAttr;
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Strores InnerClass data informastion.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class InnerClassData implements RuntimeConstants {
        ClassData cls;

        int inner_class_info_index, outer_class_info_index, inner_name_index, access;

        public InnerClassData(ClassData cls) {
            this.cls = cls;

        }

        /**
         * Read Innerclass attribute data.
         */
        public void read(DataInputStream in) throws IOException {
            inner_class_info_index = in.readUnsignedShort();
            outer_class_info_index = in.readUnsignedShort();
            inner_name_index = in.readUnsignedShort();
            access = in.readUnsignedShort();
        } // end read

        /**
         * Returns the access of this class or interface.
         */
        public String[] getAccess() {
            Vector v = new Vector();
            if ((access & ACC_PUBLIC) != 0)
                v.addElement("public");
            if ((access & ACC_FINAL) != 0)
                v.addElement("final");
            if ((access & ACC_ABSTRACT) != 0)
                v.addElement("abstract");
            String[] accflags = new String[v.size()];
            v.copyInto(accflags);
            return accflags;
        }

    } // end InnerClassData

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Strores field data informastion.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */

    public static class FieldData implements RuntimeConstants {

        ClassData cls;

        int access;

        int name_index;

        int descriptor_index;

        int attributes_count;

        int value_cpx = 0;

        boolean isSynthetic = false;

        boolean isDeprecated = false;

        Vector attrs;

        public FieldData(ClassData cls) {
            this.cls = cls;
        }

        /**
         * Read and store field info.
         */
        public void read(DataInputStream in) throws IOException {
            access = in.readUnsignedShort();
            name_index = in.readUnsignedShort();
            descriptor_index = in.readUnsignedShort();
            // Read the attributes
            int attributes_count = in.readUnsignedShort();
            attrs = new Vector(attributes_count);
            for (int i = 0; i < attributes_count; i++) {
                int attr_name_index = in.readUnsignedShort();
                if (cls.getTag(attr_name_index) != CONSTANT_UTF8)
                    continue;
                String attr_name = cls.getString(attr_name_index);
                if (attr_name.equals("ConstantValue")) {
                    if (in.readInt() != 2)
                        throw new ClassFormatError("invalid ConstantValue attr length");
                    value_cpx = in.readUnsignedShort();
                    AttrData attr = new AttrData(cls);
                    attr.read(attr_name_index);
                    attrs.addElement(attr);
                } else if (attr_name.equals("Synthetic")) {
                    if (in.readInt() != 0)
                        throw new ClassFormatError("invalid Synthetic attr length");
                    isSynthetic = true;
                    AttrData attr = new AttrData(cls);
                    attr.read(attr_name_index);
                    attrs.addElement(attr);
                } else if (attr_name.equals("Deprecated")) {
                    if (in.readInt() != 0)
                        throw new ClassFormatError("invalid Synthetic attr length");
                    isDeprecated = true;
                    AttrData attr = new AttrData(cls);
                    attr.read(attr_name_index);
                    attrs.addElement(attr);
                } else {
                    AttrData attr = new AttrData(cls);
                    attr.read(attr_name_index, in);
                    attrs.addElement(attr);
                }
            }

        } // end read

        /**
         * Returns access of a field.
         */
        public String[] getAccess() {
            Vector v = new Vector();
            if ((access & ACC_PUBLIC) != 0)
                v.addElement("public");
            if ((access & ACC_PRIVATE) != 0)
                v.addElement("private");
            if ((access & ACC_PROTECTED) != 0)
                v.addElement("protected");
            if ((access & ACC_STATIC) != 0)
                v.addElement("static");
            if ((access & ACC_FINAL) != 0)
                v.addElement("final");
            if ((access & ACC_VOLATILE) != 0)
                v.addElement("volatile");
            if ((access & ACC_TRANSIENT) != 0)
                v.addElement("transient");
            String[] accflags = new String[v.size()];
            v.copyInto(accflags);
            return accflags;
        }

        /**
         * Returns name of a field.
         */
        public String getName() {
            return cls.getStringValue(name_index);
        }

        /**
         * Returns internal signature of a field
         */
        public String getInternalSig() {
            return cls.getStringValue(descriptor_index);
        }

        /**
         * Returns java type signature of a field.
         */
        public String getType() {
            return new TypeSignature(getInternalSig()).getFieldType();
        }

        /**
         * Returns true if field is synthetic.
         */
        public boolean isSynthetic() {
            return isSynthetic;
        }

        /**
         * Returns true if field is deprecated.
         */
        public boolean isDeprecated() {
            return isDeprecated;
        }

        /**
         * Returns index of constant value in cpool.
         */
        public int getConstantValueIndex() {
            return (value_cpx);
        }

        /**
         * Returns list of attributes of field.
         */
        public Vector getAttributes() {
            return attrs;
        }

    } // End of the method //

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Stores constant pool entry information with two fields.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class CPX2 {
        int cpx1, cpx2;

        CPX2(int cpx1, int cpx2) {
            this.cpx1 = cpx1;
            this.cpx2 = cpx2;
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /*
     * represents one entry of StackMap attribute
     */
    public static class StackMapData {

        final int offset;

        final int[] locals;

        final int[] stack;

        StackMapData(int offset, int[] locals, int[] stack) {
            this.offset = offset;
            this.locals = locals;
            this.stack = stack;
        }

        StackMapData(DataInputStream in, MethodData method) throws IOException {
            offset = in.readUnsignedShort();
            int local_size = in.readUnsignedShort();
            locals = readTypeArray(in, local_size, method);
            int stack_size = in.readUnsignedShort();
            stack = readTypeArray(in, stack_size, method);
        }

        static final int[] readTypeArray(DataInputStream in, int length, MethodData method) throws IOException {
            int[] types = new int[length];
            for (int i = 0; i < length; i++) {
                types[i] = readType(in, method);
            }
            return types;
        }

        static final int readType(DataInputStream in, MethodData method) throws IOException {
            int type = in.readUnsignedByte();
            if (type == RuntimeConstants.ITEM_Object || type == RuntimeConstants.ITEM_NewObject) {
                type = type | (in.readUnsignedShort() << 8);
            }
            return type;
        }

        void print(JavapPrinter p) {
            p.out.println("   " + offset + ":");
            p.printMap("    locals = [", locals);
            p.printMap("    stack = [", stack);
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/

    /**
     * Stores constant pool entry information with one field.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class CPX {
        int cpx;

        CPX(int cpx) {
            this.cpx = cpx;
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/
    /**
     * Central data repository of the Java Disassembler. Stores all the
     * information in java class file.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class ClassData implements RuntimeConstants {

        private int magic;

        private int minor_version;

        private int major_version;

        private int cpool_count;

        private Object cpool[];

        private int access;

        private int this_class = 0;;

        private int super_class;

        private int interfaces_count;

        private int[] interfaces = new int[0];;

        private int fields_count;

        private FieldData[] fields;

        private int methods_count;

        private MethodData[] methods;

        private InnerClassData[] innerClasses;

        private int attributes_count;

        private AttrData[] attrs;

        private String classname;

        private String superclassname;

        private int source_cpx = 0;

        private byte tags[];

        private Hashtable indexHashAscii = new Hashtable();

        private String pkgPrefix = "";

        private int pkgPrefixLen = 0;

        /**
         * Read classfile to disassemble.
         */
        public ClassData(InputStream infile) {
            try {
                this.read(new DataInputStream(infile));
            } catch (FileNotFoundException ee) {
                error("cant read file");
            } catch (Error ee) {
                ee.printStackTrace();
                error("fatal error");
            } catch (Exception ee) {
                ee.printStackTrace();
                error("fatal exception");
            }
        }

        /**
         * Reads and stores class file information.
         */
        public void read(DataInputStream in) throws IOException {
            // Read the header
            magic = in.readInt();
            if (magic != JAVA_MAGIC) {
                throw new ClassFormatError("wrong magic: " + toHex(magic) + ", expected " + toHex(JAVA_MAGIC));
            }
            minor_version = in.readShort();
            major_version = in.readShort();
            if (major_version != JAVA_VERSION) {
            }

            // Read the constant pool
            readCP(in);
            access = in.readUnsignedShort();
            this_class = in.readUnsignedShort();
            super_class = in.readUnsignedShort();

            // Read interfaces.
            interfaces_count = in.readUnsignedShort();
            if (interfaces_count > 0) {
                interfaces = new int[interfaces_count];
            }
            for (int i = 0; i < interfaces_count; i++) {
                interfaces[i] = in.readShort();
            }

            // Read the fields
            readFields(in);

            // Read the methods
            readMethods(in);

            // Read the attributes
            attributes_count = in.readUnsignedShort();
            attrs = new AttrData[attributes_count];
            for (int k = 0; k < attributes_count; k++) {
                int name_cpx = in.readUnsignedShort();
                if (getTag(name_cpx) == CONSTANT_UTF8 && getString(name_cpx).equals("SourceFile")) {
                    if (in.readInt() != 2)
                        throw new ClassFormatError("invalid attr length");
                    source_cpx = in.readUnsignedShort();
                    AttrData attr = new AttrData(this);
                    attr.read(name_cpx);
                    attrs[k] = attr;

                } else if (getTag(name_cpx) == CONSTANT_UTF8 && getString(name_cpx).equals("InnerClasses")) {
                    int length = in.readInt();
                    int num = in.readUnsignedShort();
                    if (2 + num * 8 != length)
                        throw new ClassFormatError("invalid attr length");
                    innerClasses = new InnerClassData[num];
                    for (int j = 0; j < num; j++) {
                        InnerClassData innerClass = new InnerClassData(this);
                        innerClass.read(in);
                        innerClasses[j] = innerClass;
                    }
                    AttrData attr = new AttrData(this);
                    attr.read(name_cpx);
                    attrs[k] = attr;
                } else {
                    AttrData attr = new AttrData(this);
                    attr.read(name_cpx, in);
                    attrs[k] = attr;
                }
            }
            in.close();
        } // end ClassData.read()

        /**
         * Reads and stores constant pool info.
         */
        void readCP(DataInputStream in) throws IOException {
            cpool_count = in.readUnsignedShort();
            tags = new byte[cpool_count];
            cpool = new Object[cpool_count];
            for (int i = 1; i < cpool_count; i++) {
                byte tag = in.readByte();

                switch (tags[i] = tag) {
                case CONSTANT_UTF8:
                    String str = in.readUTF();
                    indexHashAscii.put(cpool[i] = str, new Integer(i));
                    break;
                case CONSTANT_INTEGER:
                    cpool[i] = new Integer(in.readInt());
                    break;
                case CONSTANT_FLOAT:
                    cpool[i] = new Float(in.readFloat());
                    break;
                case CONSTANT_LONG:
                    cpool[i++] = new Long(in.readLong());
                    break;
                case CONSTANT_DOUBLE:
                    cpool[i++] = new Double(in.readDouble());
                    break;
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                    cpool[i] = new CPX(in.readUnsignedShort());
                    break;

                case CONSTANT_FIELD:
                case CONSTANT_METHOD:
                case CONSTANT_INTERFACEMETHOD:
                case CONSTANT_NAMEANDTYPE:
                    cpool[i] = new CPX2(in.readUnsignedShort(), in.readUnsignedShort());
                    break;

                case 0:
                default:
                    throw new ClassFormatError("invalid constant type: " + (int) tags[i]);
                }
            }
        }

        /**
         * Reads and strores field info.
         */
        protected void readFields(DataInputStream in) throws IOException {
            int fields_count = in.readUnsignedShort();
            fields = new FieldData[fields_count];
            for (int k = 0; k < fields_count; k++) {
                FieldData field = new FieldData(this);
                field.read(in);
                fields[k] = field;
            }
        }

        /**
         * Reads and strores Method info.
         */
        protected void readMethods(DataInputStream in) throws IOException {
            int methods_count = in.readUnsignedShort();
            methods = new MethodData[methods_count];
            for (int k = 0; k < methods_count; k++) {
                MethodData method = new MethodData(this);
                method.read(in);
                methods[k] = method;
            }
        }

        /**
         * get a string
         */
        public String getString(int n) {
            return (n == 0) ? null : (String) cpool[n];
        }

        /**
         * get the type of constant given an index
         */
        public byte getTag(int n) {
            try {
                return tags[n];
            } catch (ArrayIndexOutOfBoundsException e) {
                return (byte) 100;
            }
        }

        static final String hexString = "0123456789ABCDEF";

        public static char hexTable[] = hexString.toCharArray();

        static String toHex(long val, int width) {
            StringBuffer s = new StringBuffer();
            for (int i = width - 1; i >= 0; i--)
                s.append(hexTable[((int) (val >> (4 * i))) & 0xF]);
            return "0x" + s.toString();
        }

        static String toHex(long val) {
            int width;
            for (width = 16; width > 0; width--) {
                if ((val >> (width - 1) * 4) != 0)
                    break;
            }
            return toHex(val, width);
        }

        static String toHex(int val) {
            int width;
            for (width = 8; width > 0; width--) {
                if ((val >> (width - 1) * 4) != 0)
                    break;
            }
            return toHex(val, width);
        }

        public void error(String msg) {
            System.err.println("ERROR:" + msg);
        }

        /**
         * Returns the name of this class.
         */
        public String getClassName() {
            String res = null;
            if (this_class == 0) {
                return res;
            }
            int tcpx;
            try {
                if (tags[this_class] != CONSTANT_CLASS) {
                    return res; // "<CP["+cpx+"] is not a Class> ";
                }
                tcpx = ((CPX) cpool[this_class]).cpx;
            } catch (ArrayIndexOutOfBoundsException e) {
                return res; // "#"+cpx+"// invalid constant pool index";
            } catch (Throwable e) {
                return res; // "#"+cpx+"// ERROR IN DISASSEMBLER";
            }

            try {
                return (String) (cpool[tcpx]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return res; // "class #"+scpx+"// invalid constant pool index";
            } catch (ClassCastException e) {
                return res; // "class #"+scpx+"// invalid constant pool reference";
            } catch (Throwable e) {
                return res; // "#"+cpx+"// ERROR IN DISASSEMBLER";
            }

        }

        /**
         * Returns the name of class at perticular index.
         */
        public String getClassName(int cpx) {
            String res = "#" + cpx;
            if (cpx == 0) {
                return res;
            }
            int scpx;
            try {
                if (tags[cpx] != CONSTANT_CLASS) {
                    return res; // "<CP["+cpx+"] is not a Class> ";
                }
                scpx = ((CPX) cpool[cpx]).cpx;
            } catch (ArrayIndexOutOfBoundsException e) {
                return res; // "#"+cpx+"// invalid constant pool index";
            } catch (Throwable e) {
                return res; // "#"+cpx+"// ERROR IN DISASSEMBLER";
            }
            res = "#" + scpx;
            try {
                return (String) (cpool[scpx]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return res; // "class #"+scpx+"// invalid constant pool index";
            } catch (ClassCastException e) {
                return res; // "class #"+scpx+"// invalid constant pool reference";
            } catch (Throwable e) {
                return res; // "#"+cpx+"// ERROR IN DISASSEMBLER";
            }
        }

        /**
         * Returns true if it is a class
         */
        public boolean isClass() {
            if ((access & ACC_INTERFACE) == 0)
                return true;
            return false;
        }

        /**
         * Returns true if it is a interface.
         */
        public boolean isInterface() {
            if ((access & ACC_INTERFACE) != 0)
                return true;
            return false;
        }

        /**
         * Returns true if this member is public, false otherwise.
         */
        public boolean isPublic() {
            return (access & ACC_PUBLIC) != 0;
        }

        /**
         * Returns the access of this class or interface.
         */
        public String[] getAccess() {
            Vector v = new Vector();
            if ((access & ACC_PUBLIC) != 0)
                v.addElement("public");
            if ((access & ACC_FINAL) != 0)
                v.addElement("final");
            if ((access & ACC_ABSTRACT) != 0)
                v.addElement("abstract");
            String[] accflags = new String[v.size()];
            v.copyInto(accflags);
            return accflags;
        }

        /**
         * Returns list of innerclasses.
         */
        public InnerClassData[] getInnerClasses() {
            return innerClasses;
        }

        /**
         * Returns list of attributes.
         */
        public AttrData[] getAttributes() {
            return attrs;
        }

        /**
         * Returns true if superbit is set.
         */
        public boolean isSuperSet() {
            if ((access & ACC_SUPER) != 0)
                return true;
            return false;
        }

        /**
         * Returns super class name.
         */
        public String getSuperClassName() {
            String res = null;
            if (super_class == 0) {
                return res;
            }
            int scpx;
            try {
                if (tags[super_class] != CONSTANT_CLASS) {
                    return res; // "<CP["+cpx+"] is not a Class> ";
                }
                scpx = ((CPX) cpool[super_class]).cpx;
            } catch (ArrayIndexOutOfBoundsException e) {
                return res; // "#"+cpx+"// invalid constant pool index";
            } catch (Throwable e) {
                return res; // "#"+cpx+"// ERROR IN DISASSEMBLER";
            }

            try {
                return (String) (cpool[scpx]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return res; // "class #"+scpx+"// invalid constant pool index";
            } catch (ClassCastException e) {
                return res; // "class #"+scpx+"// invalid constant pool reference";
            } catch (Throwable e) {
                return res; // "#"+cpx+"// ERROR IN DISASSEMBLER";
            }
        }

        /**
         * Returns list of super interfaces.
         */
        public String[] getSuperInterfaces() {
            String interfacenames[] = new String[interfaces.length];
            int interfacecpx = -1;
            for (int i = 0; i < interfaces.length; i++) {
                interfacecpx = ((CPX) cpool[interfaces[i]]).cpx;
                interfacenames[i] = (String) (cpool[interfacecpx]);
            }
            return interfacenames;
        }

        /**
         * Returns string at prticular constant pool index.
         */
        public String getStringValue(int cpoolx) {
            try {
                return ((String) cpool[cpoolx]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return "//invalid constant pool index:" + cpoolx;
            } catch (ClassCastException e) {
                return "//invalid constant pool ref:" + cpoolx;
            }
        }

        /**
         * Returns list of field info.
         */
        public FieldData[] getFields() {
            return fields;
        }

        /**
         * Returns list of method info.
         */
        public MethodData[] getMethods() {
            return methods;
        }

        /**
         * Returns constant pool entry at that index.
         */
        public CPX2 getCpoolEntry(int cpx) {
            return ((CPX2) (cpool[cpx]));
        }

        public Object getCpoolEntryobj(int cpx) {
            return (cpool[cpx]);
        }

        /**
         * Returns index of this class.
         */
        public int getthis_cpx() {
            return this_class;
        }

        public String TagString(int tag) {
            String res = Tables.tagName(tag);
            if (res == null)
                return "BOGUS_TAG:" + tag;
            return res;
        }

        /**
         * Returns string at that index.
         */
        public String StringValue(int cpx) {
            if (cpx == 0)
                return "#0";
            int tag;
            Object x;
            String suffix = "";
            try {
                tag = tags[cpx];
                x = cpool[cpx];
            } catch (IndexOutOfBoundsException e) {
                return "<Incorrect CP index:" + cpx + ">";
            }

            if (x == null)
                return "<NULL>";
            switch (tag) {
            case CONSTANT_UTF8: {
                StringBuffer sb = new StringBuffer();
                String s = (String) x;
                for (int k = 0; k < s.length(); k++) {
                    char c = s.charAt(k);
                    switch (c) {
                    case '\t':
                        sb.append('\\').append('t');
                        break;
                    case '\n':
                        sb.append('\\').append('n');
                        break;
                    case '\r':
                        sb.append('\\').append('r');
                        break;
                    case '\"':
                        sb.append('\\').append('\"');
                        break;
                    default:
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
            case CONSTANT_DOUBLE: {
                Double d = (Double) x;
                String sd = d.toString();
                return sd + "d";
            }
            case CONSTANT_FLOAT: {
                Float f = (Float) x;
                String sf = (f).toString();
                return sf + "f";
            }
            case CONSTANT_LONG: {
                Long ln = (Long) x;
                return ln.toString() + 'l';
            }
            case CONSTANT_INTEGER: {
                Integer in = (Integer) x;
                return in.toString();
            }
            case CONSTANT_CLASS:
                return javaName(getClassName(cpx));
            case CONSTANT_STRING:
                return StringValue(((CPX) x).cpx);
            case CONSTANT_FIELD:
            case CONSTANT_METHOD:
            case CONSTANT_INTERFACEMETHOD:
                // return
                // getShortClassName(((CPX2)x).cpx1)+"."+StringValue(((CPX2)x).cpx2);
                return javaName(getClassName(((CPX2) x).cpx1)) + "." + StringValue(((CPX2) x).cpx2);

            case CONSTANT_NAMEANDTYPE:
                return getName(((CPX2) x).cpx1) + ":" + StringValue(((CPX2) x).cpx2);
            default:
                return "UnknownTag"; // TBD
            }
        }

        /**
         * Returns resolved java type name.
         */
        public String javaName(String name) {
            if (name == null)
                return "null";
            int len = name.length();
            if (len == 0)
                return "\"\"";
            int cc = '/';
            fullname: { // xxx/yyy/zzz
                int cp;
                for (int k = 0; k < len; k += Character.charCount(cp)) {
                    cp = name.codePointAt(k);
                    if (cc == '/') {
                        if (!Character.isJavaIdentifierStart(cp))
                            break fullname;
                    } else if (cp != '/') {
                        if (!Character.isJavaIdentifierPart(cp))
                            break fullname;
                    }
                    cc = cp;
                }
                return name;
            }
            return "\"" + name + "\"";
        }

        public String getName(int cpx) {
            String res;
            try {
                return javaName((String) cpool[cpx]); // .replace('/','.');
            } catch (ArrayIndexOutOfBoundsException e) {
                return "<invalid constant pool index:" + cpx + ">";
            } catch (ClassCastException e) {
                return "<invalid constant pool ref:" + cpx + ">";
            }
        }

        /**
         * Returns unqualified class name.
         */
        public String getShortClassName(int cpx) {
            String classname = javaName(getClassName(cpx));
            pkgPrefixLen = classname.lastIndexOf("/") + 1;
            if (pkgPrefixLen != 0) {
                pkgPrefix = classname.substring(0, pkgPrefixLen);
                if (classname.startsWith(pkgPrefix)) {
                    return classname.substring(pkgPrefixLen);
                }
            }
            return classname;
        }

        /**
         * Returns source file name.
         */
        public String getSourceName() {
            return getName(source_cpx);
        }

        /**
         * Returns package name.
         */
        public String getPkgName() {
            String classname = getClassName(this_class);
            pkgPrefixLen = classname.lastIndexOf("/") + 1;
            if (pkgPrefixLen != 0) {
                pkgPrefix = classname.substring(0, pkgPrefixLen);
                return ("package  " + pkgPrefix.substring(0, pkgPrefixLen - 1) + ";\n");
            } else
                return null;
        }

        /**
         * Returns total constant pool entry count.
         */
        public int getCpoolCount() {
            return cpool_count;
        }

        public String StringTag(int cpx) {
            byte tag = 0;
            String str = null;
            try {
                if (cpx == 0)
                    throw new IndexOutOfBoundsException();
                tag = tags[cpx];
                return TagString(tag);
            } catch (IndexOutOfBoundsException e) {
                str = "Incorrect CP index:" + cpx;
            }
            return str;
        }

        /**
         * Returns minor version of class file.
         */
        public int getMinor_version() {
            return minor_version;
        }

        /**
         * Returns major version of class file.
         */
        public int getMajor_version() {
            return major_version;
        }
    }

    // From: http://hg.openjdk.java.net/jdk7/jaxp/langtools/
    /**
     * Reads and stores attribute information.
     * 
     * @author Sucheta Dambalkar (Adopted code from jdis)
     */
    public static class AttrData {
        ClassData cls;

        int name_cpx;

        int datalen;

        byte data[];

        public AttrData(ClassData cls) {
            this.cls = cls;
        }

        /**
         * Reads unknown attribute.
         */
        public void read(int name_cpx, DataInputStream in) throws IOException {

            this.name_cpx = name_cpx;
            datalen = in.readInt();
            data = new byte[datalen];
            in.readFully(data);
        }

        /**
         * Reads just the name of known attribute.
         */
        public void read(int name_cpx) {
            this.name_cpx = name_cpx;
        }

        /**
         * Returns attribute name.
         */
        public String getAttrName() {
            return cls.getString(name_cpx);
        }

        /**
         * Returns attribute data.
         */
        public byte[] getData() {
            return data;
        }
    } // End of the Class //

    /**
     * This interface defines constant that are used throughout the compiler. It
     * inherits from RuntimeConstants, which is an autogenerated class that
     * contains contstants defined in the interpreter.
     */

    public interface Constants extends RuntimeConstants {

        /**
         * End of input
         */
        public static final int EOF = -1;

        /*
         * Flags
         */
        public static final int F_VERBOSE = 1 << 0;

        public static final int F_DUMP = 1 << 1;

        public static final int F_WARNINGS = 1 << 2;

        public static final int F_DEBUG = 1 << 3;

        public static final int F_OPTIMIZE = 1 << 4;

        public static final int F_DEPENDENCIES = 1 << 5;

        /*
         * Type codes
         */
        public static final int TC_BOOLEAN = 0;

        public static final int TC_BYTE = 1;

        public static final int TC_CHAR = 2;

        public static final int TC_SHORT = 3;

        public static final int TC_INT = 4;

        public static final int TC_LONG = 5;

        public static final int TC_FLOAT = 6;

        public static final int TC_DOUBLE = 7;

        public static final int TC_NULL = 8;

        public static final int TC_ARRAY = 9;

        public static final int TC_CLASS = 10;

        public static final int TC_VOID = 11;

        public static final int TC_METHOD = 12;

        public static final int TC_ERROR = 13;

        /*
         * Type Masks
         */
        public static final int TM_NULL = 1 << TC_NULL;

        public static final int TM_VOID = 1 << TC_VOID;

        public static final int TM_BOOLEAN = 1 << TC_BOOLEAN;

        public static final int TM_BYTE = 1 << TC_BYTE;

        public static final int TM_CHAR = 1 << TC_CHAR;

        public static final int TM_SHORT = 1 << TC_SHORT;

        public static final int TM_INT = 1 << TC_INT;

        public static final int TM_LONG = 1 << TC_LONG;

        public static final int TM_FLOAT = 1 << TC_FLOAT;

        public static final int TM_DOUBLE = 1 << TC_DOUBLE;

        public static final int TM_ARRAY = 1 << TC_ARRAY;

        public static final int TM_CLASS = 1 << TC_CLASS;

        public static final int TM_METHOD = 1 << TC_METHOD;

        public static final int TM_ERROR = 1 << TC_ERROR;

        public static final int TM_INT32 = TM_BYTE | TM_SHORT | TM_CHAR | TM_INT;

        public static final int TM_NUM32 = TM_INT32 | TM_FLOAT;

        public static final int TM_NUM64 = TM_LONG | TM_DOUBLE;

        public static final int TM_INTEGER = TM_INT32 | TM_LONG;

        public static final int TM_REAL = TM_FLOAT | TM_DOUBLE;

        public static final int TM_NUMBER = TM_INTEGER | TM_REAL;

        public static final int TM_REFERENCE = TM_ARRAY | TM_CLASS | TM_NULL;

        /*
         * Class status
         */
        public static final int CS_UNDEFINED = 0;

        public static final int CS_UNDECIDED = 1;

        public static final int CS_BINARY = 2;

        public static final int CS_SOURCE = 3;

        public static final int CS_PARSED = 4;

        public static final int CS_COMPILED = 5;

        public static final int CS_NOTFOUND = 6;

        /*
         * Attributes
         */
        public static final int ATT_ALL = -1;

        public static final int ATT_CODE = 1;

        /*
         * Number of bits used in file offsets
         */
        public static final int OFFSETBITS = 19;

        public static final int MAXFILESIZE = (1 << OFFSETBITS) - 1;

        public static final int MAXLINENUMBER = (1 << (32 - OFFSETBITS)) - 1;

        /*
         * Operators
         */
        public final int COMMA = 0;

        public final int ASSIGN = 1;

        public final int ASGMUL = 2;

        public final int ASGDIV = 3;

        public final int ASGREM = 4;

        public final int ASGADD = 5;

        public final int ASGSUB = 6;

        public final int ASGLSHIFT = 7;

        public final int ASGRSHIFT = 8;

        public final int ASGURSHIFT = 9;

        public final int ASGBITAND = 10;

        public final int ASGBITOR = 11;

        public final int ASGBITXOR = 12;

        public final int COND = 13;

        public final int OR = 14;

        public final int AND = 15;

        public final int BITOR = 16;

        public final int BITXOR = 17;

        public final int BITAND = 18;

        public final int NE = 19;

        public final int EQ = 20;

        public final int GE = 21;

        public final int GT = 22;

        public final int LE = 23;

        public final int LT = 24;

        public final int INSTANCEOF = 25;

        public final int LSHIFT = 26;

        public final int RSHIFT = 27;

        public final int URSHIFT = 28;

        public final int ADD = 29;

        public final int SUB = 30;

        public final int DIV = 31;

        public final int REM = 32;

        public final int MUL = 33;

        public final int CAST = 34; // (x)y

        public final int POS = 35; // +x

        public final int NEG = 36; // -x

        public final int NOT = 37;

        public final int BITNOT = 38;

        public final int PREINC = 39; // ++x

        public final int PREDEC = 40; // --x

        public final int NEWARRAY = 41;

        public final int NEWINSTANCE = 42;

        public final int NEWFROMNAME = 43;

        public final int POSTINC = 44; // x++

        public final int POSTDEC = 45; // x--

        public final int FIELD = 46;

        public final int METHOD = 47; // x(y)

        public final int ARRAYACCESS = 48; // x[y]

        public final int NEW = 49;

        public final int INC = 50;

        public final int DEC = 51;

        public final int CONVERT = 55; // implicit conversion

        public final int EXPR = 56; // (x)

        public final int ARRAY = 57; // {x, y, ...}

        public final int GOTO = 58;

        /*
         * Value tokens
         */
        public final int IDENT = 60;

        public final int BOOLEANVAL = 61;

        public final int BYTEVAL = 62;

        public final int CHARVAL = 63;

        public final int SHORTVAL = 64;

        public final int INTVAL = 65;

        public final int LONGVAL = 66;

        public final int FLOATVAL = 67;

        public final int DOUBLEVAL = 68;

        public final int STRINGVAL = 69;

        /*
         * Type keywords
         */
        public final int BYTE = 70;

        public final int CHAR = 71;

        public final int SHORT = 72;

        public final int INT = 73;

        public final int LONG = 74;

        public final int FLOAT = 75;

        public final int DOUBLE = 76;

        public final int VOID = 77;

        public final int BOOLEAN = 78;

        /*
         * Expression keywords
         */
        public final int TRUE = 80;

        public final int FALSE = 81;

        public final int THIS = 82;

        public final int SUPER = 83;

        public final int NULL = 84;

        /*
         * Statement keywords
         */
        public final int IF = 90;

        public final int ELSE = 91;

        public final int FOR = 92;

        public final int WHILE = 93;

        public final int DO = 94;

        public final int SWITCH = 95;

        public final int CASE = 96;

        public final int DEFAULT = 97;

        public final int BREAK = 98;

        public final int CONTINUE = 99;

        public final int RETURN = 100;

        public final int TRY = 101;

        public final int CATCH = 102;

        public final int FINALLY = 103;

        public final int THROW = 104;

        public final int STAT = 105;

        public final int EXPRESSION = 106;

        public final int DECLARATION = 107;

        public final int VARDECLARATION = 108;

        /*
         * Declaration keywords
         */
        public final int IMPORT = 110;

        public final int CLASS = 111;

        public final int EXTENDS = 112;

        public final int IMPLEMENTS = 113;

        public final int INTERFACE = 114;

        public final int PACKAGE = 115;

        /*
         * Modifier keywords
         */
        public final int PRIVATE = 120;

        public final int PUBLIC = 121;

        public final int PROTECTED = 122;

        public final int CONST = 123;

        public final int STATIC = 124;

        public final int TRANSIENT = 125;

        public final int SYNCHRONIZED = 126;

        public final int NATIVE = 127;

        public final int FINAL = 128;

        public final int VOLATILE = 129;

        public final int ABSTRACT = 130;

        public final int STRICT = 165;

        /*
         * Punctuation
         */
        public final int SEMICOLON = 135;

        public final int COLON = 136;

        public final int QUESTIONMARK = 137;

        public final int LBRACE = 138;

        public final int RBRACE = 139;

        public final int LPAREN = 140;

        public final int RPAREN = 141;

        public final int LSQBRACKET = 142;

        public final int RSQBRACKET = 143;

        public final int THROWS = 144;

        /*
         * Special tokens
         */
        public final int ERROR = 145; // an error

        public final int COMMENT = 146; // not used anymore.

        public final int TYPE = 147;

        public final int LENGTH = 148;

        public final int INLINERETURN = 149;

        public final int INLINEMETHOD = 150;

        public final int INLINENEWINSTANCE = 151;

        /*
         * Added for jasm
         */
        public final int METHODREF = 152;

        public final int FIELDREF = 153;

        public final int STACK = 154;

        public final int LOCAL = 155;

        public final int CPINDEX = 156;

        public final int CPNAME = 157;

        public final int SIGN = 158;

        public final int BITS = 159;

        public final int INF = 160;

        public final int NAN = 161;

        public final int INNERCLASS = 162;

        public final int OF = 163;

        public final int SYNTHETIC = 164;

        // last used=165;

        /*
         * Operator precedence
         */
        public static final int opPrecedence[] = { 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 13, 14, 15,
                16, 17, 18, 18, 19, 19, 19, 19, 19, 20, 20, 20, 21, 21, 22, 22, 22, 23, 24, 24, 24, 24, 24, 24, 25, 25,
                26, 26, 26, 26, 26, 26 };

        /*
         * Operator names
         */
        public static final String opNames[] = { ",", "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "<<<=", "&=",
                "|=", "^=", "?:", "||", "&&", "|", "^", "&", "!=", "==", ">=", ">", "<=", "<", "instanceof", "<<",
                ">>", "<<<", "+", "-", "/", "%", "*", "cast", "+", "-", "!", "~", "++", "--", "new", "new", "new",
                "++", "--", "field", "method", "[]", "new", "++", "--", null, null, null,

                "convert", "expr", "array", "goto", null,

                "Identifier", "Boolean", "Byte", "Char", "Short", "Integer", "Long", "Float", "Double", "String",

                "byte", "char", "short", "int", "long", "float", "double", "void", "boolean", null,

                "true", "false", "this", "super", "null", null, null, null, null, null,

                "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return", "try",
                "catch", "finally", "throw", "stat", "expression", "declaration", "declaration", null,

                "import", "class", "extends", "implements", "interface", "package", null, null, null, null,

                "private", "public", "protected", "const", "static", "transient", "synchronized", "native", "final",
                "volatile", "abstract", null, null, null, null,

                ";", ":", "?", "{", "}", "(", ")", "[", "]", "throws", "error", "comment", "type", "length",
                "inline-return", "inline-method", "inline-new", "method", "field", "stack", "locals", "CPINDEX",
                "CPName", "SIGN", "bits", "INF", "NaN", "InnerClass", "of", "synthetic" };

    }

    public interface IExtractClassData {

        void setInputClassName(final String in);

        String getInputClassName();

        void appMain(String argv[]);

        String convertClassnameFormat(final String inclass);

        void entry(String argv[]);

        /**
         * Process the arguments and perform the desired action
         */
        void perform();

        void error(String msg);

        void displayResults(final InputStream classinMem);

        String getResult();

        public PrintWriter getOut();

        public void setOut(PrintWriter out);

        public void setVerbose(boolean v);

    } // End of the Class //

    public static class ExtractClassData implements IExtractClassData {

        public static final String VERSION = "9999";

        private PrintWriter out = new PrintWriter(new ByteArrayOutputStream());

        private final JavapEnvironment env = new JavapEnvironment();

        private static boolean errorOccurred = false;

        private String inputClassName = "java.lang.Object";

        private String resultString = "";

        private boolean verbose = false;

        /////////////////////////////////////////////////////////////////

        public void appMain(String[] argv) {
            entry(argv);
        }

        public String convertClassnameFormat(String inclass) {

            if ((inclass == null) || (inclass.length() == 0)) {
                // Return the default java lang object
                return "java/lang/Object.class";
            }

            boolean endClass = false;
            // Replace . with /
            // Ensure that the string ends in .class
            final Pattern chkEndClassPattr = Pattern.compile(".*?\\.class$");
            final Matcher mEndClassPattr = chkEndClassPattr.matcher(inclass);
            if (mEndClassPattr.find()) {
                // Ends in .class //
                endClass = true;
            }
            final String token1 = endClass ? inclass.replace(".class", "___CLASS___") : inclass;

            final String repl1 = token1.replaceAll("\\.", "/");
            final String repl2 = repl1.replaceAll("\\\\", "/");
            return endClass ? repl2.replace("___CLASS___", ".class") : (repl2 + ".class");
        }

        public void displayResults(InputStream classinMem) {

            final InputStream classin = classinMem;
            try {
                // actual do display
                JavapPrinter printer = new JavapPrinter(classin, out, env);
                printer.print();

            } catch (IllegalArgumentException exc) {
                error(exc.getMessage());
            }

        }

        public void entry(String[] argv) {

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            this.out = new PrintWriter(baos);
            try {

                this.perform();

            } finally {
                out.close();
            } // End of the try - Catch Finally //

            final String res = new String(baos.toByteArray());
            this.resultString = res;

        }

        public void error(String msg) {
            errorOccurred = true;

        }

        public String getInputClassName() {
            return inputClassName;
        }

        public String getResult() {
            return this.resultString;
        }

        public void perform() {

            final String classNameParm =  convertClassnameFormat(this.inputClassName);
            if (this.verbose) {
                System.out.print("Verbose mode enabled");
                this.env.setShowVerbose(true);
                this.env.setShowDisassembled(true);
                this.env.setShowLineAndLocal(true);
            } else {
                System.out.print("Verbose mode disabled");
            }
            System.out.print("At perform - current thread: " + Thread.currentThread());
            final InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(classNameParm);

            if (input == null) {
                throw new IllegalStateException("Invalid class resource, classname=" + classNameParm);
            }
            displayResults(input);
        }

        public void setInputClassName(String in) {
            this.inputClassName = in;
        }

        /**
         * @return the out
         */
        public PrintWriter getOut() {
            return out;
        }

        /**
         * @param out the out to set
         */
        public void setOut(PrintWriter out) {
            this.out = out;
        }

        public void setVerbose(boolean v) {
            this.verbose= v;
            System.out.print("Setting verbose (" + v + ")");
        }

    } // End of the Class //
    
} // End of the Class ///
