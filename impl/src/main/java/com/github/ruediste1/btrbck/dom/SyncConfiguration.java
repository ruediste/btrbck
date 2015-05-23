package com.github.ruediste1.btrbck.dom;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Splitter;

@XmlRootElement
public class SyncConfiguration {

    @XmlAttribute
    public SyncDirection direction;

    @XmlAttribute
    public String sshTarget;

    @XmlAttribute
    public String remoteRepoLocation;

    @XmlAttribute
    public String remoteStreamName;

    @XmlAttribute
    public boolean createRemoteIfNecessary = true;

    /**
     * Defines which streams to push or pull. The string is a comma separated
     * list. Each element is the name of a stream which may contain * as
     * wildcard. If an element starts with a - any matching stream will be
     * excluded from the set of synced streams. For each local stream name, the
     * list is traversed from left to right. The first match decides if the
     * stream is in the set of synced streams or not. If no pattern matches, the
     * stream is not included in the set of synced streams.
     */
    @XmlAttribute
    public String streamPatterns;

    public boolean isSynced(String streamName) {
        for (String pattern : Splitter.on(',').trimResults().omitEmptyStrings()
                .split(streamPatterns)) {
            boolean inclusion = !pattern.startsWith("-");
            String regex = "-?";
            while (pattern.indexOf('*') != -1) {
                int idx = pattern.indexOf('*');
                regex += Pattern.quote(pattern.substring(0, idx));
                regex += ".*";
                pattern = pattern.substring(idx + 1);
            }
            regex += Pattern.quote(pattern);
            if (streamName.matches(regex)) {
                return inclusion;
            }
        }
        return false;
    }
}
