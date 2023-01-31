package com.lethe.disconf.scope.event;

import org.springframework.context.ApplicationEvent;

public class RefreshEvent extends ApplicationEvent {

	private static final long serialVersionUID = 4502435651176042413L;

	private final String fileName;

	private final String event;

	public RefreshEvent(Object source, String fileName, String event) {
		super(source);
		this.fileName = fileName;
		this.event = event;
	}


	public String getFileName() {
		return fileName;
	}

	public String getEvent() {
		return this.event;
	}

}
