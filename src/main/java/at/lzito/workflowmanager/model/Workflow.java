package at.lzito.workflowmanager.model;

import java.util.List;

public class Workflow {

    private String name;
    private String icon;
    private String hotkey;
    private List<AppEntry> open;
    private List<String> close;
    private boolean closeOthers;

    // --- nested model ---

    public static class AppEntry {
        private String name;
        private String path;
        private String url;
        private List<String> args;
        private int delayMs;

        public String getName()    { return name; }
        public String getPath()    { return path; }
        public String getUrl()     { return url; }
        public List<String> getArgs() { return args; }
        public int getDelayMs()    { return delayMs; }

        public void setName(String name)       { this.name = name; }
        public void setPath(String path)       { this.path = path; }
        public void setUrl(String url)         { this.url = url; }
        public void setArgs(List<String> args) { this.args = args; }
        public void setDelayMs(int delayMs)    { this.delayMs = delayMs; }
    }

    // --- getters & setters ---

    public String getName()              { return name; }
    public String getIcon()              { return icon; }
    public String getHotkey()            { return hotkey; }
    public List<AppEntry> getOpen()      { return open; }
    public List<String> getClose()       { return close; }
    public boolean isCloseOthers()       { return closeOthers; }

    public void setName(String name)             { this.name = name; }
    public void setIcon(String icon)             { this.icon = icon; }
    public void setHotkey(String hotkey)         { this.hotkey = hotkey; }
    public void setOpen(List<AppEntry> open)     { this.open = open; }
    public void setClose(List<String> close)     { this.close = close; }
    public void setCloseOthers(boolean v)        { this.closeOthers = v; }
}
