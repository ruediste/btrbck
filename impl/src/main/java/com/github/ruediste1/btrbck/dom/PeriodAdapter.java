package com.github.ruediste1.btrbck.dom;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import com.google.common.base.Strings;

/**
 * Adapter to store joda time {@link Period}s in XML as ISO 8601 strings.
 */
public class PeriodAdapter extends XmlAdapter<String, Period> {

    PeriodFormatter formatter = ISOPeriodFormat.standard();

    @Override
    public Period unmarshal(String v) throws Exception {
        if (Strings.isNullOrEmpty(v)) {
            return null;
        }
        return formatter.parsePeriod(v);
    }

    @Override
    public String marshal(Period v) throws Exception {
        if (v == null) {
            return "";
        }
        return formatter.print(v);
    }

}
