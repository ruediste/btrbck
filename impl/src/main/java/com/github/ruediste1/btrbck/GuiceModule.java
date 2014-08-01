package com.github.ruediste1.btrbck;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.RestoreVersionHistoryEntry;
import com.github.ruediste1.btrbck.dom.Retention;
import com.github.ruediste1.btrbck.dom.SnapshotVersionHistoryEntry;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.TimeUnit;
import com.github.ruediste1.btrbck.dom.VersionHistory;
import com.github.ruediste1.btrbck.dom.VersionHistoryEntry;
import com.google.inject.AbstractModule;

public class GuiceModule extends AbstractModule {

	@Override
	protected void configure() {
		try {
			bind(JAXBContext.class).toInstance(
					JAXBContext.newInstance(Stream.class, VersionHistory.class,
							VersionHistoryEntry.class,
							SnapshotVersionHistoryEntry.class,
							RestoreVersionHistoryEntry.class,
							BackupStreamRepository.class,
							ApplicationStreamRepository.class, Retention.class,
							TimeUnit.class));
		} catch (JAXBException e) {
			throw new RuntimeException("Error while creating JAXB context", e);
		}
	}
}
