package net.elehack.gradle.science;

import java.io.File;
import java.util.List;

/**
 * Interface for specifying Pandoc executions.
 */
public interface PandocSpec {
    String getSourceFormat();
    void setSourceFormat(String fmt);

    String getOutputFormat();
    void setOutputFormat(String fmt);

    List<String> getSrcFlags();
    void setSrcFlags(List<String> flags);

    List<String> getOutFlags();
    void setOutFlags(List<String> flags);

    boolean getStandalone();
    void setStandalone(boolean flag);

    List<Object> getExtraArgs();
    void setExtraArgs(List<Object> args);

    Object getBibliography();
    void setBibliography(Object bib);

    Object getCitationStyle();
    void setCitationStyle(Object csl);

    List<String> getCssStylesheets();
    void setCssStylesheets(List<String> styles);
    void css(String... styles);

    String getFilter();
    void setFilter(String filt);

    Object getTemplate();
    void setTemplate(Object tmpl);

    void sourceFormat(String fmt);

    void outputFormat(String fmt);

    void enableExtensions(String... exts);
    void disableExtensions(String... exts);

    void enableOutputExtensions(String exts);
    void disableOutputExtensions(String... exts);

    void standalone(boolean flag);

    void template(Object tmpl);

    /**
     * Add extra arguments to the Pandoc invocation.
     * @param args The extra arguments to add.
     */
    void args(Object... args);

    void bibliography(Object bib);

    void citationStyle(Object csl);

    void filter(String f);

    String getSourceFormatString();

    String getOutputFormatString();

    String getDefaultOutputExtension();

    PandocSpecImpl copySpec();

    List<String> getFullArgs();

    void execute(File input, File output);
}
