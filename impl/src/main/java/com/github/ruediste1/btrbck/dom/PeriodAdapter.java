package com.github.ruediste1.btrbck.dom;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

public class PeriodAdapter extends XmlAdapter<String, Period> {

	PeriodFormatter formatter = ISOPeriodFormat.standard();

	@Override
	public Period unmarshal(String v) throws Exception {
		return formatter.parsePeriod(v);
	}

	@Override
	public String marshal(Period v) throws Exception {
		return formatter.print(v);
	}

}
