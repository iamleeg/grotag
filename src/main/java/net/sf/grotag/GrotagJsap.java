package net.sf.grotag;

import java.io.PrintStream;

import net.sf.grotag.common.Version;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.SyntaxException;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

/**
 * Parser for Grotag's command line options.
 * 
 * @author Thomas Aglassinger
 */
public class GrotagJsap extends JSAP {
    public static final String ARG_DOCBOOK = "docbook";
    public static final String ARG_FILE = "file";
    public static final String ARG_HELP = "help";
    public static final String ARG_HTML = "html";
    public static final String ARG_LICENSE = "license";
    public static final String ARG_PRETTY = "pretty";
    public static final String ARG_VALIDATE = "validate";
    public static final String ARG_VERSION = "version";
    public static final String ARG_XHTML = "xhtml";

    /**
     * Exactly one of these options must be specified.
     */
    private static final String[] ANY_REQUIRED = new String[] { ARG_DOCBOOK, ARG_HELP, ARG_HTML, ARG_LICENSE,
            ARG_PRETTY, ARG_VERSION, ARG_VALIDATE, ARG_XHTML };

    private static final String[] HEADING = new String[] {
            "Grotag - View, convert, validate and pretty print Amigaguide documents.",
            Version.COPYRIGHT + " <http://grotag.sourceforge.net/>" };

    private static final String[] LICENSE = new String[] { "",
            "This program is free software: you can redistribute it and/or modify",
            "it under the terms of the GNU General Public License as published by",
            "the Free Software Foundation, either version 3 of the License, or", "(at your option) any later version.",
            "", "This program is distributed in the hope that it will be useful,",
            "but WITHOUT ANY WARRANTY; without even the implied warranty of",
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the",
            "GNU General Public License for more details.", "",
            "You should have received a copy of the GNU General Public License",
            "along with this program.  If not, see <http://www.gnu.org/licenses/>." };

    /**
     * Create JSAP for Grotag command line options.
     */
    public GrotagJsap() throws JSAPException {
        UnflaggedOption fileOption = new UnflaggedOption(ARG_FILE);
        fileOption.setStringParser(FileStringParser.getParser());
        fileOption.setGreedy(true);
        fileOption.setHelp("the file to be viewed or the files to be processed by --" + ARG_DOCBOOK + ", --" + ARG_HTML
                + ", --" + ARG_PRETTY + " or --" + ARG_VALIDATE);
        registerParameter(fileOption);

        Switch docBookSwitch = new Switch(ARG_DOCBOOK);
        docBookSwitch.setShortFlag('d');
        docBookSwitch.setLongFlag(ARG_DOCBOOK);
        docBookSwitch.setHelp("convert to DocBook XML using the first specified file as Amigaguide input "
                + "and the second file as DocBook XML output; if only one file is specified, the output "
                + "is written to a file with the suffix changed to \".xml\"");
        registerParameter(docBookSwitch);

        Switch htmlSwitch = new Switch(ARG_HTML);
        htmlSwitch.setShortFlag('w');
        htmlSwitch.setLongFlag(ARG_HTML);
        htmlSwitch.setHelp("convert to HTML using the first specified file as Amigaguide input "
                + "and the second as output folder; if only one file is specified, the output "
                + "is written to the current directory");
        registerParameter(htmlSwitch);

        Switch prettySwitch = new Switch(ARG_PRETTY);
        prettySwitch.setShortFlag('v');
        prettySwitch.setLongFlag(ARG_PRETTY);
        prettySwitch.setHelp("cleanup specified Amigaguide file, overwritting the original");
        registerParameter(prettySwitch);

        Switch validateSwitch = new Switch(ARG_VALIDATE);
        validateSwitch.setShortFlag('p');
        validateSwitch.setLongFlag(ARG_VALIDATE);
        validateSwitch.setHelp("validate specified Amigaguide files");
        registerParameter(validateSwitch);

        Switch helpSwitch = new Switch(ARG_HELP);
        helpSwitch.setShortFlag('h');
        helpSwitch.setLongFlag(ARG_HELP);
        helpSwitch.setHelp("show usage instructions");
        registerParameter(helpSwitch);

        Switch licenseSwitch = new Switch(ARG_LICENSE);
        licenseSwitch.setShortFlag(NO_SHORTFLAG);
        licenseSwitch.setLongFlag(ARG_LICENSE);
        licenseSwitch.setHelp("show license");
        registerParameter(licenseSwitch);

        Switch versionSwitch = new Switch(ARG_VERSION);
        versionSwitch.setShortFlag(NO_SHORTFLAG);
        versionSwitch.setLongFlag(ARG_VERSION);
        versionSwitch.setHelp("show version information");
        registerParameter(versionSwitch);

        Switch xhtmlSwitch = new Switch(ARG_XHTML);
        xhtmlSwitch.setShortFlag('x');
        xhtmlSwitch.setLongFlag(ARG_XHTML);
        xhtmlSwitch.setHelp("convert to XHTML using the first specified file as Amigaguide input "
                + "and the second as output folder; if only one file is specified, the output "
                + "is written to the current directory");
        registerParameter(xhtmlSwitch);
    }

    @Override
    public JSAPResult parse(String argument) {
        JSAPResult result = super.parse(argument);

        parseAnyRequired(result);
        return result;
    }

    @Override
    public JSAPResult parse(String[] arguments) {
        JSAPResult result = super.parse(arguments);

        parseAnyRequired(result);
        return result;
    }

    /**
     * Print help about command line options.
     */
    public void printHelp(PrintStream stream) {
        assert stream != null;
        for (String line : HEADING) {
            stream.println(line);
        }
        stream.println();
        stream.println(getHelp());
    }

    /**
     * Print summary of license.
     */
    public void printLicense(PrintStream stream) {
        assert stream != null;
        for (String line : LICENSE) {
            stream.println(line);
        }
    }

    /**
     * Print version number.
     */
    public void printVersion(PrintStream stream) {
        stream.println("Grotag " + Version.VERSION_TAG);
    }

    private void parseAnyRequired(JSAPResult options) {
        assert options != null;
        String commandOption = null;

        for (int i = 0; i < ANY_REQUIRED.length; i += 1) {
            String nextOption = ANY_REQUIRED[i];
            Object nextValue = options.getObject(nextOption);
            boolean hasValue = (nextValue != null);

            if (hasValue) {
                if (nextValue instanceof Boolean) {
                    hasValue = ((Boolean) nextValue).booleanValue();
                }
            }
            if (hasValue) {
                if (commandOption == null) {
                    commandOption = nextOption;
                } else {
                    SyntaxException error = new SyntaxException("option \"" + nextOption
                            + "\" must not be used together with \"" + commandOption + "\"");

                    options.addException(nextOption, error);
                }
            }
        }
    }
}
